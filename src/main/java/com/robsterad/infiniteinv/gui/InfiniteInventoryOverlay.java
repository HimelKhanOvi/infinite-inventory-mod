package com.robsterad.infiniteinv.gui;

import com.robsterad.infiniteinv.config.InfiniteInvConfig;
import com.robsterad.infiniteinv.network.ExtractItemPayload;
import com.robsterad.infiniteinv.network.SyncInventoryPayload;
import com.robsterad.infiniteinv.network.UpdateUiPrefsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfiniteInventoryOverlay {

    private static EditBox searchBox;
    public static List<SyncInventoryPayload.NetworkItemData> cachedItems = new ArrayList<>();

    public static boolean panelVisible = true;
    public static boolean panelActive  = false;

    private enum SortMode {
        RECENT("N",    "Newest"),
        DESCENDING("▼", "Count: High→Low"),
        ASCENDING("▲",  "Count: Low→High"),
        ALPHA_ASC("A↑", "Name: A→Z"),
        ALPHA_DESC("A↓", "Name: Z→A");

        final String shortLabel;
        final String hoverName;

        SortMode(String shortLabel, String hoverName) {
            this.shortLabel = shortLabel;
            this.hoverName  = hoverName;
        }
    }

    private static SortMode currentSort = SortMode.RECENT;
    private static int currentPage  = 0;
    private static int itemsPerPage = 1;
    private static boolean showTooltips = true;
    private static double lastMouseX = -1;
    private static double lastMouseY = -1;

    private static final int COLUMNS = 7;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_WIDTH = (COLUMNS * SLOT_SIZE) + 12; // 138 px

    public static void applyUiPrefs(boolean visible, String sortModeName, boolean tooltips) {
        panelVisible = visible;
        showTooltips = tooltips;
        try {
            currentSort = SortMode.valueOf(sortModeName);
        } catch (IllegalArgumentException e) {
            currentSort = SortMode.RECENT;
        }
        currentPage = 0;
    }

    private static void sendUiPrefsUpdate() {
        ClientPlayNetworking.send(new UpdateUiPrefsPayload(panelVisible, currentSort.name(), showTooltips));
    }

    public static boolean isSearchFocused() {
        return searchBox != null && searchBox.isFocused();
    }

    public static boolean onCharTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.isFocused()) {
            searchBox.charTyped(event);
            currentPage = 0;
            return true;
        }
        return false;
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            panelActive = false;
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            panelActive = true;

            searchBox = new EditBox(client.font, 0, 0, 60, 14, Component.literal(""));
            searchBox.setHint(Component.literal("Search..."));

            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                double mx = event.x();
                double my = event.y();
                int btn = event.button();

                int[] bounds = calculateBounds(containerScreen);
                int startX = bounds[0];
                int startY = bounds[1];
                int panelHeight = bounds[3];

                int bottomY = startY + panelHeight - 18;

                if (btn == 0 && mx >= startX + 118 && mx <= startX + 134 && my >= bottomY && my <= bottomY + 14) {
                    panelVisible = !panelVisible;
                    sendUiPrefsUpdate();
                    return false;
                }

                if (!panelVisible) return true;

                if (btn == 0) {
                    if (searchBox != null && mx >= startX + 4 && mx <= startX + 64 && my >= bottomY && my <= bottomY + 14) {
                        searchBox.setFocused(true);
                        return false;
                    }
                    if (searchBox != null) searchBox.setFocused(false);

                    if (mx >= startX + 68 && mx <= startX + 86 && my >= bottomY && my <= bottomY + 14) {
                        currentSort = SortMode.values()[(currentSort.ordinal() + 1) % SortMode.values().length];
                        currentPage = 0;
                        sendUiPrefsUpdate();
                        return false;
                    }

                    if (mx >= startX + 90 && mx <= startX + 114 && my >= bottomY && my <= bottomY + 14) {
                        showTooltips = !showTooltips;
                        sendUiPrefsUpdate();
                        return false;
                    }

                    if (mx >= startX + 4 && mx <= startX + 22 && my >= startY + 4 && my <= startY + 18) {
                        if (currentPage > 0) currentPage--;
                        return false;
                    }

                    if (mx >= startX + PANEL_WIDTH - 22 && mx <= startX + PANEL_WIDTH - 4 && my >= startY + 4 && my <= startY + 18) {
                        currentPage++;
                        return false;
                    }
                }

                ItemStack hovered = findHoveredItemStack(mx, my, startX, startY, PANEL_WIDTH, panelHeight);
                if (hovered != null) {
                    if (InfiniteInvConfig.INSTANCE.takeStack.matchesMouse(btn)) {
                        ClientPlayNetworking.send(new ExtractItemPayload(hovered, false));
                    } else if (InfiniteInvConfig.INSTANCE.takeOne.matchesMouse(btn)) {
                        ClientPlayNetworking.send(new ExtractItemPayload(hovered, true));
                    }
                    return false;
                }

                return true;
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                if (!panelVisible) return true;
                if (searchBox != null && searchBox.isFocused()) {
                    if (event.key() == GLFW.GLFW_KEY_E || event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                        searchBox.keyPressed(event);
                        return false;
                    }
                    return true;
                }

                int[] bounds = calculateBounds(containerScreen);
                ItemStack hovered = findHoveredItemStack(lastMouseX, lastMouseY, bounds[0], bounds[1], bounds[2], bounds[3]);
                if (hovered != null) {
                    if (InfiniteInvConfig.INSTANCE.takeStack.matchesKey(event.key(), event.scancode())) {
                        ClientPlayNetworking.send(new ExtractItemPayload(hovered, false));
                        return false;
                    } else if (InfiniteInvConfig.INSTANCE.takeOne.matchesKey(event.key(), event.scancode())) {
                        ClientPlayNetworking.send(new ExtractItemPayload(hovered, true));
                        return false;
                    }
                }
                return true;
            });
        });
    }

    private static int[] calculateBounds(int left, int top, int width, int height) {
        Minecraft client = Minecraft.getInstance();
        int scaledWidth = client.getWindow().getGuiScaledWidth();
        int scaledHeight = client.getWindow().getGuiScaledHeight();

        int pWidth = (width <= 0) ? 176 : width;
        int pHeight = (height <= 0) ? 166 : height;
        
        int startX = (left <= 0) ? (scaledWidth - pWidth) / 2 + pWidth + 4 : left + pWidth + 4;
        int startY = (top <= 0) ? (scaledHeight - pHeight) / 2 : top;

        if (startX + PANEL_WIDTH > scaledWidth) {
            startX = scaledWidth - PANEL_WIDTH - 2;
        }

        return new int[]{startX, startY, PANEL_WIDTH, pHeight};
    }

    private static int[] calculateBounds(AbstractContainerScreen<?> screen) {
        return calculateBounds(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize());
    }

    public static void renderOverlay(AbstractContainerScreen<?> screen, GuiGraphics ctx, int mx, int my, float delta, int leftPos, int topPos, int imageWidth, int imageHeight) {
        Minecraft client = Minecraft.getInstance();

        int[] bounds = calculateBounds(leftPos, topPos, imageWidth, imageHeight);
        int startX = bounds[0];
        int startY = bounds[1];
        int panelWidth = bounds[2];
        int panelHeight = bounds[3];

        lastMouseX = mx;
        lastMouseY = my;

        int bottomY = startY + panelHeight - 18;

        if (searchBox != null) {
            searchBox.setPosition(startX + 4, bottomY);
        }

        if (!panelVisible) {
            drawCustomButton(ctx, client, "▶", startX, bottomY, 16, 14, mx, my);
            return;
        }

        // GUI Background Fix (Forced Solid Render)
        ctx.fill(startX - 1, startY - 1, startX + panelWidth + 1, startY + panelHeight + 1, 0xFF000000);
        ctx.fill(startX, startY, startX + panelWidth, startY + panelHeight, 0xFF101419);

        // Header Buttons
        drawCustomButton(ctx, client, "<", startX + 4, startY + 4, 18, 14, mx, my);
        drawCustomButton(ctx, client, ">", startX + panelWidth - 22, startY + 4, 18, 14, mx, my);

        List<SyncInventoryPayload.NetworkItemData> all = getSortedFilteredAll();
        int gridStartY = startY + 22;
        int gridHeight = bottomY - gridStartY - 4;
        int rows = Math.max(1, gridHeight / SLOT_SIZE);
        itemsPerPage = Math.max(1, COLUMNS * rows);

        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / itemsPerPage));
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        String pageText = (currentPage + 1) + "/" + totalPages;
        ctx.drawString(client.font, pageText, startX + (panelWidth - client.font.width(pageText)) / 2, startY + 7, 0xFFFFFFFF, true);

        if (searchBox != null) {
            searchBox.render(ctx, mx, my, delta);
        }
        drawCustomButton(ctx, client, currentSort.shortLabel, startX + 68, bottomY, 18, 14, mx, my);
        drawCustomButton(ctx, client, showTooltips ? "T:ON" : "T:OFF", startX + 90, bottomY, 24, 14, mx, my);
        drawCustomButton(ctx, client, "◀", startX + 118, bottomY, 16, 14, mx, my);

        List<SyncInventoryPayload.NetworkItemData> page = getProcessedItems(gridHeight);
        ItemStack hoveredItem = null;

        for (int i = 0; i < page.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int ix = startX + 6 + (col * SLOT_SIZE);
            int iy = gridStartY + (row * SLOT_SIZE);

            ctx.renderItem(page.get(i).stack(), ix, iy);

            if (page.get(i).count() > 0) {
                String countText = formatCount(page.get(i).count());
                int textW = client.font.width(countText);
                ctx.drawString(client.font, countText, ix + 17 - textW, iy + 9, 0xFFFFFFFF, true);
            }

            if (showTooltips && mx >= ix && mx < ix + 18 && my >= iy && my < iy + 18) {
                hoveredItem = page.get(i).stack();
            }
        }

        if (hoveredItem != null) {
            ctx.setTooltipForNextFrame(client.font, hoveredItem, mx, my);
        }
    }

    private static void drawCustomButton(GuiGraphics ctx, Minecraft client, String text, int x, int y, int w, int h, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        int bgColor = hovered ? 0xFF555555 : 0xFF222222;
        int borderColor = 0xFF777777;

        ctx.fill(x, y, x + w, y + h, bgColor);
        ctx.fill(x, y, x + w, y + 1, borderColor);
        ctx.fill(x, y + h - 1, x + w, y + h, borderColor);
        ctx.fill(x, y, x + 1, y + h, borderColor);
        ctx.fill(x + w - 1, y, x + w, y + h, borderColor);

        int textW = client.font.width(text);
        ctx.drawString(client.font, text, x + (w - textW) / 2, y + (h - 8) / 2, 0xFFFFFFFF, true);
    }

    private static ItemStack findHoveredItemStack(double mx, double my, int startX, int startY, int panelWidth, int panelHeight) {
        if (!panelVisible) return null;
        int gridStartY = startY + 22;
        int bottomY = startY + panelHeight - 18;
        int gridHeight = bottomY - gridStartY - 4;

        List<SyncInventoryPayload.NetworkItemData> list = getProcessedItems(gridHeight);
        for (int i = 0; i < list.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + 6 + (col * SLOT_SIZE);
            int y = gridStartY + (row * SLOT_SIZE);

            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                return list.get(i).stack();
            }
        }
        return null;
    }

    private static List<SyncInventoryPayload.NetworkItemData> getSortedFilteredAll() {
        if (searchBox == null) return new ArrayList<>();
        String q = searchBox.getValue().toLowerCase(Locale.ROOT);
        List<SyncInventoryPayload.NetworkItemData> list = new ArrayList<>();
        for (SyncInventoryPayload.NetworkItemData data : cachedItems) {
            if (matchesQuery(data.stack(), q))
                list.add(data);
        }

        list.sort((a, b) -> {
            switch (currentSort) {
                case DESCENDING: {
                    int c = Long.compare(b.count(), a.count());
                    return c != 0 ? c : a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
                }
                case ASCENDING: {
                    int c = Long.compare(a.count(), b.count());
                    return c != 0 ? c : a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
                }
                case ALPHA_ASC:
                    return a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
                case ALPHA_DESC:
                    return b.stack().getHoverName().getString().compareToIgnoreCase(a.stack().getHoverName().getString());
                default: {
                    int c = Long.compare(b.timestamp(), a.timestamp());
                    return c != 0 ? c : a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
                }
            }
        });

        return list;
    }

    private static List<SyncInventoryPayload.NetworkItemData> getProcessedItems(int gridHeight) {
        List<SyncInventoryPayload.NetworkItemData> all = getSortedFilteredAll();
        int rows = Math.max(1, gridHeight / SLOT_SIZE);
        itemsPerPage = Math.max(1, COLUMNS * rows);
        int start = currentPage * itemsPerPage;
        if (start >= all.size()) return new ArrayList<>();
        return all.subList(start, Math.min(start + itemsPerPage, all.size()));
    }

    private static boolean matchesQuery(ItemStack stack, String q) {
        if (q.isEmpty()) return true;
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (stack.getItem().getName(stack).getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (enchantmentMatches(stack.get(DataComponents.STORED_ENCHANTMENTS), q)) return true;
        if (enchantmentMatches(stack.get(DataComponents.ENCHANTMENTS), q)) return true;
        return false;
    }

    private static boolean enchantmentMatches(ItemEnchantments enchants, String q) {
        if (enchants == null) return false;
        for (var entry : enchants.entrySet()) {
            if (Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString().toLowerCase(Locale.ROOT).contains(q))
                return true;
        }
        return false;
    }

    private static String formatCount(long count) {
        if (count >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        if (count >= 1_000)     return String.format(Locale.ROOT, "%.1fk", count / 1_000.0);
        return String.valueOf(count);
    }
}

package com.robsterad.infiniteinv.gui;

import com.robsterad.infiniteinv.config.InfiniteInvConfig;
import com.robsterad.infiniteinv.network.ExtractItemPayload;
import com.robsterad.infiniteinv.network.SyncInventoryPayload;
import com.robsterad.infiniteinv.network.UpdateUiPrefsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
    private static Button sortButton, prevPageBtn, nextPageBtn, tooltipBtn, collapseBtn;
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

    private static final int PANEL_WIDTH = 150;
    private static final int COLUMNS = 7;
    private static final int SLOT_SIZE = 20;

    public static void applyUiPrefs(boolean visible, String sortModeName, boolean tooltips) {
        panelVisible = visible;
        showTooltips = tooltips;
        try {
            currentSort = SortMode.valueOf(sortModeName);
        } catch (IllegalArgumentException e) {
            currentSort = SortMode.RECENT;
        }
        currentPage = 0;
        if (collapseBtn != null) collapseBtn.setMessage(Component.literal(panelVisible ? "◀" : "▶"));
        if (sortButton != null) sortButton.setMessage(Component.literal(currentSort.shortLabel));
        if (tooltipBtn != null) tooltipBtn.setMessage(Component.literal(showTooltips ? "T:ON" : "T:OFF"));
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
            if (!(screen instanceof AbstractContainerScreen<?>)) return;

            panelActive = true;

            searchBox = new EditBox(client.font, 0, 0, 70, 16, Component.literal(""));
            searchBox.setHint(Component.literal("Search..."));

            sortButton  = Button.builder(Component.literal(currentSort.shortLabel), b -> {})
                    .pos(0, 0).size(20, 16).build();
            tooltipBtn  = Button.builder(Component.literal(showTooltips ? "T:ON" : "T:OFF"), b -> {})
                    .pos(0, 0).size(36, 16).build();
            collapseBtn = Button.builder(Component.literal(panelVisible ? "◀" : "▶"), b -> {})
                    .pos(0, 0).size(20, 16).build();

            prevPageBtn = Button.builder(Component.literal("<"), b -> {})
                    .pos(0, 0).size(20, 16).build();
            nextPageBtn = Button.builder(Component.literal(">"), b -> {})
                    .pos(0, 0).size(20, 16).build();

            Screens.getButtons(screen).addAll(List.of(
                searchBox, sortButton, tooltipBtn, collapseBtn, prevPageBtn, nextPageBtn
            ));

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                if (s instanceof AbstractContainerScreen<?> containerScreen) {
                    renderOverlay(containerScreen, ctx, mx, my, delta);
                }
            });

            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                double mx = event.x();
                double my = event.y();
                int btn = event.button();

                if (btn == 0 && collapseBtn.isMouseOver(mx, my)) {
                    panelVisible = !panelVisible;
                    collapseBtn.setMessage(Component.literal(panelVisible ? "◀" : "▶"));
                    sendUiPrefsUpdate();
                    return false;
                }

                if (!panelVisible) return true;

                if (btn == 0) {
                    if (searchBox.isMouseOver(mx, my)) {
                        searchBox.setFocused(true);
                        searchBox.mouseClicked(event, false);
                        return false;
                    }
                    searchBox.setFocused(false);

                    if (sortButton.isMouseOver(mx, my)) {
                        currentSort = SortMode.values()[(currentSort.ordinal() + 1) % SortMode.values().length];
                        sortButton.setMessage(Component.literal(currentSort.shortLabel));
                        currentPage = 0;
                        sendUiPrefsUpdate();
                        return false;
                    }

                    if (tooltipBtn.isMouseOver(mx, my)) {
                        showTooltips = !showTooltips;
                        tooltipBtn.setMessage(Component.literal(showTooltips ? "T:ON" : "T:OFF"));
                        sendUiPrefsUpdate();
                        return false;
                    }

                    if (prevPageBtn.isMouseOver(mx, my)) {
                        if (currentPage > 0) currentPage--;
                        return false;
                    }
                    if (nextPageBtn.isMouseOver(mx, my)) {
                        currentPage++;
                        return false;
                    }
                }

                int vanillaCenterX = scaledWidth / 2;
                int startX = vanillaCenterX + 90;
                int startY = (scaledHeight - 166) / 2;

                ItemStack hovered = findHoveredItemStack(mx, my, startX, startY, 166);
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
                if (searchBox.isFocused()) {
                    if (event.key() == GLFW.GLFW_KEY_E || event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                        searchBox.keyPressed(event);
                        return false;
                    }
                    return true;
                }

                int vanillaCenterX = scaledWidth / 2;
                int startX = vanillaCenterX + 90;
                int startY = (scaledHeight - 166) / 2;

                ItemStack hovered = findHoveredItemStack(lastMouseX, lastMouseY, startX, startY, 166);
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

    public static void renderOverlay(AbstractContainerScreen<?> screen, GuiGraphics ctx, int mx, int my, float delta) {
        Minecraft client = Minecraft.getInstance();
        int scaledWidth = client.getWindow().getGuiScaledWidth();
        int scaledHeight = client.getWindow().getGuiScaledHeight();

        int vanillaCenterX = scaledWidth / 2;
        int startX = vanillaCenterX + 92;
        int startY = (scaledHeight - 166) / 2;
        int panelHeight = 166;

        if (startX + PANEL_WIDTH > scaledWidth) {
            startX = scaledWidth - PANEL_WIDTH - 4;
        }

        lastMouseX = mx;
        lastMouseY = my;

        if (prevPageBtn != null) prevPageBtn.setPosition(startX, startY);
        if (nextPageBtn != null) nextPageBtn.setPosition(startX + PANEL_WIDTH - 20, startY);

        int bottomY = startY + panelHeight - 18;
        if (searchBox != null) searchBox.setPosition(startX, bottomY);
        if (sortButton != null) sortButton.setPosition(startX + 72, bottomY);
        if (tooltipBtn != null) tooltipBtn.setPosition(startX + 94, bottomY);
        if (collapseBtn != null) collapseBtn.setPosition(startX + 132, bottomY);

        if (!panelVisible) return;

        ctx.fill(startX - 2, startY - 2, startX + PANEL_WIDTH + 2, startY + panelHeight, 0xEE141414);

        List<SyncInventoryPayload.NetworkItemData> all = getSortedFilteredAll();
        int gridStartY = startY + 20;
        int gridHeight = bottomY - gridStartY - 2;
        int rows = Math.max(1, gridHeight / SLOT_SIZE);
        itemsPerPage = Math.max(1, COLUMNS * rows);

        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / itemsPerPage));
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        String pageText = (currentPage + 1) + "/" + totalPages;
        ctx.drawCenteredString(client.font, pageText, startX + PANEL_WIDTH / 2, startY + 4, 0xFFFFFFFF);

        List<SyncInventoryPayload.NetworkItemData> page = getProcessedItems(gridHeight);
        ItemStack hoveredItem = null;

        for (int i = 0; i < page.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int ix = startX + (col * SLOT_SIZE) + 2;
            int iy = gridStartY + (row * SLOT_SIZE);

            ctx.renderItem(page.get(i).stack(), ix, iy);

            if (page.get(i).count() > 0) {
                String countText = formatCount(page.get(i).count());
                float scale = 0.65f;
                int textW = client.font.width(countText);
                int textH = client.font.lineHeight;

                float anchorX = ix + 16f;
                float anchorY = iy + 16f;

                ctx.pose().pushMatrix();
                ctx.pose().translate(anchorX, anchorY);
                ctx.pose().scale(scale, scale);
                ctx.drawString(client.font, countText, -textW, -textH, 0xFFFFFFFF, true);
                ctx.pose().popMatrix();
            }

            if (showTooltips && mx >= ix && mx < ix + 18 && my >= iy && my < iy + 18) {
                hoveredItem = page.get(i).stack();
            }
        }

        if (sortButton != null && sortButton.isMouseOver(mx, my))
            ctx.setTooltipForNextFrame(client.font, Component.literal("Sort: " + currentSort.hoverName), mx, my);
        if (tooltipBtn != null && tooltipBtn.isMouseOver(mx, my))
            ctx.setTooltipForNextFrame(client.font, Component.literal("Toggle Tooltips"), mx, my);
        if (collapseBtn != null && collapseBtn.isMouseOver(mx, my))
            ctx.setTooltipForNextFrame(client.font, Component.literal(panelVisible ? "Hide panel" : "Show panel"), mx, my);

        if (hoveredItem != null) {
            ctx.setTooltipForNextFrame(client.font, hoveredItem, mx, my);
        }
    }

    private static ItemStack findHoveredItemStack(double mx, double my, int startX, int startY, int availableHeight) {
        if (!panelVisible) return null;
        int gridStartY = startY + 20;
        int bottomY = startY + availableHeight - 18;
        int gridHeight = bottomY - gridStartY - 2;

        List<SyncInventoryPayload.NetworkItemData> list = getProcessedItems(gridHeight);
        for (int i = 0; i < list.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + (col * SLOT_SIZE) + 2;
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

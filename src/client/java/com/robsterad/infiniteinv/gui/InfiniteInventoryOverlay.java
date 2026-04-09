package com.robsterad.infiniteinv.gui;

import com.robsterad.infiniteinv.network.ExtractItemPayload;
import com.robsterad.infiniteinv.network.SyncInventoryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfiniteInventoryOverlay {

    private static TextFieldWidget searchBox;
    private static ButtonWidget sortButton, prevPageBtn, nextPageBtn, tooltipBtn, collapseBtn;
    public static List<SyncInventoryPayload.NetworkItemData> cachedItems = new ArrayList<>();

    public static boolean panelVisible = true;

    private enum SortMode {
        RECENT("N",   "Newest"),
        DESCENDING("▼", "Count: High→Low"),
        ASCENDING("▲",  "Count: Low→High"),
        ALPHA_ASC("A↑", "Name: A→Z"),
        ALPHA_DESC("A↓","Name: Z→A");

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
    private static boolean showTooltips = false;

    // collapse button width
    private static final int COLLAPSE_W = 20;

    public static boolean onCharTyped(char chr, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            searchBox.charTyped(chr, modifiers);
            currentPage = 0;
            return true;
        }
        return false;
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) return;

            int startX     = (scaledWidth / 2) + 93;
            int panelWidth = scaledWidth - startX - 5;
            if (panelWidth < 50) return;

            int bottomY = scaledHeight - 25;
            int sortW = 20;
            int toolW = 36;

            int searchW = panelWidth - sortW - toolW - COLLAPSE_W - 6;

            searchBox = new TextFieldWidget(client.textRenderer, startX, bottomY, searchW, 14, Text.literal(""));
            searchBox.setPlaceholder(Text.literal("Search..."));

            sortButton  = ButtonWidget.builder(Text.literal(currentSort.shortLabel), b -> {})
                    .dimensions(startX + searchW + 2, bottomY, sortW, 14).build();
            tooltipBtn  = ButtonWidget.builder(Text.literal(showTooltips ? "T:ON" : "T:OFF"), b -> {})
                    .dimensions(startX + searchW + sortW + 4, bottomY, toolW, 14).build();
            collapseBtn = ButtonWidget.builder(Text.literal(panelVisible ? "◀" : "▶"), b -> {})
                    .dimensions(startX + searchW + sortW + toolW + 6, bottomY, COLLAPSE_W, 14).build();

            prevPageBtn = ButtonWidget.builder(Text.literal("<"), b -> {})
                    .dimensions(startX, 10, 20, 14).build();
            nextPageBtn = ButtonWidget.builder(Text.literal(">"), b -> {})
                    .dimensions(startX + panelWidth - 20, 10, 20, 14).build();

            // MOUSE CLICK
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, btn) -> {
                if (btn != 0) return true;

                // collapse button always visible
                if (collapseBtn.isMouseOver(mx, my)) {
                    panelVisible = !panelVisible;
                    collapseBtn.setMessage(Text.literal(panelVisible ? "◀" : "▶"));
                    return false;
                }

                // rest is only visible when overlay is on
                if (!panelVisible) return true;

                if (searchBox.isMouseOver(mx, my)) {
                    searchBox.setFocused(true);
                    searchBox.mouseClicked(mx, my, btn);
                    return false;
                }
                searchBox.setFocused(false);

                if (sortButton.isMouseOver(mx, my)) {
                    currentSort = SortMode.values()[(currentSort.ordinal() + 1) % SortMode.values().length];
                    sortButton.setMessage(Text.literal(currentSort.shortLabel));
                    currentPage = 0;
                    return false;
                }

                if (tooltipBtn.isMouseOver(mx, my)) {
                    showTooltips = !showTooltips;
                    tooltipBtn.setMessage(Text.literal(showTooltips ? "T:ON" : "T:OFF"));
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

                // click on items
                List<SyncInventoryPayload.NetworkItemData> list = getProcessedItems(panelWidth, scaledHeight);
                int columns = Math.max(1, (panelWidth - 8) / 18);
                for (int i = 0; i < list.size(); i++) {
                    int x = startX + 4 + (i % columns) * 18;
                    int y = 28 + (i / columns) * 18;
                    if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                        ClientPlayNetworking.send(new ExtractItemPayload(list.get(i).stack()));
                        return false;
                    }
                }

                return true;
            });

            // KEYBOARD
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, sc, mod) -> {
                if (!panelVisible) return true;
                if (searchBox.isFocused()) {
                    if (key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_BACKSPACE) {
                        searchBox.keyPressed(key, sc, mod);
                        return false;
                    }
                }
                return true;
            });

            // RENDER
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {

                collapseBtn.render(ctx, mx, my, delta);

                if (!panelVisible) return;

                // panel background
                ctx.fill(startX, 8, scaledWidth - 5, scaledHeight - 8, 0x88222222);

                searchBox.render(ctx, mx, my, delta);
                sortButton.render(ctx, mx, my, delta);
                tooltipBtn.render(ctx, mx, my, delta);
                prevPageBtn.render(ctx, mx, my, delta);
                nextPageBtn.render(ctx, mx, my, delta);

                // PAGING
                List<SyncInventoryPayload.NetworkItemData> all = getSortedFilteredAll();
                int columns    = Math.max(1, (panelWidth - 8) / 18);
                int rows       = (scaledHeight - 65) / 18;
                itemsPerPage   = Math.max(1, columns * rows);
                int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / itemsPerPage));
                currentPage    = Math.min(currentPage, Math.max(0, totalPages - 1));

                ctx.drawCenteredTextWithShadow(client.textRenderer,
                        (currentPage + 1) + "/" + totalPages,
                        startX + panelWidth / 2, 13, -1);

                List<SyncInventoryPayload.NetworkItemData> page = getProcessedItems(panelWidth, scaledHeight);
                ItemStack hoveredItem = null;

                for (int i = 0; i < page.size(); i++) {
                    int ix = startX + 4 + (i % columns) * 18;
                    int iy = 28 + (i / columns) * 18;

                    ctx.drawItem(page.get(i).stack(), ix, iy);


                    if (page.get(i).count() > 0) {
                        String countText = formatCount(page.get(i).count());
                        float scale      = 0.65f;
                        int   textW      = client.textRenderer.getWidth(countText);
                        int   textH      = client.textRenderer.fontHeight;


                        float anchorX = ix + 16f;
                        float anchorY = iy + 16f;

                        Matrix3x2fStack mat = ctx.getMatrices();
                        mat.pushMatrix();
                        mat.translate(anchorX, anchorY);
                        mat.scale(scale, scale);

                        ctx.drawTextWithShadow(client.textRenderer, countText, -textW, -textH, -1);
                        mat.popMatrix();
                    }

                    if (showTooltips && mx >= ix && mx < ix + 18 && my >= iy && my < iy + 18) {
                        hoveredItem = page.get(i).stack();
                    }
                }


                if (sortButton.isMouseOver(mx, my))
                    ctx.drawTooltip(client.textRenderer, Text.literal("Sort: " + currentSort.hoverName), mx, my);
                if (tooltipBtn.isMouseOver(mx, my))
                    ctx.drawTooltip(client.textRenderer, Text.literal("Item Tooltips"), mx, my);
                if (collapseBtn.isMouseOver(mx, my))
                    ctx.drawTooltip(client.textRenderer, Text.literal(panelVisible ? "Hide panel" : "Show panel"), mx, my);

                // item tooltip
                if (hoveredItem != null) {
                    ctx.drawItemTooltip(client.textRenderer, hoveredItem, mx, my);
                    com.robsterad.infiniteinv.mixin.DrawContextAccessor accessor =
                            (com.robsterad.infiniteinv.mixin.DrawContextAccessor)(Object) ctx;
                    Runnable drawer = accessor.getTooltipDrawer();
                    if (drawer != null) {
                        drawer.run();
                        accessor.setTooltipDrawer(null);
                    }
                }
            });
        });
    }


    private static List<SyncInventoryPayload.NetworkItemData> getSortedFilteredAll() {
        String q = searchBox.getText().toLowerCase(Locale.ROOT);
        List<SyncInventoryPayload.NetworkItemData> list = new ArrayList<>();
        for (SyncInventoryPayload.NetworkItemData data : cachedItems) {
            if (data.stack().getName().getString().toLowerCase(Locale.ROOT).contains(q))
                list.add(data);
        }

        list.sort((a, b) -> {
            switch (currentSort) {
                case DESCENDING: {
                    int c = Long.compare(b.count(), a.count());
                    return c != 0 ? c : a.stack().getName().getString().compareToIgnoreCase(b.stack().getName().getString());
                }
                case ASCENDING: {
                    int c = Long.compare(a.count(), b.count());
                    return c != 0 ? c : a.stack().getName().getString().compareToIgnoreCase(b.stack().getName().getString());
                }
                case ALPHA_ASC:
                    return a.stack().getName().getString().compareToIgnoreCase(b.stack().getName().getString());
                case ALPHA_DESC:
                    return b.stack().getName().getString().compareToIgnoreCase(a.stack().getName().getString());
                default: { // RECENT
                    int c = Long.compare(b.timestamp(), a.timestamp());
                    return c != 0 ? c : a.stack().getName().getString().compareToIgnoreCase(b.stack().getName().getString());
                }
            }
        });

        return list;
    }

    private static List<SyncInventoryPayload.NetworkItemData> getProcessedItems(int panelWidth, int scaledHeight) {
        List<SyncInventoryPayload.NetworkItemData> all = getSortedFilteredAll();
        int columns    = Math.max(1, (panelWidth - 8) / 18);
        int rows       = (scaledHeight - 65) / 18;
        itemsPerPage   = Math.max(1, columns * rows);
        int start = currentPage * itemsPerPage;
        if (start >= all.size()) return new ArrayList<>();
        return all.subList(start, Math.min(start + itemsPerPage, all.size()));
    }

    private static String formatCount(long count) {
        if (count >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        if (count >= 1_000)     return String.format(Locale.ROOT, "%.1fk", count / 1_000.0);
        return String.valueOf(count);
    }
}
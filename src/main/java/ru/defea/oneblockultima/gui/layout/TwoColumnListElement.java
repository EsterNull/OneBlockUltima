package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TwoColumnListElement extends ViewElement<TwoColumnListElement> {
    private final int itemHeight;
    private int scrollOffset = 0;
    private int visibleItems = 0;
    private final int trackWidth = 6;
    private int panelColor = PANEL_COLOR;
    private final int innerPad = 4;
    private List<? extends TwoColumnEntry> leftEntries;
    private List<? extends TwoColumnEntry> rightEntries;
    private final ScrollbarElement scrollbar = new ScrollbarElement();

    public interface TwoColumnEntry {
        void drawLeft(int x, int y, int width, int height, boolean hovered, int index, FontRenderer fr, int mouseX, int mouseY);
        void drawRight(int x, int y, int width, int height, boolean hovered, int index, FontRenderer fr, int mouseX, int mouseY);
        boolean mouseClickedLeft(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton);
        boolean mouseClickedRight(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton);
    }

    public TwoColumnListElement(int itemHeight) {
        this.itemHeight = itemHeight;
    }

    public TwoColumnListElement leftEntries(List<? extends TwoColumnEntry> entries) {
        this.leftEntries = entries;
        return this;
    }

    public TwoColumnListElement rightEntries(List<? extends TwoColumnEntry> entries) {
        this.rightEntries = entries;
        return this;
    }

    public TwoColumnListElement scrollOffset(int offset) {
        this.scrollOffset = offset;
        return this;
    }

    public TwoColumnListElement panelColor(int color) {
        this.panelColor = color;
        return this;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = offset;
    }

    private int getMaxEntries() {
        int left = leftEntries != null ? leftEntries.size() : 0;
        int right = rightEntries != null ? rightEntries.size() : 0;
        return Math.max(left, right);
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if ((leftEntries == null || leftEntries.isEmpty()) && (rightEntries == null || rightEntries.isEmpty()))
            return;

        int listWidth = computedWidth - trackWidth - 2;
        visibleItems = Math.max(1, computedHeight / itemHeight);
        int maxEntries = getMaxEntries();
        if (scrollOffset > maxEntries - visibleItems) scrollOffset = Math.max(0, maxEntries - visibleItems);
        if (scrollOffset < 0) scrollOffset = 0;

        Gui.drawRect(computedX, computedY, computedX + listWidth + trackWidth + 2, computedY + computedHeight, panelColor);

        int colWidth = (listWidth - innerPad * 2) / 2;
        int contentTop = computedY;

        for (int i = 0; i < visibleItems; i++) {
            int row = scrollOffset + i;
            if (row >= maxEntries) break;

            int y = contentTop + i * itemHeight;

            boolean leftHovered = mouseX >= computedX && mouseX <= computedX + colWidth &&
                                  mouseY >= y && mouseY < y + itemHeight;
            boolean rightHovered = mouseX >= computedX + colWidth + innerPad && mouseX <= computedX + listWidth &&
                                   mouseY >= y && mouseY < y + itemHeight;

            if (row < (leftEntries != null ? leftEntries.size() : 0)) {
                int bg = (row % 2 == 0) ? DARK_GRAY_COLOR_2 : DARK_GRAY_COLOR_3;
                Gui.drawRect(computedX + innerPad, y, computedX + colWidth, y + itemHeight, bg);
                leftEntries.get(row).drawLeft(computedX + innerPad, y, colWidth - innerPad, itemHeight, leftHovered, row, fr, mouseX, mouseY);
            }
            if (row < (rightEntries != null ? rightEntries.size() : 0)) {
                int bg = (row % 2 == 0) ? DARK_GRAY_COLOR_2 : DARK_GRAY_COLOR_3;
                Gui.drawRect(computedX + colWidth + innerPad, y, computedX + listWidth, y + itemHeight, bg);
                rightEntries.get(row).drawRight(computedX + colWidth + innerPad, y, colWidth - innerPad, itemHeight, rightHovered, row, fr, mouseX, mouseY);
            }
        }

        if (maxEntries > visibleItems) {
            layoutScrollbar(listWidth);
            scrollbar.draw(fr, mouseX, mouseY, partialTicks);
            scrollOffset = scrollbar.getScrollOffset();
        }
    }

    private void layoutScrollbar(int listWidth) {
        scrollbar.setComputedPosition(computedX + listWidth + 2, computedY);
        scrollbar.setComputedSize(trackWidth, computedHeight);
        scrollbar.trackWidth(trackWidth)
                .totalItems(getMaxEntries())
                .visibleItems(visibleItems)
                .scrollOffset(scrollOffset);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if ((leftEntries == null || leftEntries.isEmpty()) && (rightEntries == null || rightEntries.isEmpty()))
            return false;

        visibleItems = Math.max(1, computedHeight / itemHeight);
        int listWidth = computedWidth - trackWidth - 2;
        int colWidth = (listWidth - innerPad * 2) / 2;

        if (getMaxEntries() > visibleItems) {
            layoutScrollbar(listWidth);
            if (scrollbar.mouseClicked(mouseX, mouseY, mouseButton)) {
                scrollOffset = scrollbar.getScrollOffset();
                return true;
            }
        }

        if (mouseX >= computedX && mouseX <= computedX + listWidth) {
            for (int i = 0; i < visibleItems; i++) {
                int row = scrollOffset + i;
                if (row >= getMaxEntries()) break;

                int y = computedY + i * itemHeight;
                int localY = mouseY - y;

                if (mouseY >= y && mouseY < y + itemHeight) {
                    if (mouseX >= computedX && mouseX <= computedX + colWidth && row < (leftEntries != null ? leftEntries.size() : 0)) {
                        int localX = mouseX - (computedX + innerPad);
                        assert leftEntries != null;
                        return leftEntries.get(row).mouseClickedLeft(mouseX, mouseY, localX, localY, colWidth - innerPad, itemHeight, mouseButton);
                    }
                    if (mouseX >= computedX + colWidth + innerPad && mouseX <= computedX + listWidth && row < (rightEntries != null ? rightEntries.size() : 0)) {
                        int localX = mouseX - (computedX + colWidth + innerPad);
                        assert rightEntries != null;
                        return rightEntries.get(row).mouseClickedRight(mouseX, mouseY, localX, localY, colWidth - innerPad, itemHeight, mouseButton);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        if (getMaxEntries() <= visibleItems) return false;
        scrollbar.totalItems(getMaxEntries()).visibleItems(visibleItems).scrollOffset(scrollOffset);
        boolean scrolled = scrollbar.handleMouseInput(dWheel);
        scrollOffset = scrollbar.getScrollOffset();
        return scrolled;
    }

    @Override
    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (scrollbar.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
            scrollOffset = scrollbar.getScrollOffset();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        scrollbar.mouseReleased(mouseX, mouseY, state);
        return false;
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return 120;
    }
}

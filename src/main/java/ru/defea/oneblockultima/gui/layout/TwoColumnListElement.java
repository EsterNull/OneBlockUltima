package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import ru.defea.oneblockultima.Constants;

import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TwoColumnListElement extends ViewElement<TwoColumnListElement> {
    private final int itemHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int visibleItems = 0;
    private final int trackWidth = 6;
    private int panelColor = PANEL_COLOR;
    private final int innerPad = 4;
    private List<? extends TwoColumnEntry> leftEntries;
    private List<? extends TwoColumnEntry> rightEntries;
    private int thumbY;
    private int thumbHeight;
    private boolean dragging = false;
    private int dragGrabOffset = 0;

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
        maxScroll = Math.max(0, maxEntries - visibleItems);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
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
            int scrollbarX = computedX + listWidth + 2;
            int scrollbarHeight = computedHeight;
            Gui.drawRect(scrollbarX, contentTop, scrollbarX + trackWidth, contentTop + scrollbarHeight, DARK_GRAY_COLOR_2);

            computeThumb();
            Gui.drawRect(scrollbarX, thumbY, scrollbarX + trackWidth, thumbY + thumbHeight, Constants.GRAY_COLOR_1);
        }
    }

    private void computeThumb() {
        int trackHeight = computedHeight;
        float ratio = (float) visibleItems / getMaxEntries();
        thumbHeight = Math.max(10, (int) (trackHeight * ratio));
        float thumbPos;
        if (trackHeight <= thumbHeight) {
            thumbPos = 0;
        } else {
            thumbPos = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        }
        thumbY = computedY + (int) (thumbPos * (trackHeight - thumbHeight));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if ((leftEntries == null || leftEntries.isEmpty()) && (rightEntries == null || rightEntries.isEmpty()))
            return false;

        int listWidth = computedWidth - trackWidth - 2;
        int colWidth = (listWidth - innerPad * 2) / 2;
        int scrollbarX = computedX + listWidth + 2;

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + trackWidth &&
            mouseY >= computedY && mouseY <= computedY + computedHeight) {
            computeThumb();
            if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                dragging = true;
                dragGrabOffset = mouseY - thumbY;
                return true;
            }
            int range = computedHeight - thumbHeight;
            int clickY = mouseY - computedY - thumbHeight / 2;
            float ratio = range > 0 ? Math.max(0, Math.min(1, (float) clickY / range)) : 0;
            scrollOffset = Math.round(ratio * maxScroll);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
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
        scrollOffset -= dWheel > 0 ? 1 : -1;
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        return true;
    }

    @Override
    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!dragging || clickedMouseButton != 0) return false;
        computeThumb();
        int range = computedHeight - thumbHeight;
        float ratio = range > 0 ? (float) (mouseY - dragGrabOffset - computedY) / range : 0;
        ratio = Math.max(0, Math.min(1, ratio));
        scrollOffset = Math.round(ratio * maxScroll);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        return true;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
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

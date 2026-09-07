package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ScrollbarElement extends ViewElement<ScrollbarElement> {
    private int totalItems;
    private int visibleItems;
    private int scrollOffset;
    private int trackColor = DARK_GRAY_COLOR_2;
    private int thumbColor = GRAY_COLOR_1;
    private boolean thumbHovered = false;
    private int thumbY;
    private int thumbHeight;
    private int trackX;
    private int trackWidth = 6;
    private boolean dragging = false;
    private int dragGrabOffset = 0;

    public ScrollbarElement totalItems(int total) {
        this.totalItems = total;
        return this;
    }

    public ScrollbarElement visibleItems(int visible) {
        this.visibleItems = visible;
        return this;
    }

    public ScrollbarElement scrollOffset(int offset) {
        this.scrollOffset = offset;
        return this;
    }

    public ScrollbarElement trackWidth(int width) {
        this.trackWidth = width;
        return this;
    }

    public ScrollbarElement trackColor(int color) {
        this.trackColor = color;
        return this;
    }

    public ScrollbarElement thumbColor(int color) {
        this.thumbColor = color;
        return this;
    }

    public int getMaxScroll() {
        return Math.max(0, totalItems - visibleItems);
    }

    public void clampScroll() {
        int max = getMaxScroll();
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (totalItems <= visibleItems) return;

        trackX = computedX;
        int trackY = computedY;
        int trackHeight = computedHeight;

        g.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, trackColor);

        computeThumb();

        int color = thumbHovered ? GRAY_COLOR_5 : thumbColor;
        g.fill(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, color);
    }

    private void computeThumb() {
        int trackHeight = computedHeight;
        float ratio = (float) visibleItems / totalItems;
        thumbHeight = Math.max(10, (int) (trackHeight * ratio));
        int maxOffset = getMaxScroll();
        float thumbPos;
        if (trackHeight <= thumbHeight) {
            thumbPos = 0;
        } else {
            thumbPos = maxOffset > 0 ? (float) scrollOffset / maxOffset : 0;
        }
        thumbY = computedY + (int) (thumbPos * (trackHeight - thumbHeight));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false;
        if (totalItems <= visibleItems) return false;
        if (mouseX >= computedX && mouseX <= computedX + trackWidth &&
                mouseY >= computedY && mouseY <= computedY + computedHeight) {
            computeThumb();
            if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                dragging = true;
                dragGrabOffset = mouseY - thumbY;
                return true;
            }
            int trackHeight = computedHeight;
            int range = trackHeight - thumbHeight;
            int clickY = mouseY - computedY - thumbHeight / 2;
            float ratio = range > 0 ? (float) clickY / range : 0;
            scrollOffset = Math.round(ratio * getMaxScroll());
            clampScroll();
            dragging = true;
            dragGrabOffset = thumbHeight / 2;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!dragging || clickedMouseButton != 0) return false;
        computeThumb();
        int range = computedHeight - thumbHeight;
        float ratio = range > 0 ? (float) (mouseY - dragGrabOffset - computedY) / range : 0;
        ratio = Math.max(0, Math.min(1, ratio));
        scrollOffset = Math.round(ratio * getMaxScroll());
        clampScroll();
        return true;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        return false;
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        if (totalItems <= visibleItems) return false;
        scrollOffset -= dWheel > 0 ? 1 : -1;
        clampScroll();
        return true;
    }

    public void updateHover(int mouseX, int mouseY) {
        thumbHovered = mouseX >= trackX && mouseX <= trackX + trackWidth &&
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }

    @Override
    public int getPreferredWidth() {
        return trackWidth;
    }

    @Override
    public int getPreferredHeight() {
        return 100;
    }
}

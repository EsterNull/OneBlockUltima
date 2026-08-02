package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

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
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (totalItems <= visibleItems) return;

        trackX = computedX;
        int trackY = computedY;
        int trackHeight = computedHeight;

        Gui.drawRect(trackX, trackY, trackX + trackWidth, trackY + trackHeight, trackColor);

        float ratio = (float) visibleItems / totalItems;
        thumbHeight = Math.max(10, (int) (trackHeight * ratio));
        int maxOffset = getMaxScroll();
        float thumbPos = maxOffset > 0 ? (float) scrollOffset / maxOffset : 0;
        thumbY = trackY + (int) (thumbPos * (trackHeight - thumbHeight));

        int color = thumbHovered ? GRAY_COLOR_5 : thumbColor;
        Gui.drawRect(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, color);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false;
        if (totalItems <= visibleItems) return false;
        if (mouseX >= computedX && mouseX <= computedX + trackWidth &&
            mouseY >= computedY && mouseY <= computedY + computedHeight) {
            int clickY = mouseY - computedY - thumbHeight / 2;
            float ratio = (float) clickY / (computedHeight - thumbHeight);
            scrollOffset = Math.round(ratio * getMaxScroll());
            clampScroll();
            return true;
        }
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

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import ru.defea.oneblockultima.Constants;

import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ScrollableListElement extends ViewElement<ScrollableListElement> {
    private final int itemHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int visibleItems = 0;
    private int trackWidth = 6;
    private int panelColor = PANEL_COLOR;
    private int hoveredRow = -1;
    private List<? extends ScrollableListEntry> entries;

    public interface ScrollableListEntry {
        void draw(int x, int y, int width, int height, boolean hovered, boolean selected, FontRenderer fr, int mouseX, int mouseY);
        boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton);
    }

    public ScrollableListElement(int itemHeight) {
        this.itemHeight = itemHeight;
    }

    public ScrollableListElement entries(List<? extends ScrollableListEntry> entries) {
        this.entries = entries;
        return this;
    }

    public ScrollableListElement scrollOffset(int offset) {
        this.scrollOffset = offset;
        return this;
    }

    public ScrollableListElement trackWidth(int width) {
        this.trackWidth = width;
        return this;
    }

    public ScrollableListElement panelColor(int color) {
        this.panelColor = color;
        return this;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getHoveredRow() {
        return hoveredRow;
    }

    public void clampScroll() {
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (panelColor != 0) {
            Gui.drawRect(computedX, computedY, computedX + computedWidth, computedY + computedHeight, panelColor);
        }

        if (entries == null || entries.isEmpty()) return;

        int listWidth = computedWidth - trackWidth - 2;
        visibleItems = computedHeight / itemHeight;
        maxScroll = Math.max(0, entries.size() - visibleItems);
        clampScroll();

        hoveredRow = -1;
        int contentTop = computedY;
        for (int i = 0; i < visibleItems && scrollOffset + i < entries.size(); i++) {
            int row = scrollOffset + i;
            int y = contentTop + i * itemHeight;
            boolean hovered = mouseX >= computedX && mouseX <= computedX + listWidth &&
                              mouseY >= y && mouseY < y + itemHeight;
            if (hovered) hoveredRow = row;

            int bg = (row % 2 == 0) ? DARK_GRAY_COLOR_2 : DARK_GRAY_COLOR_3;
            Gui.drawRect(computedX, y, computedX + listWidth, y + itemHeight, bg);

            ScrollableListEntry entry = entries.get(row);
            entry.draw(computedX, y, listWidth, itemHeight, hovered, false, fr, mouseX, mouseY);
        }

        if (entries.size() > visibleItems) {
            int thumbWidth = trackWidth;
            int trackX = computedX + listWidth + 2;
            Gui.drawRect(trackX, contentTop, trackX + thumbWidth, contentTop + computedHeight, DARK_GRAY_COLOR_2);

            float ratio = (float) visibleItems / entries.size();
            int thumbH = Math.max(10, (int) (computedHeight * ratio));
            float thumbPos = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
            int thumbY = contentTop + (int) (thumbPos * (computedHeight - thumbH));

            Gui.drawRect(trackX, thumbY, trackX + thumbWidth, thumbY + thumbH, Constants.GRAY_COLOR_1);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (entries == null || entries.isEmpty()) return false;

        int listWidth = computedWidth - trackWidth - 2;

        int trackX = computedX + listWidth + 2;
        if (mouseX >= trackX && mouseX <= trackX + trackWidth &&
            mouseY >= computedY && mouseY <= computedY + computedHeight) {
            int clickY = mouseY - computedY - 5;
            float ratio = Math.max(0, Math.min(1, (float) clickY / (computedHeight - 10)));
            scrollOffset = Math.round(ratio * maxScroll);
            clampScroll();
            return true;
        }

        if (mouseX >= computedX && mouseX <= computedX + listWidth) {
            for (int i = 0; i < visibleItems && scrollOffset + i < entries.size(); i++) {
                int row = scrollOffset + i;
                int y = computedY + i * itemHeight;
                if (mouseY >= y && mouseY < y + itemHeight) {
                    ScrollableListEntry entry = entries.get(row);
                    int localX = mouseX - computedX;
                    int localY = mouseY - y;
                    return entry.mouseClicked(mouseX, mouseY, localX, localY, listWidth, itemHeight, mouseButton);
                }
            }
        }
        return false;
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        if (entries == null || entries.size() <= visibleItems) return false;
        scrollOffset -= dWheel > 0 ? 1 : -1;
        clampScroll();
        return true;
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return 200;
    }
}

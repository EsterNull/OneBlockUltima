package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class RowElement extends ViewElement<RowElement> {
    private final Alignment rowAlignment;
    private final List<ViewElement<?>> children = new ArrayList<>();
    private int rowHeight = 20;
    private int rowGap = 4;
    private boolean topAlign = false;

    public RowElement(Alignment alignment) {
        this.rowAlignment = alignment;
    }

    public RowElement gap(int gap) {
        this.rowGap = gap;
        return this;
    }

    public RowElement height(int height) {
        this.rowHeight = height;
        return this;
    }

    public RowElement topAlign(boolean topAlign) {
        this.topAlign = topAlign;
        return this;
    }

    public ButtonElement<?> button(int id, String text) {
        ButtonElement<?> e = new ButtonElement<>(id, text);
        children.add(e);
        return e;
    }

    public ButtonElement<?> button(int id, String text, int width) {
        ButtonElement<?> e = new ButtonElement<>(id, text).width(width);
        children.add(e);
        return e;
    }

    public ButtonToggleElement buttonToggle(int id, boolean stateTriggered) {
        ButtonToggleElement e = new ButtonToggleElement(id, stateTriggered);
        children.add(e);
        return e;
    }

    public RowElement label(String text) {
        children.add(new LabelElement(text));
        return this;
    }

    public RowElement label(String text, int color) {
        children.add(new LabelElement(text).color(color));
        return this;
    }

    public RowElement spacer(int width) {
        children.add(new SpacerElement(width, 0));
        return this;
    }

    public RowElement add(ViewElement<?> element) {
        children.add(element);
        return this;
    }

    public List<ViewElement<?>> getChildren() {
        return children;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
        int naturalWidth = 0;
        for (ViewElement<?> child : children) {
            naturalWidth += child.getPreferredWidth(font);
        }

        int actualHeight = Math.max(rowHeight, computedHeight);

        if (rowAlignment == Alignment.SPACE_BETWEEN && children.size() > 1) {
            int freeSpace = computedWidth - naturalWidth;
            float gapBetween = freeSpace > 0 ? (float) freeSpace / (children.size() - 1) : rowGap;
            float cx = computedX;
            for (ViewElement<?> child : children) {
                int childWidth = child.getPreferredWidth(font);
                int childHeight = child.getPreferredHeight(font);
                int y;
                if (topAlign) {
                    y = computedY;
                } else {
                    y = computedY + (actualHeight - childHeight) / 2;
                }
                child.setComputedPosition(Math.round(cx), y);
                child.setComputedSize(childWidth, childHeight);
                child.createWidgets(screen, font, factory);
                cx += childWidth + gapBetween;
            }
            return;
        }

        int totalWidth = naturalWidth + rowGap * Math.max(0, children.size() - 1);

        int startX;
        Alignment align = rowAlignment != null ? rowAlignment : Alignment.CENTER;
        switch (align) {
            case CENTER:
                startX = computedX + (computedWidth - totalWidth) / 2;
                break;
            case RIGHT:
                startX = computedX + computedWidth - totalWidth;
                break;
            default:
                startX = computedX;
                break;
        }

        int cx = startX;
        for (ViewElement<?> child : children) {
            int childWidth = child.getPreferredWidth(font);
            int childHeight = child.getPreferredHeight(font);
            int y;
            if (topAlign) {
                y = computedY;
            } else {
                y = computedY + (actualHeight - childHeight) / 2;
            }
            child.setComputedPosition(cx, y);
            child.setComputedSize(childWidth, childHeight);
            child.createWidgets(screen, font, factory);
            cx += childWidth + rowGap;
        }
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        for (ViewElement<?> child : children) {
            if (child.isVisible()) child.draw(g, font, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean actionPerformed(AbstractWidget button) {
        for (ViewElement<?> child : children) {
            if (child.actionPerformed(button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (ViewElement<?> child : children) {
            if (child.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        for (ViewElement<?> child : children) {
            if (child.mouseReleased(mouseX, mouseY, state)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (ViewElement<?> child : children) {
            if (child.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) return true;
        }
        return false;
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        for (ViewElement<?> child : children) {
            if (child.handleMouseInput(dWheel)) return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        for (ViewElement<?> child : children) {
            if (child.keyTyped(typedChar, keyCode)) return true;
        }
        return false;
    }

    @Override
    public void updateCursorCounter() {
        for (ViewElement<?> child : children) {
            child.updateCursorCounter();
        }
    }

    @Override
    public void tick() {
        for (ViewElement<?> child : children) {
            child.tick();
        }
    }

    @Override
    public int getPreferredWidth() {
        return getPreferredWidth(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredWidth(Font font) {
        int total = 0;
        for (ViewElement<?> child : children) {
            total += child.getPreferredWidth(font);
        }
        total += rowGap * Math.max(0, children.size() - 1);
        return total;
    }

    @Override
    public int getPreferredHeight() {
        return getPreferredHeight(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredHeight(Font font) {
        int max = rowHeight;
        for (ViewElement<?> child : children) {
            int h = child.getPreferredHeight(font);
            if (h > max) max = h;
        }
        return max;
    }
}

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.List;

public class ColumnElement extends ViewElement {
    private final List<ViewElement> children = new ArrayList<>();
    private int colGap = 4;
    private Alignment colAlignment = Alignment.LEFT;

    public ColumnElement gap(int gap) {
        this.colGap = gap;
        return this;
    }

    public ColumnElement align(Alignment alignment) {
        this.colAlignment = alignment;
        return this;
    }

    @Override
    public ColumnElement widthPercent(int percent) {
        super.widthPercent(percent);
        return this;
    }

    public ButtonElement button(int id, String text) {
        ButtonElement e = new ButtonElement(id, text);
        children.add(e);
        return e;
    }

    public ColumnElement label(String text) {
        children.add(new LabelElement(text));
        return this;
    }

    public ColumnElement label(String text, int color) {
        children.add(new LabelElement(text).color(color));
        return this;
    }

    public ColumnElement spacer(int height) {
        children.add(new SpacerElement(height));
        return this;
    }

    public ColumnElement add(ViewElement element) {
        children.add(element);
        return this;
    }

    public List<ViewElement> getChildren() {
        return children;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        int cy = computedY;
        for (ViewElement child : children) {
            if (!child.isVisible()) continue;

            int childWidth = child.getPreferredWidth(fontRenderer);
            int childHeight = child.getPreferredHeight(fontRenderer);
            if (childWidth <= 0) childWidth = computedWidth;
            if (childWidth > computedWidth) childWidth = computedWidth;

            int x;
            switch (colAlignment) {
                case CENTER:
                    x = computedX + (computedWidth - childWidth) / 2;
                    break;
                case RIGHT:
                    x = computedX + computedWidth - childWidth;
                    break;
                default:
                    x = computedX;
                    break;
            }

            child.setComputedPosition(x, cy);
            child.setComputedSize(childWidth, childHeight);
            child.createWidgets(buttonList, fontRenderer, factory);
            cy += childHeight + colGap;
        }
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        for (ViewElement child : children) {
            if (child.isVisible()) child.draw(fr, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean actionPerformed(GuiButton button) {
        for (ViewElement child : children) {
            if (child.actionPerformed(button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (ViewElement child : children) {
            if (child.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        for (ViewElement child : children) {
            if (child.mouseReleased(mouseX, mouseY, state)) return true;
        }
        return false;
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        for (ViewElement child : children) {
            if (child.handleMouseInput(dWheel)) return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        for (ViewElement child : children) {
            if (child.keyTyped(typedChar, keyCode)) return true;
        }
        return false;
    }

    @Override
    public void updateCursorCounter() {
        for (ViewElement child : children) {
            child.updateCursorCounter();
        }
    }

    @Override
    public int getPreferredWidth() {
        int max = 0;
        for (ViewElement child : children) {
            int w = child.getPreferredWidth();
            if (w > max) max = w;
        }
        return max;
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        int max = 0;
        for (ViewElement child : children) {
            int w = child.getPreferredWidth(fr);
            if (w > max) max = w;
        }
        return max;
    }

    @Override
    public int getPreferredHeight() {
        int total = 0;
        boolean first = true;
        for (ViewElement child : children) {
            if (!child.isVisible()) continue;
            if (!first) total += colGap;
            total += child.getPreferredHeight();
            first = false;
        }
        return total;
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
        int total = 0;
        boolean first = true;
        for (ViewElement child : children) {
            if (!child.isVisible()) continue;
            if (!first) total += colGap;
            total += child.getPreferredHeight(fr);
            first = false;
        }
        return total;
    }
}

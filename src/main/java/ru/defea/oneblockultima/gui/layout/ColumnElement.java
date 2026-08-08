package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ColumnElement extends ViewElement<ColumnElement> {
    private final List<ViewElement<?>> children = new ArrayList<>();
    private int colGap = 4;
    private Alignment colAlignment = Alignment.LEFT;
    private boolean colCenterVertical = false;

    public ColumnElement gap(int gap) {
        this.colGap = gap;
        return this;
    }

    public ColumnElement align(Alignment alignment) {
        this.colAlignment = alignment;
        return this;
    }

    public ColumnElement centerVertical() {
        this.colCenterVertical = true;
        return this;
    }

    public ColumnElement title(String key) {
        children.add(new TitleElement(key));
        return this;
    }

    public ColumnElement title(String key, Object... args) {
        children.add(new TitleElement(key, args));
        return this;
    }

    public RowElement row(Alignment alignment) {
        RowElement e = new RowElement(alignment);
        children.add(e);
        return e;
    }

    public ButtonElement<?> button(int id, String text) {
        ButtonElement<?> e = new ButtonElement<>(id, text);
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

    public ColumnElement add(ViewElement<?> element) {
        children.add(element);
        return this;
    }

    public List<ViewElement<?>> getChildren() {
        return children;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        int visibleCount = 0;
        int flexCount = 0;
        int fixedHeight = 0;
        for (ViewElement<?> child : children) {
            if (!child.isVisible()) continue;
            visibleCount++;
            if (child.isFlexible()) {
                flexCount++;
            } else {
                fixedHeight += ViewFactory.computeElementHeight(fontRenderer, child, computedHeight) + colGap;
            }
        }
        if (visibleCount > 0) fixedHeight -= colGap;

        if (colAlignment == Alignment.SPACE_BETWEEN && visibleCount > 1) {
            int naturalHeight = 0;
            for (ViewElement<?> child : children) {
                if (!child.isVisible()) continue;
                naturalHeight += child.getPreferredHeight(fontRenderer);
            }

            float gapBetween = colGap;
            float cy = computedY;
            int freeSpace = computedHeight - naturalHeight;
            if (freeSpace > 0) {
                gapBetween = (float) freeSpace / (visibleCount - 1);
            }

            for (ViewElement<?> child : children) {
                if (!child.isVisible()) continue;

                int childWidth = ViewFactory.computeElementWidth(fontRenderer, child, computedWidth);
                int childHeight = child.getPreferredHeight(fontRenderer);

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

                child.setComputedPosition(x, Math.round(cy));
                child.setComputedSize(childWidth, childHeight);
                child.createWidgets(buttonList, fontRenderer, factory);
                cy += childHeight + gapBetween;
            }
            return;
        }

        int flexHeight = flexCount > 0 ? Math.max(0, (computedHeight - fixedHeight - flexCount * colGap) / flexCount) : 0;
        int totalHeight = fixedHeight + flexCount * (flexHeight + colGap);
        float startY = colCenterVertical && totalHeight < computedHeight
                ? computedY + (float) (computedHeight - totalHeight) / 2
                : computedY;

        int maxY = computedY + computedHeight;
        for (ViewElement<?> child : children) {
            if (!child.isVisible()) continue;

            int childWidth = ViewFactory.computeElementWidth(fontRenderer, child, computedWidth);
            int childHeight = child.isFlexible()
                    ? flexHeight
                    : ViewFactory.computeElementHeight(fontRenderer, child, computedHeight);
            if (startY + childHeight > maxY) break;

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

            child.setComputedPosition(x, Math.round(startY));
            child.setComputedSize(childWidth, childHeight);
            child.createWidgets(buttonList, fontRenderer, factory);
            startY += childHeight + colGap;
        }
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        for (ViewElement<?> child : children) {
            if (child.isVisible()) child.draw(fr, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean actionPerformed(GuiButton button) {
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
        int max = 0;
        for (ViewElement<?> child : children) {
            int w = child.getPreferredWidth();
            if (w > max) max = w;
        }
        return max;
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        int max = 0;
        for (ViewElement<?> child : children) {
            int w = child.getPreferredWidth(fr);
            if (w > max) max = w;
        }
        return max;
    }

    @Override
    public int getPreferredHeight() {
        int total = 0;
        boolean first = true;
        for (ViewElement<?> child : children) {
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
        for (ViewElement<?> child : children) {
            if (!child.isVisible()) continue;
            if (!first) total += colGap;
            total += child.getPreferredHeight(fr);
            first = false;
        }
        return total;
    }
}

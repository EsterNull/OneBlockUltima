package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_1;
import static ru.defea.oneblockultima.Constants.TRANSPARENT_DARK_GRAY_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ViewFactory {
    private final int screenWidth;
    private final int screenHeight;
    private int paddingTop = 10;
    private int paddingBottom = 10;
    private int paddingLeft = 10;
    private int paddingRight = 10;
    private int gap = 4;
    private Alignment defaultAlignment = Alignment.CENTER;
    private boolean centerVertical = false;
    private int panelColor = TRANSPARENT_DARK_GRAY_COLOR_1;
    private int panelBorderColor = DARK_GRAY_COLOR_1;
    private int margin = 0;
    private boolean fitContent = false;
    private final List<ViewElement<?>> elements = new ArrayList<>();
    private final List<EditBox> textFields = new ArrayList<>();
    private int totalContentHeight = 0;

    private Screen screen;
    private Font font;

    public ViewFactory(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public ViewFactory padding(int padding) {
        this.paddingTop = padding;
        this.paddingBottom = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return this;
    }

    public ViewFactory padding(int vertical, int horizontal) {
        this.paddingTop = vertical;
        this.paddingBottom = vertical;
        this.paddingLeft = horizontal;
        this.paddingRight = horizontal;
        return this;
    }

    public ViewFactory padding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
        return this;
    }

    public ViewFactory gap(int gap) {
        this.gap = gap;
        return this;
    }

    public ViewFactory align(Alignment alignment) {
        this.defaultAlignment = alignment;
        return this;
    }

    public ViewFactory centerVertical() {
        this.centerVertical = true;
        return this;
    }

    public ViewFactory panel(int color, int borderColor) {
        this.panelColor = color;
        this.panelBorderColor = borderColor;
        return this;
    }

    public ViewFactory margin(int margin) {
        this.margin = margin;
        return this;
    }

    public ViewFactory fitContent() {
        this.fitContent = true;
        return this;
    }

    public ViewFactory fitContent(boolean fitContent) {
        this.fitContent = fitContent;
        return this;
    }

    public ViewFactory title(String key) {
        elements.add(new TitleElement(key));
        return this;
    }

    public ViewFactory title(String key, Object... args) {
        elements.add(new TitleElement(key, args));
        return this;
    }

    public LabelElement label(String text) {
        LabelElement e = new LabelElement(text);
        elements.add(e);
        return e;
    }

    public LabelElement label(int color, String text) {
        LabelElement e = new LabelElement(text).color(color);
        elements.add(e);
        return e;
    }

    public ButtonElement<?> button(int id, String text) {
        ButtonElement<?> e = new ButtonElement<>(id, text);
        elements.add(e);
        return e;
    }

    public ButtonElement<?> button(int id, int width, String text) {
        ButtonElement<?> e = new ButtonElement<>(id, text).width(width);
        elements.add(e);
        return e;
    }

    public ButtonElement<?> button(String text) {
        ButtonElement<?> e = new ButtonElement<>(text);
        elements.add(e);
        return e;
    }

    public ButtonToggleElement buttonToggle(int id, boolean stateTriggered) {
        ButtonToggleElement e = new ButtonToggleElement(id, stateTriggered);
        elements.add(e);
        return e;
    }

    public ButtonToggleElement buttonToggle(boolean stateTriggered) {
        ButtonToggleElement e = new ButtonToggleElement(stateTriggered);
        elements.add(e);
        return e;
    }

    public TextFieldElement textField() {
        return textField(200);
    }

    public TextFieldElement textField(int width) {
        TextFieldElement e = new TextFieldElement(width);
        elements.add(e);
        return e;
    }

    public RowElement row() {
        RowElement e = new RowElement(defaultAlignment);
        elements.add(e);
        return e;
    }

    public RowElement row(Alignment alignment) {
        RowElement e = new RowElement(alignment);
        elements.add(e);
        return e;
    }

    public ViewFactory spacer(int height) {
        elements.add(new SpacerElement(height));
        return this;
    }

    public ViewFactory separator() {
        elements.add(new SeparatorElement());
        return this;
    }

    public ViewFactory add(ViewElement<?> element) {
        if (!elements.contains(element)) elements.add(element);
        return this;
    }

    public void build(Screen screen, Font font) {
        build(screen, font, 0, 0, screenWidth, screenHeight);
    }

    public void build(Screen screen, Font font, int originX, int originY, int maxWidth, int maxHeight) {
        this.screen = screen;
        this.font = font;
        textFields.clear();
        int boxWidth = Math.min(screenWidth, maxWidth);
        int boxHeight = Math.min(screenHeight, maxHeight);
        int contentY = originY + margin + paddingTop;
        int maxContentWidth = boxWidth - margin * 2 - paddingLeft - paddingRight;
        int availableHeight = boxHeight - margin * 2 - paddingTop - paddingBottom;
        int visibleCount = 0;
        int fixedHeight = 0;
        int flexCount = 0;

        int contentX;
        int contentWidth;
        if (fitContent) {
            contentWidth = computeNaturalContentWidth(font);
            if (contentWidth <= 0 || contentWidth > maxContentWidth) contentWidth = maxContentWidth;
            contentX = originX + margin + paddingLeft + (maxContentWidth - contentWidth) / 2;
        } else {
            contentWidth = maxContentWidth;
            contentX = originX + margin + paddingLeft;
        }

        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            visibleCount++;
            if (e.isFlexible()) {
                flexCount++;
            } else {
                fixedHeight += computeElementHeight(font, e, availableHeight) + gap;
            }
        }
        if (visibleCount > 0) fixedHeight -= gap;
        int flexHeight = flexCount > 0 ? Math.max(0, (availableHeight - fixedHeight - flexCount * gap) / flexCount) : 0;

        int totalHeight = fixedHeight + flexCount * (flexHeight + gap);
        totalContentHeight = totalHeight;
        int cursorY;
        if (centerVertical && totalHeight < availableHeight) {
            cursorY = contentY + (availableHeight - totalHeight) / 2;
        } else {
            cursorY = contentY;
        }

        int maxY = originY + boxHeight - margin - paddingBottom;

        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;

            int elemWidth = computeElementWidth(font, e, contentWidth);
            int elemHeight = e.isFlexible() ? flexHeight
                    : computeElementHeight(font, e, availableHeight);

            int x;
            Alignment align = e.getAlignment() != null ? e.getAlignment() : defaultAlignment;
            switch (align) {
                case CENTER:
                    x = contentX + (contentWidth - elemWidth) / 2;
                    break;
                case RIGHT:
                    x = contentX + contentWidth - elemWidth;
                    break;
                default:
                    x = contentX;
                    break;
            }

            if (cursorY + elemHeight > maxY) {
                break;
            }

            e.setComputedPosition(x, cursorY);
            e.setComputedSize(elemWidth, elemHeight);
            e.createWidgets(screen, font, this);

            cursorY += elemHeight + gap;
        }
    }

    public int getTotalContentHeight() {
        return totalContentHeight;
    }

    public static int computeElementWidth(Font fr, ViewElement<?> e, int containerWidth) {
        int elemWidth = e.getWidthPercent() >= 0
                ? containerWidth * e.getWidthPercent() / 100
                : e.getExplicitWidth() > 0 ? e.getExplicitWidth() : e.getPreferredWidth(fr);
        if (elemWidth <= 0) elemWidth = containerWidth;
        if (elemWidth > containerWidth) elemWidth = containerWidth;
        if (e.isStretchToContent()) elemWidth = containerWidth;
        return elemWidth;
    }

    public static int computeElementHeight(Font fr, ViewElement<?> e, int containerHeight) {
        int elemHeight = e.getHeightPercent() >= 0
                ? containerHeight * e.getHeightPercent() / 100
                : e.getPreferredHeight(fr);
        if (elemHeight < 0) elemHeight = 0;
        return elemHeight;
    }

    public int computeNaturalContentWidth(Font fr) {
        int maxWidth = 0;
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.getWidthPercent() >= 0) return -1;
            int w = e.getPreferredWidth(fr);
            if (w > maxWidth) maxWidth = w;
        }
        return maxWidth;
    }

    public int computeNaturalContentHeight(Font fr) {
        int total = 0;
        int count = 0;
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.isFlexible()) return -1;
            total += computeElementHeight(fr, e, screenHeight) + gap;
            count++;
        }
        if (count > 0) total -= gap;
        return total;
    }

    public int getContentMinX() {
        int minX = Integer.MAX_VALUE;
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.computedX < minX) minX = e.computedX;
        }
        return minX == Integer.MAX_VALUE ? 0 : minX;
    }

    public int getContentMaxX() {
        int maxX = Integer.MIN_VALUE;
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.computedX + e.computedWidth > maxX) maxX = e.computedX + e.computedWidth;
        }
        return maxX == Integer.MIN_VALUE ? 0 : maxX;
    }

    public int getContentMinY() {
        int minY = Integer.MAX_VALUE;
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.computedY < minY) minY = e.computedY;
        }
        return minY == Integer.MAX_VALUE ? 0 : minY;
    }

    public int getContentMaxY() {
        int maxY = Integer.MIN_VALUE;
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.computedY + e.computedHeight > maxY) maxY = e.computedY + e.computedHeight;
        }
        return maxY == Integer.MIN_VALUE ? 0 : maxY;
    }

    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        draw(g, font, mouseX, mouseY, partialTicks, 0, 0, screenWidth, screenHeight);
    }

    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks, int originX, int originY, int maxWidth, int maxHeight) {
        int boxWidth = Math.min(screenWidth, maxWidth);
        int boxHeight = Math.min(screenHeight, maxHeight);
        if (panelColor != 0) {
            int px = originX + margin;
            int py;
            int pw = boxWidth - margin * 2;
            int ph;

            if (fitContent) {
                int firstX = getContentMinX() - 4;
                int lastX = getContentMaxX() + 4;
                if (firstX < originX + margin) firstX = originX + margin;
                px = firstX;
                pw = lastX - firstX;
                int firstY = getContentMinY() - 4;
                int lastY = getContentMaxY() + 4;
                py = firstY;
                ph = lastY - firstY;
            } else {
                py = originY + margin;
                ph = boxHeight - margin * 2;
            }

            g.fill(px, py, px + pw, py + ph, panelColor);
            if (panelBorderColor != 0) {
                g.fill(px, py, px + pw, py + 1, panelBorderColor);
                g.fill(px, py + ph - 1, px + pw, py + ph - 1 + 1, panelBorderColor);
                g.fill(px, py, px + 1, py + ph, panelBorderColor);
                g.fill(px + pw - 1, py, px + pw - 1 + 1, py + ph, panelBorderColor);
            }
        }

        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            e.draw(g, font, mouseX, mouseY, partialTicks);
        }
    }

    public boolean actionPerformed(AbstractWidget button) {
        for (ViewElement<?> e : elements) {
            if (e.actionPerformed(button)) return true;
        }
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (EditBox tf : textFields) {
            tf.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        }
        return false;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.keyTyped(typedChar, keyCode)) return true;
        }
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.mouseReleased(mouseX, mouseY, state)) return true;
        }
        return false;
    }

    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) return true;
        }
        return false;
    }

    public boolean handleMouseInput(int dWheel) {
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            if (e.handleMouseInput(dWheel)) return true;
        }
        return false;
    }

    public void updateScreen() {
        for (ViewElement<?> e : elements) {
            if (!e.isVisible()) continue;
            e.tick();
            e.updateCursorCounter();
        }
    }

    public void removeAll() {
        elements.clear();
        textFields.clear();
    }

    public void removeElement(ViewElement<?> element) {
        elements.remove(element);
    }

    public void addTextField(EditBox tf) {
        textFields.add(tf);
    }

    public List<EditBox> getTextFields() {
        return textFields;
    }

    public List<ViewElement<?>> getElements() {
        return elements;
    }

    public List<AbstractWidget> getWidgets() {
        List<AbstractWidget> result = new ArrayList<>();
        for (ViewElement<?> e : elements) {
            AbstractWidget w = e.getWidget();
            if (w != null) result.add(w);
        }
        return result;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getContentX() {
        return margin + paddingLeft;
    }

    public int getContentY() {
        return margin + paddingTop;
    }

    public int getContentWidth() {
        return screenWidth - margin * 2 - paddingLeft - paddingRight;
    }

    public Screen getScreen() {
        return screen;
    }

    public Font getFont() {
        return font;
    }
}

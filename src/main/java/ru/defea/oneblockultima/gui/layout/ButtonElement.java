package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.defea.oneblockultima.gui.layout.ViewFactory;

import javax.annotation.Nonnull;
import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ButtonElement<T extends ButtonElement<T>> extends ViewElement<T> {
    public static final int BUTTON_PADDING = 16;
    public static final int BUTTON_HEIGHT_PADDING = 8;

    private final int id;
    protected String text;
    protected int width = 0;
    protected int height = 0;
    protected boolean enabled = true;
    protected DrawableButton widget;
    private short borderSize = 1;
    private int borderColor = GRAY_COLOR_2;
    private int textColor = WHITE_COLOR_1;
    private int textColorHovered = WHITE_COLOR_1;
    private Runnable onPressAction;
    private ViewFactory owningFactory;

    private String sideLabel;
    private LabelPosition sideLabelPosition = LabelPosition.RIGHT;
    private int sideLabelColor = WHITE_COLOR_1;
    private int sideLabelGap = 6;

    public enum LabelPosition {
        LEFT, RIGHT
    }

    public ButtonElement(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public ButtonElement(String text) {
        this.id = -1;
        this.text = text;
    }

    public ButtonElement(int id, String text, int textColor) {
        this.id = id;
        this.text = text;
        this.textColor = textColor;
    }

    public ButtonElement(String text, int textColor) {
        this.id = -1;
        this.text = text;
        this.textColor = textColor;
    }

    public ButtonElement(int id, String text, short borderSize) {
        this.id = id;
        this.text = text;
        this.borderSize = borderSize;
    }

    public ButtonElement(String text, short borderSize) {
        this.id = -1;
        this.text = text;
        this.borderSize = borderSize;
    }

    public ButtonElement(int id, String text, int textColor, short borderSize) {
        this.id = id;
        this.text = text;
        this.textColor = textColor;
        this.borderSize = borderSize;
    }

    public ButtonElement(String text, int textColor, short borderSize) {
        this.id = -1;
        this.text = text;
        this.textColor = textColor;
        this.borderSize = borderSize;
    }

    public T width(int width) {
        this.width = width;
        return self();
    }

    public T height(int height) {
        this.height = height;
        return self();
    }

    public T enabled(boolean enabled) {
        this.enabled = enabled;
        if (widget != null) widget.active = enabled;
        return self();
    }

    @Override
    public T visible(boolean visible) {
        super.visible(visible);
        if (widget != null) widget.visible = visible;
        return self();
    }

    public T text(String text) {
        this.text = text;
        if (widget != null) widget.setMessage(Component.literal(text == null ? "" : text));
        return self();
    }

    public T onPress(Runnable action) {
        this.onPressAction = action;
        return self();
    }

    public T sideLabel(String label) {
        this.sideLabel = label;
        return self();
    }

    public T sideLabelPosition(LabelPosition position) {
        this.sideLabelPosition = position;
        return self();
    }

    public T sideLabelColor(int color) {
        this.sideLabelColor = color;
        return self();
    }

    public T sideLabelGap(int gap) {
        this.sideLabelGap = gap;
        return self();
    }

    public int getId() {
        return id;
    }

    protected Font getFont() {
        return Minecraft.getInstance().font;
    }

    private int autoWidth(Font fr) {
        int textW = fr.width(text != null ? text : "");
        if (sideLabel != null && !sideLabel.isEmpty()) {
            textW += sideLabelGap + fr.width(sideLabel);
        }
        return textW + BUTTON_PADDING;
    }

    private int autoHeight(Font fr) {
        return fr.lineHeight + BUTTON_HEIGHT_PADDING;
    }

    @Override
    public int getPreferredWidth() {
        return getPreferredWidth(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredWidth(Font fr) {
        if (widthPercent >= 0) return 0;
        if (width > 0) return width;
        return autoWidth(fr);
    }

    @Override
    public int getPreferredHeight() {
        return height > 0 ? height : 20;
    }

    @Override
    public int getPreferredHeight(Font fr) {
        if (heightPercent >= 0) return 0;
        if (height > 0) return height;
        return autoHeight(fr);
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setTextColorHovered(int color) {
        this.textColorHovered = color;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
    }

    public void setBorderSize(short size) {
        this.borderSize = size;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getTextColorHovered() {
        return textColorHovered;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public short getBorderSize() {
        return borderSize;
    }

    protected int widgetFillColor(boolean hovered) {
        if (!enabled) return DISABLED_BUTTON_FILL;
        return hovered ? GRAY_COLOR_6 : DARK_GRAY_COLOR_1;
    }

    protected int widgetTextColor(boolean hovered) {
        if (!enabled) return DISABLED_BUTTON_TEXT;
        return hovered ? textColorHovered : textColor;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
        int btnWidth = width > 0 ? width : computedWidth;
        int btnHeight = height > 0 ? height : computedHeight;
        widget = new DrawableButton(this, factory, btnWidth, btnHeight);
        widget.visible = visible;
        widget.active = enabled;
        this.owningFactory = factory;
        if (screen instanceof ru.defea.oneblockultima.gui.ModScreen)
        {
            ((ru.defea.oneblockultima.gui.ModScreen) screen).registerRenderableWidget(widget);
        }
        else
        {
            screen.renderables.add(widget);
            ((java.util.List<net.minecraft.client.gui.components.events.GuiEventListener>) (java.util.List<?>) screen.children()).add(widget);
        }
    }

    @Override
    public AbstractWidget getWidget() {
        return widget;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
    }

    public static class DrawableButton extends AbstractWidget {
        final ButtonElement<?> owner;
        final ViewFactory factory;

        DrawableButton(ButtonElement<?> owner, ViewFactory factory, int width, int height) {
            super(owner.computedX, owner.computedY, width, height, Component.literal(owner.text == null ? "" : owner.text));
            this.owner = owner;
            this.factory = factory;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() &&
                    mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            Font font = Minecraft.getInstance().font;
            boolean hasLabel = owner.sideLabel != null && !owner.sideLabel.isEmpty();
            int boxW = owner.width > 0 ? owner.width : BUTTON_PADDING;
            int actualBoxW = hasLabel ? Math.min(boxW, this.width) : this.width;
            int boxX = hasLabel && owner.sideLabelPosition == LabelPosition.LEFT ? this.getX() + (this.width - actualBoxW) : this.getX();

            g.fill(boxX, this.getY(), boxX + actualBoxW, this.getY() + this.height, owner.widgetFillColor(this.isHovered));
            int bc = owner.borderColor;
            int bs = owner.borderSize;
            g.fill(boxX, this.getY(), boxX + actualBoxW, this.getY() + bs, bc);
            g.fill(boxX, this.getY() + this.height - bs, boxX + actualBoxW, this.getY() + this.height - bs + bs, bc);
            g.fill(boxX, this.getY(), boxX + bs, this.getY() + this.height, bc);
            g.fill(boxX + actualBoxW - bs, this.getY(), boxX + actualBoxW - bs + bs, this.getY() + this.height, bc);

            String msg = owner.text == null ? "" : owner.text;
            int textW = font.width(msg);
            int tx = boxX + (actualBoxW - textW) / 2;
            int ty = this.getY() + (this.height - font.lineHeight) / 2;
            g.drawString(font, msg, tx, ty, owner.widgetTextColor(this.isHovered), false);

            if (hasLabel) {
                int labelW = font.width(owner.sideLabel);
                int labelX = owner.sideLabelPosition == LabelPosition.RIGHT
                        ? boxX + actualBoxW + owner.sideLabelGap
                        : boxX - owner.sideLabelGap - labelW;
                int labelY = this.getY() + (this.height - font.lineHeight) / 2;
                g.drawString(font, owner.sideLabel, labelX, labelY,
                        owner.enabled ? owner.sideLabelColor : DISABLED_BUTTON_TEXT, false);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (owner.onPressAction != null) owner.onPressAction.run();
            else if (factory != null) factory.actionPerformed(this);
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput narration) {
            narration.add(NarratedElementType.TITLE, Component.literal(owner.text == null ? "" : owner.text));
        }
    }
}

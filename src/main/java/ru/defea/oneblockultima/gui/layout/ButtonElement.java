package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

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
    protected GuiButton guiButton;
    private short borderSize = 1;
    private int borderColor = GRAY_COLOR_2;
    private int textColor = WHITE_COLOR_1;
    private int textColorHovered = WHITE_COLOR_1;

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
        if (guiButton != null) guiButton.enabled = enabled;
        return self();
    }

    @Override
    public T visible(boolean visible) {
        super.visible(visible);
        if (guiButton != null) guiButton.visible = visible;
        return self();
    }

    public T text(String text) {
        this.text = text;
        if (guiButton != null) guiButton.displayString = text;
        return self();
    }

    public int getId() {
        return id;
    }

    private int autoWidth(FontRenderer fr) {
        int textW = fr.getStringWidth(text != null ? text : "");
        return textW + BUTTON_PADDING;
    }

    private int autoHeight(FontRenderer fr) {
        return fr.FONT_HEIGHT + BUTTON_HEIGHT_PADDING;
    }

    @Override
    public int getPreferredWidth() {
        return width;
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        if (widthPercent >= 0) return 0;
        if (width > 0) return width;
        return autoWidth(fr);
    }

    @Override
    public int getPreferredHeight() {
        return height > 0 ? height : 20;
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
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

    public int getTextColor() { return textColor; }

    public int getTextColorHovered() { return textColorHovered; }

    public int getBorderColor() { return borderColor; }

    public short getBorderSize() { return borderSize; }

    protected int widgetFillColor(boolean hovered) {
        if (!enabled) return DISABLED_BUTTON_FILL;
        return hovered ? GRAY_COLOR_6 : DARK_GRAY_COLOR_1;
    }

    protected int widgetTextColor(boolean hovered) {
        if (!enabled) return DISABLED_BUTTON_TEXT;
        return hovered ? textColorHovered : textColor;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        int btnWidth = width > 0 ? width : computedWidth;
        int btnHeight = height > 0 ? height : computedHeight;
        guiButton = new GuiButton(id, computedX, computedY, btnWidth, btnHeight, text) {
            @Override
            public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY) {
                if (this.visible) {
                    this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                            mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                    drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, widgetFillColor(hovered));
                    drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + borderSize, borderColor);
                    drawRect(this.xPosition, this.yPosition + this.height - borderSize, this.xPosition + this.width, this.yPosition + this.height, borderColor);
                    drawRect(this.xPosition, this.yPosition, this.xPosition + borderSize, this.yPosition + this.height, borderColor);
                    drawRect(this.xPosition + this.width - borderSize, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, borderColor);

                    this.drawCenteredString(mc.fontRendererObj, this.displayString,
                            this.xPosition + this.width / 2,
                            this.yPosition + (this.height - mc.fontRendererObj.FONT_HEIGHT) / 2,
                            widgetTextColor(hovered));
                }
            }
        };
        guiButton.enabled = enabled;
        buttonList.add(guiButton);
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
    }
}

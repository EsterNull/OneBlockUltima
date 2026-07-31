package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

import static ru.defea.oneblockultima.Constants.*;
import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_1;

public class ButtonElement extends ViewElement {
    private static final int BUTTON_PADDING = 16;
    private static final int BUTTON_HEIGHT_PADDING = 8;

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

    public ButtonElement width(int width) {
        this.width = width;
        return this;
    }

    public ButtonElement height(int height) {
        this.height = height;
        return this;
    }

    public ButtonElement enabled(boolean enabled) {
        this.enabled = enabled;
        if (guiButton != null) guiButton.enabled = enabled;
        return this;
    }

    @Override
    public ButtonElement visible(boolean visible) {
        super.visible(visible);
        if (guiButton != null) guiButton.visible = visible;
        return this;
    }

    public ButtonElement text(String text) {
        this.text = text;
        if (guiButton != null) guiButton.displayString = text;
        return this;
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
        return hovered ? GRAY_COLOR_6 : DARK_GRAY_COLOR_1;
    }

    protected int widgetTextColor(boolean hovered) {
        return hovered ? textColorHovered : textColor;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        int btnWidth = width > 0 ? width : computedWidth;
        int btnHeight = height > 0 ? height : computedHeight;
        guiButton = new GuiButton(id, computedX, computedY, btnWidth, btnHeight, text) {
            @Override
            public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                if (this.visible) {
                    this.hovered = mouseX >= this.x && mouseY >= this.y &&
                            mouseX < this.x + this.width && mouseY < this.y + this.height;

                    drawRect(this.x, this.y, this.x + this.width, this.y + this.height, widgetFillColor(hovered));
                    drawRect(this.x, this.y, this.x + this.width, this.y + borderSize, borderColor);
                    drawRect(this.x, this.y + this.height - borderSize, this.x + this.width, this.y + this.height, borderColor);
                    drawRect(this.x, this.y, this.x + borderSize, this.y + this.height, borderColor);
                    drawRect(this.x + this.width - borderSize, this.y, this.x + this.width, this.y + this.height, borderColor);

                    this.drawCenteredString(fontRenderer, this.displayString,
                            this.x + this.width / 2,
                            this.y + (this.height - fontRenderer.FONT_HEIGHT) / 2,
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

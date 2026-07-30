package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

public class ButtonElement extends ViewElement {
    private static final int BUTTON_PADDING = 16;
    private static final int BUTTON_HEIGHT_PADDING = 8;

    private final int id;
    private String text;
    private int width = 0;
    private int height = 0;
    private boolean enabled = true;
    private GuiButton guiButton;

    public ButtonElement(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public ButtonElement(String text) {
        this.id = -1;
        this.text = text;
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

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        int btnWidth = width > 0 ? width : computedWidth;
        int btnHeight = height > 0 ? height : computedHeight;
        guiButton = new GuiButton(id, computedX, computedY, btnWidth, btnHeight, text);
        guiButton.enabled = enabled;
        buttonList.add(guiButton);
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
    }
}

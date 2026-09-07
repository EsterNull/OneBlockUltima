package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import ru.defea.oneblockultima.gui.ModScreen;

import java.util.List;

import static ru.defea.oneblockultima.Constants.WHITE_COLOR_2;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TextFieldElement extends ViewElement<TextFieldElement> {
    private int width;
    private EditBox textField;
    private String text = "";
    private boolean focused = false;
    private boolean backgroundDrawing = true;
    private boolean enabled = true;
    private int textColor = WHITE_COLOR_2;
    private int maxStringLength = Integer.MAX_VALUE;
    private boolean fitToText = false;
    private static final int FIT_TEXT_PADDING = 8;
    private static final int MAX_FIT_WIDTH = 440;

    public TextFieldElement(int width) {
        this.width = width;
    }

    public TextFieldElement width(int width) {
        this.width = width;
        return this;
    }

    public TextFieldElement text(String text) {
        this.text = text;
        if (textField != null) textField.setValue(text);
        return this;
    }

    public TextFieldElement focused(boolean focused) {
        this.focused = focused;
        if (textField != null) textField.setFocused(focused);
        return this;
    }

    public TextFieldElement maxLength(int max) {
        this.maxStringLength = max;
        if (textField != null) textField.setMaxLength(max);
        return this;
    }

    public TextFieldElement fitToText() {
        this.fitToText = true;
        return this;
    }

    public TextFieldElement enableBackgroundDrawing(boolean draw) {
        this.backgroundDrawing = draw;
        if (textField != null) textField.setBordered(draw);
        return this;
    }

    public TextFieldElement enabled(boolean enabled) {
        this.enabled = enabled;
        if (textField != null) textField.setEditable(enabled);
        if (!enabled) textColor(WHITE_COLOR_2);
        return this;
    }

    public TextFieldElement textColor(int color) {
        this.textColor = color;
        if (textField != null) textField.setTextColor(color);
        return this;
    }

    public TextFieldElement height(int height) {
        return this;
    }

    public EditBox getTextField() {
        return textField;
    }

    public String getText() {
        return textField != null ? textField.getValue() : text;
    }

    public void setText(String text) {
        this.text = text;
        if (textField != null) textField.setValue(text);
    }

    public boolean isFocused() {
        return textField != null && textField.isFocused();
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
        int fieldWidth = width > 0 ? width : computedWidth;
        if (fitToText) fieldWidth = fitWidth(font, fieldWidth);
        int fieldHeight = getFieldHeight();
        textField = new EditBox(font, computedX, computedY, fieldWidth, fieldHeight, Component.literal(""));
        textField.setMaxLength(maxStringLength);
        textField.setValue(text);
        textField.setFocused(focused);
        textField.setBordered(backgroundDrawing);
        textField.setEditable(enabled);
        textField.setTextColor(textColor);
        if (screen instanceof ModScreen && enabled)
        {
            ((ModScreen) screen).registerRenderableWidget(textField);
        }
        else
        {
            screen.renderables.add(textField);
        }
    }

    @Override
    public AbstractWidget getWidget() {
        return textField;
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    @Override
    public void updateCursorCounter() {
    }

    @Override
    public int getPreferredWidth() {
        return getPreferredWidth(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredWidth(Font font) {
        if (!fitToText) return width;
        return fitWidth(font, width);
    }

    private int fitWidth(Font font, int baseWidth) {
        if (font == null || text.isEmpty()) return baseWidth;
        return Math.max(baseWidth, Math.min(font.width(text) + FIT_TEXT_PADDING, MAX_FIT_WIDTH));
    }

    @Override
    public int getPreferredHeight() {
        return getFieldHeight();
    }

    private static int getFieldHeight() {
        Minecraft mc = Minecraft.getInstance();
        int fontHeight = mc.font != null ? mc.font.lineHeight : 9;
        return fontHeight + 4;
    }
}

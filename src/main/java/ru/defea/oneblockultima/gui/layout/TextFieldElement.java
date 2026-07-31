package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;

import java.util.List;

import static ru.defea.oneblockultima.Constants.WHITE_COLOR_2;

public class TextFieldElement extends ViewElement {
    private final int width;
    private static final int HEIGHT = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 4;
    private GuiTextField textField;
    private String text = "";
    private boolean focused = false;
    private boolean backgroundDrawing = true;
    private boolean enabled = true;
    private int textColor = WHITE_COLOR_2;
    private int maxStringLength = 256;

    public TextFieldElement(int width) {
        this.width = width;
    }

    public TextFieldElement text(String text) {
        this.text = text;
        if (textField != null) textField.setText(text);
        return this;
    }

    public TextFieldElement focused(boolean focused) {
        this.focused = focused;
        if (textField != null) textField.setFocused(focused);
        return this;
    }

    public TextFieldElement maxLength(int max) {
        this.maxStringLength = max;
        if (textField != null) textField.setMaxStringLength(max);
        return this;
    }

    public TextFieldElement enableBackgroundDrawing(boolean draw) {
        this.backgroundDrawing = draw;
        if (textField != null) textField.setEnableBackgroundDrawing(draw);
        return this;
    }

    public TextFieldElement enabled(boolean enabled) {
        this.enabled = enabled;
        if (textField != null) textField.setEnabled(enabled);
        if (!enabled) textColor(WHITE_COLOR_2);
        return this;
    }

    public TextFieldElement textColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextFieldElement height(int height) {
        return this;
    }

    public TextFieldElement widthPercent(int percent) {
        super.widthPercent(percent);
        return this;
    }

    public TextFieldElement heightPercent(int percent) {
        super.heightPercent(percent);
        return this;
    }

    public GuiTextField getTextField() {
        return textField;
    }

    public String getText() {
        return textField != null ? textField.getText() : text;
    }

    public void setText(String text) {
        this.text = text;
        if (textField != null) textField.setText(text);
    }

    public boolean isFocused() {
        return textField != null && textField.isFocused();
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        int id = factory.getTextFields().size();
        int fieldWidth = width > 0 ? width : computedWidth;
        textField = new GuiTextField(id, fontRenderer, computedX, computedY, fieldWidth, HEIGHT);
        textField.setText(text);
        textField.setFocused(focused);
        textField.setMaxStringLength(maxStringLength);
        textField.setEnableBackgroundDrawing(backgroundDrawing);
        textField.setEnabled(enabled);
        textField.setTextColor(textColor);
        factory.addTextField(textField);
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (textField != null) textField.drawTextBox();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (textField != null && enabled) {
            textField.mouseClicked(mouseX, mouseY, mouseButton);
            return textField.isFocused();
        }
        return false;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        if (textField != null && textField.isFocused() && enabled) {
            return textField.textboxKeyTyped(typedChar, keyCode);
        }
        return false;
    }

    @Override
    public void updateCursorCounter() {
        if (textField != null) textField.updateCursorCounter();
    }

    @Override
    public int getPreferredWidth() {
        return width;
    }

    @Override
    public int getPreferredHeight() {
        return HEIGHT;
    }
}

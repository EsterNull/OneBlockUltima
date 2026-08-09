package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class InlineClickableElement extends ViewElement<InlineClickableElement> {
    public interface ClickHandler {
        boolean onClick(int mouseX, int mouseY, int mouseButton);
    }

    private final int bgColor;
    private final int hoverColor;
    private int textColor = WHITE_COLOR_1;
    private String text;
    private final ClickHandler clickHandler;

    public InlineClickableElement(String text, int bgColor, int hoverColor, ClickHandler handler) {
        this.text = text;
        this.bgColor = bgColor;
        this.hoverColor = hoverColor;
        this.clickHandler = handler;
    }

    public InlineClickableElement text(String text) {
        this.text = text;
        return this;
    }

    public InlineClickableElement textColor(int color) {
        this.textColor = color;
        return this;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = mouseX >= computedX && mouseX <= computedX + computedWidth &&
                mouseY >= computedY && mouseY <= computedY + computedHeight;
        int bg = hovered ? hoverColor : bgColor;
        Gui.drawRect(computedX, computedY, computedX + computedWidth, computedY + computedHeight, bg);
        if (text != null && !text.isEmpty()) {
            float tx = computedX + (computedWidth - fr.getStringWidth(text)) / 2.0f;
            float ty = computedY + (computedHeight - 8) / 2.0f;
            fr.drawStringWithShadow(text, tx, ty, textColor);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean hit = mouseX >= computedX && mouseX <= computedX + computedWidth &&
                      mouseY >= computedY && mouseY <= computedY + computedHeight;
        if (!hit) return false;
        if (clickHandler != null) return clickHandler.onClick(mouseX, mouseY, mouseButton);
        return false;
    }

    @Override
    public int getPreferredWidth() {
        return 30;
    }

    @Override
    public int getPreferredHeight() {
        return 14;
    }
}

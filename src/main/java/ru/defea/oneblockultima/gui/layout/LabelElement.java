package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

public class LabelElement extends ViewElement {
    private String text;
    private int color = 0xFFFFFF;
    private boolean centered = false;
    private float scale = 1.0f;

    public LabelElement(String text) {
        this.text = text;
    }

    public LabelElement color(int color) {
        this.color = color;
        return this;
    }

    public LabelElement centered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public LabelElement text(String text) {
        this.text = text;
        return this;
    }

    @Override
    public LabelElement width(int width) {
        super.width(width);
        return this;
    }

    public LabelElement height(int height) {
        return this;
    }

    public LabelElement scale(float scale) {
        this.scale = scale;
        return this;
    }

    public String getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (text == null || text.isEmpty()) return;
        if (scale != 1.0f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(computedX, computedY, 0);
            GlStateManager.scale(scale, scale, 1.0f);
            int drawX = centered ? (int)((computedWidth / scale - fr.getStringWidth(text)) / 2) : 0;
            fr.drawString(text, drawX, 0, color);
            GlStateManager.popMatrix();
        } else if (centered) {
            fr.drawString(text, computedX + (computedWidth - fr.getStringWidth(text)) / 2, computedY, color);
        } else {
            fr.drawString(text, computedX, computedY, color);
        }
    }

    @Override
    public int getPreferredWidth() {
        if (explicitWidth > 0) return explicitWidth;
        return 0;
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        if (explicitWidth > 0) return explicitWidth;
        if (text == null || text.isEmpty()) return 0;
        return fr.getStringWidth(text);
    }

    @Override
    public int getPreferredHeight() {
        return 10;
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
        return fr.FONT_HEIGHT + 2;
    }
}

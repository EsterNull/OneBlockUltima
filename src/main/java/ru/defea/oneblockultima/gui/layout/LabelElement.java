package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class LabelElement extends ViewElement<LabelElement> {
    private String text;
    private int color = WHITE_COLOR_1;
    private boolean centered = false;
    private float scale = 1.0f;

    public LabelElement(String text) {
        this.text = text;
    }

    public LabelElement color(int color) {
        this.color = color;
        return this;
    }

    public LabelElement centered() {
        this.centered = true;
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
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (text == null || text.isEmpty()) return;
        if (scale != 1.0f) {
            g.pose().pushPose();
            g.pose().translate(computedX, computedY, 0);
            int s = (int) Math.round(scale);
            if (s < 1) s = 1;
            g.pose().scale(s, s, 1.0f);
            Font f = Minecraft.getInstance().font;
            int drawX = centered ? (int) ((computedWidth / s - f.width(text)) / 2) : 0;
            g.drawString(f, text, drawX, 0, color);
            g.pose().popPose();
        } else if (centered) {
            g.drawCenteredString(font, text, computedX + computedWidth / 2, computedY, color);
        } else {
            g.drawString(font, text, computedX, computedY, color);
        }
    }

    @Override
    public int getPreferredWidth() {
        return getPreferredWidth(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredWidth(Font font) {
        if (explicitWidth > 0) return explicitWidth;
        if (text == null || text.isEmpty()) return 0;
        return font.width(text);
    }

    @Override
    public int getPreferredHeight() {
        return getPreferredHeight(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredHeight(Font font) {
        return font.lineHeight + 2;
    }
}

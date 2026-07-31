package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_1;

public class SeparatorElement extends ViewElement {
    private int color = DARK_GRAY_COLOR_1;
    private int height = 2;

    public SeparatorElement color(int color) {
        this.color = color;
        return this;
    }

    public SeparatorElement height(int height) {
        this.height = height;
        return this;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(computedX, computedY, computedX + computedWidth, computedY + height, color);
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return height;
    }
}

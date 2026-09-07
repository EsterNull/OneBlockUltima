package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class SeparatorElement extends ViewElement<SeparatorElement> {
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
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        g.fill(computedX, computedY, computedX + computedWidth, computedY + height, color);
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

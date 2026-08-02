package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class SpacerElement extends ViewElement<SpacerElement> {
    private final int width;
    private final int height;

    public SpacerElement(int height) {
        this.width = 0;
        this.height = height;
    }

    public SpacerElement(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public int getPreferredWidth() {
        return width;
    }

    @Override
    public int getPreferredHeight() {
        return height;
    }
}

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TextureElement extends ViewElement<TextureElement> {
    private final ResourceLocation texture;
    private final int width;
    private final int height;
    private final float u;
    private final float v;

    public TextureElement(ResourceLocation texture, int textureWidth, int textureHeight) {
        this.texture = texture;
        this.width = textureWidth;
        this.height = textureHeight;
        this.u = 0;
        this.v = 0;
    }

    public TextureElement(ResourceLocation texture, int textureWidth, int textureHeight,
                          float u, float v) {
        this.texture = texture;
        this.width = textureWidth;
        this.height = textureHeight;
        this.u = u;
        this.v = v;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (texture == null) return;
        g.blit(texture, computedX, computedY, computedWidth, computedHeight,
                u, v, width, height, width, height);
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

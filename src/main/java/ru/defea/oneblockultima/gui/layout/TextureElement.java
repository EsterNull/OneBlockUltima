package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

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
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (texture == null) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(
            computedX, computedY, u, v,
            computedWidth, computedHeight, width, height
        );
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

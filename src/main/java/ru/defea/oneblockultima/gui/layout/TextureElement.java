package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class TextureElement extends ViewElement {
    private ResourceLocation texture;
    private int textureWidth;
    private int textureHeight;
    private float u;
    private float v;
    private float uWidth;
    private float vHeight;

    public TextureElement(ResourceLocation texture, int textureWidth, int textureHeight,
                          float u, float v, float uWidth, float vHeight) {
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.u = u;
        this.v = v;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
    }

    public TextureElement size(int width, int height) {
        setComputedSize(width, height);
        return this;
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
            computedWidth, computedHeight, textureWidth, textureHeight
        );
    }

    @Override
    public int getPreferredWidth() {
        return computedWidth > 0 ? computedWidth : textureWidth;
    }

    @Override
    public int getPreferredHeight() {
        return computedHeight > 0 ? computedHeight : textureHeight;
    }
}

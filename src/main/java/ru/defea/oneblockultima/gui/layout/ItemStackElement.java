package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ItemStackElement extends ViewElement<ItemStackElement> {
    private ItemStack stack;
    private int size = 16;

    public ItemStackElement(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStackElement size(int size) {
        this.size = size;
        return this;
    }

    public ItemStackElement stack(ItemStack stack) {
        this.stack = stack;
        return this;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (stack == null || stack.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        try
        {
            float scale = size / 16.0F;
            GlStateManager.translate(computedX + computedWidth / 2.0F, computedY + computedHeight / 2.0F, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            GlStateManager.translate(-8.0F, -8.0F, 0.0F);
            GlStateManager.enableDepth();
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableDepth();
        }
        finally
        {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public int getPreferredWidth() {
        return size;
    }

    @Override
    public int getPreferredHeight() {
        return size;
    }
}

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemStackElement extends ViewElement {
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
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, computedX, computedY);
        RenderHelper.disableStandardItemLighting();
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

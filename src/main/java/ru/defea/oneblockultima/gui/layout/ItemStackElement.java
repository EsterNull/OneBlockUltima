package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

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
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (stack == null || stack.isEmpty()) return;
        g.pose().pushPose();
        try {
            float scale = size / 16.0F;
            g.pose().translate(computedX + computedWidth / 2.0F, computedY + computedHeight / 2.0F, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.pose().translate(-8.0F, -8.0F, 0.0F);
            g.renderItem(stack, 0, 0);
        } finally {
            g.pose().popPose();
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

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import ru.defea.oneblockultima.util.RenderUtil;

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
        if (stack == null) return;
        GL11.glPushMatrix();
        try
        {
            float scale = size / 16.0F;
            GL11.glTranslatef(computedX + computedWidth / 2.0F, computedY + computedHeight / 2.0F, 0.0F);
            GL11.glScalef(scale, scale, 1.0F);
            GL11.glTranslatef(-8.0F, -8.0F, 0.0F);
            RenderUtil.renderItemIntoGUI(fr, stack, 0, 0);
        }
        finally
        {
            GL11.glPopMatrix();
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

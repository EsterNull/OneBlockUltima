package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.init.Blocks;
import ru.defea.oneblockultima.util.RenderUtil;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class BlockElement extends ViewElement<BlockElement> {
    private final net.minecraft.block.Block block;
    private int meta;
    private int size = 16;

    public BlockElement(net.minecraft.block.Block block) {
        this(block, 0);
    }

    public BlockElement(net.minecraft.block.Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    public static BlockElement fromRegistry(String registry) {
        net.minecraft.block.Block block = (net.minecraft.block.Block) net.minecraft.block.Block.blockRegistry.getObject(registry);
        if (block == null) return null;
        return new BlockElement(block, 0);
    }

    public static BlockElement fromItemStack(net.minecraft.item.ItemStack stack) {
        if (stack == null) return null;
        net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(stack.getItem());
        if (block == Blocks.air) return null;
        return new BlockElement(block, stack.getMetadata());
    }

    public BlockElement size(int size) {
        this.size = size;
        return this;
    }

    public BlockElement meta(int meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (block == null) return;
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(block, 1, meta);
        if (stack.getItem() == null) return;
        RenderUtil.renderItemAndEffectIntoGUI(fr, stack, computedX, computedY);
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

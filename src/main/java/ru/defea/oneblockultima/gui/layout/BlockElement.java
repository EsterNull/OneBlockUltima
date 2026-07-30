package ru.defea.oneblockultima.gui.layout;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.List;

public class BlockElement extends ViewElement {
    private IBlockState blockState;
    private int size = 16;

    public BlockElement(IBlockState state) {
        this.blockState = state;
    }

    public static BlockElement fromRegistry(String registry) {
        ResourceLocation rl = new ResourceLocation(registry);
        net.minecraft.block.Block block = ForgeRegistries.BLOCKS.getValue(rl);
        if (block == null) return null;
        return new BlockElement(block.getDefaultState());
    }

    public static BlockElement fromItemStack(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return null;
        net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(stack.getItem());
        if (block == null || block == Blocks.AIR) return null;
        return new BlockElement(block.getStateFromMeta(stack.getMetadata()));
    }

    public BlockElement size(int size) {
        this.size = size;
        return this;
    }

    public BlockElement state(IBlockState state) {
        this.blockState = state;
        return this;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (blockState == null) return;
        ModelUtil.renderBlockModelToGUI(blockState, computedX, computedY, size);
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

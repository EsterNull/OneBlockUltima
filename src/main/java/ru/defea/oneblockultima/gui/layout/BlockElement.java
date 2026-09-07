package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class BlockElement extends ViewElement<BlockElement> {
    private BlockState blockState;
    private int size = 16;

    public BlockElement(BlockState state) {
        this.blockState = state;
    }

    public static BlockElement fromRegistry(String registry) {
        ResourceLocation rl = ResourceLocation.parse(registry);
        Block block = BuiltInRegistries.BLOCK.get(rl);
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) return null;
        return new BlockElement(block.defaultBlockState());
    }

    public static BlockElement fromItemStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Block block = Block.byItem(stack.getItem());
        if (block == net.minecraft.world.level.block.Blocks.AIR) return null;
        return new BlockElement(block.defaultBlockState());
    }

    public BlockElement size(int size) {
        this.size = size;
        return this;
    }

    public BlockElement state(BlockState state) {
        this.blockState = state;
        return this;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (blockState == null) return;
        ModelUtil.renderBlockModelToGUI(g, blockState, computedX, computedY, size);
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

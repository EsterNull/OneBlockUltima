package ru.defea.oneblockultima.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCompressedBase;

import javax.annotation.Nonnull;

public class ItemBlockCompressed extends BlockItem {
    private final int level;

    public ItemBlockCompressed(Block block, int level)
    {
        super(block, new Item.Properties());
        this.level = level;
    }

    public int getLevel()
    {
        return level;
    }

    @Override
    protected BlockState getPlacementState(@Nonnull BlockPlaceContext context)
    {
        BlockState state = super.getPlacementState(context);
        if (state != null && this.getBlock() instanceof BlockCompressedBase)
        {
            BlockCompressedBase base = (BlockCompressedBase) this.getBlock();
            IntegerProperty prop = base.getLevelProperty();
            int clamped = Math.min(level, base.getMaxLevel() - 1);
            state = state.setValue(prop, clamped);
        }
        return state;
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack)
    {
        String baseName = this.getBlock() instanceof BlockCompressedBase
                ? ((BlockCompressedBase) this.getBlock()).getBaseName()
                : OneBlockUltima.MODID;
        return Component.translatable("tile." + baseName + "." + level + ".name");
    }
}

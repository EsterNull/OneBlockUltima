package ru.defea.oneblockultima.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;

public class BlockCompressedMineralBlock extends Block {
    public BlockCompressedMineralBlock()
    {
        super(Properties.of().mapColor(MapColor.METAL).strength(10F * 9F, 20F * 9F).sound(SoundType.METAL));
    }

    protected BlockCompressedMineralBlock(String name)
    {
        this();
    }

    @Override
    public float getExplosionResistance(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Explosion explosion) {
        return Blocks.IRON_BLOCK.getExplosionResistance() * 9.0F;
    }
}

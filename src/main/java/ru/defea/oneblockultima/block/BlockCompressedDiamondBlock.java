package ru.defea.oneblockultima.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.Explosion;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;

public class BlockCompressedDiamondBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 5;
    private static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedDiamondBlock()
    {
        super(LEVEL, Properties.of().mapColor(MapColor.DIAMOND).strength(5F * 9F, 10F * 9F).sound(SoundType.METAL), "compressed_diamond_block");
    }

    @Override
    public IntegerProperty getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public float getExplosionResistance(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Explosion explosion) {
        return Blocks.DIAMOND_BLOCK.getExplosionResistance() * 9.0F;
    }
}

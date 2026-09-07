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

public class BlockCompressedLapisBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;
    private static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedLapisBlock()
    {
        super(LEVEL, Properties.of().mapColor(MapColor.LAPIS).strength(3.0F * 9.0F, 5.0F * 9.0F).sound(SoundType.STONE), "compressed_lapis_block");
    }

    @Override
    public IntegerProperty getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public float getExplosionResistance(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Explosion explosion) {
        return Blocks.LAPIS_BLOCK.getExplosionResistance() * 9.0F;
    }
}

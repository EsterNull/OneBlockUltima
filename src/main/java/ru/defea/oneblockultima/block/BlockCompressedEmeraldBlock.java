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

public class BlockCompressedEmeraldBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 5;
    private static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedEmeraldBlock()
    {
        super(LEVEL, Properties.of().mapColor(MapColor.EMERALD).strength(5F * 9F, 10F * 9F).sound(SoundType.METAL), "compressed_emerald_block");
    }

    @Override
    public IntegerProperty getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public float getExplosionResistance(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Explosion explosion) {
        return Blocks.EMERALD_BLOCK.getExplosionResistance() * 9.0F;
    }
}

package ru.defea.oneblockultima.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;

import javax.annotation.Nonnull;

public class BlockCompressedBedrock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 4;
    private static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedBedrock()
    {
        super(LEVEL, Properties.of().mapColor(MapColor.STONE).strength(-1.0F, 6000000.0F * 9F).sound(net.minecraft.world.level.block.SoundType.STONE), "compressed_bedrock");
    }

    @Override
    public IntegerProperty getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public boolean canHarvestBlock(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Player player) {
        return false;
    }

    @Override
    public float getExplosionResistance(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Explosion explosion) {
        return Blocks.BEDROCK.getExplosionResistance() * 9.0F;
    }
}

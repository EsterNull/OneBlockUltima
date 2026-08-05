package ru.defea.oneblockultima.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class BlockCompressedRedstoneBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;
    private static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedRedstoneBlock()
    {
        super(Material.IRON, MapColor.TNT, "compressed_redstone_block");
        this.setSoundType(SoundType.METAL);
        this.setHardness(3.0F * 9.0F);
        this.setResistance(3.0F * 9.0F);
    }

    @Override
    protected PropertyInteger getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public float getExplosionResistance(@Nonnull Entity exploder) {
        return Blocks.REDSTONE_BLOCK.getExplosionResistance(exploder) * 9.0F;
    }

    @Override
    public boolean canProvidePower(@Nonnull IBlockState state)
    {
        return true;
    }

    @Override
    public int getWeakPower(@Nonnull IBlockState blockState, @Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side)
    {
        return 15;
    }
}

package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;

public class BlockCase extends Block
{
    private static final AxisAlignedBB CASE_AABB = new AxisAlignedBB(0.1875D, 0.0D, 0.125D, 0.8125D, 0.6875D, 0.875D);

    public BlockCase()
    {
        super(Material.GROUND);
        setHardness(1.5F);
        setResistance(10.0F);
        setRegistryName(OneBlockUltima.MODID, "case_block");
        setUnlocalizedName("case_block");
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess source, @Nonnull BlockPos pos)
    {
        return CASE_AABB;
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullCube(@Nonnull IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullBlock(@Nonnull IBlockState state)
    {
        return false;
    }

    @Override
    public boolean doesSideBlockRendering(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing face)
    {
        return false;
    }

    @Override
    @Nonnull
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }
}
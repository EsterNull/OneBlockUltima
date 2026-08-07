package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockFluidBarrier extends Block
{
    public BlockFluidBarrier()
    {
        super(Material.GROUND);
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setRegistryName(OneBlockUltima.MODID, "fluid_barrier");
        setUnlocalizedName("fluid_barrier");
        this.setLightOpacity(0);
        this.setTickRandomly(false);
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(@Nonnull IBlockState blockState, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end)
    {
        return null;
    }

    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state)
    {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess source, @Nonnull BlockPos pos)
    {
        return FULL_BLOCK_AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState blockState, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos)
    {
        return NULL_AABB;
    }

    @Override
    public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox, @Nonnull java.util.List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185477_7_)
    {
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
        return true;
    }

    @Override
    public boolean isPassable(@Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos)
    {
        return true;
    }

    @Override
    public boolean isReplaceable(@Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos)
    {
        return true;
    }

    @Override
    @Nonnull
    public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess worldIn, @Nonnull IBlockState state, @Nonnull BlockPos pos, @Nonnull EnumFacing face)
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean canBeReplacedByLeaves(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos)
    {
        return true;
    }
}

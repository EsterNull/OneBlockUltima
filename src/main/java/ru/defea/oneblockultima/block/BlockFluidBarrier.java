package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockFluidBarrier extends Block
{
    public BlockFluidBarrier()
    {
        super(Material.ground);
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setUnlocalizedName("fluid_barrier");
        this.setTextureName("oneblockultima:one_block_generator_texture");
        this.setLightOpacity(0);
        this.setTickRandomly(false);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end)
    {
        return null;
    }

    @Override
    public float getExplosionResistance(Entity exploder)
    {
        return 0.0F;
    }

    @Override
    public int getRenderType()
    {
        return -1;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
    {
        return null;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBox, List collidingBoxes, Entity entityIn)
    {
    }

    @Override
    public boolean isPassable(IBlockAccess world, int x, int y, int z)
    {
        return true;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z)
    {
        return true;
    }

    @Override
    public boolean canBeReplacedByLeaves(IBlockAccess world, int x, int y, int z)
    {
        return true;
    }

    @Override
    public boolean isAir(IBlockAccess world, int x, int y, int z)
    {
        return true;
    }
}

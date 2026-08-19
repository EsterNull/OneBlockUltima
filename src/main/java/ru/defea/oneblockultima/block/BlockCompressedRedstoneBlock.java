package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

public class BlockCompressedRedstoneBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;

    public BlockCompressedRedstoneBlock()
    {
        super(Material.iron, "compressed_redstone_block", MAX_LEVEL);
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(3.0F * 9.0F);
        this.setResistance(3.0F * 9.0F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.redstone_block.getExplosionResistance(exploder) * 9.0F;
    }

    @Override
    public boolean canProvidePower()
    {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess blockAccess, int x, int y, int z, int side)
    {
        return 15;
    }
}

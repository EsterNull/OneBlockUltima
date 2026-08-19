package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;

public class BlockCompressedBedrock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 4;

    public BlockCompressedBedrock()
    {
        super(Material.rock, "compressed_bedrock", MAX_LEVEL);
        this.setBlockUnbreakable();
        this.setStepSound(Block.soundTypeStone);
        this.setResistance(6000000.0F * 9F);
    }

    @Override
    public boolean canHarvestBlock(EntityPlayer player, int meta) {
        return false;
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.bedrock.getExplosionResistance(exploder) * 9.0F;
    }

    @Override
    public boolean isToolEffective(String tool, int meta) {
        return false;
    }
}

package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class BlockCompressedStoneBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;

    public BlockCompressedStoneBlock()
    {
        super(Material.rock, "compressed_stone_block", MAX_LEVEL);
        this.setStepSound(Block.soundTypeStone);
        this.setHardness(1.5F * 9F);
        this.setResistance(10F * 9F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.stone.getExplosionResistance(exploder) * 9.0F;
    }
}

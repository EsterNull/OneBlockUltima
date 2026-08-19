package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class BlockCompressedGoldBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 5;

    public BlockCompressedGoldBlock()
    {
        super(Material.iron, "compressed_gold_block", MAX_LEVEL);
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(3F * 9F);
        this.setResistance(10F * 9F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.gold_block.getExplosionResistance(exploder) * 9.0F;
    }
}

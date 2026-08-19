package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class BlockCompressedIronBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 5;

    public BlockCompressedIronBlock()
    {
        super(Material.iron, "compressed_iron_block", MAX_LEVEL);
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(5F * 9F);
        this.setResistance(10F * 9F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.iron_block.getExplosionResistance(exploder) * 9.0F;
    }
}

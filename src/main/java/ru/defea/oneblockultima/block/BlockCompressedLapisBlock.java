package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class BlockCompressedLapisBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;

    public BlockCompressedLapisBlock()
    {
        super(Material.iron, "compressed_lapis_block", MAX_LEVEL);
        this.setStepSound(Block.soundTypeStone);
        this.setHardness(3.0F * 9.0F);
        this.setResistance(5.0F * 9.0F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.lapis_block.getExplosionResistance(exploder) * 9.0F;
    }
}

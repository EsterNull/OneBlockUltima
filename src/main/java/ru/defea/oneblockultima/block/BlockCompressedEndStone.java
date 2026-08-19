package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class BlockCompressedEndStone extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;

    public BlockCompressedEndStone()
    {
        super(Material.rock, "compressed_end_stone", MAX_LEVEL);
        this.setStepSound(Block.soundTypeStone);
        this.setHardness(3F * 9F);
        this.setResistance(15F * 9F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.end_stone.getExplosionResistance(exploder) * 9.0F;
    }
}

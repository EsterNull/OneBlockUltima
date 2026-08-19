package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class BlockCompressedNetherrack extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;

    public BlockCompressedNetherrack()
    {
        super(Material.rock, "compressed_netherrack", MAX_LEVEL);
        this.setStepSound(Block.soundTypeStone);
        this.setHardness(2F * 9F);
        this.setResistance(10F * 9F);
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.netherrack.getExplosionResistance(exploder) * 9.0F;
    }
}

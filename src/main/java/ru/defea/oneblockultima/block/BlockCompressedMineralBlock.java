package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import ru.defea.oneblockultima.OneBlockUltima;

public class BlockCompressedMineralBlock extends Block {
    public BlockCompressedMineralBlock()
    {
        super(Material.iron);
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(10F * 9F);
        this.setResistance(20F * 9F);
        setCreativeTab(OneBlockUltima.modTab);
        this.setUnlocalizedName("compressed_mineral_block");
        this.setTextureName("oneblockultima:compressed_mineral_block");
    }

    @Override
    public float getExplosionResistance(Entity exploder) {
        return Blocks.iron_block.getExplosionResistance(exploder) * 9.0F;
    }
}

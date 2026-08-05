package ru.defea.oneblockultima.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

import javax.annotation.Nonnull;

public class BlockCompressedGoldBlock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 5;
    private static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedGoldBlock()
    {
        super(Material.IRON, MapColor.GOLD, "compressed_gold_block");
        this.setSoundType(SoundType.METAL);
        this.setHardness(3F * 9F);
        this.setResistance(10F * 9F);
    }

    @Override
    protected PropertyInteger getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public float getExplosionResistance(@Nonnull Entity exploder) {
        return Blocks.GOLD_BLOCK.getExplosionResistance(exploder) * 9.0F;
    }
}

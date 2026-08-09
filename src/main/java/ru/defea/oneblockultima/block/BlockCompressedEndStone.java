package ru.defea.oneblockultima.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

import javax.annotation.Nonnull;

public class BlockCompressedEndStone extends BlockCompressedBase {
    public static final int MAX_LEVEL = 6;
    private static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedEndStone()
    {
        super(Material.ROCK, MapColor.SAND, "compressed_end_stone");
        this.setSoundType(SoundType.STONE);
        this.setHardness(3F * 9F);
        this.setResistance(15F * 9F);
    }

    @Override
    protected PropertyInteger getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public float getExplosionResistance(@Nonnull Entity exploder) {
        return Blocks.END_STONE.getExplosionResistance(exploder) * 9.0F;
    }
}

package ru.defea.oneblockultima.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class BlockCompressedBedrock extends BlockCompressedBase {
    public static final int MAX_LEVEL = 4;
    private static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, MAX_LEVEL - 1);

    public BlockCompressedBedrock()
    {
        super(Material.ROCK, MapColor.STONE, "compressed_bedrock");
        this.setBlockUnbreakable();
        this.setSoundType(SoundType.STONE);
        this.setResistance(6000000.0F * 9F);
    }

    @Override
    protected PropertyInteger getLevelProperty()
    {
        return LEVEL;
    }

    @Override
    public boolean canHarvestBlock(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
        return false;
    }

    @Override
    public float getExplosionResistance(@Nonnull Entity exploder) {
        return Blocks.BEDROCK.getExplosionResistance(exploder) * 9.0F;
    }

    @Override
    public boolean isToolEffective(@Nonnull String tool, @Nonnull IBlockState state) {
        return false;
    }
}

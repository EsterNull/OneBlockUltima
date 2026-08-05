package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;

public abstract class BlockCompressedBase extends Block {
    private final int maxLevel;

    protected BlockCompressedBase(Material material, MapColor mapColor, String baseName)
    {
        super(material, mapColor);
        this.maxLevel = this.getLevelProperty().getAllowedValues().size();
        setCreativeTab(OneBlockUltima.modTab);
        this.setUnlocalizedName(baseName);
        this.setRegistryName(OneBlockUltima.MODID, baseName);
    }

    protected abstract PropertyInteger getLevelProperty();

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, this.getLevelProperty());
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(this.getLevelProperty(), Math.max(0, Math.min(this.maxLevel - 1, meta)));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(this.getLevelProperty());
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(this.getLevelProperty());
    }
}

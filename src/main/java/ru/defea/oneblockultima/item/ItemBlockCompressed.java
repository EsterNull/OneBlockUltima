package ru.defea.oneblockultima.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

public class ItemBlockCompressed extends ItemBlock {
    private final String baseName;
    private final int maxLevel;

    public ItemBlockCompressed(Block block, String baseName, int maxLevel)
    {
        super(block);
        this.baseName = baseName;
        this.maxLevel = maxLevel;
        this.setHasSubtypes(true);
    }

    @Override
    @Nonnull
    public String getUnlocalizedName(ItemStack stack)
    {
        return "tile." + this.baseName + "." + stack.getMetadata();
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items)
    {
        if (tab == CreativeTabs.SEARCH || this.isInCreativeTab(tab))
        {
            for (int meta = 0; meta < this.maxLevel; meta++)
            {
                items.add(new ItemStack(this, 1, meta));
            }
        }
    }
}

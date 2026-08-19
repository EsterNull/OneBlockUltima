package ru.defea.oneblockultima.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.List;

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
    public int getMetadata(int damage)
    {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        return "tile." + this.baseName + "." + stack.getMetadata();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getSubItems(Item item, CreativeTabs tab, List items)
    {
        if (tab == CreativeTabs.tabAllSearch || this.getCreativeTab() == tab)
        {
            for (int meta = 0; meta < this.maxLevel; meta++)
            {
                items.add(new ItemStack(this, 1, meta));
            }
        }
    }
}

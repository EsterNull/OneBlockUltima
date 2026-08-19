package ru.defea.oneblockultima.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemGuideBook extends Item
{
    public ItemGuideBook()
    {
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("obu_guide_book");
        this.setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World worldIn, EntityPlayer playerIn)
    {
        if (worldIn.isRemote)
        {
            OneBlockUltima.proxy.openGuideBookGui(playerIn);
        }
        return stack;
    }
}

package ru.defea.oneblockultima.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;

public class ItemGuideBook extends Item
{
    public ItemGuideBook()
    {
        setCreativeTab(OneBlockUltima.modTab);
        this.setRegistryName("obu_guide_book");
        this.setUnlocalizedName("obu_guide_book");
        this.setMaxStackSize(1);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World worldIn, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand handIn)
    {
        if (worldIn.isRemote)
        {
            OneBlockUltima.proxy.openGuideBookGui(playerIn);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }
}

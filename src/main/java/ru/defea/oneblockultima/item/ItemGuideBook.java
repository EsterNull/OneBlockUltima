package ru.defea.oneblockultima.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;

public class ItemGuideBook extends Item
{
    public ItemGuideBook()
    {
        super(new Item.Properties().stacksTo(1));
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level worldIn, @Nonnull Player playerIn, @Nonnull InteractionHand handIn)
    {
        if (worldIn.isClientSide)
        {
            OneBlockUltima.proxy.openGuideBookGui(playerIn);
        }
        return InteractionResultHolder.success(playerIn.getItemInHand(handIn));
    }
}

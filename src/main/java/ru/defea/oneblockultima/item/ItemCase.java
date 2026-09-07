package ru.defea.oneblockultima.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.util.CaseUtil;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemCase extends Item
{
    public ItemCase()
    {
        super(new Item.Properties().stacksTo(64));
    }

    @Nonnull
    //@Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level worldIn, @Nonnull Player playerIn, @Nonnull InteractionHand handIn)
    {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (worldIn.isClientSide)
        {
            if (!CaseUtil.readContents(stack, worldIn.registryAccess()).isEmpty())
            {
                OneBlockUltima.proxy.openCaseRouletteGui(playerIn, stack);
            }
            else
            {
                playerIn.sendSystemMessage(Component.translatable("gui.oneblockultima.case.empty"));
            }
        }
        return InteractionResultHolder.success(stack);
    }

    //@Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull net.minecraft.world.item.Item.TooltipContext context, @Nonnull TooltipFlag flagIn, @Nonnull List<Component> tooltip)
    {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA))
        {
            CompoundTag nbt = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).getUnsafe();
            if (nbt != null && nbt.contains(CaseUtil.NBT_CASE_SET_ID))
            {
                String setId = nbt.getString(CaseUtil.NBT_CASE_SET_ID);
                BlockSetConfig.BlockSetDefinition set = setId.isEmpty() ? null : BlockSetConfig.get().getSet(setId);
                if (set != null)
                {
                    tooltip.add(Component.translatable("gui.oneblockultima.case.tooltip.set", ContainerSetsConfig.getLocalizedSetName(set)));
                }
            }
        }
        tooltip.add(Component.translatable("gui.oneblockultima.case.tooltip.right_click"));
    }
}

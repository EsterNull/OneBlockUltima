package ru.defea.oneblockultima.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.util.CaseUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemCase extends Item
{
    public ItemCase()
    {
        setRegistryName(OneBlockUltima.MODID, "case");
        setUnlocalizedName("case");
        setCreativeTab(OneBlockUltima.modTab);
        setMaxStackSize(64);
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, @Nonnull EnumHand handIn)
    {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote)
        {
            if (!CaseUtil.readContents(stack).isEmpty())
            {
                OneBlockUltima.proxy.openCaseRouletteGui(playerIn, stack);
            }
            else
            {
                playerIn.sendMessage(new net.minecraft.util.text.TextComponentTranslation("gui.oneblockultima.case.empty"));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull net.minecraft.client.util.ITooltipFlag flagIn)
    {
        if (stack.hasTagCompound())
        {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null && nbt.hasKey(CaseUtil.NBT_CASE_SET_ID))
            {
                String setId = nbt.getString(CaseUtil.NBT_CASE_SET_ID);
                BlockSetConfig.BlockSetDefinition set = setId.isEmpty() ? null : BlockSetConfig.get().getSet(setId);
                if (set != null)
                {
                    tooltip.add(I18n.format("gui.oneblockultima.case.tooltip.set", ContainerSetsConfig.getLocalizedSetName(set)));
                }
            }
        }
        tooltip.add(I18n.format("gui.oneblockultima.case.tooltip.right_click"));
    }
}
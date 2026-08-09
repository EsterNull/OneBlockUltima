package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

import javax.annotation.Nonnull;

import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class CommandOBUSell extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "obuSell";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender)
    {
        return "/obuSell";
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, ICommandSender sender, @Nonnull String[] args)
    {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.only_player")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.sell_disabled")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.not_generated")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty())
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.not_generated")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (!isObuGenerated(heldItem))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.not_generated")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        double price = BlockPriceConfig.get().getPriceFromItemStack(heldItem);
        if (price <= 0)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.not_found")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        int count = heldItem.getCount();
        double totalValue = price * count;
        String itemName = heldItem.getDisplayName();
        player.getHeldItemMainhand().shrink(count);

        data.addCurrency(totalValue);
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);

        sender.sendMessage(new TextComponentString(I18n.format("command.obuSell.success", count, itemName, totalValue, data.getCurrency())).setStyle(new Style().setColor(TextFormatting.GREEN)));
    }

    public static boolean isObuGenerated(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        net.minecraft.nbt.NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        return nbt.hasKey(NBT_OBU_GENERATED) && nbt.getBoolean(NBT_OBU_GENERATED);
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean checkPermission(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender)
    {
        return true;
    }
}

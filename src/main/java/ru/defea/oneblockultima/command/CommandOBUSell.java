package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class CommandOBUSell extends CommandBase
{
    @Override
    public String getName()
    {
        return "obuSell";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/obuSell";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.only_player")));
            return;
        }

        if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.sell_disabled")));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.not_generated")));
            return;
        }

        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty())
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.not_generated")));
            return;
        }

        if (!isObuGenerated(heldItem))
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.not_generated")));
            return;
        }

        double price = BlockPriceConfig.get().getPriceFromItemStack(heldItem);
        if (price <= 0)
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.not_found")));
            return;
        }

        int count = heldItem.getCount();
        double totalValue = price * count;
        String itemName = heldItem.getDisplayName();
        player.getHeldItemMainhand().shrink(count);

        data.addCurrency(totalValue);
        PacketSyncPlayerData.sendToPlayer(player);

        sender.sendMessage(new TextComponentString("\u00a7a" + I18n.format("command.obuSell.success", count, itemName, totalValue, data.getCurrency())));
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
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }
}

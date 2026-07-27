package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

public class CommandOBUSellAll extends CommandBase
{
    @Override
    public String getName()
    {
        return "obuSellAll";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/obuSellAll";
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

        InventoryPlayer inventory = player.inventory;
        double totalPrice = 0;
        int totalCount = 0;
        Item targetType = null;

        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (!CommandOBUSell.isObuGenerated(stack)) continue;

            if (targetType == null)
            {
                targetType = stack.getItem();
            }
            else if (stack.getItem() != targetType)
            {
                continue;
            }

            double price = BlockPriceConfig.get().getPriceFromItemStack(stack);
            if (price <= 0) continue;

            int count = stack.getCount();
            totalPrice += price * count;
            totalCount += count;
        }

        if (totalCount == 0)
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.obuSellAll.empty")));
            return;
        }

        int soldCount = 0;
        String itemName = "";
        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != targetType) continue;
            if (!CommandOBUSell.isObuGenerated(stack)) continue;

            int count = stack.getCount();
            if (soldCount == 0)
            {
                itemName = stack.getDisplayName();
            }
            stack.shrink(count);
            soldCount += count;
        }

        data.addCurrency(totalPrice);
        PacketSyncPlayerData.sendToPlayer(player);

        sender.sendMessage(new TextComponentString("\u00a7a" + I18n.format("command.obuSellAll.success", soldCount, totalPrice, data.getCurrency())));
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

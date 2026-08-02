package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
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

public class CommandOBUSellAll extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "obuSellAll";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender)
    {
        return "/obuSellAll";
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
            sender.sendMessage(new TextComponentString(I18n.format("command.obuSellAll.empty")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        int soldCount = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != targetType) continue;
            if (!CommandOBUSell.isObuGenerated(stack)) continue;

            int count = stack.getCount();
            stack.shrink(count);
            soldCount += count;
        }

        data.addCurrency(totalPrice);
        PacketSyncPlayerData.sendToPlayer(player);

        sender.sendMessage(new TextComponentString(I18n.format("command.obuSellAll.success", soldCount, totalPrice, data.getCurrency())).setStyle(new Style().setColor(TextFormatting.GREEN)));
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

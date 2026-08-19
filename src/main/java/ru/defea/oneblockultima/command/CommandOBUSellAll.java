package ru.defea.oneblockultima.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

public class CommandOBUSellAll extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "obuSellAll";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/obuSellAll";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public java.util.List getCommandAliases()
    {
        // Minecraft 1.7.10 command lookup is case-sensitive; register a
        // lowercase alias so /obusellall works as well as /obuSellAll.
        return java.util.Collections.singletonList("obusellall");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayerMP))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.only_player")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.sell_disabled")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.not_generated")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        InventoryPlayer inventory = player.inventory;
        double totalPrice = 0;
        int totalCount = 0;
        Item targetType = null;

        // Sell all generator-made items of the same type as the item in hand
        // (matches the documented behaviour). Fall back to the first generated
        // item found in the inventory when the hand is empty or not a generated item.
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && heldItem.stackSize > 0 && CommandOBUSell.isObuGenerated(heldItem))
        {
            targetType = heldItem.getItem();
        }

        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || stack.stackSize <= 0) continue;

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

            int count = stack.stackSize;
            totalPrice += price * count;
            totalCount += count;
        }

        if (totalCount == 0)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.obuSellAll.empty")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        int soldCount = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || stack.stackSize <= 0) continue;
            if (stack.getItem() != targetType) continue;
            if (!CommandOBUSell.isObuGenerated(stack)) continue;

            int count = stack.stackSize;
            inventory.setInventorySlotContents(i, null);
            soldCount += count;
        }
        player.inventoryContainer.detectAndSendChanges();

        data.addCurrency(totalPrice);
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);

        sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocalFormatted("command.obuSellAll.success", soldCount, totalPrice, data.getCurrency())).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        return true;
    }
}

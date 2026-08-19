package ru.defea.oneblockultima.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class CommandOBUSell extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "obuSell";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/obuSell";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public java.util.List getCommandAliases()
    {
        // Minecraft 1.7.10 command lookup is case-sensitive; register a
        // lowercase alias so /obusell works as well as /obuSell.
        return java.util.Collections.singletonList("obusell");
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

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || heldItem.stackSize <= 0)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.not_generated")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        if (!isObuGenerated(heldItem))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.not_generated")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        double price = BlockPriceConfig.get().getPriceFromItemStack(heldItem);
        if (price <= 0)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.not_found")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        int count = heldItem.stackSize;
        double totalValue = price * count;
        String itemName = heldItem.getDisplayName();
        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        player.inventoryContainer.detectAndSendChanges();

        data.addCurrency(totalValue);
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);

        sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocalFormatted("command.obuSell.success", count, itemName, totalValue, data.getCurrency())).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
    }

    public static boolean isObuGenerated(ItemStack stack)
    {
        if (stack == null || stack.stackSize <= 0) return false;
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
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        return true;
    }
}

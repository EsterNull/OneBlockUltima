package ru.defea.oneblockultima.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

import java.util.List;

public class CommandAddUltimaBalance extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "addUltimaBalance";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/addUltimaBalance <player> <amount>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length != 2)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.usage") + getCommandUsage(sender)).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        double amount;
        try
        {
            amount = Double.parseDouble(args[1].replace(',', '.'));
        }
        catch (NumberFormatException ex)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.addUltimaBalance.integer")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().getPlayerByUsername(args[0]);
        if (player == null)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.player_not_found")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.addUltimaBalance.no_data")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        data.addCurrency(amount);
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);
        sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocalFormatted("command.addUltimaBalance.success", player.getCommandSenderName(), amount, data.getCurrency())).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        if (!(sender instanceof EntityPlayerMP) || sender != player)
        {
            player.addChatMessage(new ChatComponentText(StatCollector.translateToLocalFormatted("command.addUltimaBalance.success", player.getCommandSenderName(), amount, data.getCurrency())).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }
}

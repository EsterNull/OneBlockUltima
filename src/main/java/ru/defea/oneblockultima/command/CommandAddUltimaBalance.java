package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

import javax.annotation.Nonnull;

public class CommandAddUltimaBalance extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "addUltimaBalance";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender)
    {
        return "/addUltimaBalance <amount>";
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args)
    {
        if (args.length != 1)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.usage", getUsage(sender))).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        double amount;
        try
        {
            amount = Double.parseDouble(args[0].replace(',', '.'));
        }
        catch (NumberFormatException ex)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.addUltimaBalance.integer")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.addUltimaBalance.player")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP)sender.getCommandSenderEntity();
        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.addUltimaBalance.no_data")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        data.addCurrency(amount);
        PacketSyncPlayerData.sendToPlayer(player);
        sender.sendMessage(new TextComponentString(I18n.format("command.addUltimaBalance.success", amount, data.getCurrency())).setStyle(new Style().setColor(TextFormatting.GREEN)));
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }
}

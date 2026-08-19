package ru.defea.oneblockultima.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.util.List;

public class CommandSetOwner extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "setOwner";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/setOwner <x> <y> <z> <playerName>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length != 4)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.usage") + getCommandUsage(sender)).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        int x = CommandBase.parseInt(sender, args[0]);
        int y = CommandBase.parseInt(sender, args[1]);
        int z = CommandBase.parseInt(sender, args[2]);
        EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().getPlayerByUsername(args[3]);

        if (player == null)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.player_not_found")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        TileEntity tileEntity = MinecraftServer.getServer().getEntityWorld().getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityOneBlockGenerator) {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            generator.setOwnerId(player.getUniqueID());
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.setOwner.success")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        }
        else
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.no_generator")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, "~");
        }
        else if (args.length == 2)
        {
            return getListOfStringsMatchingLastWord(args, "~");
        }
        else if (args.length == 3)
        {
            return getListOfStringsMatchingLastWord(args, "~");
        }
        else if (args.length == 4)
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

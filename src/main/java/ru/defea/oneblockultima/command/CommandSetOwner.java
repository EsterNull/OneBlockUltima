package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import javax.annotation.Nonnull;
import java.util.List;

public class CommandSetOwner extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "setOwner";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender)
    {
        return "/setOwner <x> <y> <z> <playerName>";
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length != 4)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.usage") + getUsage(sender)).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        BlockPos pos = CommandBase.parseBlockPos(sender, args, 0, false);
        EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(args[3]);

        if (player == null)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.player_not_found")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        TileEntity tileEntity = server.getEntityWorld().getTileEntity(pos);
        if (tileEntity instanceof TileEntityOneBlockGenerator) {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            generator.setOwnerId(player.getUniqueID());
            sender.sendMessage(new TextComponentString(I18n.format("command.setOwner.success")).setStyle(new Style().setColor(TextFormatting.GREEN)));
        }
        else
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.no_generator")).setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args, BlockPos targetPos)
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
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }

        return super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }
}

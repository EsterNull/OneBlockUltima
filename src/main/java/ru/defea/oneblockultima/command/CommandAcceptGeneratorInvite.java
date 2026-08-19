package ru.defea.oneblockultima.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.util.ArrayList;
import java.util.List;

public class CommandAcceptGeneratorInvite extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "acceptGeneratorInvite";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/acceptGeneratorInvite";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayerMP))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.only_player")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        World world = player.worldObj;
        int gx = (int) Math.floor(player.posX);
        int gy = (int) Math.floor(player.posY) - 1;
        int gz = (int) Math.floor(player.posZ);

        if (world.getBlock(gx, gy, gz) != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.not_near_generator")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        TileEntity tileEntity = world.getTileEntity(gx, gy, gz);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.no_generator")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        if (generator.isMemberLimitReached() && !generator.hasAccess(player.getUniqueID()))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.inviteGeneratorMember.member_limit")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        if (!generator.acceptInvite(player.getUniqueID()))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.no_invite")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.acceptGeneratorInvite.accepted")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        return new ArrayList();
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

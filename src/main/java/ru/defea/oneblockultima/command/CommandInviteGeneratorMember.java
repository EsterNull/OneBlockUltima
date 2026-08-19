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
import net.minecraft.world.World;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.util.ArrayList;
import java.util.List;

public class CommandInviteGeneratorMember extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "inviteGeneratorMember";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/inviteGeneratorMember <playerName>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayerMP))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.only_player")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        if (args.length != 1)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.inviteGeneratorMember.usage")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        EntityPlayerMP owner = (EntityPlayerMP) sender;
        World world = owner.worldObj;
        int gx = (int) Math.floor(owner.posX);
        int gy = (int) Math.floor(owner.posY) - 1;
        int gz = (int) Math.floor(owner.posZ);

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
        if (!generator.isOwner(owner))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.inviteGeneratorMember.owner_only")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().getPlayerByUsername(args[0]);
        if (target == null)
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.player_not_found")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        if (target.getUniqueID().equals(owner.getUniqueID()))
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.inviteGeneratorMember.self_invite")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        if (generator.isMemberLimitReached())
        {
            sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.inviteGeneratorMember.member_limit")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return;
        }

        generator.addPendingInvite(target.getUniqueID(), owner.getUniqueID(), Math.max(1, ModSettings.get().getInviteDurationTicks()));
        target.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("command.inviteGeneratorMember.invitation_received")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        owner.addChatMessage(new ChatComponentText(StatCollector.translateToLocalFormatted("command.inviteGeneratorMember.invitation_sent", target.getCommandSenderName())).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length != 1)
        {
            return new ArrayList();
        }

        return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
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

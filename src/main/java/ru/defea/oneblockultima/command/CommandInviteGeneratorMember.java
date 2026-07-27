package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.util.ArrayList;
import java.util.List;

public class CommandInviteGeneratorMember extends CommandBase
{
    @Override
    public String getName()
    {
        return "inviteGeneratorMember";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/inviteGeneratorMember <playerName>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.only_player")));
            return;
        }

        if (args.length != 1)
        {
            sender.sendMessage(new TextComponentString("§c" + I18n.format("command.inviteGeneratorMember.usage")));
            return;
        }

        EntityPlayerMP owner = (EntityPlayerMP) sender.getCommandSenderEntity();
        World world = owner.world;
        BlockPos generatorPos = new BlockPos(owner.getPosition().getX(), owner.getPosition().getY() - 1, owner.getPosition().getZ());

        if (world.getBlockState(generatorPos).getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.not_near_generator")));
            return;
        }

        TileEntity tileEntity = world.getTileEntity(generatorPos);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            sender.sendMessage(new TextComponentString("§c" + I18n.format("command.no_generator")));
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        if (!generator.isOwner(owner))
        {
            sender.sendMessage(new TextComponentString("§c" + I18n.format("command.inviteGeneratorMember.owner_only")));
            return;
        }

        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(args[0]);
        if (target == null)
        {
            sender.sendMessage(new TextComponentString("§c" + I18n.format("command.player_not_found")));
            return;
        }

        if (target.getUniqueID().equals(owner.getUniqueID()))
        {
            sender.sendMessage(new TextComponentString("§c" + I18n.format("command.inviteGeneratorMember.self_invite")));
            return;
        }

        generator.addPendingInvite(target.getUniqueID(), owner.getUniqueID(), 1200);
        target.sendMessage(new TextComponentString("§a" + I18n.format("command.inviteGeneratorMember.invitation_received")));
        owner.sendMessage(new TextComponentString("§a" + I18n.format("command.inviteGeneratorMember.invitation_sent", target.getName())));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
    {
        if (args.length != 1)
        {
            return new ArrayList<>();
        }

        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
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

package ru.defea.oneblockultima.command;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CommandInviteGeneratorMember extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "inviteGeneratorMember";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender)
    {
        return "/inviteGeneratorMember <playerName>";
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, ICommandSender sender, @Nonnull String[] args)
    {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.only_player")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (args.length != 1)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.usage")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        EntityPlayerMP owner = (EntityPlayerMP) sender.getCommandSenderEntity();
        World world = owner.world;
        BlockPos generatorPos = new BlockPos(owner.getPosition().getX(), owner.getPosition().getY() - 1, owner.getPosition().getZ());

        if (world.getBlockState(generatorPos).getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.not_near_generator")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        TileEntity tileEntity = world.getTileEntity(generatorPos);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.no_generator")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        if (!generator.isOwner(owner))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.owner_only")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(args[0]);
        if (target == null)
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.player_not_found")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (target.getUniqueID().equals(owner.getUniqueID()))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.self_invite")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (generator.isMemberLimitReached())
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.member_limit")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        generator.addPendingInvite(target.getUniqueID(), owner.getUniqueID(), Math.max(1, ModSettings.get().getInviteDurationTicks()));
        target.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.invitation_received")).setStyle(new Style().setColor(TextFormatting.GREEN)));
        owner.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.invitation_sent", target.getName())).setStyle(new Style().setColor(TextFormatting.GREEN)));
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args, BlockPos targetPos)
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
    public boolean checkPermission(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender)
    {
        return true;
    }
}

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
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CommandAcceptGeneratorInvite extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "acceptGeneratorInvite";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender)
    {
        return "/acceptGeneratorInvite";
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, ICommandSender sender, @Nonnull String[] args)
    {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.only_player")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        World world = player.world;
        BlockPos generatorPos = new BlockPos(player.getPosition().getX(), player.getPosition().getY() - 1, player.getPosition().getZ());

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
        if (generator.isMemberLimitReached() && !generator.hasAccess(player.getUniqueID()))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.inviteGeneratorMember.member_limit")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        if (!generator.acceptInvite(player.getUniqueID()))
        {
            sender.sendMessage(new TextComponentString(I18n.format("command.no_invite")).setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        sender.sendMessage(new TextComponentString(I18n.format("command.acceptGeneratorInvite.accepted")).setStyle(new Style().setColor(TextFormatting.GREEN)));
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, BlockPos targetPos)
    {
        return new ArrayList<>();
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

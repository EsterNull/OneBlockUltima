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

public class CommandDeclineGeneratorInvite extends CommandBase
{
    @Override
    public String getName()
    {
        return "declineGeneratorInvite";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/declineGeneratorInvite";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.only_player")));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        World world = player.world;
        BlockPos generatorPos = new BlockPos(player.getPosition().getX(), player.getPosition().getY() - 1, player.getPosition().getZ());

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
        if (!generator.declineInvite(player.getUniqueID()))
        {
            sender.sendMessage(new TextComponentString("\u00a7c" + I18n.format("command.no_invite")));
            return;
        }

        sender.sendMessage(new TextComponentString("§a" + I18n.format("command.declineGeneratorInvite.declined")));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
    {
        return new ArrayList<>();
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}

package ru.defea.oneblockultima.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

public final class CommandDeclineGeneratorInvite
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("declineGeneratorInvite")
                .requires(s -> s.hasPermission(0))
                .executes(ctx -> {
                    execute(ctx.getSource());
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                }));
    }

    private static void execute(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.translatable("command.only_player").withStyle(ChatFormatting.RED));
            return;
        }

        Level world = player.level();
        BlockPos generatorPos = player.blockPosition().below();

        if (world.getBlockState(generatorPos).getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            source.sendFailure(Component.translatable("command.not_near_generator").withStyle(ChatFormatting.RED));
            return;
        }

        BlockEntity tileEntity = world.getBlockEntity(generatorPos);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            source.sendFailure(Component.translatable("command.no_generator").withStyle(ChatFormatting.RED));
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        if (!generator.declineInvite(player.getUUID()))
        {
            source.sendFailure(Component.translatable("command.no_invite").withStyle(ChatFormatting.RED));
            return;
        }

        source.sendSuccess(() -> Component.translatable("command.declineGeneratorInvite.declined").withStyle(ChatFormatting.GREEN), false);
    }
}

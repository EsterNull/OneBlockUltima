package ru.defea.oneblockultima.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

public final class CommandSetOwner
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("setOwner")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    execute(ctx.getSource(), new BlockPos(x, y, z), EntityArgument.getPlayer(ctx, "target"));
                                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                                }))))));
    }

    private static void execute(CommandSourceStack source, BlockPos pos, ServerPlayer player)
    {
        Level world = source.getServer().getLevel(Level.OVERWORLD);
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            generator.setOwnerId(player.getUUID());
            source.sendSuccess(() -> Component.translatable("command.setOwner.success").withStyle(ChatFormatting.GREEN), false);
        }
        else
        {
            source.sendFailure(Component.translatable("command.no_generator").withStyle(ChatFormatting.RED));
        }
    }
}

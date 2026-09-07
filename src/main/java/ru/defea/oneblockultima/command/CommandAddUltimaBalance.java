package ru.defea.oneblockultima.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;

public final class CommandAddUltimaBalance
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("addUltimaBalance")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(ctx -> {
                                    execute(ctx.getSource(),
                                            EntityArgument.getPlayer(ctx, "target"),
                                            StringArgumentType.getString(ctx, "amount"));
                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                }))));
    }

    private static void execute(CommandSourceStack source, ServerPlayer player, String amountStr)
    {
        if (amountStr == null || amountStr.isEmpty())
        {
            source.sendFailure(Component.translatable("command.usage")
                    .append(Component.literal(" /addUltimaBalance <player> <amount>")).withStyle(ChatFormatting.RED));
            return;
        }

        double amount;
        try
        {
            amount = Double.parseDouble(amountStr.replace(',', '.'));
        }
        catch (NumberFormatException ex)
        {
            source.sendFailure(Component.translatable("command.addUltimaBalance.integer").withStyle(ChatFormatting.RED));
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            source.sendFailure(Component.translatable("command.addUltimaBalance.no_data").withStyle(ChatFormatting.RED));
            return;
        }

        data.addCurrency(amount);
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);
        Component msg = Component.translatable("command.addUltimaBalance.success", player.getName(), amount, data.getCurrency())
                .withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> msg, false);
        if (!(source.getEntity() instanceof ServerPlayer) || source.getEntity() != player)
        {
            player.sendSystemMessage(msg);
        }
    }
}

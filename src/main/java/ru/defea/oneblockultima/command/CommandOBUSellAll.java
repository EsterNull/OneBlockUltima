package ru.defea.oneblockultima.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.util.CurrencyUtil;

public final class CommandOBUSellAll
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("obuSellAll")
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

        if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
        {
            source.sendFailure(Component.translatable("command.sell_disabled").withStyle(ChatFormatting.RED));
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            source.sendFailure(Component.translatable("command.not_generated").withStyle(ChatFormatting.RED));
            return;
        }

        Inventory inventory = player.getInventory();
        long totalCents = 0;
        int totalCount = 0;
        Item targetType = null;

        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!CommandOBUSell.isObuGenerated(stack)) continue;
            if (targetType == null)
            {
                targetType = stack.getItem();
            }
            else if (stack.getItem() != targetType)
            {
                continue;
            }
            double price = BlockPriceConfig.get().getPriceFromItemStack(stack);
            if (price <= 0) continue;
            int count = stack.getCount();
            totalCents += CurrencyUtil.toCents(price * count);
            totalCount += count;
        }

        if (totalCount == 0)
        {
            source.sendFailure(Component.translatable("command.obuSellAll.empty").withStyle(ChatFormatting.RED));
            return;
        }

        int soldCount = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != targetType) continue;
            if (!CommandOBUSell.isObuGenerated(stack)) continue;
            int count = stack.getCount();
            stack.shrink(count);
            soldCount += count;
        }

        data.addCurrency(CurrencyUtil.fromCents(totalCents));
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);

        final int fSold = soldCount;
        final double fPrice = CurrencyUtil.fromCents(totalCents);
        source.sendSuccess(() -> Component.translatable("command.obuSellAll.success", fSold, fPrice, data.getCurrency())
                .withStyle(ChatFormatting.GREEN), false);
    }
}

package ru.defea.oneblockultima.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.util.CurrencyUtil;

import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public final class CommandOBUSell
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("obuSell")
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

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty())
        {
            source.sendFailure(Component.translatable("command.not_generated").withStyle(ChatFormatting.RED));
            return;
        }

        if (!isObuGenerated(heldItem))
        {
            source.sendFailure(Component.translatable("command.not_generated").withStyle(ChatFormatting.RED));
            return;
        }

        double price = BlockPriceConfig.get().getPriceFromItemStack(heldItem);
        if (price <= 0)
        {
            source.sendFailure(Component.translatable("command.not_found").withStyle(ChatFormatting.RED));
            return;
        }

        int count = heldItem.getCount();
        double totalValue = CurrencyUtil.roundToCents(price * count);
        Component itemName = heldItem.getDisplayName();
        heldItem.shrink(count);

        data.addCurrency(totalValue);
        OneBlockPlayerDataProvider.saveToEntity(player, data);
        PacketSyncPlayerData.sendToPlayer(player);

        source.sendSuccess(() -> Component.translatable("command.obuSell.success", count, itemName, totalValue, data.getCurrency())
                .withStyle(ChatFormatting.GREEN), false);
    }

    public static boolean isObuGenerated(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        net.minecraft.nbt.CompoundTag nbt = customData.getUnsafe();
        if (nbt == null) return false;
        return nbt.getBoolean(NBT_OBU_GENERATED);
    }
}

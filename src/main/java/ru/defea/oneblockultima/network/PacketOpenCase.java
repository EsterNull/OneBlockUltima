package ru.defea.oneblockultima.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.util.CaseUtil;

import java.util.List;
import java.util.function.Supplier;

public class PacketOpenCase implements CustomPacketPayload
{
    public static final Type<PacketOpenCase> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "open_case"));
    public static final StreamCodec<FriendlyByteBuf, PacketOpenCase> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf),
            PacketOpenCase::new
    );

    private net.minecraft.nbt.CompoundTag caseNbt;

    public PacketOpenCase()
    {
    }

    public PacketOpenCase(net.minecraft.nbt.CompoundTag caseNbt)
    {
        this.caseNbt = caseNbt;
    }

    public PacketOpenCase(FriendlyByteBuf buf)
    {
        this.caseNbt = buf.readNbt();
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeNbt(caseNbt);
    }

    @Override
    public Type<PacketOpenCase> type()
    {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null)
        {
            context.enqueueWork(() -> process(player));
        }
        context.setPacketHandled(true);
    }

    private static void process(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        net.minecraft.world.item.ItemStack held = findCaseStack(player);
        if (held.isEmpty())
        {
            return;
        }

        List<CaseUtil.WeightedStack> contents = CaseUtil.readContents(held, player.level().registryAccess());
        int index = CaseUtil.rollIndex(contents, player.level().random);
        if (index < 0)
        {
            return;
        }

        net.minecraft.world.item.ItemStack winner = contents.get(index).stack.copy();
        if (!player.isCreative())
        {
            held.shrink(1);
        }
        if (player.getInventory().add(winner.copy()))
        {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        else
        {
            dropItem(player, winner.copy());
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.oneblockultima.case.reward",
                winner.getDisplayName(), String.valueOf(winner.getCount())));
        ModMessages.sendToPlayer(new PacketCaseResult(index), player);
    }

    private static net.minecraft.world.item.ItemStack findCaseStack(ServerPlayer player)
    {
        net.minecraft.world.item.ItemStack main = player.getMainHandItem();
        if (CaseUtil.isCaseItem(main))
        {
            return main;
        }
        net.minecraft.world.item.ItemStack off = player.getOffhandItem();
        if (CaseUtil.isCaseItem(off))
        {
            return off;
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    private static void dropItem(ServerPlayer player, net.minecraft.world.item.ItemStack stack)
    {
        net.minecraft.world.entity.item.ItemEntity entityItem = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), stack);
        entityItem.setNoPickUpDelay();
        player.level().addFreshEntity(entityItem);
    }
}

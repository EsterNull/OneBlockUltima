package ru.defea.oneblockultima.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;

public class PacketRequestSync implements CustomPacketPayload
{
    public static final Type<PacketRequestSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "request_sync"));
    public static final StreamCodec<FriendlyByteBuf, PacketRequestSync> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
            },
            buf -> new PacketRequestSync()
    );

    public PacketRequestSync()
    {
    }

    public PacketRequestSync(FriendlyByteBuf buf)
    {
    }

    public void write(FriendlyByteBuf buf)
    {
    }

    @Override
    public Type<PacketRequestSync> type()
    {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null)
        {
            return;
        }

        context.enqueueWork(() -> {
            IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
            if (data != null)
            {
                OneBlockPlayerDataProvider.loadFromEntity(player, data);
                OneBlockUltima.getLogger().warn("[OBU-Balance] Sync requested, currency after reload={}", data.getCurrency());
                PacketSyncPlayerData.sendToPlayer(player);
            }
        });
        context.setPacketHandled(true);
    }
}
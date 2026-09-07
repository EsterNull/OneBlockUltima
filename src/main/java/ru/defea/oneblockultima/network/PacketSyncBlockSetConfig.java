package ru.defea.oneblockultima.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class PacketSyncBlockSetConfig implements CustomPacketPayload
{
    public static final Type<PacketSyncBlockSetConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "sync_block_set_config"));
    public static final StreamCodec<FriendlyByteBuf, PacketSyncBlockSetConfig> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf),
            PacketSyncBlockSetConfig::new
    );

    private byte[] jsonBytes;

    public PacketSyncBlockSetConfig()
    {
    }

    public PacketSyncBlockSetConfig(String json)
    {
        this.jsonBytes = json.getBytes(StandardCharsets.UTF_8);
    }

    public PacketSyncBlockSetConfig(FriendlyByteBuf buf)
    {
        int length = buf.readInt();
        jsonBytes = new byte[length];
        buf.readBytes(jsonBytes);
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeInt(jsonBytes.length);
        buf.writeBytes(jsonBytes);
    }

    public String getJson()
    {
        return new String(jsonBytes, StandardCharsets.UTF_8);
    }

    @Override
    public Type<PacketSyncBlockSetConfig> type()
    {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        context.enqueueWork(() -> {
            String json = getJson();
            OneBlockUltima.logDebug("[Sync] Received BlockSetConfig, size: " + (jsonBytes != null ? jsonBytes.length : 0) + " bytes");
            if (json != null && !json.isEmpty())
            {
                BlockSetConfig.loadFromServerJson(json);
                OneBlockUltima.logDebug("[Sync] Applied server BlockSetConfig, sets count: " + BlockSetConfig.get().getSets().size());
            }
        });
        context.setPacketHandled(true);
    }
}

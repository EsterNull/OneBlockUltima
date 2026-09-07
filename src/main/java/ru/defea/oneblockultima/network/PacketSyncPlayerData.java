package ru.defea.oneblockultima.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;

import java.util.function.Supplier;

public class PacketSyncPlayerData implements CustomPacketPayload
{
    public static final Type<PacketSyncPlayerData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "sync_player_data"));
    public static final StreamCodec<FriendlyByteBuf, PacketSyncPlayerData> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf),
            PacketSyncPlayerData::new
    );

    private CompoundTag tag;

    public PacketSyncPlayerData()
    {
    }

    public PacketSyncPlayerData(CompoundTag tag)
    {
        this.tag = tag;
    }

    public PacketSyncPlayerData(FriendlyByteBuf buf)
    {
        this.tag = buf.readNbt();
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeNbt(tag);
    }

    @Override
    public Type<PacketSyncPlayerData> type()
    {
        return TYPE;
    }

    public static void sendToPlayer(ServerPlayer player)
    {
        IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(player);
        if (!(playerData instanceof OneBlockPlayerData))
        {
            return;
        }

        OneBlockPlayerData data = (OneBlockPlayerData) playerData;
        CompoundTag sync = new CompoundTag();
        sync.putDouble("currency", data.getCurrency());
        sync.putInt("brokenBlocksTotal", data.getBrokenBlocksCount());
        CompoundTag levels = new CompoundTag();
        for (java.util.Map.Entry<String, Integer> entry : data.getSetLevels().entrySet())
        {
            levels.putInt(entry.getKey(), entry.getValue());
        }
        sync.put("setLevels", levels);
        CompoundTag brokenBlocksBySet = new CompoundTag();
        for (java.util.Map.Entry<String, Integer> entry : data.getBrokenBlocksBySet().entrySet())
        {
            brokenBlocksBySet.putInt(entry.getKey(), entry.getValue());
        }
        sync.put("brokenBlocksBySet", brokenBlocksBySet);
        ModMessages.sendToPlayer(new PacketSyncPlayerData(sync), player);
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null)
            {
                return;
            }

            IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
            if (data instanceof OneBlockPlayerData)
            {
                OneBlockPlayerData playerData = (OneBlockPlayerData) data;
                OneBlockUltima.getLogger().warn("[OBU-Balance] Client received sync, old={} new={}",
                        playerData.getCurrency(), tag.getDouble("currency"));
                playerData.setCurrency(tag.getDouble("currency"));
                playerData.setBrokenBlocksTotal(tag.getInt("brokenBlocksTotal"));
                playerData.getSetLevels().clear();
                CompoundTag levels = tag.getCompound("setLevels");
                for (String key : levels.getAllKeys())
                {
                    playerData.getSetLevels().put(key, levels.getInt(key));
                }
                playerData.getBrokenBlocksBySet().clear();
                CompoundTag brokenBlocksBySet = tag.getCompound("brokenBlocksBySet");
                for (String key : brokenBlocksBySet.getAllKeys())
                {
                    playerData.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInt(key));
                }
            }
        });
        context.setPacketHandled(true);
    }
}

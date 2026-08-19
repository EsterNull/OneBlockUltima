package ru.defea.oneblockultima.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;

import java.nio.charset.StandardCharsets;

public class PacketSyncBlockSetConfig implements IMessage
{
    private byte[] jsonBytes;

    public PacketSyncBlockSetConfig()
    {
    }

    public PacketSyncBlockSetConfig(String json)
    {
        this.jsonBytes = json.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        int length = buf.readInt();
        jsonBytes = new byte[length];
        buf.readBytes(jsonBytes);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(jsonBytes.length);
        buf.writeBytes(jsonBytes);
    }

    public String getJson()
    {
        return new String(jsonBytes, StandardCharsets.UTF_8);
    }

    public static class Handler implements IMessageHandler<PacketSyncBlockSetConfig, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncBlockSetConfig message, MessageContext ctx)
        {
            OneBlockUltima.getLogger().info("[Sync] Received BlockSetConfig, size: " + (message.jsonBytes != null ? message.jsonBytes.length : 0) + " bytes");
            Minecraft.getMinecraft().addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    String json = message.getJson();
                    if (json != null && !json.isEmpty())
                    {
                        BlockSetConfig.loadFromServerJson(json);
                        OneBlockUltima.getLogger().info("[Sync] Applied server BlockSetConfig, sets count: " + BlockSetConfig.get().getSets().size());
                    }
                }
            });
            return null;
        }
    }
}

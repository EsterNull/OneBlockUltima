package ru.defea.oneblockultima.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;

public class PacketSyncPlayerData implements IMessage
{
    private NBTTagCompound data;

    public PacketSyncPlayerData()
    {
    }

    public PacketSyncPlayerData(NBTTagCompound data)
    {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, data);
    }

    public static void sendToPlayer(EntityPlayer player)
    {
        ru.defea.oneblockultima.capability.IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(player);
        if (!(playerData instanceof OneBlockPlayerData) || !(player instanceof net.minecraft.entity.player.EntityPlayerMP))
        {
            return;
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("currency", playerData.getCurrency());
        tag.setInteger("brokenBlocksTotal", playerData.getBrokenBlocksCount());
        NBTTagCompound levels = new NBTTagCompound();
        for (java.util.Map.Entry<String, Integer> entry : ((OneBlockPlayerData) playerData).getSetLevels().entrySet())
        {
            levels.setInteger(entry.getKey(), entry.getValue());
        }
        tag.setTag("setLevels", levels);
        NBTTagCompound brokenBlocksBySet = new NBTTagCompound();
        for (java.util.Map.Entry<String, Integer> entry : ((OneBlockPlayerData) playerData).getBrokenBlocksBySet().entrySet())
        {
            brokenBlocksBySet.setInteger(entry.getKey(), entry.getValue());
        }
        tag.setTag("brokenBlocksBySet", brokenBlocksBySet);
        ModMessages.sendToPlayer(new PacketSyncPlayerData(tag), (net.minecraft.entity.player.EntityPlayerMP) player);
    }

    public static class Handler implements IMessageHandler<PacketSyncPlayerData, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncPlayerData message, MessageContext ctx)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                if (player == null)
                {
                    return;
                }

                ru.defea.oneblockultima.capability.IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
                if (data instanceof OneBlockPlayerData)
                {
                    OneBlockPlayerData playerData = (OneBlockPlayerData) data;
                    playerData.setCurrency(message.data.getDouble("currency"));
                    playerData.setBrokenBlocksTotal(message.data.getInteger("brokenBlocksTotal"));
                    playerData.getSetLevels().clear();
                    NBTTagCompound levels = message.data.getCompoundTag("setLevels");
                    for (Object keyObj : levels.getKeySet())
                    {
                        String key = keyObj.toString();
                        playerData.getSetLevels().put(key, levels.getInteger(key));
                    }
                    playerData.getBrokenBlocksBySet().clear();
                    NBTTagCompound brokenBlocksBySet = message.data.getCompoundTag("brokenBlocksBySet");
                    for (Object keyObj : brokenBlocksBySet.getKeySet())
                    {
                        String key = keyObj.toString();
                        playerData.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInteger(key));
                    }
                }
            });
            return null;
        }
    }
}

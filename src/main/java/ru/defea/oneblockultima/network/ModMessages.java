package ru.defea.oneblockultima.network;

import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import ru.defea.oneblockultima.OneBlockUltima;

public final class ModMessages
{
    private static SimpleNetworkWrapper network;

    private ModMessages()
    {
    }

    public static void register()
    {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(OneBlockUltima.MODID);
        network.registerMessage(PacketOneBlockAction.Handler.class, PacketOneBlockAction.class, 0, Side.SERVER);
        network.registerMessage(PacketSyncPlayerData.Handler.class, PacketSyncPlayerData.class, 1, Side.CLIENT);
        network.registerMessage(PacketSyncBlockSetConfig.Handler.class, PacketSyncBlockSetConfig.class, 2, Side.CLIENT);
    }

    public static void sendToServer(IMessage message)
    {
        network.sendToServer(message);
    }

    public static void sendToPlayer(IMessage message, EntityPlayerMP player)
    {
        network.sendTo(message, player);
    }

    public static void sendToAll(IMessage message)
    {
        network.sendToAll(message);
    }

    public static void sendToAllAround(IMessage message, cpw.mods.fml.common.network.NetworkRegistry.TargetPoint point)
    {
        network.sendToAllAround(message, point);
    }
}
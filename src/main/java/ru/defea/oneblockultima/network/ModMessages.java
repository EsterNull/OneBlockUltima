package ru.defea.oneblockultima.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
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
        network.registerMessage(PacketOpenCase.Handler.class, PacketOpenCase.class, 3, Side.SERVER);
        network.registerMessage(PacketCaseResult.Handler.class, PacketCaseResult.class, 4, Side.CLIENT);
    }

    public static void sendToServer(IMessage message)
    {
        network.sendToServer(message);
    }

    public static void sendToPlayer(IMessage message, net.minecraft.entity.player.EntityPlayerMP player)
    {
        network.sendTo(message, player);
    }
}

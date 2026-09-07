package ru.defea.oneblockultima.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import ru.defea.oneblockultima.OneBlockUltima;

public final class ModMessages
{
    private static final int PROTOCOL_VERSION = 1;
    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "main_channel"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();

    public static void register()
    {
        INSTANCE.messageBuilder(PacketOneBlockAction.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> p.write(buf))
                .decoder(buf -> new PacketOneBlockAction(buf))
                .consumerMainThread((p, ctx) -> p.handle(ctx))
                .add();

        INSTANCE.messageBuilder(PacketSyncPlayerData.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> p.write(buf))
                .decoder(buf -> new PacketSyncPlayerData(buf))
                .consumerMainThread((p, ctx) -> p.handle(ctx))
                .add();

        INSTANCE.messageBuilder(PacketSyncBlockSetConfig.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> p.write(buf))
                .decoder(buf -> new PacketSyncBlockSetConfig(buf))
                .consumerMainThread((p, ctx) -> p.handle(ctx))
                .add();

        INSTANCE.messageBuilder(PacketOpenCase.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> p.write(buf))
                .decoder(buf -> new PacketOpenCase(buf))
                .consumerMainThread((p, ctx) -> p.handle(ctx))
                .add();

        INSTANCE.messageBuilder(PacketCaseResult.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> p.write(buf))
                .decoder(buf -> new PacketCaseResult(buf))
                .consumerMainThread((p, ctx) -> p.handle(ctx))
                .add();

        INSTANCE.messageBuilder(PacketRequestSync.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> p.write(buf))
                .decoder(buf -> new PacketRequestSync(buf))
                .consumerMainThread((p, ctx) -> p.handle(ctx))
                .add();
    }

    public static void sendToServer(CustomPacketPayload payload)
    {
        INSTANCE.send(payload, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player)
    {
        INSTANCE.send(payload, PacketDistributor.PLAYER.with(player));
    }
}

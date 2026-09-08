package ru.defea.oneblockultima.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.gui.containers.ContainerClaimGenerator;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;

import java.util.function.Supplier;

public class PacketOneBlockAction implements CustomPacketPayload
{
    public static final Type<PacketOneBlockAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "one_block_action"));
    public static final StreamCodec<FriendlyByteBuf, PacketOneBlockAction> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf),
            PacketOneBlockAction::new
    );

    public enum Action
    {
        SELECT_SET,
        UPGRADE_SET,
        TOGGLE_FLUIDS,
        TOGGLE_MOBS,
        TOGGLE_CHESTS,
        TOGGLE_SAPLINGS,
        CLAIM_OWNER
    }

    private final BlockPos generatorPos;
    private final Action action;
    private final String setId;

    public PacketOneBlockAction(BlockPos generatorPos, Action action, String setId)
    {
        this.generatorPos = generatorPos;
        this.action = action;
        this.setId = setId;
    }

    public PacketOneBlockAction(FriendlyByteBuf buf)
    {
        this.generatorPos = buf.readBlockPos();
        int actionOrdinal = buf.readByte();
        if (actionOrdinal < 0 || actionOrdinal >= Action.values().length)
        {
            throw new IllegalStateException("Unknown OneBlock action ordinal: " + actionOrdinal);
        }
        this.action = Action.values()[actionOrdinal];
        this.setId = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(generatorPos);
        buf.writeByte(action.ordinal());
        buf.writeUtf(setId);
    }

    @Override
    public Type<PacketOneBlockAction> type()
    {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null)
        {
            context.enqueueWork(() -> {
                if (player.containerMenu instanceof ContainerClaimGenerator)
                {
                    ContainerClaimGenerator claimContainer = (ContainerClaimGenerator) player.containerMenu;
                    if (!claimContainer.getGeneratorPos().equals(generatorPos))
                    {
                        return;
                    }

                    if (action == Action.CLAIM_OWNER)
                    {
                        boolean success = claimContainer.claimOwnership();
                        if (success)
                        {
                            PacketSyncPlayerData.sendToPlayer(player);
                            player.closeContainer();
                        }
                    }
                    return;
                }

                if (!(player.containerMenu instanceof ContainerOneBlock))
                {
                    return;
                }

                ContainerOneBlock container = (ContainerOneBlock) player.containerMenu;
                if (!container.getGeneratorPos().equals(generatorPos))
                {
                    return;
                }

                switch (action)
                {
                    case SELECT_SET:
                        container.applySelectSet(setId);
                        break;
                    case UPGRADE_SET:
                        if (container.applyUpgradeSet(setId))
                        {
                            PacketSyncPlayerData.sendToPlayer(player);
                        }
                        break;
                    case TOGGLE_FLUIDS:
                        container.applyToggleFluidGeneration();
                        break;
                    case TOGGLE_MOBS:
                        container.applyToggleMobGeneration();
                        break;
                    case TOGGLE_CHESTS:
                        container.applyToggleChestGeneration();
                        break;
                    case TOGGLE_SAPLINGS:
                        container.applyToggleSaplingGeneration();
                        break;
                    default:
                        break;
                }
                player.containerMenu.broadcastChanges();
            });
        }
        context.setPacketHandled(true);
    }
}

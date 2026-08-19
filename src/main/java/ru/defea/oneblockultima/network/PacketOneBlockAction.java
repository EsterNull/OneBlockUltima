package ru.defea.oneblockultima.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.gui.containers.ContainerClaimGenerator;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;

public class PacketOneBlockAction implements IMessage
{
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

    private int x;
    private int y;
    private int z;
    private int actionOrdinal;
    private String setId;

    public PacketOneBlockAction()
    {
    }

    public PacketOneBlockAction(int x, int y, int z, Action action, String setId)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.actionOrdinal = action.ordinal();
        this.setId = setId != null ? setId : "";
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        actionOrdinal = buf.readByte() & 0xFF;
        setId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(actionOrdinal);
        ByteBufUtils.writeUTF8String(buf, setId);
    }

    public static void sendToServer(int x, int y, int z, Action action, String setId)
    {
        ModMessages.sendToServer(new PacketOneBlockAction(x, y, z, action, setId));
    }

    public static class Handler implements IMessageHandler<PacketOneBlockAction, IMessage>
    {
        @Override
        public IMessage onMessage(PacketOneBlockAction message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            try
            {
                if (message.actionOrdinal < 0 || message.actionOrdinal >= Action.values().length)
                {
                    return null;
                }
                Action action = Action.values()[message.actionOrdinal];

                if (player.openContainer instanceof ContainerClaimGenerator)
                {
                    ContainerClaimGenerator claimContainer = (ContainerClaimGenerator) player.openContainer;
                    if (claimContainer.getGeneratorX() != message.x
                            || claimContainer.getGeneratorY() != message.y
                            || claimContainer.getGeneratorZ() != message.z)
                    {
                        return null;
                    }

                    if (action == Action.CLAIM_OWNER)
                    {
                        boolean success = claimContainer.claimOwnership();
                        if (success)
                        {
                            PacketSyncPlayerData.sendToPlayer(player);
                            player.closeScreen();
                        }
                    }
                    return null;
                }

                if (!(player.openContainer instanceof ContainerOneBlock))
                {
                    return null;
                }

                ContainerOneBlock container = (ContainerOneBlock) player.openContainer;
                if (container.getGeneratorX() != message.x
                        || container.getGeneratorY() != message.y
                        || container.getGeneratorZ() != message.z)
                {
                    return null;
                }

                if (action == Action.SELECT_SET)
                {
                    container.applySelectSet(message.setId);
                    player.openContainer.detectAndSendChanges();
                }
                else if (action == Action.UPGRADE_SET)
                {
                    boolean success = container.applyUpgradeSet(message.setId);
                    if (success)
                    {
                        PacketSyncPlayerData.sendToPlayer(player);
                    }
                    player.openContainer.detectAndSendChanges();
                }
                else if (action == Action.TOGGLE_FLUIDS)
                {
                    container.applyToggleFluidGeneration();
                    player.openContainer.detectAndSendChanges();
                }
                else if (action == Action.TOGGLE_MOBS)
                {
                    container.applyToggleMobGeneration();
                    player.openContainer.detectAndSendChanges();
                }
                else if (action == Action.TOGGLE_CHESTS)
                {
                    container.applyToggleChestGeneration();
                    player.openContainer.detectAndSendChanges();
                }
                else if (action == Action.TOGGLE_SAPLINGS)
                {
                    container.applyToggleSaplingGeneration();
                    player.openContainer.detectAndSendChanges();
                }
            }
            catch (Exception e)
            {
                OneBlockUltima.getLogger().error("Error handling PacketOneBlockAction", e);
            }
            return null;
        }
    }
}

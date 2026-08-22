package ru.defea.oneblockultima.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.gui.GuiCaseRoulette;

public class PacketCaseResult implements IMessage
{
    private int index;

    public PacketCaseResult()
    {
    }

    public PacketCaseResult(int index)
    {
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        index = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(index);
    }

    public static class Handler implements IMessageHandler<PacketCaseResult, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketCaseResult message, MessageContext ctx)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> GuiCaseRoulette.receiveResult(message.index));
            return null;
        }
    }
}
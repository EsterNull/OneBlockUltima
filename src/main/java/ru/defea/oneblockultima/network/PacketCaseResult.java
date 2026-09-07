package ru.defea.oneblockultima.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.gui.GuiCaseRoulette;

import java.util.function.Supplier;

public class PacketCaseResult implements CustomPacketPayload
{
    public static final Type<PacketCaseResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "case_result"));
    public static final StreamCodec<FriendlyByteBuf, PacketCaseResult> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf),
            PacketCaseResult::new
    );

    private int index;

    public PacketCaseResult()
    {
    }

    public PacketCaseResult(int index)
    {
        this.index = index;
    }

    public PacketCaseResult(FriendlyByteBuf buf)
    {
        this.index = buf.readInt();
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeInt(index);
    }

    @Override
    public Type<PacketCaseResult> type()
    {
        return TYPE;
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof GuiCaseRoulette gui)
            {
                gui.receiveResult(index);
            }
        });
        context.setPacketHandled(true);
    }
}

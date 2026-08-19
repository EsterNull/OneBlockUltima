package ru.defea.oneblockultima;

import io.netty.buffer.Unpooled;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.network.PacketSyncBlockSetConfig;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class PacketSyncBlockSetConfigTest {

    @BeforeClass
    public static void initMinecraftBootstrap() {
        Bootstrap.register();
    }

    @Test
    public void toBytesFromBytesRoundTripPreservesData() {
        String json = "{\"sets\":[{\"id\":\"test\",\"blocks\":[],\"mobs\":[]}],\"settings\":{}}";
        PacketSyncBlockSetConfig original = new PacketSyncBlockSetConfig(json);

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);

        PacketSyncBlockSetConfig restored = new PacketSyncBlockSetConfig();
        restored.fromBytes(buf);

        assertEquals(json, restored.getJson());
        buf.release();
    }

    @Test
    public void roundTripPreservesEmptyString() {
        String json = "";
        PacketSyncBlockSetConfig original = new PacketSyncBlockSetConfig(json);

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);

        PacketSyncBlockSetConfig restored = new PacketSyncBlockSetConfig();
        restored.fromBytes(buf);

        assertEquals("", restored.getJson());
        buf.release();
    }

    @Test
    public void roundTripPreservesUnicode() {
        String json = "{\"name\":\"Тест набор\",\"sets\":[]}";
        PacketSyncBlockSetConfig original = new PacketSyncBlockSetConfig(json);

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);

        PacketSyncBlockSetConfig restored = new PacketSyncBlockSetConfig();
        restored.fromBytes(buf);

        assertEquals(json, restored.getJson());
        buf.release();
    }

    @Test
    public void roundTripPreservesLargePayload() {
        StringBuilder sb = new StringBuilder("{\"sets\":[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"set_").append(i).append("\",\"blocks\":[],\"mobs\":[]}");
        }
        sb.append("],\"settings\":{}}");
        String json = sb.toString();

        PacketSyncBlockSetConfig original = new PacketSyncBlockSetConfig(json);

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);
        int readableAfterWrite = buf.readableBytes();

        PacketSyncBlockSetConfig restored = new PacketSyncBlockSetConfig();
        restored.fromBytes(buf);

        assertEquals(json, restored.getJson());
        assertEquals(json.getBytes(StandardCharsets.UTF_8).length + 4, readableAfterWrite);
        buf.release();
    }

    @Test
    public void roundTripPreservesSpecialCharacters() {
        String json = "{\"key\":\"value\\nwith\\nnewlines\",\"path\":\"C:\\\\Users\\\\test\"}";
        PacketSyncBlockSetConfig original = new PacketSyncBlockSetConfig(json);

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);

        PacketSyncBlockSetConfig restored = new PacketSyncBlockSetConfig();
        restored.fromBytes(buf);

        assertEquals(json, restored.getJson());
        buf.release();
    }

    @Test
    public void actualConfigJsonRoundTrips() {
        String json = ru.defea.oneblockultima.config.BlockSetConfig.get().toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        PacketSyncBlockSetConfig original = new PacketSyncBlockSetConfig(json);

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        original.toBytes(buf);

        PacketSyncBlockSetConfig restored = new PacketSyncBlockSetConfig();
        restored.fromBytes(buf);

        assertEquals(json, restored.getJson());
        buf.release();
    }
}

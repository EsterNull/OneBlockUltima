package ru.defea.oneblockultima;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Test-only {@link World} that can be constructed without a running Minecraft server.
 *
 * <p>{@link World} has only two abstract methods and its constructor performs no
 * chunk/block access, so a lightweight stub is enough for exercising pure-tick logic
 * (invite expiry, pending entries, generation caching, {@code ModEvents#onWorldTick}).
 * {@code Bootstrap.register()} must be called before use.
 */
public final class TestDummyWorld
{
    private TestDummyWorld()
    {
    }

    /**
     * Creates a world with the default terrain type (neither {@code OneBlockWorldType}
     * nor any other modded type) and the given client/server flag.
     */
    public static World newWorld(boolean remote)
    {
        return newWorld(remote, null);
    }

    /**
     * Creates a world with the given terrain type ({@code null} leaves the default).
     */
    public static World newWorld(boolean remote, WorldType terrainType)
    {
        WorldInfo info = new WorldInfo(new NBTTagCompound());
        if (terrainType != null)
        {
            info.setTerrainType(terrainType);
        }

        WorldProvider provider = new WorldProvider()
        {
            @Override
            @Nonnull
            public DimensionType getDimensionType()
            {
                //noinspection DataFlowIssue
                return null;
            }
        };

        //noinspection DataFlowIssue
        return new World(null, info, provider, new Profiler(), remote)
        {
            private final Map<Long, Chunk> chunkCache = new HashMap<>();

            {
                chunkProvider = createChunkProvider();
            }

            @Override
            @Nonnull
            protected IChunkProvider createChunkProvider()
            {
                final World self = this;
                return new IChunkProvider()
                {
                    @Override
                    public Chunk getLoadedChunk(int x, int z)
                    {
                        return provideChunk(x, z);
                    }

                    @Override
                    @Nonnull
                    public Chunk provideChunk(int x, int z)
                    {
                        long key = ChunkPos.asLong(x, z);
                        Chunk chunk = chunkCache.get(key);
                        if (chunk == null)
                        {
                            chunk = new Chunk(self, x, z);
                            chunkCache.put(key, chunk);
                        }
                        return chunk;
                    }

                    @Override
                    public boolean tick()
                    {
                        return false;
                    }

                    @Override
                    @Nonnull
                    public String makeString()
                    {
                        return "TestDummyWorld";
                    }

                    @Override
                    public boolean isChunkGeneratedAt(int x, int z)
                    {
                        return false;
                    }
                };
            }

            @Override
            protected boolean isChunkLoaded(int x, int z, boolean allowEmpty)
            {
                return false;
            }
        };
    }

    /**
     * Assigns a world to a tile entity, bypassing the normal server-side flow.
     */
    public static void setWorld(TileEntity tileEntity, World world)
    {
        try
        {
            Field field = TileEntity.class.getDeclaredField("world");
            field.setAccessible(true);
            field.set(tileEntity, world);
        }
        catch (Exception e)
        {
            throw new AssertionError("Unable to set TileEntity.world", e);
        }
    }
}

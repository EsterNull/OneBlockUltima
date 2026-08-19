package ru.defea.oneblockultima;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only {@link World} that can be constructed without a running Minecraft server.
 *
 * <p>In 1.7.10 the lightweight client constructor
 * {@code World(ISaveHandler, String, WorldProvider, WorldSettings, Profiler)} performs
 * no chunk/block access, so a lightweight stub is enough for exercising pure-tick logic
 * (invite expiry, pending entries, generation caching, {@code ModEvents#onWorldTick}).
 * The terrain type travels through the {@link WorldSettings} because the constructor
 * builds its own {@link net.minecraft.world.storage.WorldInfo}. {@code Bootstrap.register()} must be called before
 * use.
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
        return newWorld(remote, terrainType, null);
    }

    /**
     * Creates a world with the given terrain type and save handler.
     */
    public static World newWorld(boolean remote, WorldType terrainType, ISaveHandler saveHandler)
    {
        WorldType type = terrainType != null ? terrainType : WorldType.DEFAULT;
        WorldSettings settings = new WorldSettings(0L, WorldSettings.GameType.NOT_SET, false, false, type);

        WorldProvider provider = new WorldProvider()
        {
            @Override
            public String getDimensionName()
            {
                return "TestDummyWorld";
            }
        };

        return new World(saveHandler, "TestDummyWorld", provider, settings, new Profiler())
        {
            private final Map<Long, Chunk> chunkCache = new HashMap<>();

            {
                isRemote = remote;
                chunkProvider = createChunkProvider();
                mapStorage = new MapStorage(null);
            }

            @Override
            protected IChunkProvider createChunkProvider()
            {
                final World self = this;
                return new IChunkProvider()
                {
                    @Override
                    public boolean chunkExists(int x, int z)
                    {
                        return chunkCache.containsKey(chunkKey(x, z));
                    }

                    @Override
                    public Chunk provideChunk(int x, int z)
                    {
                        long key = chunkKey(x, z);
                        Chunk chunk = chunkCache.get(key);
                        if (chunk == null)
                        {
                            chunk = new Chunk(self, x, z);
                            chunkCache.put(key, chunk);
                        }
                        return chunk;
                    }

                    @Override
                    public Chunk loadChunk(int x, int z)
                    {
                        return provideChunk(x, z);
                    }

                    @Override
                    public void populate(IChunkProvider provider, int x, int z)
                    {
                    }

                    @Override
                    public boolean saveChunks(boolean p_73149_1_, IProgressUpdate p_73149_2_)
                    {
                        return true;
                    }

                    @Override
                    public boolean unloadQueuedChunks()
                    {
                        return false;
                    }

                    @Override
                    public boolean canSave()
                    {
                        return true;
                    }

                    @Override
                    public String makeString()
                    {
                        return "TestDummyWorld";
                    }

                    @Override
                    @SuppressWarnings("rawtypes")
                    public List getPossibleCreatures(EnumCreatureType creatureType, int x, int y, int z)
                    {
                        return null;
                    }

                    @Override
                    public ChunkPosition findClosestStructure(World world, String structureName, int x, int y, int z)
                    {
                        return null;
                    }

                    @Override
                    public int getLoadedChunkCount()
                    {
                        return chunkCache.size();
                    }

                    @Override
                    public void recreateStructures(int x, int z)
                    {
                    }

                    @Override
                    public void saveExtraData()
                    {
                    }
                };
            }

            @Override
            protected int getRenderDistanceChunks()
            {
                return 8;
            }

            @Override
            public void setSpawnLocation(int x, int y, int z)
            {
                getWorldInfo().setSpawnPosition(x, y, z);
            }

            @Override
            public Entity getEntityByID(int id)
            {
                return null;
            }
        };
    }

    private static long chunkKey(int x, int z)
    {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    /**
     * Assigns a world to a tile entity, bypassing the normal server-side flow.
     */
    public static void setWorld(TileEntity tileEntity, World world)
    {
        tileEntity.setWorldObj(world);
    }
}

package ru.defea.oneblockultima.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import ru.defea.oneblockultima.OneBlockUltima;

import java.util.HashMap;
import java.util.Map;

public class GeneratedBlockRegistry extends WorldSavedData
{
    private static final String DATA_NAME = OneBlockUltima.MODID + "_generated_blocks";
    private static final long DIRTY_FLUSH_INTERVAL_MS = 2000L;

    private final Map<Long, GeneratedBlockEntry> entries = new HashMap<>();
    private long lastDirtyMs = 0L;
    private boolean pendingDirty = false;

    public GeneratedBlockRegistry()
    {
        this(DATA_NAME);
    }

    public GeneratedBlockRegistry(String name)
    {
        super(name);
    }

    public static GeneratedBlockRegistry get(World world)
    {
        GeneratedBlockRegistry data = (GeneratedBlockRegistry) world.loadItemData(
                GeneratedBlockRegistry.class,
                DATA_NAME
        );

        if (data == null)
        {
            data = new GeneratedBlockRegistry();
            world.setItemData(DATA_NAME, data);
        }

        return data;
    }

    public void markGenerated(int x, int y, int z, int gx, int gy, int gz, String setId, int currency, int level, String blockRegistry, int blockMeta)
    {
        entries.put(getKey(x, y, z), new GeneratedBlockEntry(gx, gy, gz, setId, currency, level, blockRegistry, blockMeta));
        markDirtyThrottled();
    }

    public boolean isGenerated(int x, int y, int z)
    {
        return entries.containsKey(getKey(x, y, z));
    }

    public GeneratedBlockEntry getEntry(int x, int y, int z)
    {
        return entries.get(getKey(x, y, z));
    }

    public int getGeneratorX(int x, int y, int z)
    {
        GeneratedBlockEntry entry = entries.get(getKey(x, y, z));
        return entry == null ? 0 : entry.generatorX;
    }

    public int getGeneratorY(int x, int y, int z)
    {
        GeneratedBlockEntry entry = entries.get(getKey(x, y, z));
        return entry == null ? 0 : entry.generatorY;
    }

    public int getGeneratorZ(int x, int y, int z)
    {
        GeneratedBlockEntry entry = entries.get(getKey(x, y, z));
        return entry == null ? 0 : entry.generatorZ;
    }

    public void remove(int x, int y, int z)
    {
        if (entries.remove(getKey(x, y, z)) != null)
        {
            markDirtyThrottled();
        }
    }

    private static long getKey(int x, int y, int z)
    {
        return ((long) (x & 0xFFFFFF) << 40) | ((long) (y & 0xFFFF) << 24) | (long) (z & 0xFFFFFF);
    }

    /**
     * Throttles write frequency: a heavy NBT dump of the whole registry runs at most once every 2 seconds.
     * A guaranteed flush on world unload is performed via {@link #flushPendingDirty()}.
     */
    private void markDirtyThrottled()
    {
        pendingDirty = true;
        long now = System.currentTimeMillis();
        if (now - lastDirtyMs >= DIRTY_FLUSH_INTERVAL_MS)
        {
            lastDirtyMs = now;
            pendingDirty = false;
            markDirty();
        }
    }

    public void flushPendingDirty()
    {
        if (pendingDirty)
        {
            pendingDirty = false;
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        entries.clear();
        NBTTagList list = nbt.getTagList("entries", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < list.tagCount(); i++)
        {
            NBTTagCompound entryTag = list.getCompoundTagAt(i);
            int x = entryTag.getInteger("x");
            int y = entryTag.getInteger("y");
            int z = entryTag.getInteger("z");
            String blockRegistry = entryTag.getString("blockRegistry");
            int blockMeta = entryTag.getInteger("blockMeta");
            entries.put(getKey(x, y, z), new GeneratedBlockEntry(
                    entryTag.getInteger("gx"),
                    entryTag.getInteger("gy"),
                    entryTag.getInteger("gz"),
                    entryTag.getString("setId"),
                    entryTag.getInteger("currency"),
                    entryTag.getInteger("level"),
                    blockRegistry.isEmpty() ? null : blockRegistry,
                    blockMeta
            ));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        NBTTagList list = new NBTTagList();

        for (Map.Entry<Long, GeneratedBlockEntry> entry : entries.entrySet())
        {
            NBTTagCompound entryTag = new NBTTagCompound();
            GeneratedBlockEntry value = entry.getValue();

            entryTag.setInteger("x", extractX(entry.getKey()));
            entryTag.setInteger("y", extractY(entry.getKey()));
            entryTag.setInteger("z", extractZ(entry.getKey()));
            entryTag.setInteger("gx", value.generatorX);
            entryTag.setInteger("gy", value.generatorY);
            entryTag.setInteger("gz", value.generatorZ);
            entryTag.setString("setId", value.setId);
            entryTag.setInteger("currency", value.currency);
            entryTag.setInteger("level", value.level);
            entryTag.setString("blockRegistry", value.blockRegistry == null ? "" : value.blockRegistry);
            entryTag.setInteger("blockMeta", value.blockMeta);
            list.appendTag(entryTag);
        }

        compound.setTag("entries", list);
    }

    private static int extractX(long key)
    {
        return (int) (key >> 40);
    }

    private static int extractY(long key)
    {
        return (int) ((key >> 24) & 0xFFFF);
    }

    private static int extractZ(long key)
    {
        return (int) (key & 0xFFFFFF);
    }

    public static class GeneratedBlockEntry
    {
        public final int generatorX;
        public final int generatorY;
        public final int generatorZ;
        public final String setId;
        public final int currency;
        public final int level;
        public final String blockRegistry;
        public final int blockMeta;

        public GeneratedBlockEntry(int generatorX, int generatorY, int generatorZ, String setId, int currency, int level)
        {
            this(generatorX, generatorY, generatorZ, setId, currency, level, null, 0);
        }

        public GeneratedBlockEntry(int generatorX, int generatorY, int generatorZ, String setId, int currency, int level, String blockRegistry, int blockMeta)
        {
            this.generatorX = generatorX;
            this.generatorY = generatorY;
            this.generatorZ = generatorZ;
            this.setId = setId;
            this.currency = currency;
            this.level = level;
            this.blockRegistry = blockRegistry;
            this.blockMeta = blockMeta;
        }
    }
}

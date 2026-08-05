package ru.defea.oneblockultima.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class GeneratedBlockRegistry extends WorldSavedData
{
    private static final String DATA_NAME = OneBlockUltima.MODID + "_generated_blocks";
    private static final long DIRTY_FLUSH_INTERVAL_MS = 2000L;

    private final Map<BlockPos, GeneratedBlockEntry> entries = new HashMap<>();
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
        GeneratedBlockRegistry data = (GeneratedBlockRegistry) world.getPerWorldStorage().getOrLoadData(
                GeneratedBlockRegistry.class,
                DATA_NAME
        );

        if (data == null)
        {
            data = new GeneratedBlockRegistry();
            world.getPerWorldStorage().setData(DATA_NAME, data);
        }

        return data;
    }

    public void markGenerated(BlockPos pos, BlockPos generatorPos, String setId, int currency, int level, String blockRegistry, int blockMeta)
    {
        entries.put(pos, new GeneratedBlockEntry(generatorPos, setId, currency, level, blockRegistry, blockMeta));
        markDirtyThrottled();
    }

    public boolean isGenerated(BlockPos pos)
    {
        return entries.containsKey(pos);
    }

    public GeneratedBlockEntry getEntry(BlockPos pos)
    {
        return entries.get(pos);
    }

    public BlockPos getGeneratorPos(BlockPos pos)
    {
        GeneratedBlockEntry entry = entries.get(pos);
        return entry == null ? null : entry.generatorPos;
    }

    public void remove(BlockPos pos)
    {
        if (entries.remove(pos) != null)
        {
            markDirtyThrottled();
        }
    }

    /**
     * Ограничивает частоту записи: тяжелый NBT-дамп всего реестра выполняется не чаще раза в 2 секунды.
     * Гарантированный сброс при выгрузке мира выполняется через {@link #flushPendingDirty()}.
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
            BlockPos pos = new BlockPos(
                    entryTag.getInteger("x"),
                    entryTag.getInteger("y"),
                    entryTag.getInteger("z")
            );
            BlockPos generatorPos = new BlockPos(
                    entryTag.getInteger("gx"),
                    entryTag.getInteger("gy"),
                    entryTag.getInteger("gz")
            );
            String blockRegistry = entryTag.getString("blockRegistry");
            int blockMeta = entryTag.getInteger("blockMeta");
            entries.put(pos, new GeneratedBlockEntry(
                    generatorPos,
                    entryTag.getString("setId"),
                    entryTag.getInteger("currency"),
                    entryTag.getInteger("level"),
                    blockRegistry.isEmpty() ? null : blockRegistry,
                    blockMeta
            ));
        }
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound)
    {
        NBTTagList list = new NBTTagList();

        for (Map.Entry<BlockPos, GeneratedBlockEntry> entry : entries.entrySet())
        {
            NBTTagCompound entryTag = new NBTTagCompound();
            BlockPos pos = entry.getKey();
            GeneratedBlockEntry value = entry.getValue();

            entryTag.setInteger("x", pos.getX());
            entryTag.setInteger("y", pos.getY());
            entryTag.setInteger("z", pos.getZ());
            entryTag.setInteger("gx", value.generatorPos.getX());
            entryTag.setInteger("gy", value.generatorPos.getY());
            entryTag.setInteger("gz", value.generatorPos.getZ());
            entryTag.setString("setId", value.setId);
            entryTag.setInteger("currency", value.currency);
            entryTag.setInteger("level", value.level);
            entryTag.setString("blockRegistry", value.blockRegistry == null ? "" : value.blockRegistry);
            entryTag.setInteger("blockMeta", value.blockMeta);
            list.appendTag(entryTag);
        }

        compound.setTag("entries", list);
        return compound;
    }

    public static class GeneratedBlockEntry
    {
        public final BlockPos generatorPos;
        public final String setId;
        public final int currency;
        public final int level;
        public final String blockRegistry;
        public final int blockMeta;

        public GeneratedBlockEntry(BlockPos generatorPos, String setId, int currency, int level)
        {
            this(generatorPos, setId, currency, level, null, 0);
        }

        public GeneratedBlockEntry(BlockPos generatorPos, String setId, int currency, int level, String blockRegistry, int blockMeta)
        {
            this.generatorPos = generatorPos;
            this.setId = setId;
            this.currency = currency;
            this.level = level;
            this.blockRegistry = blockRegistry;
            this.blockMeta = blockMeta;
        }
    }
}

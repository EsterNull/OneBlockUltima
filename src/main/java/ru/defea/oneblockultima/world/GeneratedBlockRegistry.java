package ru.defea.oneblockultima.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class GeneratedBlockRegistry extends SavedData
{
    private static final String DATA_NAME = OneBlockUltima.MODID + "_generated_blocks";
    private static final long DIRTY_FLUSH_INTERVAL_MS = 2000L;

    private final Map<BlockPos, GeneratedBlockEntry> entries = new HashMap<>();
    private long lastDirtyMs = 0L;
    private boolean pendingDirty = false;

    public GeneratedBlockRegistry()
    {
    }

    public static GeneratedBlockRegistry get(Level world)
    {
        if (!(world instanceof ServerLevel serverWorld))
            return new GeneratedBlockRegistry();
        SavedData.Factory<GeneratedBlockRegistry> factory = new SavedData.Factory<>(
                GeneratedBlockRegistry::new,
                GeneratedBlockRegistry::load,
                null
        );
        return serverWorld.getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    public static GeneratedBlockRegistry load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider)
    {
        GeneratedBlockRegistry data = new GeneratedBlockRegistry();
        data.read(nbt);
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

    private void markDirtyThrottled()
    {
        pendingDirty = true;
        long now = System.currentTimeMillis();
        if (now - lastDirtyMs >= DIRTY_FLUSH_INTERVAL_MS)
        {
            lastDirtyMs = now;
            pendingDirty = false;
            this.setDirty();
        }
    }

    public void flushPendingDirty()
    {
        if (pendingDirty)
        {
            pendingDirty = false;
            this.setDirty();
        }
    }

    public void read(CompoundTag nbt)
    {
        entries.clear();
        ListTag list = nbt.getList("entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = new BlockPos(
                    entryTag.getInt("x"),
                    entryTag.getInt("y"),
                    entryTag.getInt("z")
            );
            BlockPos generatorPos = new BlockPos(
                    entryTag.getInt("gx"),
                    entryTag.getInt("gy"),
                    entryTag.getInt("gz")
            );
            String blockRegistry = entryTag.getString("blockRegistry");
            int blockMeta = entryTag.getInt("blockMeta");
            entries.put(pos, new GeneratedBlockEntry(
                    generatorPos,
                    entryTag.getString("setId"),
                    entryTag.getInt("currency"),
                    entryTag.getInt("level"),
                    blockRegistry.isEmpty() ? null : blockRegistry,
                    blockMeta
            ));
        }
    }

    @Nonnull
    @Override
    public CompoundTag save(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();

        for (Map.Entry<BlockPos, GeneratedBlockEntry> entry : entries.entrySet())
        {
            CompoundTag entryTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            GeneratedBlockEntry value = entry.getValue();

            entryTag.putInt("x", pos.getX());
            entryTag.putInt("y", pos.getY());
            entryTag.putInt("z", pos.getZ());
            entryTag.putInt("gx", value.generatorPos.getX());
            entryTag.putInt("gy", value.generatorPos.getY());
            entryTag.putInt("gz", value.generatorPos.getZ());
            entryTag.putString("setId", value.setId);
            entryTag.putInt("currency", value.currency);
            entryTag.putInt("level", value.level);
            entryTag.putString("blockRegistry", value.blockRegistry == null ? "" : value.blockRegistry);
            entryTag.putInt("blockMeta", value.blockMeta);
            list.add(entryTag);
        }

        compound.put("entries", list);
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

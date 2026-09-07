package ru.defea.oneblockultima.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import ru.defea.oneblockultima.OneBlockUltima;

public class SpawnConfigData extends SavedData
{
    private static final String DATA_NAME = OneBlockUltima.MODID + "_spawn_data";

    public boolean spawnInitialized = false;
    public boolean spawnTeleportDone = false;

    public SpawnConfigData()
    {
    }

    public static SpawnConfigData get(Level world)
    {
        ServerLevel serverLevel = (ServerLevel) world;
        DimensionDataStorage storage = serverLevel.getDataStorage();
        SavedData.Factory<SpawnConfigData> factory = new SavedData.Factory<>(
                SpawnConfigData::new,
                (tag, provider) -> {
                    SpawnConfigData d = new SpawnConfigData();
                    d.readFromNBT(tag);
                    return d;
                },
                DataFixTypes.LEVEL
        );
        return storage.computeIfAbsent(factory, DATA_NAME);
    }

    public void readFromNBT(CompoundTag nbt)
    {
        spawnInitialized = nbt.getBoolean("spawnInitialized");
        spawnTeleportDone = nbt.getBoolean("spawnTeleportDone");
    }

    @Override
    public CompoundTag save(CompoundTag compound, HolderLookup.Provider provider)
    {
        compound.putBoolean("spawnInitialized", spawnInitialized);
        compound.putBoolean("spawnTeleportDone", spawnTeleportDone);
        return compound;
    }
}

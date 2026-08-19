package ru.defea.oneblockultima.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import ru.defea.oneblockultima.OneBlockUltima;

public class SpawnConfigData extends WorldSavedData
{
    private static final String DATA_NAME = OneBlockUltima.MODID + "_spawn_data";

    public boolean spawnInitialized = false;
    public boolean spawnTeleportDone = false;

    public SpawnConfigData()
    {
        this(DATA_NAME);
    }

    public SpawnConfigData(String name)
    {
        super(name);
    }

    public static SpawnConfigData get(World world)
    {
        MapStorage storage = world.mapStorage;
        SpawnConfigData data = (SpawnConfigData) storage.loadData(
                SpawnConfigData.class,
                DATA_NAME
        );

        if (data == null)
        {
            data = new SpawnConfigData();
            storage.setData(DATA_NAME, data);
        }

        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        spawnInitialized = nbt.getBoolean("spawnInitialized");
        spawnTeleportDone = nbt.getBoolean("spawnTeleportDone");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        compound.setBoolean("spawnInitialized", spawnInitialized);
        compound.setBoolean("spawnTeleportDone", spawnTeleportDone);
    }
}

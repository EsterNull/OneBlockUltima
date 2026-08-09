package ru.defea.oneblockultima.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import java.util.Map;

public class OneBlockPlayerDataStorage implements Capability.IStorage<IOneBlockPlayerData>
{
    @Override
    public NBTBase writeNBT(Capability<IOneBlockPlayerData> capability, IOneBlockPlayerData instance, EnumFacing side)
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("currency", instance.getCurrency());

        NBTTagCompound levels = new NBTTagCompound();
        NBTTagCompound brokenBlocks = new NBTTagCompound();
        if (instance instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData data = (OneBlockPlayerData) instance;
            for (Map.Entry<String, Integer> entry : data.getSetLevels().entrySet())
            {
                levels.setInteger(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : data.getBrokenBlocksBySet().entrySet())
            {
                brokenBlocks.setInteger(entry.getKey(), entry.getValue());
            }
            tag.setInteger("brokenBlocksTotal", data.getBrokenBlocksCount());
        }
        tag.setTag("setLevels", levels);
        tag.setTag("brokenBlocksBySet", brokenBlocks);
        return tag;
    }

    @Override
    public void readNBT(Capability<IOneBlockPlayerData> capability, IOneBlockPlayerData instance, EnumFacing side, NBTBase nbt)
    {
        if (!(instance instanceof OneBlockPlayerData) || !(nbt instanceof NBTTagCompound))
        {
            return;
        }

        NBTTagCompound tag = (NBTTagCompound) nbt;
        OneBlockPlayerData data = (OneBlockPlayerData) instance;
        data.setCurrency(tag.getDouble("currency"));
        data.getSetLevels().clear();

        NBTTagCompound levels = tag.getCompoundTag("setLevels");
        for (String key : levels.getKeySet())
        {
            data.getSetLevels().put(key, levels.getInteger(key));
        }

        data.setBrokenBlocksTotal(tag.getInteger("brokenBlocksTotal"));
        NBTTagCompound brokenBlocks = tag.getCompoundTag("brokenBlocksBySet");
        data.setBrokenBlocksBySet(null);
        for (String key : brokenBlocks.getKeySet())
        {
            data.getBrokenBlocksBySet().put(key, brokenBlocks.getInteger(key));
        }
    }
}

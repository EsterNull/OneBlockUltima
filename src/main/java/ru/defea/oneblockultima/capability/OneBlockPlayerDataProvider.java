package ru.defea.oneblockultima.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import java.util.Map;

public class OneBlockPlayerDataProvider implements IExtendedEntityProperties
{
    public static final String EXT_KEY = "oneblockultima_player_data";

    private final OneBlockPlayerData instance = new OneBlockPlayerData();

    public static void register()
    {
    }

    public static IOneBlockPlayerData get(EntityPlayer player)
    {
        if (player == null)
        {
            return null;
        }

        OneBlockPlayerDataProvider provider = (OneBlockPlayerDataProvider) player.getExtendedProperties(EXT_KEY);
        if (provider == null)
        {
            provider = new OneBlockPlayerDataProvider();
            player.registerExtendedProperties(EXT_KEY, provider);
        }

        return provider.instance;
    }

    public static void saveToEntity(EntityPlayer player, IOneBlockPlayerData data)
    {
    }

    public static void loadFromEntity(EntityPlayer player, IOneBlockPlayerData data)
    {
    }

    @Override
    public void saveNBTData(NBTTagCompound compound)
    {
        compound.setDouble("currency", instance.getCurrency());
        compound.setInteger("brokenBlocksTotal", instance.getBrokenBlocksCount());

        NBTTagCompound setLevels = new NBTTagCompound();
        for (Map.Entry<String, Integer> entry : instance.getSetLevels().entrySet())
        {
            setLevels.setInteger(entry.getKey(), entry.getValue());
        }
        compound.setTag("setLevels", setLevels);

        NBTTagCompound brokenBlocksBySet = new NBTTagCompound();
        for (Map.Entry<String, Integer> entry : instance.getBrokenBlocksBySet().entrySet())
        {
            brokenBlocksBySet.setInteger(entry.getKey(), entry.getValue());
        }
        compound.setTag("brokenBlocksBySet", brokenBlocksBySet);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound)
    {
        instance.setCurrency(compound.getDouble("currency"));
        instance.setBrokenBlocksTotal(compound.getInteger("brokenBlocksTotal"));

        instance.getSetLevels().clear();
        NBTTagCompound setLevels = compound.getCompoundTag("setLevels");
        for (Object keyObj : setLevels.getKeySet())
        {
            String key = keyObj.toString();
            instance.getSetLevels().put(key, setLevels.getInteger(key));
        }

        instance.getBrokenBlocksBySet().clear();
        NBTTagCompound brokenBlocksBySet = compound.getCompoundTag("brokenBlocksBySet");
        for (Object keyObj : brokenBlocksBySet.getKeySet())
        {
            String key = keyObj.toString();
            instance.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInteger(key));
        }
    }

    @Override
    public void init(Entity entity, World world)
    {
    }
}

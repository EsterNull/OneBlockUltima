package ru.defea.oneblockultima.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.*;

import javax.annotation.Nullable;
import java.util.Map;

public class OneBlockPlayerDataProvider implements ICapabilitySerializable<NBTTagCompound>
{
    private static final String PERSISTENT_TAG = "oneblockultima_player_data";
    private static final String CURRENCY_TAG = "currency";
    private static final String BROKEN_BLOCKS_TOTAL_TAG = "brokenBlocksTotal";
    private static final String SET_LEVELS_TAG = "setLevels";
    private static final String BROKEN_BLOCKS_BY_SET_TAG = "brokenBlocksBySet";

    @CapabilityInject(IOneBlockPlayerData.class)
    public static Capability<IOneBlockPlayerData> ONE_BLOCK_PLAYER_DATA = null;

    private final OneBlockPlayerData instance = new OneBlockPlayerData();

    public static void register()
    {
        CapabilityManager.INSTANCE.register(
                IOneBlockPlayerData.class,
                new OneBlockPlayerDataStorage(),
                OneBlockPlayerData.class
        );
    }

    public static IOneBlockPlayerData get(ICapabilityProvider provider)
    {
        if (provider != null && ONE_BLOCK_PLAYER_DATA != null && provider.hasCapability(ONE_BLOCK_PLAYER_DATA, null))
        {
            return provider.getCapability(ONE_BLOCK_PLAYER_DATA, null);
        }

        return null;
    }

    public static void saveToEntity(EntityPlayer player, IOneBlockPlayerData data)
    {
        if (player == null || data == null)
        {
            return;
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble(CURRENCY_TAG, data.getCurrency());
        tag.setInteger(BROKEN_BLOCKS_TOTAL_TAG, data.getBrokenBlocksCount());

        NBTTagCompound setLevels = new NBTTagCompound();
        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData playerData = (OneBlockPlayerData) data;
            for (Map.Entry<String, Integer> entry : playerData.getSetLevels().entrySet())
            {
                setLevels.setInteger(entry.getKey(), entry.getValue());
            }
        }
        tag.setTag(SET_LEVELS_TAG, setLevels);

        NBTTagCompound brokenBlocksBySet = new NBTTagCompound();
        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData playerData = (OneBlockPlayerData) data;
            for (Map.Entry<String, Integer> entry : playerData.getBrokenBlocksBySet().entrySet())
            {
                brokenBlocksBySet.setInteger(entry.getKey(), entry.getValue());
            }
        }
        tag.setTag(BROKEN_BLOCKS_BY_SET_TAG, brokenBlocksBySet);

        player.getEntityData().setTag(PERSISTENT_TAG, tag);
    }

    public static void loadFromEntity(EntityPlayer player, IOneBlockPlayerData data)
    {
        if (player == null || data == null)
        {
            return;
        }

        NBTTagCompound tag = player.getEntityData().getCompoundTag(PERSISTENT_TAG);
        if (tag == null || tag.hasNoTags())
        {
            return;
        }

        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData playerData = (OneBlockPlayerData) data;
            playerData.setCurrency(tag.getDouble(CURRENCY_TAG));
            playerData.setBrokenBlocksTotal(tag.getInteger(BROKEN_BLOCKS_TOTAL_TAG));

            playerData.getSetLevels().clear();
            NBTTagCompound setLevels = tag.getCompoundTag(SET_LEVELS_TAG);
            for (String key : setLevels.getKeySet())
            {
                playerData.getSetLevels().put(key, setLevels.getInteger(key));
            }

            playerData.getBrokenBlocksBySet().clear();
            NBTTagCompound brokenBlocksBySet = tag.getCompoundTag(BROKEN_BLOCKS_BY_SET_TAG);
            for (String key : brokenBlocksBySet.getKeySet())
            {
                playerData.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInteger(key));
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
    {
        return capability == ONE_BLOCK_PLAYER_DATA;
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        if (capability == ONE_BLOCK_PLAYER_DATA)
        {
            return ONE_BLOCK_PLAYER_DATA.cast(instance);
        }

        return null;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("currency", instance.getCurrency());
        tag.setInteger("brokenBlocksTotal", instance.getBrokenBlocksCount());

        NBTTagCompound setLevels = new NBTTagCompound();
        for (Map.Entry<String, Integer> entry : instance.getSetLevels().entrySet())
        {
            setLevels.setInteger(entry.getKey(), entry.getValue());
        }
        tag.setTag("setLevels", setLevels);

        NBTTagCompound brokenBlocksBySet = new NBTTagCompound();
        for (Map.Entry<String, Integer> entry : instance.getBrokenBlocksBySet().entrySet())
        {
            brokenBlocksBySet.setInteger(entry.getKey(), entry.getValue());
        }
        tag.setTag("brokenBlocksBySet", brokenBlocksBySet);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        instance.setCurrency(nbt.getDouble("currency"));
        instance.setBrokenBlocksTotal(nbt.getInteger("brokenBlocksTotal"));

        instance.getSetLevels().clear();
        NBTTagCompound setLevels = nbt.getCompoundTag("setLevels");
        for (String key : setLevels.getKeySet())
        {
            instance.getSetLevels().put(key, setLevels.getInteger(key));
        }

        instance.getBrokenBlocksBySet().clear();
        NBTTagCompound brokenBlocksBySet = nbt.getCompoundTag("brokenBlocksBySet");
        for (String key : brokenBlocksBySet.getKeySet())
        {
            instance.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInteger(key));
        }
    }
}

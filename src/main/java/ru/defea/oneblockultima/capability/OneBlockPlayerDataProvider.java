package ru.defea.oneblockultima.capability;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.Map;
import ru.defea.oneblockultima.util.CurrencyUtil;

public class OneBlockPlayerDataProvider implements ICapabilitySerializable<CompoundTag>
{
    private static final String PERSISTENT_TAG = "oneblockultima_player_data";
    private static final String PLAYER_PERSISTED_TAG = "PlayerPersisted";
    private static final String CURRENCY_TAG = "currency";
    private static final String CURRENCY_TAG_CENTS = "currencyCents";
    private static final String BROKEN_BLOCKS_TOTAL_TAG = "brokenBlocksTotal";
    private static final String SET_LEVELS_TAG = "setLevels";
    private static final String BROKEN_BLOCKS_BY_SET_TAG = "brokenBlocksBySet";

    public static final Capability<IOneBlockPlayerData> ONE_BLOCK_PLAYER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final OneBlockPlayerData instance = new OneBlockPlayerData();

    public static IOneBlockPlayerData get(ICapabilityProvider provider)
    {
        if (provider != null && ONE_BLOCK_PLAYER_DATA != null && provider.getCapability(ONE_BLOCK_PLAYER_DATA).isPresent())
        {
            return provider.getCapability(ONE_BLOCK_PLAYER_DATA).orElse(null);
        }

        return null;
    }

    public static void saveToEntity(Player player, IOneBlockPlayerData data)
    {
        if (player == null || data == null)
        {
            return;
        }

        CompoundTag tag = new CompoundTag();
        tag.putLong(CURRENCY_TAG_CENTS, data instanceof OneBlockPlayerData
                ? ((OneBlockPlayerData) data).getCurrencyCents()
                : CurrencyUtil.toCents(data.getCurrency()));
        tag.putInt(BROKEN_BLOCKS_TOTAL_TAG, data.getBrokenBlocksCount());

        CompoundTag setLevels = new CompoundTag();
        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData playerData = (OneBlockPlayerData) data;
            for (Map.Entry<String, Integer> entry : playerData.getSetLevels().entrySet())
            {
                setLevels.putInt(entry.getKey(), entry.getValue());
            }
        }
        tag.put(SET_LEVELS_TAG, setLevels);

        CompoundTag brokenBlocksBySet = new CompoundTag();
        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData playerData = (OneBlockPlayerData) data;
            for (Map.Entry<String, Integer> entry : playerData.getBrokenBlocksBySet().entrySet())
            {
                brokenBlocksBySet.putInt(entry.getKey(), entry.getValue());
            }
        }
        tag.put(BROKEN_BLOCKS_BY_SET_TAG, brokenBlocksBySet);

        CompoundTag persistentData = player.getPersistentData();
        persistentData.put(PERSISTENT_TAG, tag);
        CompoundTag playerPersisted = persistentData.getCompound(PLAYER_PERSISTED_TAG);
        playerPersisted.put(PERSISTENT_TAG, tag);
        persistentData.put(PLAYER_PERSISTED_TAG, playerPersisted);
    }

    private static CompoundTag readPersistentTag(Player player)
    {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag tag = persistentData.getCompound(PERSISTENT_TAG);
        if (tag == null || tag.isEmpty())
        {
            tag = persistentData.getCompound(PLAYER_PERSISTED_TAG).getCompound(PERSISTENT_TAG);
        }
        return tag == null ? new CompoundTag() : tag;
    }

    public static void loadFromEntity(Player player, IOneBlockPlayerData data)
    {
        if (player == null || data == null)
        {
            return;
        }

        CompoundTag tag = readPersistentTag(player);
        if (tag == null || tag.isEmpty())
        {
            return;
        }

        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData playerData = (OneBlockPlayerData) data;
            if (tag.contains(CURRENCY_TAG_CENTS))
            {
                playerData.setCurrencyCents(tag.getLong(CURRENCY_TAG_CENTS));
            }
            else
            {
                playerData.setCurrencyCents(CurrencyUtil.toCents(tag.getDouble(CURRENCY_TAG)));
            }
            playerData.setBrokenBlocksTotal(tag.getInt(BROKEN_BLOCKS_TOTAL_TAG));

            playerData.getSetLevels().clear();
            CompoundTag setLevels = tag.getCompound(SET_LEVELS_TAG);
            for (String key : setLevels.getAllKeys())
            {
                playerData.getSetLevels().put(key, setLevels.getInt(key));
            }

            playerData.getBrokenBlocksBySet().clear();
            CompoundTag brokenBlocksBySet = tag.getCompound(BROKEN_BLOCKS_BY_SET_TAG);
            for (String key : brokenBlocksBySet.getAllKeys())
            {
                playerData.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInt(key));
            }
        }
    }

    @Nullable
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing)
    {
        if (capability == ONE_BLOCK_PLAYER_DATA)
        {
            return (LazyOptional<T>) LazyOptional.of(() -> (IOneBlockPlayerData) instance);
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("currencyCents", instance.getCurrencyCents());
        tag.putInt("brokenBlocksTotal", instance.getBrokenBlocksCount());

        CompoundTag setLevels = new CompoundTag();
        for (Map.Entry<String, Integer> entry : instance.getSetLevels().entrySet())
        {
            setLevels.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("setLevels", setLevels);

        CompoundTag brokenBlocksBySet = new CompoundTag();
        for (Map.Entry<String, Integer> entry : instance.getBrokenBlocksBySet().entrySet())
        {
            brokenBlocksBySet.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("brokenBlocksBySet", brokenBlocksBySet);
        return tag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag nbt)
    {
        if (nbt.contains("currencyCents"))
        {
            instance.setCurrencyCents(nbt.getLong("currencyCents"));
        }
        else
        {
            instance.setCurrencyCents(CurrencyUtil.toCents(nbt.getDouble("currency")));
        }
        instance.setBrokenBlocksTotal(nbt.getInt("brokenBlocksTotal"));

        instance.getSetLevels().clear();
        CompoundTag setLevels = nbt.getCompound("setLevels");
        for (String key : setLevels.getAllKeys())
        {
            instance.getSetLevels().put(key, setLevels.getInt(key));
        }

        instance.getBrokenBlocksBySet().clear();
        CompoundTag brokenBlocksBySet = nbt.getCompound("brokenBlocksBySet");
        for (String key : brokenBlocksBySet.getAllKeys())
        {
            instance.getBrokenBlocksBySet().put(key, brokenBlocksBySet.getInt(key));
        }
    }
}

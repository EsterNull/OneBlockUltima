package ru.defea.oneblockultima.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class BlockPriceConfig
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "block_prices.json";

    private static BlockPriceConfig instance;

    private Map<String, Integer> prices = new LinkedHashMap<>();

    public static void load()
    {
        instance = loadInternal();
    }

    public static BlockPriceConfig get()
    {
        if (instance == null)
        {
            instance = loadInternal();
        }
        return instance;
    }

    public int getPrice(String registry)
    {
        if (registry == null) return 0;
        Integer price = prices.get(registry);
        return price != null ? price : 0;
    }

    public int getPriceFromItemStack(ItemStack stack)
    {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item instanceof net.minecraft.item.ItemBlock)
        {
            Block block = ((net.minecraft.item.ItemBlock) item).getBlock();
            net.minecraft.util.ResourceLocation reg = block.getRegistryName();
            if (reg != null)
            {
                return getPrice(reg.toString());
            }
        }
        return 0;
    }

    public Map<String, Integer> getPrices()
    {
        return Collections.unmodifiableMap(prices);
    }

    public List<Map.Entry<String, Integer>> getPricesList()
    {
        return new ArrayList<>(prices.entrySet());
    }

    public void setPrice(String registry, int price)
    {
        if (registry == null) return;
        prices.put(registry, price);
        save();
    }

    public void removePrice(String registry)
    {
        if (registry == null) return;
        prices.remove(registry);
        save();
    }

    public void replaceAll(Map<String, Integer> newPrices)
    {
        if (newPrices == null) return;
        prices.clear();
        prices.putAll(newPrices);
        save();
    }

    public boolean hasPrice(String registry)
    {
        return registry != null && prices.containsKey(registry);
    }

    public boolean isBlockPlaceable(String registry)
    {
        if (registry == null) return false;
        try
        {
            net.minecraft.util.ResourceLocation rl = new net.minecraft.util.ResourceLocation(registry);
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null) return false;
            Item item = Item.getItemFromBlock(block);
            return item != null && item != Item.getItemFromBlock(net.minecraft.init.Blocks.AIR);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static ItemStack createItemStack(String registry)
    {
        if (registry == null) return ItemStack.EMPTY;
        try
        {
            net.minecraft.util.ResourceLocation rl = new net.minecraft.util.ResourceLocation(registry);

            // Try blocks first
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block != null)
            {
                Item item = Item.getItemFromBlock(block);
                if (item != null && item != Item.getItemFromBlock(net.minecraft.init.Blocks.AIR))
                {
                    return new ItemStack(item, 1, 0);
                }
            }

            // Try non-block items (diamonds, emeralds, etc.)
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != net.minecraft.init.Items.AIR)
            {
                return new ItemStack(item, 1, 0);
            }

            return ItemStack.EMPTY;
        }
        catch (Exception e)
        {
            return ItemStack.EMPTY;
        }
    }

    private static File getFile()
    {
        if (Loader.instance().getConfigDir() != null)
        {
            return new File(Loader.instance().getConfigDir(), FILE_NAME);
        }
        return null;
    }

    private static BlockPriceConfig loadInternal()
    {
        File file = getFile();
        BlockPriceConfig config = new BlockPriceConfig();

        if (file == null || !file.exists())
        {
            return config;
        }

        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))
        {
            BlockPriceConfig loaded = GSON.fromJson(reader, BlockPriceConfig.class);
            if (loaded != null && loaded.prices != null)
            {
                config.prices = loaded.prices;
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load block prices", e);
        }

        return config;
    }

    public void save()
    {
        File file = getFile();
        if (file == null) return;
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))
        {
            GSON.toJson(this, writer);
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to save block prices", e);
        }
    }

    public void reload()
    {
        instance = loadInternal();
    }
}

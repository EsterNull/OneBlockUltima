package ru.defea.oneblockultima.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class BlockPriceConfig
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT_GSON = new GsonBuilder().create();
    private static final String FILE_NAME = "oneblockultima/block_prices.json";

    private static BlockPriceConfig instance;
    static File configFile;

    public enum BalanceMode
    {
        BREAK_BLOCK,
        SELL_BLOCK
    }

    private final Map<String, Double> prices = new LinkedHashMap<>();
    private String balanceMode = "BREAK_BLOCK";

    public static void load(File configDir)
    {
        if (configDir == null)
        {
            OneBlockUltima.getLogger().error("Config directory is null, cannot load block prices");
            instance = new BlockPriceConfig();
            return;
        }

        configFile = new File(configDir, FILE_NAME);
        reload();
    }

    public static void reset()
    {
        instance = null;
        configFile = null;
    }

    public static BlockPriceConfig get()
    {
        if (instance == null)
        {
            if (configFile != null)
            {
                reload();
            }
            else
            {
                instance = loadDefaultFromResources();
            }
        }
        return instance;
    }

    public double getPrice(String registry)
    {
        if (registry == null) return 0;
        Double price = prices.get(registry);
        return price != null ? price : 0;
    }

    public double getPrice(String registry, int meta)
    {
        if (registry == null) return 0;
        if (meta != 0)
        {
            String metaKey = registry + ":" + meta;
            Double metaPrice = prices.get(metaKey);
            if (metaPrice != null) return metaPrice;
        }
        return getPrice(registry);
    }

    public double getPriceFromItemStack(ItemStack stack)
    {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        net.minecraft.util.ResourceLocation reg;
        if (item instanceof net.minecraft.item.ItemBlock)
        {
            Block block = ((net.minecraft.item.ItemBlock) item).getBlock();
            reg = block.getRegistryName();
        }
        else
        {
            reg = item.getRegistryName();
        }
        if (reg != null)
        {
            int meta = stack.getMetadata();
            String metaKey = reg + ":" + meta;
            Double metaPrice = prices.get(metaKey);
            if (metaPrice != null) return metaPrice;
            return getPrice(reg.toString());
        }
        return 0;
    }

    public Map<String, Double> getPrices()
    {
        return Collections.unmodifiableMap(prices);
    }

    public List<Map.Entry<String, Double>> getPricesList()
    {
        return new ArrayList<>(prices.entrySet());
    }

    public void setPrice(String registry, double price)
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

    public void replaceAll(Map<String, Double> newPrices)
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

    public BalanceMode getBalanceMode()
    {
        try
        {
            return BalanceMode.valueOf(balanceMode);
        }
        catch (Exception e)
        {
            return BalanceMode.BREAK_BLOCK;
        }
    }

    public void setBalanceMode(BalanceMode mode)
    {
        this.balanceMode = mode != null ? mode.name() : BalanceMode.BREAK_BLOCK.name();
        save();
    }

    public static ItemStack createItemStack(String registry)
    {
        return createItemStack(registry, 0);
    }

    public static ItemStack createItemStack(String registry, int meta)
    {
        if (registry == null) return ItemStack.EMPTY;
        try
        {
            net.minecraft.util.ResourceLocation rl = new net.minecraft.util.ResourceLocation(registry);

            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block != null)
            {
                Item item = Item.getItemFromBlock(block);
                if (item != Item.getItemFromBlock(net.minecraft.init.Blocks.AIR))
                {
                    return new ItemStack(item, 1, meta);
                }
            }

            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != net.minecraft.init.Items.AIR)
            {
                return new ItemStack(item, 1, meta);
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
        return configFile;
    }

    public static void reload()
    {
        File file = getFile();
        BlockPriceConfig loaded = null;

        if (file != null && file.exists())
        {
            loaded = loadFromFile(file);
        }

        if (loaded == null || loaded.prices.isEmpty())
        {
            copyDefaultToConfigFile(file);
            if (file != null && file.exists())
            {
                loaded = loadFromFile(file);
            }
        }

        if (loaded == null || loaded.prices.isEmpty())
        {
            loaded = loadDefaultFromResources();
        }

        instance = loaded;
    }

    private static BlockPriceConfig loadFromFile(File file)
    {
        if (file == null || !file.exists())
        {
            return null;
        }

        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))
        {
            BlockPriceConfig loaded = GSON.fromJson(reader, BlockPriceConfig.class);
            if (loaded != null)
            {
                return loaded;
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load block_prices.json", e);
        }

        return null;
    }

    private static void copyDefaultToConfigFile(File file)
    {
        if (file == null)
        {
            return;
        }

        try (InputStream input = BlockPriceConfig.class.getResourceAsStream("/assets/oneblockultima/block_prices.json"))
        {
            if (input == null)
            {
                return;
            }

            if (file.getParentFile() != null && !file.getParentFile().exists())
            {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            }

            try (OutputStream output = Files.newOutputStream(file.toPath()))
            {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) >= 0)
                {
                    output.write(buffer, 0, read);
                }
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to copy default block_prices.json to config directory", e);
        }
    }

    public static BlockPriceConfig loadDefaultFromResources()
    {
        try (InputStream input = BlockPriceConfig.class.getResourceAsStream("/assets/oneblockultima/block_prices.json"))
        {
            if (input == null)
            {
                return new BlockPriceConfig();
            }

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
            {
                BlockPriceConfig loaded = GSON.fromJson(reader, BlockPriceConfig.class);
                return loaded != null ? loaded : new BlockPriceConfig();
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load default block_prices.json from resources", e);
            return new BlockPriceConfig();
        }
    }

    public void save()
    {
        File file = getFile();
        if (file == null) return;
        try
        {
            if (file.getParentFile() != null && !file.getParentFile().exists())
            {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            }

            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))
            {
                COMPACT_GSON.toJson(this, writer);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to save block prices", e);
        }
    }
}

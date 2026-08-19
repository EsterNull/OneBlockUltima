package ru.defea.oneblockultima.gui.containers;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameRegistry;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;

import java.util.*;

public class ContainerBlockPrices
{
    private final Map<String, Double> stagedPrices = new LinkedHashMap<>();
    private List<Map.Entry<String, Double>> filteredEntries = new ArrayList<>();
    private final List<SearchResult> searchResults = new ArrayList<>();
    private String searchQuery = "";
    private BlockPriceConfig.BalanceMode currentBalanceMode;

    private boolean setCostMultiplierMode;
    private double setCostIncreaseValue;

    private String editingRegistry = "";
    private String editingName = "";
    private int editingMeta = 0;
    private double editingPrice = 0;

    public static class SearchResult
    {
        public final String registry;
        public final String name;
        public final ItemStack stack;
        public final int meta;

        public SearchResult(String registry, String name, ItemStack stack)
        {
            this.registry = registry;
            this.name = name;
            this.stack = stack;
            this.meta = stack != null ? stack.getMetadata() : 0;
        }
    }

    public ContainerBlockPrices()
    {
        this.currentBalanceMode = BlockPriceConfig.get().getBalanceMode();
        BlockSetConfig.SettingsDefinition settings = BlockSetConfig.get().getSettings();
        this.setCostMultiplierMode = "multiplier".equalsIgnoreCase(settings.setCostIncreaseMode);
        this.setCostIncreaseValue = settings.setCostIncreaseValue;
        stagedPrices.clear();
        stagedPrices.putAll(BlockPriceConfig.get().getPrices());
        reloadPriceEntries();
    }

    public List<Map.Entry<String, Double>> getFilteredEntries() { return filteredEntries; }
    public List<SearchResult> getSearchResults() { return searchResults; }
    public String getSearchQuery() { return searchQuery; }
    public BlockPriceConfig.BalanceMode getCurrentBalanceMode() { return currentBalanceMode; }
    public boolean isSetCostMultiplierMode() { return setCostMultiplierMode; }
    public double getSetCostIncreaseValue() { return setCostIncreaseValue; }
    public String getEditingRegistry() { return editingRegistry; }
    public String getEditingName() { return editingName; }
    public int getEditingMeta() { return editingMeta; }
    public double getEditingPrice() { return editingPrice; }

    public void toggleSetCostMode()
    {
        setCostMultiplierMode = !setCostMultiplierMode;
    }

    public void setSetCostIncreaseValue(double value)
    {
        this.setCostIncreaseValue = Math.max(0, value);
    }

    public void reloadPriceEntries()
    {
        List<Map.Entry<String, Double>> priceEntries = new ArrayList<>(stagedPrices.entrySet());
        filteredEntries = new ArrayList<>(priceEntries);
    }

    public void resetToDefault()
    {
        BlockPriceConfig defaults = BlockPriceConfig.loadDefaultFromResources();
        stagedPrices.clear();
        stagedPrices.putAll(defaults.getPrices());
        currentBalanceMode = defaults.getBalanceMode();
        setCostMultiplierMode = false;
        setCostIncreaseValue = 50.0;
        reloadPriceEntries();
    }

    public void toggleBalanceMode()
    {
        currentBalanceMode = (currentBalanceMode == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
                ? BlockPriceConfig.BalanceMode.SELL_BLOCK
                : BlockPriceConfig.BalanceMode.BREAK_BLOCK;
    }

    public void startAddBlock()
    {
        editingRegistry = "";
        editingName = "";
        editingMeta = 0;
        editingPrice = 0;
        searchQuery = "";
    }

    public void setSearchQuery(String query)
    {
        this.searchQuery = query;
    }

    public void selectPriceEntry(int index)
    {
        Map.Entry<String, Double> e = filteredEntries.get(index);
        String entryKey = e.getKey();
        int entryMeta = parseMetaFromKey(entryKey);
        editingRegistry = entryMeta > 0 ? entryKey.substring(0, entryKey.lastIndexOf(':')) : entryKey;
        editingMeta = entryMeta;
        editingPrice = e.getValue();
        editingName = getBlockDisplayName(editingRegistry, editingMeta);
    }

    public void selectSearchResult(int index)
    {
        SearchResult r = searchResults.get(index);
        editingRegistry = r.registry;
        editingMeta = r.meta;
        editingName = r.name;
        String priceKey = editingMeta > 0 ? editingRegistry + ":" + editingMeta : editingRegistry;
        editingPrice = stagedPrices.getOrDefault(priceKey, 0.0);
    }

    public void deletePriceEntry(int index)
    {
        if (index < 0 || index >= filteredEntries.size()) return;
        Map.Entry<String, Double> e = filteredEntries.get(index);
        stagedPrices.remove(e.getKey());
        reloadPriceEntries();
    }

    public void deleteEditingPrice()
    {
        if (editingRegistry.isEmpty()) return;
        String priceKey = editingMeta > 0 ? editingRegistry + ":" + editingMeta : editingRegistry;
        stagedPrices.remove(priceKey);
        reloadPriceEntries();
    }

    public void savePrice(double price)
    {
        price = Math.max(0, price);

        if (editingRegistry.isEmpty()) return;

        String priceKey = editingMeta > 0 ? editingRegistry + ":" + editingMeta : editingRegistry;
        stagedPrices.put(priceKey, price);
    }

    public void flushToConfig()
    {
        BlockPriceConfig.get().replaceAll(stagedPrices);
        BlockPriceConfig.get().setBalanceMode(currentBalanceMode);

        BlockSetConfig.SettingsDefinition settings = BlockSetConfig.get().getSettings();
        settings.setCostIncreaseMode = setCostMultiplierMode ? "multiplier" : "fixed";
        settings.setCostIncreaseValue = setCostIncreaseValue;
        BlockSetConfig.invalidateComputedLevels();
        BlockSetConfig.saveCurrentConfig();
    }

    public void performSearch()
    {
        searchResults.clear();
        String query = searchQuery == null ? "" : searchQuery;
        boolean emptyQuery = query.isEmpty();

        String[] parts = emptyQuery ? new String[0] : query.split(" ");
        List<String> searchTerms = new ArrayList<>();
        String modFilter = null;
        String idFilter = null;

        for (String part : parts)
        {
            if (part.isEmpty()) continue;
            if (part.startsWith("@")) modFilter = part.substring(1).toLowerCase(Locale.ROOT);
            else if (part.startsWith("&")) idFilter = part.substring(1).toLowerCase(Locale.ROOT);
            else searchTerms.add(part.toLowerCase(Locale.ROOT));
        }

        for (Object keyObj : Block.blockRegistry.getKeys())
        {
            Block block = (Block) Block.blockRegistry.getObject(keyObj);
            if (block == null) continue;

            String registry = getRegistryName(block);
            if (registry == null) continue;

            int colon = registry.indexOf(':');
            String registryId = colon >= 0 ? registry.substring(colon + 1) : registry;
            String modId = colon >= 0 ? registry.substring(0, colon) : "";

            if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
            if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

            Item item = Item.getItemFromBlock(block);
            if (item == null) continue;

            List<ItemStack> subItems = new ArrayList<>();
            item.getSubItems(item, CreativeTabs.tabAllSearch, subItems);
            if (subItems.isEmpty()) subItems.add(new ItemStack(item, 1, 0));

            for (ItemStack subStack : subItems)
            {
                if (subStack == null || subStack.getItem() != item) continue;
                int meta = subStack.getMetadata();
                String key = registry + ":" + meta;
                if (stagedPrices.containsKey(key) || (meta == 0 && stagedPrices.containsKey(registry))) continue;

                String name = "";
                try { name = subStack.getDisplayName(); } catch (Exception ignored) {}
                if (!emptyQuery && !searchTerms.isEmpty() && mismatchesSearchTerms(name, searchTerms)) continue;

                searchResults.add(new SearchResult(registry, name, subStack.copy()));
            }
        }

        for (Object keyObj : Item.itemRegistry.getKeys())
        {
            Item item = (Item) Item.itemRegistry.getObject(keyObj);
            if (item == null || item instanceof ItemBlock) continue;
            if (item instanceof ru.defea.oneblockultima.item.ItemAdvancementIcon) continue;

            String registry = getRegistryName(item);
            if (registry == null) continue;

            int colon = registry.indexOf(':');
            String registryId = colon >= 0 ? registry.substring(colon + 1) : registry;
            String modId = colon >= 0 ? registry.substring(0, colon) : "";

            if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
            if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

            List<ItemStack> subItems = new ArrayList<>();
            item.getSubItems(item, CreativeTabs.tabAllSearch, subItems);
            if (subItems.isEmpty()) subItems.add(new ItemStack(item, 1, 0));

            for (ItemStack subStack : subItems)
            {
                if (subStack == null || subStack.getItem() != item) continue;
                int meta = subStack.getMetadata();
                String key = registry + ":" + meta;
                if (stagedPrices.containsKey(key) || (meta == 0 && stagedPrices.containsKey(registry))) continue;

                String name = "";
                try { name = subStack.getDisplayName(); } catch (Exception ignored) {}
                if (!emptyQuery && !searchTerms.isEmpty() && mismatchesSearchTerms(name, searchTerms)) continue;

                searchResults.add(new SearchResult(registry, name, subStack.copy()));
            }
        }

        searchResults.sort(Comparator.comparing(r -> r.name.toLowerCase(Locale.ROOT)));
    }

    private boolean mismatchesSearchTerms(String name, List<String> terms)
    {
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String term : terms)
        {
            if (!lowerName.contains(term)) return true;
        }
        return false;
    }

    private static String getRegistryName(Block block)
    {
        if (block == null) return null;
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
        if (id != null) return id.modId + ":" + id.name;
        Object name = Block.blockRegistry.getNameForObject(block);
        return name != null ? String.valueOf(name) : null;
    }

    private static String getRegistryName(Item item)
    {
        if (item == null) return null;
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(item);
        if (id != null) return id.modId + ":" + id.name;
        Object name = Item.itemRegistry.getNameForObject(item);
        return name != null ? String.valueOf(name) : null;
    }

    public String getBlockDisplayName(String registry, int meta)
    {
        ItemStack stack = BlockPriceConfig.createItemStack(registry, meta);
        if (stack != null)
        {
            try { return stack.getDisplayName(); } catch (Exception ignored) {}
        }
        return registry;
    }

    public static int parseMetaFromKey(String key)
    {
        if (key == null) return 0;
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0) return 0;
        try { return Integer.parseInt(key.substring(lastColon + 1)); }
        catch (NumberFormatException e) { return 0; }
    }

    public static String parseRegistryFromKey(String key)
    {
        if (key == null) return "";
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0) return key;
        try { Integer.parseInt(key.substring(lastColon + 1)); return key.substring(0, lastColon); }
        catch (NumberFormatException e) { return key; }
    }

    public static String formatPrice(double price)
    {
        if (price == (long) price) return String.valueOf((long) price);
        return String.valueOf(price);
    }
}

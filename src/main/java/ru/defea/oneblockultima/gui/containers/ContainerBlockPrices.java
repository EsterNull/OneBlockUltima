package ru.defea.oneblockultima.gui.containers;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.config.BlockPriceConfig;

import java.util.*;

public class ContainerBlockPrices
{
    private final Map<String, Double> stagedPrices = new LinkedHashMap<>();
    private List<Map.Entry<String, Double>> priceEntries = new ArrayList<>();
    private List<Map.Entry<String, Double>> filteredEntries = new ArrayList<>();
    private final List<SearchResult> searchResults = new ArrayList<>();
    private String searchQuery = "";
    private BlockPriceConfig.BalanceMode currentBalanceMode;

    private String editingRegistry = "";
    private String editingName = "";
    private int editingMeta = 0;
    private double editingPrice = 0;
    private boolean editingExisting = false;

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
            this.meta = stack.isEmpty() ? 0 : stack.getMetadata();
        }
    }

    public ContainerBlockPrices()
    {
        this.currentBalanceMode = BlockPriceConfig.get().getBalanceMode();
        stagedPrices.clear();
        stagedPrices.putAll(BlockPriceConfig.get().getPrices());
        reloadPriceEntries();
    }

    public List<Map.Entry<String, Double>> getFilteredEntries() { return filteredEntries; }
    public List<SearchResult> getSearchResults() { return searchResults; }
    public String getSearchQuery() { return searchQuery; }
    public BlockPriceConfig.BalanceMode getCurrentBalanceMode() { return currentBalanceMode; }
    public String getEditingRegistry() { return editingRegistry; }
    public String getEditingName() { return editingName; }
    public int getEditingMeta() { return editingMeta; }
    public double getEditingPrice() { return editingPrice; }

    public void reloadPriceEntries()
    {
        priceEntries = new ArrayList<>(stagedPrices.entrySet());
        filteredEntries = new ArrayList<>(priceEntries);
    }

    public void toggleBalanceMode()
    {
        currentBalanceMode = (currentBalanceMode == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
                ? BlockPriceConfig.BalanceMode.SELL_BLOCK
                : BlockPriceConfig.BalanceMode.BREAK_BLOCK;
    }

    public void startAddBlock()
    {
        editingExisting = false;
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
        editingExisting = true;
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
        editingExisting = stagedPrices.containsKey(priceKey);
    }

    public boolean deletePriceEntry(int index)
    {
        if (index < 0 || index >= filteredEntries.size()) return false;
        Map.Entry<String, Double> e = filteredEntries.get(index);
        stagedPrices.remove(e.getKey());
        reloadPriceEntries();
        return true;
    }

    public boolean savePrice(String priceText)
    {
        double price = 0;
        try
        {
            String text = priceText.trim().replace(',', '.');
            price = Double.parseDouble(text);
        }
        catch (NumberFormatException e) {}
        price = Math.max(0, price);

        if (editingRegistry.isEmpty()) return false;

        String priceKey = editingMeta > 0 ? editingRegistry + ":" + editingMeta : editingRegistry;
        stagedPrices.put(priceKey, price);
        return true;
    }

    public void flushToConfig()
    {
        BlockPriceConfig.get().replaceAll(stagedPrices);
        BlockPriceConfig.get().setBalanceMode(currentBalanceMode);
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

        for (net.minecraft.block.Block block : ForgeRegistries.BLOCKS)
        {
            ResourceLocation reg = block.getRegistryName();
            if (reg == null) continue;

            String registry = reg.toString();
            String registryId = reg.getResourcePath();
            String modId = reg.getResourceDomain();

            if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
            if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

            Item item = Item.getItemFromBlock(block);
            if (item == null || item == Items.AIR) continue;

            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);
            if (subItems.isEmpty()) subItems.add(new ItemStack(item, 1, 0));

            for (ItemStack subStack : subItems)
            {
                if (subStack.isEmpty() || subStack.getItem() != item) continue;
                int meta = subStack.getMetadata();
                String key = registry + ":" + meta;
                if (stagedPrices.containsKey(key) || (meta == 0 && stagedPrices.containsKey(registry))) continue;

                String name = "";
                try { name = subStack.getDisplayName(); } catch (Exception ignored) {}
                if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;

                searchResults.add(new SearchResult(registry, name, subStack.copy()));
            }
        }

        for (Item item : ForgeRegistries.ITEMS)
        {
            if (item == null || item == Items.AIR || item instanceof ItemBlock) continue;
            ResourceLocation reg = item.getRegistryName();
            if (reg == null) continue;

            String registry = reg.toString();
            String registryId = reg.getResourcePath();
            String modId = reg.getResourceDomain();

            if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
            if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);
            if (subItems.isEmpty()) subItems.add(new ItemStack(item, 1, 0));

            for (ItemStack subStack : subItems)
            {
                if (subStack.isEmpty() || subStack.getItem() != item) continue;
                int meta = subStack.getMetadata();
                String key = registry + ":" + meta;
                if (stagedPrices.containsKey(key) || (meta == 0 && stagedPrices.containsKey(registry))) continue;

                String name = "";
                try { name = subStack.getDisplayName(); } catch (Exception ignored) {}
                if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;

                searchResults.add(new SearchResult(registry, name, subStack.copy()));
            }
        }

        searchResults.sort(Comparator.comparing(r -> r.name.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesSearchTerms(String name, List<String> terms)
    {
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String term : terms)
        {
            if (!lowerName.contains(term)) return false;
        }
        return true;
    }

    public String getBlockDisplayName(String registry, int meta)
    {
        ItemStack stack = BlockPriceConfig.createItemStack(registry, meta);
        if (!stack.isEmpty())
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

package ru.defea.oneblockultima.gui.containers;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.config.BlockSetConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerSetsConfig
{
    public static final int VIEW_SETS = 0;
    public static final int VIEW_SET_DETAILS = 1;
    public static final int VIEW_ADD_ENTRY = 2;
    public static final int VIEW_CONFIRM_DELETE = 3;
    public static final int VIEW_EDIT = 4;
    public static final int VIEW_REQUIRED_MODS_EDITOR = 5;
    public static final int VIEW_REQUIRED_MODS_ADD = 6;
    public static final int VIEW_UNLOCK_CONDITIONS = 7;

    public enum SearchType { BLOCKS, MOBS }
    public enum EntryType { BLOCK, MOB }

    public static class BlockDisplayEntry
    {
        public final int blockIndex;
        public final int meta;
        public BlockDisplayEntry(int blockIndex, int meta) { this.blockIndex = blockIndex; this.meta = meta; }
    }

    public static class SearchResult
    {
        public final String registry;
        public final String name;
        public final String modId;
        public final ItemStack stack;
        public final Class<?> entityClass;
        public final boolean isMob;
        public final boolean isFluid;
        public final Fluid fluid;

        public SearchResult(String registry, String name, String modId, ItemStack stack)
        {
            this.registry = registry; this.name = name; this.modId = modId;
            this.stack = stack; this.isMob = false; this.isFluid = false;
            this.entityClass = null; this.fluid = null;
        }

        public SearchResult(String registry, String name, String modId, Class<?> entityClass)
        {
            this.registry = registry; this.name = name; this.modId = modId;
            this.stack = ItemStack.EMPTY; this.isMob = true; this.isFluid = false;
            this.entityClass = entityClass; this.fluid = null;
        }

        public SearchResult(String registry, String name, String modId, Fluid fluid)
        {
            this.registry = registry; this.name = name; this.modId = modId;
            this.stack = ItemStack.EMPTY; this.isMob = false; this.isFluid = true;
            this.entityClass = null; this.fluid = fluid;
        }
    }

    private static Map<String, Map<String, String>> staticSetLocalizedNames = new HashMap<>();

    private int currentView = VIEW_SETS;
    private BlockSetConfig config;
    private List<BlockSetConfig.BlockSetDefinition> sets = new ArrayList<>();
    private List<BlockSetConfig.BlockSetDefinition> filteredSets = new ArrayList<>();
    private List<SearchResult> searchResults = new ArrayList<>();

    private int selectedSetIndex = -1;
    private int selectedBlockIndex = -1;
    private int selectedBlockMeta = -1;
    private int selectedMobIndex = -1;
    private int deleteTargetIndex = -1;
    private int editingCurrencyIndex = -1;
    private int editingSetSourceIndex = -1;
    private EntryType editingEntryType = EntryType.BLOCK;

    private int scrollOffset = 0;
    private int entryScrollOffset = 0;
    private int searchScrollOffset = 0;
    private int requiredModsScrollOffset = 0;
    private int unlockConditionsScrollOffset = 0;

    private BlockSetConfig.BlockSetDefinition editingSet = null;
    private boolean isNewSet = false;
    private String statusMessage = "";
    private int statusTimer = 0;

    private String savedNewSetName = "";
    private String savedNewSetId = "";
    private String savedNewSetCost = "0";
    private String savedNewSetMods = "";

    private String searchQuery = "";
    private SearchType currentSearchType = SearchType.BLOCKS;
    private EntryType currentEntryType = EntryType.BLOCK;

    private List<String> requiredModsEditorMods = new ArrayList<>();
    private BlockSetConfig.SetRequiredModsDefinition.TYPE requiredModsEditorType = BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL;
    private boolean requiredModsEditorInitialized = false;
    private final Set<String> selectedRequiredModsForRemoval = new LinkedHashSet<>();
    private final Set<String> selectedRequiredModsToAdd = new LinkedHashSet<>();

    private String unlockConditionsEditorMode = "any";
    private final List<BlockSetConfig.UnlockConditionDefinition> unlockConditionsEditorConditions = new ArrayList<>();
    private int selectedUnlockConditionIndex = -1;
    private List<BlockSetConfig.BlockSetDefinition> availableSetsForConditions = new ArrayList<>();
    private String newConditionTypeToAdd = "broken_blocks_total";
    private String newConditionSetId = "";

    public ContainerSetsConfig()
    {
        loadStaticCustomNames();
        reloadConfig();
    }

    public int getCurrentView() { return currentView; }
    public int getSelectedSetIndex() { return selectedSetIndex; }
    public int getSelectedBlockIndex() { return selectedBlockIndex; }
    public void setSelectedBlockIndex(int v) { selectedBlockIndex = v; }
    public int getSelectedBlockMeta() { return selectedBlockMeta; }
    public void setSelectedBlockMeta(int v) { selectedBlockMeta = v; }
    public int getSelectedMobIndex() { return selectedMobIndex; }
    public void setSelectedMobIndex(int v) { selectedMobIndex = v; }
    public int getDeleteTargetIndex() { return deleteTargetIndex; }
    public int getEditingCurrencyIndex() { return editingCurrencyIndex; }
    public EntryType getEditingEntryType() { return editingEntryType; }

    public int getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(int v) { scrollOffset = v; }
    public int getEntryScrollOffset() { return entryScrollOffset; }
    public void setEntryScrollOffset(int v) { entryScrollOffset = v; }
    public int getSearchScrollOffset() { return searchScrollOffset; }
    public void setSearchScrollOffset(int v) { searchScrollOffset = v; }
    public int getRequiredModsScrollOffset() { return requiredModsScrollOffset; }
    public void setRequiredModsScrollOffset(int v) { requiredModsScrollOffset = v; }
    public int getUnlockConditionsScrollOffset() { return unlockConditionsScrollOffset; }
    public void setUnlockConditionsScrollOffset(int v) { unlockConditionsScrollOffset = v; }

    public BlockSetConfig.BlockSetDefinition getEditingSet() { return editingSet; }
    public boolean isNewSet() { return isNewSet; }
    public String getStatusMessage() { return statusMessage; }
    public int getStatusTimer() { return statusTimer; }
    public void setStatusTimer(int t) { statusTimer = t; }

    public String getSavedNewSetName() { return savedNewSetName; }
    public String getSavedNewSetId() { return savedNewSetId; }
    public String getSavedNewSetCost() { return savedNewSetCost; }
    public String getSavedNewSetMods() { return savedNewSetMods; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String q) { searchQuery = q; }
    public SearchType getCurrentSearchType() { return currentSearchType; }
    public void setCurrentSearchType(SearchType t) { currentSearchType = t; }
    public EntryType getCurrentEntryType() { return currentEntryType; }
    public void setCurrentEntryType(EntryType t) { currentEntryType = t; }

    public List<String> getRequiredModsEditorMods() { return requiredModsEditorMods; }
    public BlockSetConfig.SetRequiredModsDefinition.TYPE getRequiredModsEditorType() { return requiredModsEditorType; }
    public void setRequiredModsEditorType(BlockSetConfig.SetRequiredModsDefinition.TYPE t) { requiredModsEditorType = t; }
    public boolean isRequiredModsEditorInitialized() { return requiredModsEditorInitialized; }
    public void setRequiredModsEditorInitialized(boolean v) { requiredModsEditorInitialized = v; }
    public Set<String> getSelectedRequiredModsForRemoval() { return selectedRequiredModsForRemoval; }
    public Set<String> getSelectedRequiredModsToAdd() { return selectedRequiredModsToAdd; }

    public String getUnlockConditionsEditorMode() { return unlockConditionsEditorMode; }
    public void setUnlockConditionsEditorMode(String v) { unlockConditionsEditorMode = v; }
    public List<BlockSetConfig.UnlockConditionDefinition> getUnlockConditionsEditorConditions() { return unlockConditionsEditorConditions; }
    public int getSelectedUnlockConditionIndex() { return selectedUnlockConditionIndex; }
    public void setSelectedUnlockConditionIndex(int v) { selectedUnlockConditionIndex = v; }
    public List<BlockSetConfig.BlockSetDefinition> getAvailableSetsForConditions() { return availableSetsForConditions; }
    public String getNewConditionTypeToAdd() { return newConditionTypeToAdd; }
    public void setNewConditionTypeToAdd(String v) { newConditionTypeToAdd = v; }
    public String getNewConditionSetId() { return newConditionSetId; }
    public void setNewConditionSetId(String v) { newConditionSetId = v; }

    public List<BlockSetConfig.BlockSetDefinition> getSets() { return sets; }
    public List<BlockSetConfig.BlockSetDefinition> getFilteredSets() { return filteredSets; }
    public List<SearchResult> getSearchResults() { return searchResults; }

    public void reloadConfig()
    {
        BlockSetConfig.reload();
        config = BlockSetConfig.get();
        sets.clear();
        sets.addAll(config != null ? config.getSets() : Collections.emptyList());
        updateFilteredSets();
    }

    public void changeView(int view)
    {
        if (currentView != view)
        {
            if ((currentView == VIEW_REQUIRED_MODS_EDITOR || currentView == VIEW_REQUIRED_MODS_ADD)
                    && view != VIEW_REQUIRED_MODS_EDITOR && view != VIEW_REQUIRED_MODS_ADD)
            {
                requiredModsEditorInitialized = false;
            }
            currentView = view;
            scrollOffset = 0;
        }
    }

    public void updateFilteredSets()
    {
        if (searchQuery.isEmpty())
        {
            filteredSets = new ArrayList<>(sets);
            return;
        }
        String query = searchQuery.toLowerCase(Locale.ROOT);
        filteredSets = sets.stream()
                .filter(set -> {
                    String name = getLocalizedSetName(set).toLowerCase(Locale.ROOT);
                    return name.contains(query) || set.id.toLowerCase(Locale.ROOT).contains(query);
                })
                .collect(Collectors.toList());
    }

    public void discardEditingSetChanges()
    {
        editingSet = null;
        isNewSet = false;
        editingSetSourceIndex = -1;
        selectedBlockIndex = -1;
        selectedBlockMeta = -1;
        selectedMobIndex = -1;
        editingCurrencyIndex = -1;
        requiredModsEditorInitialized = false;
        reloadConfig();
    }

    public List<BlockDisplayEntry> buildBlockDisplayEntries()
    {
        List<BlockDisplayEntry> entries = new ArrayList<>();
        if (editingSet == null || editingSet.blocks == null) return entries;
        for (int i = 0; i < editingSet.blocks.size(); i++)
        {
            BlockSetConfig.BlockElementDefinition block = editingSet.blocks.get(i);
            if (block == null) continue;
            for (int meta : block.getMetaValues())
                entries.add(new BlockDisplayEntry(i, meta));
        }
        return entries;
    }

    public Set<String> getExistingBlockRegistries()
    {
        Set<String> result = new HashSet<>();
        if (editingSet != null && editingSet.blocks != null)
        {
            for (BlockSetConfig.BlockElementDefinition block : editingSet.blocks)
            {
                if (block != null && block.registry != null && !block.registry.isEmpty())
                {
                    List<Integer> metas = block.getMetaValues();
                    if (metas.isEmpty()) result.add(block.registry);
                    else for (int m : metas) result.add(block.registry + ":" + m);
                }
            }
        }
        return result;
    }

    public Set<String> getExistingMobRegistries()
    {
        Set<String> result = new HashSet<>();
        if (editingSet != null && editingSet.mobs != null)
        {
            for (BlockSetConfig.MobElementDefinition mob : editingSet.mobs)
                if (mob != null && mob.registry != null && !mob.registry.isEmpty())
                    result.add(mob.registry);
        }
        return result;
    }

    public void loadSetDetails(int index)
    {
        if (index < 0 || index >= sets.size()) return;
        selectedSetIndex = index;
        editingSetSourceIndex = index;
        editingSet = BlockSetConfig.copyBlockSetDefinition(sets.get(index));
        isNewSet = false;
        selectedBlockIndex = -1;
        selectedBlockMeta = -1;
        selectedMobIndex = -1;
        entryScrollOffset = 0;
        savedNewSetName = "";
        savedNewSetId = "";
        savedNewSetCost = "0";
        savedNewSetMods = "";
    }

    public void addNewSet()
    {
        BlockSetConfig.BlockSetDefinition newSet = new BlockSetConfig.BlockSetDefinition();
        newSet.id = "";
        newSet.unlockCost = 0;
        newSet.blocks = new ArrayList<>();
        newSet.mobs = new ArrayList<>();
        newSet.requiredMods = new BlockSetConfig.SetRequiredModsDefinition();
        newSet.unlockConditions = new BlockSetConfig.UnlockConditionGroup();
        newSet.unlockConditions.conditions = new ArrayList<>();
        editingSet = newSet;
        isNewSet = true;
        editingSetSourceIndex = -1;
        selectedBlockIndex = -1;
        selectedBlockMeta = -1;
        selectedMobIndex = -1;
        entryScrollOffset = 0;
        savedNewSetName = "";
        savedNewSetId = "";
        savedNewSetCost = "0";
        savedNewSetMods = "";
    }

    public boolean saveSetDetails(String name, String id, String costStr)
    {
        if (editingSet == null) return false;

        if (isNewSet)
        {
            if (id.isEmpty())
            {
                statusMessage = I18n.format("gui.oneblockultima.config.error.empty_id");
                statusTimer = 100;
                return false;
            }
            for (BlockSetConfig.BlockSetDefinition set : sets)
            {
                if (set != null && id.equals(set.id))
                {
                    statusMessage = I18n.format("gui.oneblockultima.config.error.duplicate_id");
                    statusTimer = 100;
                    return false;
                }
            }
            editingSet.id = id;
            if (!name.isEmpty()) saveLocalizedName(id, name);
            try { editingSet.unlockCost = Integer.parseInt(costStr); }
            catch (NumberFormatException e)
            {
                statusMessage = I18n.format("gui.oneblockultima.config.error.invalid_cost");
                statusTimer = 100;
                return false;
            }
            if (editingSet.blocks == null) editingSet.blocks = new ArrayList<>();
            if (editingSet.mobs == null) editingSet.mobs = new ArrayList<>();
            if (editingSet.requiredMods == null) editingSet.requiredMods = new BlockSetConfig.SetRequiredModsDefinition();
            sets.add(editingSet);
            updateFilteredSets();
            saveConfigToFile();
            statusMessage = I18n.format("gui.oneblockultima.config.set_created", id);
            statusTimer = 60;
            isNewSet = false;
            editingSet = null;
            savedNewSetName = "";
            savedNewSetId = "";
            savedNewSetCost = "0";
            savedNewSetMods = "";
            return true;
        }
        else
        {
            if (!name.isEmpty()) saveLocalizedName(editingSet.id, name);
            try { editingSet.unlockCost = Integer.parseInt(costStr); }
            catch (NumberFormatException e)
            {
                statusMessage = I18n.format("gui.oneblockultima.config.error.invalid_cost");
                statusTimer = 100;
                return false;
            }
            if (editingSetSourceIndex >= 0 && editingSetSourceIndex < sets.size())
                sets.set(editingSetSourceIndex, editingSet);
            saveConfigToFile();
            statusMessage = I18n.format("gui.oneblockultima.config.saved");
            statusTimer = 60;
            editingSet.computedLevels = null;
            editingSetSourceIndex = -1;
            updateFilteredSets();
            return true;
        }
    }

    public void confirmDeleteSet(int index)
    {
        if (index < 0 || index >= sets.size()) return;
        deleteTargetIndex = index;
    }

    public void executeDeleteSet()
    {
        if (deleteTargetIndex < 0 || deleteTargetIndex >= sets.size()) return;
        String id = sets.get(deleteTargetIndex).id;
        sets.remove(deleteTargetIndex);
        updateFilteredSets();
        if (selectedSetIndex >= sets.size()) selectedSetIndex = sets.size() - 1;
        saveConfigToFile();
        statusMessage = I18n.format("gui.oneblockultima.config.set_deleted", id);
        statusTimer = 60;
        deleteTargetIndex = -1;
    }

    public void initRequiredModsEditor()
    {
        if (editingSet == null) return;
        requiredModsEditorMods.clear();
        if (editingSet.requiredMods != null)
        {
            requiredModsEditorType = editingSet.requiredMods.getType();
            requiredModsEditorMods.addAll(editingSet.requiredMods.getMods());
        }
        else requiredModsEditorType = BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL;
        requiredModsEditorInitialized = true;
        requiredModsScrollOffset = 0;
    }

    public void applyRequiredModsToEditingSet()
    {
        if (editingSet == null) return;
        if (editingSet.requiredMods == null)
            editingSet.requiredMods = new BlockSetConfig.SetRequiredModsDefinition();
        editingSet.requiredMods.getMods().clear();
        editingSet.requiredMods.getMods().addAll(requiredModsEditorMods);
        editingSet.requiredMods.setType(requiredModsEditorType);
    }

    public List<String> getAllModIds()
    {
        List<String> result = new ArrayList<>();
        for (ModContainer mod : Loader.instance().getActiveModList())
        {
            String modId = mod.getModId();
            if (!modId.equals("minecraft") && !modId.equals("forge") && !modId.equals("mcp"))
                result.add(modId);
        }
        Collections.sort(result);
        return result;
    }

    public void addEntryToCurrentSet(EntryType type, SearchResult result, int baseLevel, int baseChance)
    {
        if (editingSet == null) return;
        if (type == EntryType.BLOCK)
        {
            BlockSetConfig.BlockElementDefinition entry = new BlockSetConfig.BlockElementDefinition();
            entry.registry = result.registry;
            entry.meta = result.stack != null && !result.stack.isEmpty() ? result.stack.getMetadata() : 0;
            entry.baseLevel = baseLevel;
            entry.baseChance = baseChance;
            if (editingSet.blocks == null) editingSet.blocks = new ArrayList<>();
            editingSet.blocks.add(entry);
            statusMessage = I18n.format("gui.oneblockultima.config.block_added", result.name);
        }
        else
        {
            BlockSetConfig.MobElementDefinition entry = new BlockSetConfig.MobElementDefinition();
            entry.registry = result.registry;
            entry.baseLevel = baseLevel;
            entry.baseChance = baseChance;
            entry.count = 1;
            if (editingSet.mobs == null) editingSet.mobs = new ArrayList<>();
            editingSet.mobs.add(entry);
            statusMessage = I18n.format("gui.oneblockultima.config.mob_added", result.name);
        }
        statusTimer = 60;
        editingSet.computedLevels = null;
    }

    public void removeSelectedEntry()
    {
        if (editingSet == null) return;
        if (selectedBlockIndex >= 0 && editingSet.blocks != null && selectedBlockIndex < editingSet.blocks.size())
        {
            BlockSetConfig.BlockElementDefinition block = editingSet.blocks.get(selectedBlockIndex);
            boolean hasSpecificMeta = selectedBlockMeta >= 0 && block.metas != null && block.metas.size() > 1 && block.metas.contains(selectedBlockMeta);
            if (hasSpecificMeta) block.metas.remove(Integer.valueOf(selectedBlockMeta));
            else editingSet.blocks.remove(selectedBlockIndex);
            statusMessage = I18n.format("gui.oneblockultima.config.removed");
            statusTimer = 60;
            selectedBlockIndex = -1;
            selectedBlockMeta = -1;
            editingSet.computedLevels = null;
        }
        else if (selectedMobIndex >= 0 && editingSet.mobs != null && selectedMobIndex < editingSet.mobs.size())
        {
            editingSet.mobs.remove(selectedMobIndex);
            statusMessage = I18n.format("gui.oneblockultima.config.removed");
            statusTimer = 60;
            selectedMobIndex = -1;
            editingSet.computedLevels = null;
        }
    }

    public void editEntry(int index, EntryType type)
    {
        if (editingSet == null) return;
        editingEntryType = type;
        editingCurrencyIndex = index;
    }

    public boolean saveCurrency(int newLevel, int newChance)
    {
        if (editingSet == null || editingCurrencyIndex < 0) return false;
        try
        {
            if (editingEntryType == EntryType.BLOCK && editingSet.blocks != null && editingCurrencyIndex < editingSet.blocks.size())
            {
                BlockSetConfig.BlockElementDefinition entry = editingSet.blocks.get(editingCurrencyIndex);
                boolean hasMultipleMetas = entry.metas != null && entry.metas.size() > 1;
                boolean hasSelectedMeta = selectedBlockMeta >= 0 && entry.metas != null && entry.metas.contains(selectedBlockMeta);
                if (hasMultipleMetas && hasSelectedMeta)
                {
                    entry.metas.remove(Integer.valueOf(selectedBlockMeta));
                    entry.meta = entry.metas.get(0);
                    BlockSetConfig.BlockElementDefinition split = new BlockSetConfig.BlockElementDefinition();
                    split.registry = entry.registry;
                    split.meta = selectedBlockMeta;
                    split.metas = new ArrayList<>();
                    split.metas.add(selectedBlockMeta);
                    split.baseLevel = newLevel;
                    split.baseChance = newChance;
                    split.nbtTags = entry.nbtTags;
                    editingSet.blocks.add(editingCurrencyIndex + 1, split);
                }
                else
                {
                    entry.baseLevel = newLevel;
                    entry.baseChance = newChance;
                }
            }
            else if (editingEntryType == EntryType.MOB && editingSet.mobs != null && editingCurrencyIndex < editingSet.mobs.size())
            {
                BlockSetConfig.MobElementDefinition entry = editingSet.mobs.get(editingCurrencyIndex);
                entry.baseLevel = newLevel;
                entry.baseChance = newChance;
            }
            else return false;
            editingSet.computedLevels = null;
            statusMessage = I18n.format("gui.oneblockultima.config.level_chance_updated");
            statusTimer = 60;
            return true;
        }
        catch (NumberFormatException e)
        {
            statusMessage = I18n.format("gui.oneblockultima.config.error.invalid_level_chance");
            statusTimer = 100;
            return false;
        }
    }

    public void initUnlockConditionsEditor()
    {
        if (editingSet == null) return;
        unlockConditionsEditorConditions.clear();
        if (editingSet.unlockConditions != null)
        {
            unlockConditionsEditorMode = editingSet.unlockConditions.mode;
            unlockConditionsEditorConditions.addAll(editingSet.unlockConditions.conditions);
        }
        else unlockConditionsEditorMode = "any";
        selectedUnlockConditionIndex = -1;
        unlockConditionsScrollOffset = 0;
        availableSetsForConditions.clear();
        for (BlockSetConfig.BlockSetDefinition s : sets)
            if (!s.id.equals(editingSet.id)) availableSetsForConditions.add(s);
        if (!availableSetsForConditions.isEmpty())
            newConditionSetId = availableSetsForConditions.get(0).id;
    }

    public void addUnlockCondition(String type, String setId, String levelStr, String countStr)
    {
        BlockSetConfig.UnlockConditionDefinition cond = new BlockSetConfig.UnlockConditionDefinition();
        cond.type = type;
        if ("set_level".equals(type) || "broken_blocks".equals(type)) cond.setId = setId;
        try { if (!levelStr.isEmpty()) cond.level = Integer.parseInt(levelStr); } catch (NumberFormatException ignored) {}
        try { if (!countStr.isEmpty()) cond.count = Integer.parseInt(countStr); } catch (NumberFormatException ignored) {}
        unlockConditionsEditorConditions.add(cond);
        advanceToNextUnusedSet(type);
    }

    private Set<String> getUsedSetIdsForType(String type)
    {
        Set<String> used = new HashSet<>();
        for (BlockSetConfig.UnlockConditionDefinition cond : unlockConditionsEditorConditions)
        {
            if (type.equals(cond.type) && cond.setId != null && !cond.setId.isEmpty())
                used.add(cond.setId);
        }
        return used;
    }

    private void advanceToNextUnusedSet(String type)
    {
        if (availableSetsForConditions.isEmpty()) { newConditionSetId = ""; return; }
        Set<String> used = getUsedSetIdsForType(type);
        List<BlockSetConfig.BlockSetDefinition> unused = new ArrayList<>();
        for (BlockSetConfig.BlockSetDefinition s : availableSetsForConditions)
            if (!used.contains(s.id)) unused.add(s);
        if (unused.isEmpty()) { newConditionSetId = ""; return; }
        int idx = 0;
        for (int i = 0; i < unused.size(); i++)
            if (unused.get(i).id.equals(newConditionSetId)) { idx = (i + 1) % unused.size(); break; }
        newConditionSetId = unused.get(idx).id;
    }

    public void deleteSelectedUnlockCondition()
    {
        if (selectedUnlockConditionIndex >= 0 && selectedUnlockConditionIndex < unlockConditionsEditorConditions.size())
        {
            unlockConditionsEditorConditions.remove(selectedUnlockConditionIndex);
            selectedUnlockConditionIndex = -1;
        }
    }

    public void applyUnlockConditionsToEditingSet()
    {
        if (editingSet == null) return;
        if (editingSet.unlockConditions == null)
            editingSet.unlockConditions = new BlockSetConfig.UnlockConditionGroup();
        editingSet.unlockConditions.mode = unlockConditionsEditorMode;
        editingSet.unlockConditions.conditions.clear();
        editingSet.unlockConditions.conditions.addAll(unlockConditionsEditorConditions);
    }

    public void resetToDefault()
    {
        try
        {
            File file = BlockSetConfig.getConfigFile();
            if (file == null)
            {
                statusMessage = I18n.format("gui.oneblockultima.status.reset_failed");
                statusTimer = 100;
                return;
            }
            if (file.getParentFile() != null && !file.getParentFile().exists())
                file.getParentFile().mkdirs();
            try (java.io.InputStream input = BlockSetConfig.class.getResourceAsStream("/assets/oneblockultima/blocksets.json"))
            {
                if (input == null)
                {
                    statusMessage = I18n.format("gui.oneblockultima.status.reset_failed");
                    statusTimer = 100;
                    return;
                }
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1)
                    output.write(buffer, 0, read);
                Files.write(file.toPath(), output.toByteArray());
            }
            reloadConfig();
            statusMessage = I18n.format("gui.oneblockultima.status.reset_success");
            statusTimer = 60;
        }
        catch (Exception e)
        {
            statusMessage = I18n.format("gui.oneblockultima.status.reset_failed");
            statusTimer = 100;
        }
    }

    public void performSearch(String query)
    {
        searchResults.clear();

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

        if (currentSearchType == SearchType.BLOCKS)
        {
            Set<String> existingBlocks = getExistingBlockRegistries();
            for (Block block : ForgeRegistries.BLOCKS)
            {
                ResourceLocation reg = block.getRegistryName();
                if (reg == null) continue;
                String registry = reg.toString();
                String registryId = reg.getResourcePath();
                String modId = reg.getResourceDomain();
                if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
                if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

                Fluid fluid = block instanceof IFluidBlock ? ((IFluidBlock) block).getFluid() : FluidRegistry.lookupFluidForBlock(block);
                if (fluid != null)
                {
                    if (existingBlocks.contains(registry)) continue;
                    String name = fluid.getLocalizedName(new FluidStack(fluid, 1000));
                    if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;
                    searchResults.add(new SearchResult(registry, name, modId, fluid));
                    continue;
                }

                Item item = Item.getItemFromBlock(block);
                if (item == null || item == Items.AIR) continue;
                NonNullList<ItemStack> subItems = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, subItems);
                if (subItems.isEmpty()) subItems.add(new ItemStack(item, 1, 0));
                for (ItemStack subStack : subItems)
                {
                    if (subStack.isEmpty() || subStack.getItem() != item) continue;
                    if (existingBlocks.contains(registry + ":" + subStack.getMetadata())) continue;
                    String name = "";
                    try { name = subStack.getDisplayName(); } catch (Exception ignored) {}
                    if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;
                    searchResults.add(new SearchResult(registry, name, modId, subStack.copy()));
                }
            }

            for (Item item : ForgeRegistries.ITEMS)
            {
                ResourceLocation reg = item.getRegistryName();
                if (reg == null) continue;
                if (item instanceof net.minecraft.item.ItemBlock) continue;
                if (item == Items.AIR) continue;
                String registry = reg.toString();
                String registryId = reg.getResourcePath();
                String modId = reg.getResourceDomain();
                if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
                if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;
                if (existingBlocks.contains(registry + ":0")) continue;
                String name = "";
                try { name = new ItemStack(item, 1).getDisplayName(); } catch (Exception ignored) {}
                if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;
                searchResults.add(new SearchResult(registry, name, modId, new ItemStack(item, 1)));
            }
        }

        if (currentSearchType == SearchType.MOBS)
        {
            Set<String> existingMobs = getExistingMobRegistries();
            Set<ResourceLocation> entityNames = EntityList.getEntityNameList();
            if (entityNames != null)
            {
                for (ResourceLocation reg : entityNames)
                {
                    String registry = reg.toString();
                    if (existingMobs.contains(registry)) continue;
                    String registryId = reg.getResourcePath();
                    String modId = reg.getResourceDomain();
                    if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
                    if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;
                    String name = registry;
                    try
                    {
                        String entityName = EntityList.getTranslationName(reg);
                        if (entityName != null && !entityName.isEmpty())
                        {
                            String translationKey = "entity." + entityName + ".name";
                            String localized = I18n.format(translationKey);
                            if (!localized.equals(translationKey)) name = localized;
                        }
                    } catch (Exception ignored) {}
                    if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;
                    Class<?> entityClass = EntityList.getClass(reg);
                    if (entityClass != null && EntityLivingBase.class.isAssignableFrom(entityClass))
                        searchResults.add(new SearchResult(registry, name, modId, entityClass));
                }
            }
        }

        searchResults.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        if (searchResults.size() > 200) searchResults = searchResults.subList(0, 200);
    }

    private boolean matchesSearchTerms(String name, List<String> searchTerms)
    {
        if (searchTerms == null || searchTerms.isEmpty()) return true;
        String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (String term : searchTerms)
        {
            if (term.isEmpty()) continue;
            if (!lowerName.contains(term)) return false;
        }
        return true;
    }

    public void saveConfigToFile()
    {
        try
        {
            List<BlockSetConfig.BlockSetDefinition> snapshot = new ArrayList<>(sets);
            BlockSetConfig.applySets(snapshot);
            BlockSetConfig.saveCurrentConfig();
            BlockSetConfig.reload();
            config = BlockSetConfig.get();
            sets.clear();
            sets.addAll(config != null ? config.getSets() : Collections.emptyList());
            updateFilteredSets();
        }
        catch (Exception e)
        {
            statusMessage = I18n.format("gui.oneblockultima.status.save_failed") + ": " + e.getMessage();
            statusTimer = 100;
        }
    }

    public ItemStack getItemStackFromEntry(BlockSetConfig.BlockElementDefinition entry, int meta)
    {
        ItemStack stack = ItemStack.EMPTY;
        try
        {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.registry));
            if (block != null)
            {
                Fluid fluid = getFluidForRegistry(entry.registry);
                if (fluid != null) return ItemStack.EMPTY;
                net.minecraft.block.state.IBlockState state = block.getStateFromMeta(meta);
                stack = block.getPickBlock(state, null, null, null, null);
                if (!stack.isEmpty()) return stack;
                Item item = Item.getItemFromBlock(block);
                if (item != null && item != Items.AIR) stack = new ItemStack(item, 1, meta);
            }
            if (stack.isEmpty())
            {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.registry));
                if (item != null && item != Items.AIR) stack = new ItemStack(item, 1, meta);
            }
        } catch (Exception ignored) {}
        return stack;
    }

    public ItemStack getItemStackFromEntry(BlockSetConfig.BlockElementDefinition entry)
    {
        return getItemStackFromEntry(entry, entry.meta);
    }

    public Fluid getFluidForRegistry(String registry)
    {
        try
        {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registry));
            if (block == null) return null;
            if (block instanceof IFluidBlock) return ((IFluidBlock) block).getFluid();
            return FluidRegistry.lookupFluidForBlock(block);
        } catch (Exception ignored) { return null; }
    }

    public static String getLocalizedSetName(BlockSetConfig.BlockSetDefinition set)
    {
        if (set == null || set.id == null) return "-";
        Minecraft mc = Minecraft.getMinecraft();
        String langCode = mc.getLanguageManager().getCurrentLanguage().getLanguageCode().toLowerCase();
        if (staticSetLocalizedNames.containsKey(set.id))
        {
            Map<String, String> langMap = staticSetLocalizedNames.get(set.id);
            if (langMap.containsKey(langCode)) return langMap.get(langCode);
        }
        String key = "gui.oneblockultima.set." + set.id;
        String localized = I18n.format(key);
        return localized.equals(key) ? set.id : localized;
    }

    public String getLocalizedNameForBlock(BlockSetConfig.BlockElementDefinition entry, int meta)
    {
        try
        {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.registry));
            if (block != null)
            {
                if (block instanceof IFluidBlock || FluidRegistry.lookupFluidForBlock(block) != null)
                {
                    Fluid fluid = block instanceof IFluidBlock ? ((IFluidBlock) block).getFluid() : FluidRegistry.lookupFluidForBlock(block);
                    if (fluid != null) return fluid.getLocalizedName(new FluidStack(fluid, 1000));
                }
                ItemStack stack = getItemStackFromEntry(entry, meta);
                if (!stack.isEmpty()) return stack.getDisplayName();
            }
        } catch (Exception ignored) {}
        return entry.registry + ":" + meta;
    }

    public String getLocalizedNameForBlock(BlockSetConfig.BlockElementDefinition entry)
    {
        return getLocalizedNameForBlock(entry, entry.meta);
    }

    public String getLocalizedNameForMob(BlockSetConfig.MobElementDefinition entry)
    {
        try
        {
            ResourceLocation reg = new ResourceLocation(entry.registry);
            String entityName = EntityList.getTranslationName(reg);
            if (entityName != null && !entityName.isEmpty())
            {
                String key = "entity." + entityName + ".name";
                String loc = I18n.format(key);
                if (!loc.equals(key)) return loc;
            }
        } catch (Exception ignored) {}
        return entry.registry;
    }

    public String getRequiredModsButtonLabel()
    {
        if (editingSet == null || editingSet.requiredMods == null)
            return I18n.format("gui.oneblockultima.config.required_mods") + ": -";
        BlockSetConfig.SetRequiredModsDefinition mods = editingSet.requiredMods;
        if (mods.getMods().isEmpty()) return I18n.format("gui.oneblockultima.config.required_mods") + ": " + I18n.format("gui.oneblockultima.config.none");
        return I18n.format("gui.oneblockultima.config.required_mods") + ": " + (mods.getType() == BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL ? I18n.format("gui.oneblockultima.config.all") : I18n.format("gui.oneblockultima.config.any")) + " [" + mods.getMods().size() + "]";
    }

    public String getUnlockConditionsButtonLabel()
    {
        if (editingSet == null || editingSet.unlockConditions == null)
            return I18n.format("gui.oneblockultima.unlock_conditions") + ": -";
        BlockSetConfig.UnlockConditionGroup group = editingSet.unlockConditions;
        if (group.conditions == null || group.conditions.isEmpty())
            return I18n.format("gui.oneblockultima.unlock_conditions") + ": " + I18n.format("gui.oneblockultima.config.none");
        return I18n.format("gui.oneblockultima.unlock_conditions") + ": " + ("all".equalsIgnoreCase(group.mode)
                ? I18n.format("gui.oneblockultima.config.all")
                : I18n.format("gui.oneblockultima.config.any")) + " [" + group.conditions.size() + "]";
    }

    public String getRequiredModsEditorTypeLabel()
    {
        return I18n.format(requiredModsEditorType == BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL ? "gui.oneblockultima.config.all" : "gui.oneblockultima.config.any");
    }

    public List<String> getModNamesForSelector()
    {
        List<String> result = new ArrayList<>();
        for (ModContainer mod : Loader.instance().getActiveModList())
        {
            String modId = mod.getModId();
            String modName = mod.getName();
            if (modId.equalsIgnoreCase("minecraft") || modId.equalsIgnoreCase("forge") || modId.equalsIgnoreCase("mcp") || modId.equalsIgnoreCase("fml") || modId.equals("oneblockultima") || "Forge Mod Loader".equals(modName)) continue;
            result.add(modName + " (" + modId + ")");
        }
        Collections.sort(result);
        return result;
    }

    public static class RequiredModEntry
    {
        public final String modId;
        public final String displayName;
        public RequiredModEntry(String modId, String displayName) { this.modId = modId; this.displayName = displayName; }
    }

    public List<RequiredModEntry> getCurrentRequiredModEntries()
    {
        List<RequiredModEntry> result = new ArrayList<>();
        for (String modId : requiredModsEditorMods)
        {
            String displayName = "";
            for (ModContainer mod : Loader.instance().getActiveModList())
            {
                if (mod.getModId().equals(modId)) { displayName = mod.getName(); break; }
            }
            result.add(new RequiredModEntry(modId, displayName));
        }
        return result;
    }

    public List<RequiredModEntry> getAvailableRequiredModEntries()
    {
        List<RequiredModEntry> result = new ArrayList<>();
        for (ModContainer mod : Loader.instance().getActiveModList())
        {
            String modId = mod.getModId();
            String modName = mod.getName();
            if (modId.equalsIgnoreCase("minecraft") || modId.equalsIgnoreCase("forge") || modId.equalsIgnoreCase("mcp") || modId.equalsIgnoreCase("fml") || modId.equals("oneblockultima") || "Forge Mod Loader".equals(modName)) continue;
            if (!requiredModsEditorMods.contains(modId))
                result.add(new RequiredModEntry(modId, modName));
        }
        Collections.sort(result, (a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        return result;
    }

    public void selectRequiredModForRemoval(String modId)
    {
        if (selectedRequiredModsForRemoval.contains(modId))
            selectedRequiredModsForRemoval.remove(modId);
        else selectedRequiredModsForRemoval.add(modId);
    }

    public void selectRequiredModToAdd(String modId)
    {
        if (selectedRequiredModsToAdd.contains(modId))
            selectedRequiredModsToAdd.remove(modId);
        else selectedRequiredModsToAdd.add(modId);
    }

    public void addSelectedRequiredMods()
    {
        requiredModsEditorMods.addAll(selectedRequiredModsToAdd);
        selectedRequiredModsToAdd.clear();
        requiredModsScrollOffset = 0;
    }

    public void deleteSelectedRequiredMods()
    {
        requiredModsEditorMods.removeAll(selectedRequiredModsForRemoval);
        selectedRequiredModsForRemoval.clear();
        requiredModsScrollOffset = 0;
    }

    public String cycleUnlockConditionType()
    {
        String[] types = {"broken_blocks_total", "broken_blocks", "set_level"};
        int idx = 0;
        for (int i = 0; i < types.length; i++) { if (types[i].equals(newConditionTypeToAdd)) { idx = i; break; } }
        idx = (idx + 1) % types.length;
        newConditionTypeToAdd = types[idx];
        if ("broken_blocks".equals(newConditionTypeToAdd) || "set_level".equals(newConditionTypeToAdd))
            advanceToNextUnusedSet(newConditionTypeToAdd);
        return newConditionTypeToAdd;
    }

    public String cycleUnlockConditionSet()
    {
        if (availableSetsForConditions.isEmpty()) return newConditionSetId;
        Set<String> used = getUsedSetIdsForType(newConditionTypeToAdd);
        List<BlockSetConfig.BlockSetDefinition> unused = new ArrayList<>();
        for (BlockSetConfig.BlockSetDefinition s : availableSetsForConditions)
            if (!used.contains(s.id)) unused.add(s);
        if (unused.isEmpty()) { newConditionSetId = ""; return newConditionSetId; }
        int idx = 0;
        for (int i = 0; i < unused.size(); i++)
        {
            if (unused.get(i).id.equals(newConditionSetId)) { idx = (i + 1) % unused.size(); break; }
        }
        newConditionSetId = unused.get(idx).id;
        return newConditionSetId;
    }

    public void saveLocalizedName(String setId, String name)
    {
        String langCode = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode().toLowerCase();
        if (langCode == null || langCode.isEmpty()) langCode = "en_us";
        staticSetLocalizedNames.computeIfAbsent(setId, k -> new HashMap<>()).put(langCode, name);
        saveCustomNames();
    }

    private void saveCustomNames()
    {
        try
        {
            File langDir = new File(Loader.instance().getConfigDir(), "oneblockultima/lang");
            if (!langDir.exists()) langDir.mkdirs();
            Map<String, List<String>> langLines = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> setEntry : staticSetLocalizedNames.entrySet())
            {
                String setId = setEntry.getKey();
                for (Map.Entry<String, String> langEntry : setEntry.getValue().entrySet())
                {
                    String langCode = langEntry.getKey();
                    String value = langEntry.getValue();
                    langLines.computeIfAbsent(langCode, k -> new ArrayList<>()).add("gui.oneblockultima.set." + setId + "=" + value);
                }
            }
            for (Map.Entry<String, List<String>> entry : langLines.entrySet())
                Files.write(new File(langDir, entry.getKey() + ".lang").toPath(), entry.getValue(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public static void loadStaticCustomNames()
    {
        staticSetLocalizedNames.clear();
        try
        {
            File langDir = new File(Loader.instance().getConfigDir(), "oneblockultima/lang");
            if (!langDir.exists()) return;
            for (File langFile : langDir.listFiles())
            {
                if (!langFile.getName().endsWith(".lang")) continue;
                String langCode = langFile.getName().replace(".lang", "").toLowerCase();
                List<String> lines = Files.readAllLines(langFile.toPath(), StandardCharsets.UTF_8);
                for (String line : lines)
                {
                    if (line.startsWith("gui.oneblockultima.set."))
                    {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2)
                        {
                            String key = parts[0];
                            String value = parts[1];
                            if (key.startsWith("gui.oneblockultima.set."))
                            {
                                String setId = key.substring("gui.oneblockultima.set.".length());
                                staticSetLocalizedNames.computeIfAbsent(setId, k -> new HashMap<>()).put(langCode, value);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}

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
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import static net.minecraftforge.common.util.Constants.NBT.*;
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
    public static final int VIEW_EDIT_NBT = 8;
    public static final int VIEW_NBT_ADD = 9;

    private static final int[] NBT_ADDABLE_TYPES = {
            TAG_STRING, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG,
            TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_INT_ARRAY,
            TAG_COMPOUND, TAG_LIST
    };

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

    private static final Map<String, Map<String, String>> staticSetLocalizedNames = new HashMap<>();

    private int currentView = VIEW_SETS;
    private BlockSetConfig config;
    private final List<BlockSetConfig.BlockSetDefinition> sets = new ArrayList<>();
    private List<BlockSetConfig.BlockSetDefinition> filteredSets = new ArrayList<>();
    private final List<SearchResult> searchResults = new ArrayList<>();

    private int selectedSetIndex = -1;
    private int selectedBlockIndex = -1;
    private int selectedBlockMeta = -1;
    private int selectedMobIndex = -1;
    private int deleteTargetIndex = -1;
    private int editingCurrencyIndex = -1;
    private int editingSetSourceIndex = -1;
    private EntryType editingEntryType = EntryType.BLOCK;

    private BlockSetConfig.BlockSetDefinition editingSet = null;
    private boolean pendingAddActive = false;
    private int pendingAddIndex = -1;
    private EntryType pendingAddType = EntryType.BLOCK;
    private String pendingAddRegistry = null;
    private boolean isNewSet = false;
    private String statusMessage = "";
    private int statusTimer = 0;

    private String savedNewSetName = "";
    private String savedNewSetId = "";
    private String savedNewSetCost = "0";

    private String searchQuery = "";
    private SearchType currentSearchType = SearchType.BLOCKS;
    private EntryType currentEntryType = EntryType.BLOCK;

    private final List<String> requiredModsEditorMods = new ArrayList<>();
    private BlockSetConfig.SetRequiredModsDefinition.TYPE requiredModsEditorType = BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL;
    private boolean requiredModsEditorInitialized = false;
    private final Set<String> selectedRequiredModsForRemoval = new LinkedHashSet<>();
    private final Set<String> selectedRequiredModsToAdd = new LinkedHashSet<>();

    private String unlockConditionsEditorMode = "any";
    private final List<BlockSetConfig.UnlockConditionDefinition> unlockConditionsEditorConditions = new ArrayList<>();
    private int selectedUnlockConditionIndex = -1;
    private final List<BlockSetConfig.BlockSetDefinition> availableSetsForConditions = new ArrayList<>();
    private String newConditionTypeToAdd = "broken_blocks_total";
    private String newConditionSetId = "";

    private final List<NbtEditorSegment> nbtEditorPath = new ArrayList<>();
    private int nbtEditorAddType = TAG_STRING;
    private int nbtEditorListElementType = TAG_BYTE;
    private String nbtEditorEditingKey = null;
    private int nbtEditorEditingIndex = -1;
    private String nbtEditorValueText = "";
    private String nbtEditorKeyText = "";

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

    public BlockSetConfig.BlockSetDefinition getEditingSet() { return editingSet; }
    public boolean isNewSet() { return isNewSet; }
    public String getStatusMessage() { return statusMessage; }
    public int getStatusTimer() { return statusTimer; }
    public void setStatusTimer(int t) { statusTimer = t; }

    public void setSavedNewSetName(String name) { this.savedNewSetName = name; }
    public void setSavedNewSetId(String id) { this.savedNewSetId = id; }
    public void setSavedNewSetCost(String cost) { this.savedNewSetCost = cost; }

    private static String safeFormat(String key, Object... args)
    {
        try
        {
            return I18n.format(key, args);
        }
        catch (Exception e)
        {
            return key;
        }
    }

    public String getSavedNewSetName() { return savedNewSetName; }
    public String getSavedNewSetId() { return savedNewSetId; }
    public String getSavedNewSetCost() { return savedNewSetCost; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String q) { searchQuery = q; }
    public void setCurrentSearchType(SearchType t) { currentSearchType = t; }
    public EntryType getCurrentEntryType() { return currentEntryType; }
    public void setCurrentEntryType(EntryType t) { currentEntryType = t; }

    public BlockSetConfig.SetRequiredModsDefinition.TYPE getRequiredModsEditorType() { return requiredModsEditorType; }
    public void setRequiredModsEditorType(BlockSetConfig.SetRequiredModsDefinition.TYPE t) { requiredModsEditorType = t; }
    public boolean isRequiredModsEditorInitialized() { return requiredModsEditorInitialized; }
    public void setRequiredModsEditorInitialized(boolean v) { requiredModsEditorInitialized = v; }
    public Set<String> getSelectedRequiredModsToAdd() { return selectedRequiredModsToAdd; }

    public String getUnlockConditionsEditorMode() { return unlockConditionsEditorMode; }
    public void setUnlockConditionsEditorMode(String v) { unlockConditionsEditorMode = v; }
    public List<BlockSetConfig.UnlockConditionDefinition> getUnlockConditionsEditorConditions() { return unlockConditionsEditorConditions; }
    public int getSelectedUnlockConditionIndex() { return selectedUnlockConditionIndex; }
    public void setSelectedUnlockConditionIndex(int v) { selectedUnlockConditionIndex = v; }
    public List<BlockSetConfig.BlockSetDefinition> getAvailableSetsForConditions() { return availableSetsForConditions; }
    public String getNewConditionTypeToAdd() { return newConditionTypeToAdd; }
    public String getNewConditionSetId() { return newConditionSetId; }

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
        clearPendingAdd();
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

    public Set<String> getExistingBlockKeys()
    {
        Set<String> result = new HashSet<>();
        if (editingSet != null && editingSet.blocks != null)
        {
            for (BlockSetConfig.BlockElementDefinition block : editingSet.blocks)
            {
                if (block == null || block.registry == null || block.registry.isEmpty()) continue;
                if (block.nbtTags != null && !block.nbtTags.hasNoTags()) continue;
                for (Integer meta : block.getMetaValues())
                {
                    result.add(block.registry + "@" + meta);
                }
            }
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
        savedNewSetName = "";
        savedNewSetId = "";
        savedNewSetCost = "0";
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
        savedNewSetName = "";
        savedNewSetId = "";
        savedNewSetCost = "0";
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
        }
        return true;
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
            entry.nbtTags = result.stack != null && result.stack.getTagCompound() != null
                    ? result.stack.getTagCompound().copy()
                    : new NBTTagCompound();
            if (editingSet.blocks == null) editingSet.blocks = new ArrayList<>();
            editingSet.blocks.add(entry);
            statusMessage = safeFormat("gui.oneblockultima.config.block_added", result.name);
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
            statusMessage = safeFormat("gui.oneblockultima.config.mob_added", result.name);
        }
        statusTimer = 60;
        editingSet.computedLevels = null;
    }

    public void stagePendingAdd(EntryType type, SearchResult result)
    {
        if (editingSet == null) return;
        pendingAddActive = true;
        pendingAddType = type;
        pendingAddRegistry = result.registry;
        pendingAddIndex = -1;
        if (type == EntryType.BLOCK)
        {
            BlockSetConfig.BlockElementDefinition entry = new BlockSetConfig.BlockElementDefinition();
            entry.registry = result.registry;
            entry.meta = result.stack != null && !result.stack.isEmpty() ? result.stack.getMetadata() : 0;
            entry.baseLevel = 1;
            entry.baseChance = 1;
            entry.nbtTags = result.stack != null && result.stack.getTagCompound() != null
                    ? result.stack.getTagCompound().copy()
                    : new NBTTagCompound();
            if (editingSet.blocks == null) editingSet.blocks = new ArrayList<>();
            editingSet.blocks.add(entry);
            pendingAddIndex = editingSet.blocks.size() - 1;
            selectedBlockIndex = pendingAddIndex;
            selectedBlockMeta = -1;
            selectedMobIndex = -1;
        }
        else
        {
            BlockSetConfig.MobElementDefinition entry = new BlockSetConfig.MobElementDefinition();
            entry.registry = result.registry;
            entry.baseLevel = 1;
            entry.baseChance = 1;
            entry.count = 1;
            if (editingSet.mobs == null) editingSet.mobs = new ArrayList<>();
            editingSet.mobs.add(entry);
            pendingAddIndex = editingSet.mobs.size() - 1;
            selectedMobIndex = pendingAddIndex;
            selectedBlockIndex = -1;
            selectedBlockMeta = -1;
        }
        editingEntryType = type;
        editingCurrencyIndex = pendingAddIndex;
        editingSet.computedLevels = null;
    }

    public void clearPendingAdd()
    {
        pendingAddActive = false;
        pendingAddIndex = -1;
        pendingAddRegistry = null;
    }

    public void cancelPendingAdd()
    {
        if (!pendingAddActive) return;
        int index = pendingAddIndex;
        EntryType type = pendingAddType;
        String registry = pendingAddRegistry;
        clearPendingAdd();
        editingCurrencyIndex = -1;
        selectedBlockIndex = -1;
        selectedBlockMeta = -1;
        selectedMobIndex = -1;
        if (editingSet == null || index < 0) return;
        if (type == EntryType.BLOCK && editingSet.blocks != null && index < editingSet.blocks.size())
        {
            BlockSetConfig.BlockElementDefinition e = editingSet.blocks.get(index);
            if (e != null && registry != null && registry.equals(e.registry))
            {
                editingSet.blocks.remove(index);
                editingSet.computedLevels = null;
            }
        }
        else if (type == EntryType.MOB && editingSet.mobs != null && index < editingSet.mobs.size())
        {
            BlockSetConfig.MobElementDefinition e = editingSet.mobs.get(index);
            if (e != null && registry != null && registry.equals(e.registry))
            {
                editingSet.mobs.remove(index);
                editingSet.computedLevels = null;
            }
        }
    }

    public void removeSelectedEntry()
    {
        if (editingSet == null) return;
        clearPendingAdd();
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

    public String getEditingEntryDisplayName()
    {
        if (editingSet == null || editingCurrencyIndex < 0) return "";
        if (editingEntryType == EntryType.BLOCK && editingSet.blocks != null && editingCurrencyIndex < editingSet.blocks.size())
        {
            BlockSetConfig.BlockElementDefinition entry = editingSet.blocks.get(editingCurrencyIndex);
            return getLocalizedNameForBlock(entry, getSelectedBlockMeta() >= 0 ? getSelectedBlockMeta() : entry.meta);
        }
        if (editingEntryType == EntryType.MOB && editingSet.mobs != null && editingCurrencyIndex < editingSet.mobs.size())
        {
            return getLocalizedNameForMob(editingSet.mobs.get(editingCurrencyIndex));
        }
        return "";
    }

    public NBTTagCompound getEditingEntryNbt()
    {
        NBTTagCompound tags = getEditingEntryNbtOrNull();
        return tags != null ? tags.copy() : new NBTTagCompound();
    }

    // ======== Structured NBT editor ========

    public static class NbtTagEntry
    {
        public final String key;
        public final int index;
        public final NBTBase value;

        public NbtTagEntry(String key, int index, NBTBase value)
        {
            this.key = key;
            this.index = index;
            this.value = value;
        }

        public int getTypeId()
        {
            return value != null ? value.getId() : TAG_END;
        }

        public boolean isCompound()
        {
            return value instanceof NBTTagCompound;
        }

        public boolean isList()
        {
            return value instanceof NBTTagList;
        }

        public boolean isArray()
        {
            return value instanceof NBTTagByteArray || value instanceof NBTTagIntArray;
        }

        public boolean isScalar()
        {
            return value != null && !isCompound() && !isList() && !isArray();
        }
    }

    private static class NbtEditorSegment
    {
        boolean list;
        String key;
        int index;

        NbtEditorSegment(String key)
        {
            this.list = false;
            this.key = key;
            this.index = -1;
        }

        NbtEditorSegment(int index)
        {
            this.list = true;
            this.key = null;
            this.index = index;
        }

        String label()
        {
            return list ? "[" + index + "]" : key;
        }
    }

    public List<NbtTagEntry> getNbtTags()
    {
        List<NbtTagEntry> result = new ArrayList<>();
        NBTBase node = getNbtEditorCurrent();
        if (node instanceof NBTTagCompound)
        {
            NBTTagCompound compound = (NBTTagCompound) node;
            for (String key : compound.getKeySet())
            {
                result.add(new NbtTagEntry(key, -1, compound.getTag(key)));
            }
        }
        else if (node instanceof NBTTagList)
        {
            NBTTagList list = (NBTTagList) node;
            for (int i = 0; i < list.tagCount(); i++)
            {
                result.add(new NbtTagEntry("[" + i + "]", i, list.get(i)));
            }
        }
        else if (node instanceof NBTTagByteArray)
        {
            byte[] arr = ((NBTTagByteArray) node).getByteArray();
            for (int i = 0; i < arr.length; i++)
            {
                result.add(new NbtTagEntry("[" + i + "]", i, new NBTTagByte(arr[i])));
            }
        }
        else if (node instanceof NBTTagIntArray)
        {
            int[] arr = ((NBTTagIntArray) node).getIntArray();
            for (int i = 0; i < arr.length; i++)
            {
                result.add(new NbtTagEntry("[" + i + "]", i, new NBTTagInt(arr[i])));
            }
        }
        return result;
    }

    public boolean nbtEditorAtRoot()
    {
        return nbtEditorPath.isEmpty();
    }

    public boolean nbtEditorIsListContext()
    {
        return getNbtEditorCurrent() instanceof NBTTagList;
    }

    public boolean nbtEditorIsArrayContext()
    {
        NBTBase node = getNbtEditorCurrent();
        return node instanceof NBTTagByteArray || node instanceof NBTTagIntArray;
    }

    public int nbtEditorGetArrayElementType()
    {
        NBTBase node = getNbtEditorCurrent();
        return node instanceof NBTTagIntArray ? TAG_INT : TAG_BYTE;
    }

    public String nbtEditorPathLabel()
    {
        StringBuilder sb = new StringBuilder();
        for (NbtEditorSegment seg : nbtEditorPath)
        {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(seg.label());
        }
        return sb.toString();
    }

    public void nbtEditorPush(String key)
    {
        NBTBase node = getNbtEditorCurrent();
        if (!(node instanceof NBTTagCompound)) return;
        NBTTagCompound compound = (NBTTagCompound) node;
        if (!compound.hasKey(key)) return;
        NBTBase target = compound.getTag(key);
        if (!isContainer(target)) return;
        nbtEditorPath.add(new NbtEditorSegment(key));
    }

    public void nbtEditorPushIndex(int index)
    {
        NBTBase node = getNbtEditorCurrent();
        if (!(node instanceof NBTTagList)) return;
        NBTTagList list = (NBTTagList) node;
        if (index < 0 || index >= list.tagCount()) return;
        NBTBase target = list.get(index);
        if (!isContainer(target)) return;
        nbtEditorPath.add(new NbtEditorSegment(index));
    }

    private static boolean isContainer(NBTBase value)
    {
        return value instanceof NBTTagCompound
                || value instanceof NBTTagList
                || value instanceof NBTTagByteArray
                || value instanceof NBTTagIntArray;
    }

    public void nbtEditorPop()
    {
        if (!nbtEditorPath.isEmpty()) nbtEditorPath.remove(nbtEditorPath.size() - 1);
    }

    public int getNbtEditorAddType()
    {
        return nbtEditorAddType;
    }

    public void cycleNbtEditorAddType()
    {
        for (int i = 0; i < NBT_ADDABLE_TYPES.length; i++)
        {
            if (NBT_ADDABLE_TYPES[i] == nbtEditorAddType)
            {
                nbtEditorAddType = NBT_ADDABLE_TYPES[(i + 1) % NBT_ADDABLE_TYPES.length];
                return;
            }
        }
        nbtEditorAddType = NBT_ADDABLE_TYPES[0];
    }

    public int getNbtEditorListElementType()
    {
        NBTBase node = getNbtEditorCurrent();
        if (node instanceof NBTTagList && ((NBTTagList) node).tagCount() > 0)
        {
            return ((NBTTagList) node).getTagType();
        }
        return nbtEditorListElementType;
    }

    public boolean nbtEditorListTypeIsFixed()
    {
        NBTBase node = getNbtEditorCurrent();
        return node instanceof NBTTagList && ((NBTTagList) node).tagCount() > 0;
    }

    public void cycleNbtEditorListElementType()
    {
        for (int i = 0; i < NBT_ADDABLE_TYPES.length; i++)
        {
            if (NBT_ADDABLE_TYPES[i] == nbtEditorListElementType)
            {
                nbtEditorListElementType = NBT_ADDABLE_TYPES[(i + 1) % NBT_ADDABLE_TYPES.length];
                return;
            }
        }
        nbtEditorListElementType = NBT_ADDABLE_TYPES[0];
    }

    public void nbtEditorStartAdd()
    {
        nbtEditorEditingKey = null;
        nbtEditorEditingIndex = -1;
        nbtEditorAddType = TAG_STRING;
        nbtEditorListElementType = TAG_BYTE;
        nbtEditorValueText = "";
        nbtEditorKeyText = "";
    }

    public String nbtEditorGetValueText()
    {
        return nbtEditorValueText;
    }

    public void setNbtEditorValueText(String text)
    {
        nbtEditorValueText = text == null ? "" : text;
    }

    public String nbtEditorGetKeyText()
    {
        return nbtEditorKeyText;
    }

    public void setNbtEditorKeyText(String text)
    {
        nbtEditorKeyText = text == null ? "" : text;
    }

    public void nbtEditorStartEdit(String key)
    {
        NBTBase node = getNbtEditorCurrent();
        if (!(node instanceof NBTTagCompound)) return;
        NBTTagCompound compound = (NBTTagCompound) node;
        if (!compound.hasKey(key)) return;
        NBTBase target = compound.getTag(key);
        if (isContainer(target)) return;
        nbtEditorEditingKey = key;
        nbtEditorEditingIndex = -1;
        nbtEditorAddType = target.getId();
        nbtEditorKeyText = key;
        nbtEditorValueText = formatNbtValue(target);
    }

    public void nbtEditorStartEditIndex(int index)
    {
        NBTBase node = getNbtEditorCurrent();
        if (node instanceof NBTTagList)
        {
            NBTTagList list = (NBTTagList) node;
            if (index < 0 || index >= list.tagCount()) return;
            if (isContainer(list.get(index))) return;
            nbtEditorEditingIndex = index;
            nbtEditorEditingKey = null;
            nbtEditorAddType = list.get(index).getId();
            nbtEditorKeyText = "";
            nbtEditorValueText = formatNbtValue(list.get(index));
        }
        else if (node instanceof NBTTagByteArray || node instanceof NBTTagIntArray)
        {
            int size = getArraySize(node);
            if (index < 0 || index >= size) return;
            nbtEditorEditingIndex = index;
            nbtEditorEditingKey = null;
            nbtEditorAddType = nbtEditorGetArrayElementType();
            nbtEditorKeyText = "";
            nbtEditorValueText = formatNbtValue(getArrayElement(node, index));
        }
    }

    public boolean nbtEditorIsEditing()
    {
        return nbtEditorEditingKey != null || nbtEditorEditingIndex >= 0;
    }

    public boolean nbtEditorIsEditingListElement()
    {
        return nbtEditorEditingIndex >= 0;
    }

    public int nbtEditorGetEditingTypeId()
    {
        return nbtEditorAddType;
    }

    public String nbtEditorGetEditingKey()
    {
        return nbtEditorEditingKey != null ? nbtEditorEditingKey : "";
    }

    public String nbtEditorGetValue()
    {
        NBTBase target = null;
        NBTBase node = getNbtEditorCurrent();
        if (nbtEditorEditingKey != null && node instanceof NBTTagCompound)
        {
            target = ((NBTTagCompound) node).getTag(nbtEditorEditingKey);
        }
        else if (nbtEditorEditingIndex >= 0 && node instanceof NBTTagList)
        {
            NBTTagList list = (NBTTagList) node;
            if (nbtEditorEditingIndex < list.tagCount()) target = list.get(nbtEditorEditingIndex);
        }
        else if (nbtEditorEditingIndex >= 0 && (node instanceof NBTTagByteArray || node instanceof NBTTagIntArray))
        {
            if (nbtEditorEditingIndex < getArraySize(node)) target = getArrayElement(node, nbtEditorEditingIndex);
        }
        if (target == null) return "";
        return formatNbtValue(target);
    }

    public boolean nbtEditorApply(String key, String valueText)
    {
        try
        {
            if (nbtEditorIsListContext())
            {
                NBTTagList list = getNbtEditorList();
                if (list == null) return false;
                int elementType = list.tagCount() > 0 ? list.getTagType() : nbtEditorListElementType;
                NBTBase value = parseNbtValue(elementType, valueText);
                if (nbtEditorEditingIndex >= 0)
                {
                    if (nbtEditorEditingIndex >= list.tagCount()) return false;
                    list.set(nbtEditorEditingIndex, value);
                }
                else if (list.tagCount() == 0)
                {
                    NBTTagList fresh = new NBTTagList();
                    fresh.appendTag(value);
                    if (!setNbtEditorCurrent(fresh)) return false;
                }
                else
                {
                    list.appendTag(value);
                }
                nbtEditorEditingIndex = -1;
                nbtEditorEditingKey = null;
                nbtEditorValueText = "";
                nbtEditorKeyText = "";
                nbtEditorMarkDirty();
                return true;
            }

            if (nbtEditorIsArrayContext())
            {
                NBTBase node = getNbtEditorCurrent();
                if (node == null) return false;
                NBTBase value = parseNbtValue(nbtEditorGetArrayElementType(), valueText);
                NBTBase replacement = nbtEditorEditingIndex >= 0
                        ? setArrayElement(node, nbtEditorEditingIndex, value)
                        : appendArrayElement(node, value);
                if (replacement == null) return false;
                if (!setNbtEditorCurrent(replacement)) return false;
                nbtEditorEditingIndex = -1;
                nbtEditorEditingKey = null;
                nbtEditorValueText = "";
                nbtEditorKeyText = "";
                nbtEditorMarkDirty();
                return true;
            }

            NBTTagCompound compound = getNbtEditorCompound();
            if (compound == null) return false;

            if (nbtEditorEditingKey != null)
            {
                String newKey = key == null ? "" : key.trim();
                if (newKey.isEmpty())
                {
                    statusMessage = safeFormat("gui.oneblockultima.config.nbt_error_empty_key");
                    statusTimer = 100;
                    return false;
                }
                NBTBase value = parseNbtValue(nbtEditorAddType, valueText);
                compound.removeTag(nbtEditorEditingKey);
                compound.setTag(newKey, value);
                nbtEditorEditingKey = null;
                nbtEditorValueText = "";
                nbtEditorKeyText = "";
                nbtEditorMarkDirty();
                return true;
            }

            String trimmedKey = key == null ? "" : key.trim();
            if (trimmedKey.isEmpty())
            {
                statusMessage = safeFormat("gui.oneblockultima.config.nbt_error_empty_key");
                statusTimer = 100;
                return false;
            }
            NBTBase value = parseNbtValue(nbtEditorAddType, valueText);
            compound.setTag(trimmedKey, value);
            nbtEditorValueText = "";
            nbtEditorKeyText = "";
            nbtEditorMarkDirty();
            return true;
        }
        catch (NumberFormatException e)
        {
            statusMessage = safeFormat("gui.oneblockultima.config.nbt_error_invalid_value");
            statusTimer = 100;
            return false;
        }
    }

    public boolean nbtEditorRemove(String key)
    {
        NBTTagCompound compound = getNbtEditorCompound();
        if (compound == null || !compound.hasKey(key)) return false;
        compound.removeTag(key);
        nbtEditorMarkDirty();
        return true;
    }

    public boolean nbtEditorRemoveIndex(int index)
    {
        NBTBase node = getNbtEditorCurrent();
        if (node instanceof NBTTagList)
        {
            NBTTagList list = (NBTTagList) node;
            if (index < 0 || index >= list.tagCount()) return false;
            list.removeTag(index);
            nbtEditorMarkDirty();
            return true;
        }
        if (node instanceof NBTTagByteArray || node instanceof NBTTagIntArray)
        {
            NBTBase replacement = removeArrayElement(node, index);
            if (replacement == null) return false;
            if (!setNbtEditorCurrent(replacement)) return false;
            nbtEditorMarkDirty();
            return true;
        }
        return false;
    }

    private void nbtEditorMarkDirty()
    {
        if (editingSet != null) editingSet.computedLevels = null;
    }

    private NBTTagCompound getNbtEditorCompound()
    {
        NBTBase node = getNbtEditorCurrent();
        return node instanceof NBTTagCompound ? (NBTTagCompound) node : null;
    }

    private NBTTagList getNbtEditorList()
    {
        NBTBase node = getNbtEditorCurrent();
        return node instanceof NBTTagList ? (NBTTagList) node : null;
    }

    private NBTBase getNbtEditorCurrent()
    {
        NBTBase node = getEditingEntryNbtOrNull();
        if (node == null) return null;
        for (NbtEditorSegment seg : nbtEditorPath)
        {
            if (seg.list)
            {
                if (!(node instanceof NBTTagList)) return null;
                NBTTagList list = (NBTTagList) node;
                if (seg.index < 0 || seg.index >= list.tagCount()) return null;
                node = list.get(seg.index);
            }
            else
            {
                if (!(node instanceof NBTTagCompound)) return null;
                NBTTagCompound compound = (NBTTagCompound) node;
                if (!compound.hasKey(seg.key)) return null;
                node = compound.getTag(seg.key);
            }
        }
        return node;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean setNbtEditorCurrent(NBTBase newNode)
    {
        if (nbtEditorPath.isEmpty()) return false;
        NbtEditorSegment last = nbtEditorPath.get(nbtEditorPath.size() - 1);
        NBTBase parent = getNbtEditorParent();
        if (parent instanceof NBTTagCompound)
        {
            ((NBTTagCompound) parent).setTag(last.key, newNode);
            return true;
        }
        if (parent instanceof NBTTagList)
        {
            ((NBTTagList) parent).set(last.index, newNode);
            return true;
        }
        return false;
    }

    private static int getArraySize(NBTBase node)
    {
        if (node instanceof NBTTagByteArray) return ((NBTTagByteArray) node).getByteArray().length;
        if (node instanceof NBTTagIntArray) return ((NBTTagIntArray) node).getIntArray().length;
        return 0;
    }

    private static NBTBase getArrayElement(NBTBase node, int index)
    {
        if (node instanceof NBTTagByteArray) return new NBTTagByte(((NBTTagByteArray) node).getByteArray()[index]);
        if (node instanceof NBTTagIntArray) return new NBTTagInt(((NBTTagIntArray) node).getIntArray()[index]);
        return null;
    }

    private static NBTBase setArrayElement(NBTBase node, int index, NBTBase value)
    {
        if (node instanceof NBTTagByteArray)
        {
            byte[] src = ((NBTTagByteArray) node).getByteArray();
            if (index < 0 || index >= src.length) return null;
            byte[] dst = src.clone();
            dst[index] = ((NBTTagByte) value).getByte();
            return new NBTTagByteArray(dst);
        }
        if (node instanceof NBTTagIntArray)
        {
            int[] src = ((NBTTagIntArray) node).getIntArray();
            if (index < 0 || index >= src.length) return null;
            int[] dst = src.clone();
            dst[index] = ((NBTTagInt) value).getInt();
            return new NBTTagIntArray(dst);
        }
        return null;
    }

    private static NBTBase appendArrayElement(NBTBase node, NBTBase value)
    {
        if (node instanceof NBTTagByteArray)
        {
            byte[] src = ((NBTTagByteArray) node).getByteArray();
            byte[] dst = Arrays.copyOf(src, src.length + 1);
            dst[src.length] = ((NBTTagByte) value).getByte();
            return new NBTTagByteArray(dst);
        }
        if (node instanceof NBTTagIntArray)
        {
            int[] src = ((NBTTagIntArray) node).getIntArray();
            int[] dst = Arrays.copyOf(src, src.length + 1);
            dst[src.length] = ((NBTTagInt) value).getInt();
            return new NBTTagIntArray(dst);
        }
        return null;
    }

    private static NBTBase removeArrayElement(NBTBase node, int index)
    {
        if (node instanceof NBTTagByteArray)
        {
            byte[] src = ((NBTTagByteArray) node).getByteArray();
            if (index < 0 || index >= src.length) return null;
            byte[] dst = new byte[src.length - 1];
            System.arraycopy(src, 0, dst, 0, index);
            System.arraycopy(src, index + 1, dst, index, src.length - index - 1);
            return new NBTTagByteArray(dst);
        }
        if (node instanceof NBTTagIntArray)
        {
            int[] src = ((NBTTagIntArray) node).getIntArray();
            if (index < 0 || index >= src.length) return null;
            int[] dst = new int[src.length - 1];
            System.arraycopy(src, 0, dst, 0, index);
            System.arraycopy(src, index + 1, dst, index, src.length - index - 1);
            return new NBTTagIntArray(dst);
        }
        return null;
    }

    private NBTBase getNbtEditorParent()
    {
        NBTBase node = getEditingEntryNbtOrNull();
        if (node == null) return null;
        for (int i = 0; i < nbtEditorPath.size() - 1; i++)
        {
            NbtEditorSegment seg = nbtEditorPath.get(i);
            if (seg.list)
            {
                if (!(node instanceof NBTTagList)) return null;
                NBTTagList list = (NBTTagList) node;
                if (seg.index < 0 || seg.index >= list.tagCount()) return null;
                node = list.get(seg.index);
            }
            else
            {
                if (!(node instanceof NBTTagCompound)) return null;
                NBTTagCompound compound = (NBTTagCompound) node;
                if (!compound.hasKey(seg.key)) return null;
                node = compound.getTag(seg.key);
            }
        }
        return node;
    }

    private static NBTBase parseNbtValue(int typeId, String text)
    {
        switch (typeId)
        {
            case TAG_STRING: return new NBTTagString(text);
            case TAG_BYTE: return new NBTTagByte(Byte.parseByte(text.trim()));
            case TAG_SHORT: return new NBTTagShort(Short.parseShort(text.trim()));
            case TAG_INT: return new NBTTagInt(Integer.parseInt(text.trim()));
            case TAG_LONG: return new NBTTagLong(Long.parseLong(text.trim()));
            case TAG_FLOAT: return new NBTTagFloat(Float.parseFloat(text.trim()));
            case TAG_DOUBLE: return new NBTTagDouble(Double.parseDouble(text.trim()));
            case TAG_BYTE_ARRAY:
            {
                if (text.trim().isEmpty()) return new NBTTagByteArray(new byte[0]);
                String[] parts = text.split(",");
                byte[] arr = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) arr[i] = (byte) Integer.parseInt(parts[i].trim());
                return new NBTTagByteArray(arr);
            }
            case TAG_INT_ARRAY:
            {
                if (text.trim().isEmpty()) return new NBTTagIntArray(new int[0]);
                String[] parts = text.split(",");
                int[] arr = new int[parts.length];
                for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
                return new NBTTagIntArray(arr);
            }
            case TAG_COMPOUND: return new NBTTagCompound();
            case TAG_LIST: return new NBTTagList();
            default: throw new NumberFormatException("unsupported type " + typeId);
        }
    }

    private static String formatNbtValue(NBTBase target)
    {
        switch (target.getId())
        {
            case TAG_STRING: return ((NBTTagString) target).getString();
            case TAG_BYTE: return Byte.toString(((NBTTagByte) target).getByte());
            case TAG_SHORT: return Short.toString(((NBTTagShort) target).getShort());
            case TAG_INT: return Integer.toString(((NBTTagInt) target).getInt());
            case TAG_LONG: return Long.toString(((NBTTagLong) target).getLong());
            case TAG_FLOAT: return Float.toString(((NBTTagFloat) target).getFloat());
            case TAG_DOUBLE: return Double.toString(((NBTTagDouble) target).getDouble());
            case TAG_BYTE_ARRAY:
            {
                byte[] arr = ((NBTTagByteArray) target).getByteArray();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length; i++)
                {
                    if (i > 0) sb.append(',');
                    sb.append(arr[i]);
                }
                return sb.toString();
            }
            case TAG_INT_ARRAY:
            {
                int[] arr = ((NBTTagIntArray) target).getIntArray();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length; i++)
                {
                    if (i > 0) sb.append(',');
                    sb.append(arr[i]);
                }
                return sb.toString();
            }
            default: return target.toString();
        }
    }

    public static String getNbtTypeLabel(int typeId)
    {
        return safeFormat("gui.oneblockultima.config.nbt.type." + nbtTypeKey(typeId));
    }

    public static String nbtTypeKey(int typeId)
    {
        switch (typeId)
        {
            case TAG_BYTE: return "byte";
            case TAG_SHORT: return "short";
            case TAG_INT: return "int";
            case TAG_LONG: return "long";
            case TAG_FLOAT: return "float";
            case TAG_DOUBLE: return "double";
            case TAG_BYTE_ARRAY: return "byte_array";
            case TAG_INT_ARRAY: return "int_array";
            case TAG_LIST: return "list";
            case TAG_COMPOUND: return "compound";
            case TAG_STRING: return "string";
            default: return "end";
        }
    }

    public static String nbtValuePreview(NBTBase value)
    {
        if (value == null) return "";
        if (value instanceof NBTTagCompound)
        {
            int n = ((NBTTagCompound) value).getKeySet().size();
            return "{" + n + "}";
        }
        if (value instanceof NBTTagList)
        {
            int n = ((NBTTagList) value).tagCount();
            return "[" + n + "]";
        }
        if (value instanceof NBTTagByteArray) return "byte[" + ((NBTTagByteArray) value).getByteArray().length + "]";
        if (value instanceof NBTTagIntArray) return "int[" + ((NBTTagIntArray) value).getIntArray().length + "]";
        String preview = formatNbtValue(value);
        if (preview.length() > 24) preview = preview.substring(0, 21) + "...";
        return preview;
    }

    private NBTTagCompound getEditingEntryNbtOrNull()
    {
        if (editingSet == null || editingCurrencyIndex < 0) return null;
        if (editingEntryType == EntryType.BLOCK && editingSet.blocks != null && editingCurrencyIndex < editingSet.blocks.size())
        {
            return editingSet.blocks.get(editingCurrencyIndex).nbtTags;
        }
        if (editingEntryType == EntryType.MOB && editingSet.mobs != null && editingCurrencyIndex < editingSet.mobs.size())
        {
            return editingSet.mobs.get(editingCurrencyIndex).nbtTags;
        }
        return null;
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
                    split.nbtTags = entry.nbtTags.copy();
                    split.dropItem = entry.dropItem;
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
        statusMessage = safeFormat("gui.oneblockultima.config.level_chance_updated");
        statusTimer = 60;
        return true;
        }
        catch (NumberFormatException e)
        {
            statusMessage = safeFormat("gui.oneblockultima.config.error.invalid_level_chance");
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
        availableSetsForConditions.clear();
        for (BlockSetConfig.BlockSetDefinition s : sets)
            if (!s.id.equals(editingSet.id)) availableSetsForConditions.add(s);
        List<String> availableTypes = getAvailableUnlockConditionTypes();
        if (!availableTypes.contains(newConditionTypeToAdd))
            newConditionTypeToAdd = availableTypes.isEmpty() ? "broken_blocks_total" : availableTypes.get(0);
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
        if ("broken_blocks_total".equals(type))
        {
            List<String> available = getAvailableUnlockConditionTypes();
            newConditionTypeToAdd = available.isEmpty() ? "broken_blocks_total" : available.get(0);
            if ("broken_blocks".equals(newConditionTypeToAdd) || "set_level".equals(newConditionTypeToAdd))
                advanceToNextUnusedSet(newConditionTypeToAdd);
        }
        else
        {
            advanceToNextUnusedSet(type);
        }
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
                //noinspection ResultOfMethodCallIgnored
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
            Set<String> existingBlocks = getExistingBlockKeys();
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
                    String name = fluid.getLocalizedName(new FluidStack(fluid, 1000));
                    if (!emptyQuery && !searchTerms.isEmpty() && mismatchesSearchTerms(name, searchTerms)) continue;
                    if (existingBlocks.contains(registry + "@0")) continue;
                    searchResults.add(new SearchResult(registry, name, modId, fluid));
                    continue;
                }

                Item item = Item.getItemFromBlock(block);
                if (item == Items.AIR) continue;
                NonNullList<ItemStack> subItems = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, subItems);
                if (subItems.isEmpty()) subItems.add(new ItemStack(item, 1, 0));
                for (ItemStack subStack : subItems)
                {
                    if (subStack.isEmpty() || subStack.getItem() != item) continue;
                    String name = "";
                    try { name = subStack.getDisplayName(); } catch (Exception ignored) {}
                    if (!emptyQuery && !searchTerms.isEmpty() && mismatchesSearchTerms(name, searchTerms)) continue;
                    if (existingBlocks.contains(registry + "@" + subStack.getMetadata())) continue;
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
                String name = "";
                try { name = new ItemStack(item, 1).getDisplayName(); } catch (Exception ignored) {}
                if (!emptyQuery && !searchTerms.isEmpty() && mismatchesSearchTerms(name, searchTerms)) continue;
                if (existingBlocks.contains(registry + "@0")) continue;
                searchResults.add(new SearchResult(registry, name, modId, new ItemStack(item, 1)));
            }
        }

        if (currentSearchType == SearchType.MOBS)
        {
            Set<String> existingMobs = getExistingMobRegistries();
            Set<ResourceLocation> entityNames = EntityList.getEntityNameList();
            for (ResourceLocation reg : entityNames) {
                String registry = reg.toString();
                if (existingMobs.contains(registry)) continue;
                String registryId = reg.getResourcePath();
                String modId = reg.getResourceDomain();
                if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
                if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;
                String name = registry;
                try {
                    String entityName = EntityList.getTranslationName(reg);
                    if (entityName != null && !entityName.isEmpty()) {
                        String translationKey = "entity." + entityName + ".name";
                        String localized = I18n.format(translationKey);
                        if (!localized.equals(translationKey)) name = localized;
                    }
                } catch (Exception ignored) {
                }
                if (!emptyQuery && !searchTerms.isEmpty() && mismatchesSearchTerms(name, searchTerms)) continue;
                Class<?> entityClass = EntityList.getClass(reg);
                if (entityClass != null && EntityLivingBase.class.isAssignableFrom(entityClass))
                    searchResults.add(new SearchResult(registry, name, modId, entityClass));
            }
        }

        searchResults.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    private boolean mismatchesSearchTerms(String name, List<String> searchTerms)
    {
        if (searchTerms == null || searchTerms.isEmpty()) return false;
        String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (String term : searchTerms)
        {
            if (term.isEmpty()) continue;
            if (!lowerName.contains(term)) return true;
        }
        return false;
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
                //noinspection deprecation
                net.minecraft.block.state.IBlockState state = block.getStateFromMeta(meta);
                //noinspection DataFlowIssue
                stack = block.getPickBlock(state, null, null, null, null);
                if (stack.isEmpty())
                {
                    Item item = Item.getItemFromBlock(block);
                    if (item != Items.AIR) stack = new ItemStack(item, 1, meta);
                }
            }
            if (stack.isEmpty())
            {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.registry));
                if (item != null && item != Items.AIR) stack = new ItemStack(item, 1, meta);
            }
        } catch (Exception ignored) {}
        if (!stack.isEmpty() && entry.nbtTags != null && !entry.nbtTags.hasNoTags())
        {
            stack = stack.copy();
            stack.setTagCompound(entry.nbtTags.copy());
        }
        return stack;
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
            return I18n.format("gui.oneblockultima.unlock_conditions") + ": " + I18n.format("gui.oneblockultima.config.none");
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
        result.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        return result;
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
    }

    public Set<String> getSelectedRequiredModsForRemoval() { return selectedRequiredModsForRemoval; }

    public void selectRequiredModToRemove(String modId)
    {
        if (selectedRequiredModsForRemoval.contains(modId))
            selectedRequiredModsForRemoval.remove(modId);
        else selectedRequiredModsForRemoval.add(modId);
    }

    public void deleteSelectedRequiredMods()
    {
        requiredModsEditorMods.removeAll(selectedRequiredModsForRemoval);
        selectedRequiredModsForRemoval.clear();
    }

    public void cycleUnlockConditionType()
    {
        List<String> types = getAvailableUnlockConditionTypes();
        if (types.isEmpty()) return;
        int idx = 0;
        boolean found = false;
        for (int i = 0; i < types.size(); i++)
        {
            if (types.get(i).equals(newConditionTypeToAdd)) { idx = i; found = true; break; }
        }
        if (found) idx = (idx + 1) % types.size();
        newConditionTypeToAdd = types.get(idx);
        if ("broken_blocks".equals(newConditionTypeToAdd) || "set_level".equals(newConditionTypeToAdd))
            advanceToNextUnusedSet(newConditionTypeToAdd);
    }

    public List<String> getAvailableUnlockConditionTypes()
    {
        List<String> types = new ArrayList<>(Arrays.asList("broken_blocks_total", "broken_blocks", "set_level"));
        for (BlockSetConfig.UnlockConditionDefinition cond : unlockConditionsEditorConditions)
        {
            if (cond != null && "broken_blocks_total".equalsIgnoreCase(cond.type))
            {
                types.remove("broken_blocks_total");
                break;
            }
        }
        return types;
    }

    public void cycleUnlockConditionSet()
    {
        if (availableSetsForConditions.isEmpty()) return;
        Set<String> used = getUsedSetIdsForType(newConditionTypeToAdd);
        List<BlockSetConfig.BlockSetDefinition> unused = new ArrayList<>();
        for (BlockSetConfig.BlockSetDefinition s : availableSetsForConditions)
            if (!used.contains(s.id)) unused.add(s);
        if (unused.isEmpty()) { newConditionSetId = ""; return; }
        int idx = 0;
        for (int i = 0; i < unused.size(); i++)
        {
            if (unused.get(i).id.equals(newConditionSetId)) { idx = (i + 1) % unused.size(); break; }
        }
        newConditionSetId = unused.get(idx).id;
    }

    public void saveLocalizedName(String setId, String name)
    {
        String langCode = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode().toLowerCase();
        if (langCode.isEmpty()) langCode = "en_us";
        staticSetLocalizedNames.computeIfAbsent(setId, k -> new HashMap<>()).put(langCode, name);
        saveCustomNames();
    }

    private void saveCustomNames()
    {
        try
        {
            File langDir = new File(Loader.instance().getConfigDir(), "oneblockultima/lang");
            if (!langDir.exists()) //noinspection ResultOfMethodCallIgnored
                langDir.mkdirs();
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
            for (File langFile : Objects.requireNonNull(langDir.listFiles()))
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

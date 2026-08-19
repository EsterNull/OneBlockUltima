package ru.defea.oneblockultima.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.util.NBTTagCompoundAdapter;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static ru.defea.oneblockultima.Constants.MINECRAFT_DOMAIN;

public final class BlockSetConfig
{
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(NBTTagCompound.class, new NBTTagCompoundAdapter())
            .registerTypeAdapter(SetRequiredModsDefinition.class, new SetRequiredModsDefinitionAdapter())
            .create();

    private static final Gson COMPACT_GSON = new GsonBuilder()
            .registerTypeAdapter(NBTTagCompound.class, new NBTTagCompoundAdapter())
            .registerTypeAdapter(SetRequiredModsDefinition.class, new SetRequiredModsDefinitionAdapter())
            .create();

    private static BlockSetConfig instance;
    static File configFile;

    public static void reset()
    {
        instance = null;
        configFile = null;
    }

    private List<BlockSetDefinition> sets = new ArrayList<>();
    private SettingsDefinition settings = new SettingsDefinition();

    private transient Map<String, BlockSetDefinition> setsById = new HashMap<>();
    private transient String cachedDefaultSetId;
    private transient boolean defaultSetIdComputed;

    public static BlockSetConfig get()
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
                if (instance == null)
                {
                    instance = new BlockSetConfig();
                }
                instance.buildIndex();
            }
        }
        return instance;
    }

    public static File getConfigFile()
    {
        return configFile;
    }

    public static void saveCurrentConfig()
    {
        if (instance == null || configFile == null)
        {
            return;
        }

        saveToFile(configFile, instance);
    }

    public static void invalidateComputedLevels()
    {
        if (instance == null)
        {
            return;
        }

        for (BlockSetDefinition set : instance.sets)
        {
            if (set != null)
            {
                set.computedLevels = null;
            }
        }
    }

    public static void applySets(List<BlockSetDefinition> newSets)
    {
        if (instance == null)
        {
            instance = new BlockSetConfig();
        }

        instance.sets = newSets != null ? new ArrayList<>(newSets) : new ArrayList<>();
        for (BlockSetDefinition set : instance.sets)
        {
            if (set != null)
            {
                set.computedLevels = null;
            }
        }
        instance.buildIndex();
    }

    public static void reload()
    {
        if (configFile == null)
        {
            return;
        }

        BlockSetConfig loaded = loadFromFile(configFile);
        if (loaded == null || loaded.getSets().isEmpty())
        {
            // If the file does not exist or contains no sets, copy the default config
            copyDefaultToConfigFile(configFile);
            loaded = loadFromFile(configFile);
        }

        if (loaded == null || loaded.getSets().isEmpty())
        {
            loaded = loadDefaultFromResources();
        }

        if (loaded == null || loaded.getSets().isEmpty())
        {
            loaded = new BlockSetConfig();
        }

        if (loaded.getSets().isEmpty())
        {
            loaded = new BlockSetConfig();
        }

        if (loaded.getSets().isEmpty())
        {
            saveToFile(configFile, loaded);
        }

        instance = loaded;
        instance.buildIndex();
    }

    public static void load(File configDir)
    {
        if (configDir == null)
        {
            OneBlockUltima.getLogger().error("Config directory is null, cannot load configuration");
            instance = new BlockSetConfig();
            instance.buildIndex();
            return;
        }

        configFile = new File(configDir, "oneblockultima/blocksets.json");
        reload();
    }

    private static BlockSetConfig loadFromFile(File file)
    {
        if (file == null || !file.exists())
        {
            return null;
        }

        try (FileReader reader = new FileReader(file))
        {
            return GSON.fromJson(reader, BlockSetConfig.class);
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load blocksets.json from config directory", e);
            return null;
        }
    }

    private static void saveToFile(File file, BlockSetConfig config)
    {
        if (file == null || config == null)
        {
            return;
        }

        try (OutputStream outputStream = Files.newOutputStream(file.toPath());
             java.io.Writer writer = new java.io.OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        {
            writer.write(COMPACT_GSON.toJson(config));
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to save blocksets.json to config directory", e);
        }
    }

    private static void copyDefaultToConfigFile(File file)
    {
        if (file == null)
        {
            return;
        }

        try (InputStream input = BlockSetConfig.class.getResourceAsStream("/assets/oneblockultima/blocksets.json"))
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
            OneBlockUltima.getLogger().error("Failed to copy default blocksets.json to config directory", e);
        }
    }

    private static BlockSetConfig loadDefaultFromResources()
    {
        try (InputStream input = BlockSetConfig.class.getResourceAsStream("/assets/oneblockultima/blocksets.json"))
        {
            if (input == null)
            {
                return new BlockSetConfig();
            }

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
            {
                return GSON.fromJson(reader, BlockSetConfig.class);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load default blocksets.json from resources", e);
            return new BlockSetConfig();
        }
    }

    public static class BlockElementDefinition
    {
        public String registry;
        public int meta;
        public List<Integer> metas = new ArrayList<>();
        public int baseLevel = 1;
        public int baseChance = 0;
        public String dropItem = null;
        public NBTTagCompound nbtTags = new NBTTagCompound();

        public List<Integer> getMetaValues()
        {
            if (metas != null && !metas.isEmpty())
            {
                return metas;
            }

            return Collections.singletonList(meta);
        }
    }

    public static class MobElementDefinition
    {
        public String registry;
        public int baseLevel = 1;
        public int baseChance = 0;
        public int count = 1;
        public NBTTagCompound nbtTags = new NBTTagCompound();
    }

    // internal runtime element used for unified computations of percentages
    private static class InternalElement
    {
        String registry;
        int meta;
        int baseLevel;
        int baseChance;
        String dropItem = null;
        int count;
        boolean isMob;
        NBTTagCompound nbtTags = new NBTTagCompound();
    }

    private void buildIndex()
    {
        setsById = new HashMap<>();
        defaultSetIdComputed = false;
        if (sets == null)
        {
            sets = new ArrayList<>();
        }

        for (BlockSetDefinition set : sets)
        {
            if (set != null && set.id != null)
            {
                mergeBlockMetas(set);
                setsById.put(set.id, set);
            }
        }
    }

    private static void mergeBlockMetas(BlockSetDefinition set)
    {
        if (set.blocks == null || set.blocks.size() <= 1) return;

        Map<String, BlockElementDefinition> merged = new LinkedHashMap<>();
        List<BlockElementDefinition> result = new ArrayList<>();

        for (BlockElementDefinition block : set.blocks)
        {
            if (block == null) continue;

            String key = block.registry + "@" + block.baseLevel + "@" + block.baseChance + "@"
                    + (block.nbtTags == null ? "" : block.nbtTags.toString());
            BlockElementDefinition existing = merged.get(key);

            if (existing == null)
            {
                merged.put(key, block);
                result.add(block);
            }
            else
            {
                List<Integer> existingMetas = existing.getMetaValues();
                List<Integer> newMetas = block.getMetaValues();
                Set<Integer> combined = new LinkedHashSet<>(existingMetas);
                combined.addAll(newMetas);
                List<Integer> sorted = new ArrayList<>(combined);
                sorted.sort(Integer::compareTo);
                existing.metas = sorted;
                existing.meta = sorted.get(0);
            }
        }

        set.blocks = result;
    }

    public List<BlockSetDefinition> getSets()
    {
        return sets == null ? Collections.emptyList() : sets;
    }

    public String toJson()
    {
        return COMPACT_GSON.toJson(this);
    }

    public static void loadFromServerJson(String json)
    {
        try
        {
            BlockSetConfig loaded = GSON.fromJson(json, BlockSetConfig.class);
            if (loaded != null)
            {
                loaded.buildIndex();
                instance = loaded;
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load server blockset config", e);
        }
    }

    public BlockSetDefinition getSet(String id)
    {
        return setsById.get(id);
    }

    @Nonnull
    public String getDefaultSetId()
    {
        if (defaultSetIdComputed)
        {
            return cachedDefaultSetId;
        }

        BlockSetConfig current = get();
        if (current != null && current.sets != null && !current.sets.isEmpty())
        {
            for (BlockSetDefinition set : current.sets)
            {
                if (set != null && set.isAvailable() && set.id != null && !set.id.isEmpty())
                {
                    cachedDefaultSetId = set.id;
                    defaultSetIdComputed = true;
                    return cachedDefaultSetId;
                }
            }

            BlockSetDefinition first = current.sets.get(0);
            if (first != null && first.id != null && !first.id.isEmpty())
            {
                cachedDefaultSetId = first.id;
                defaultSetIdComputed = true;
                return cachedDefaultSetId;
            }
        }

        BlockSetConfig fallback = loadDefaultFromResources();
        if (fallback != null && fallback.getSets() != null && !fallback.getSets().isEmpty())
        {
            cachedDefaultSetId = fallback.getDefaultSetId();
            defaultSetIdComputed = true;
            return cachedDefaultSetId;
        }

        cachedDefaultSetId = "";
        defaultSetIdComputed = true;
        return cachedDefaultSetId;
    }

    public static boolean isRegistryModUnloaded(String registry)
    {
        if (registry == null || registry.isEmpty())
        {
            return true;
        }

        try
        {
            ResourceLocation loc = new ResourceLocation(registry);
            String domain = loc.getResourceDomain();
            if (MINECRAFT_DOMAIN.equals(domain))
            {
                return false;
            }
            if (Loader.instance() == null)
            {
                return true;
            }
            return !Loader.isModLoaded(domain);
        }
        catch (Exception e)
        {
            return true;
        }
    }

    public static boolean isBlockAvailable(String registry)
    {
        if (isRegistryModUnloaded(registry))
        {
            return false;
        }

        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registry)) != null;
    }

    public static boolean isMobAvailable(String registry)
    {
        if (isRegistryModUnloaded(registry))
        {
            return false;
        }

        return EntityList.getClass(new ResourceLocation(registry)) != null;
    }

    public SettingsDefinition getSettings()
    {
        if (settings == null)
        {
            settings = new SettingsDefinition();
        }
        return settings;
    }

    public static class SettingsDefinition
    {
        public boolean disableFluidGeneration = false;
        public boolean disableMobGeneration = false;
        public boolean disableChestGeneration = false;
        public boolean disableSaplingGeneration = false;

        public String setCostIncreaseMode = "fixed";
        public double setCostIncreaseValue = 50.0;
    }

    public static class UnlockConditionGroup
    {
        public String mode = "any";
        public List<UnlockConditionDefinition> conditions = new ArrayList<>();
    }

    public static class UnlockConditionDefinition
    {
        public String type = "set_level";
        public String setId;
        public int level = 0;
        public int count = 0;

        public boolean isSatisfied(IOneBlockPlayerData data)
        {
            return isSatisfied(data, null);
        }

        public boolean isSatisfied(IOneBlockPlayerData data, TileEntityOneBlockGenerator generator)
        {
            if (data == null)
            {
                return false;
            }

            switch (type == null ? "" : type.toLowerCase(Locale.ROOT))
            {
                case "broken_blocks_total":
                    return data.getBrokenBlocksCount() >= count;
                case "broken_blocks":
                    return data.getBrokenBlocksCount(setId) >= count;
                case "set_level":
                    int generatorLevel = generator == null ? data.getSetLevel(setId) : generator.getSetLevel(setId);
                    return generatorLevel >= level;
                default:
                    return false;
            }
        }
    }

    public static class SetRequiredModsDefinition
    {
        public enum TYPE
        {
            ANY,
            ALL
        }

        private TYPE type = TYPE.ALL;
        private List<String> mods = new ArrayList<>();

        public SetRequiredModsDefinition() {}

        public SetRequiredModsDefinition(TYPE type, List<String> mods)
        {
            this.type = type != null ? type : TYPE.ALL;
            this.mods = mods != null ? new ArrayList<>(mods) : new ArrayList<>();
        }

        public boolean isAvailable()
        {
            List<String> activeMods = getMods();
            if (activeMods == null || activeMods.isEmpty())
            {
                return true;
            }

            if (getType() == TYPE.ALL)
            {
                for (String modId : activeMods)
                {
                    if (modId == null || modId.isEmpty())
                    {
                        continue;
                    }
                    if (!isModLoadedSafe(modId))
                    {
                        return false;
                    }
                }
                return true;
            }

            for (String modId : activeMods)
            {
                if (modId == null || modId.isEmpty())
                {
                    continue;
                }
                if (isModLoadedSafe(modId))
                {
                    return true;
                }
            }
            return false;
        }

        private static boolean isModLoadedSafe(String modId)
        {
            try
            {
                return Loader.isModLoaded(modId);
            }
            catch (Exception e)
            {
                return false;
            }
        }

        public List<String> getMods()
        {
            if (mods == null)
            {
                mods = new ArrayList<>();
            }
            return mods;
        }

        public TYPE getType()
        {
            return type != null ? type : TYPE.ALL;
        }

        public void setType(TYPE newType)
        {
            this.type = newType != null ? newType : TYPE.ALL;
        }

        public void clear()
        {
            getMods().clear();
        }

        public void addMod(String mod)
        {
            if (mod != null && !mod.isEmpty())
            {
                getMods().add(mod);
            }
        }
    }

    private static final class SetRequiredModsDefinitionAdapter extends TypeAdapter<SetRequiredModsDefinition>
    {
        @Override
        public void write(JsonWriter out, SetRequiredModsDefinition value) throws IOException
        {
            if (value == null)
            {
                out.nullValue();
                return;
            }

            out.beginObject();
            out.name("type").value(value.getType().name());
            out.name("mods");
            out.beginArray();
            for (String mod : value.getMods())
            {
                out.value(mod);
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public SetRequiredModsDefinition read(JsonReader in) throws IOException
        {
            SetRequiredModsDefinition definition = new SetRequiredModsDefinition();
            JsonToken token = in.peek();

            if (token == JsonToken.NULL)
            {
                in.nextNull();
                return definition;
            }

            if (token == JsonToken.BEGIN_ARRAY)
            {
                definition.setType(SetRequiredModsDefinition.TYPE.ALL);
                in.beginArray();
                while (in.hasNext())
                {
                    definition.addMod(in.nextString());
                }
                in.endArray();
                return definition;
            }

            if (token == JsonToken.BEGIN_OBJECT)
            {
                in.beginObject();
                while (in.hasNext())
                {
                    String name = in.nextName();
                    if ("type".equals(name))
                    {
                        try
                        {
                            definition.setType(SetRequiredModsDefinition.TYPE.valueOf(in.nextString().toUpperCase(Locale.ROOT)));
                        }
                        catch (Exception ignored)
                        {
                            definition.setType(SetRequiredModsDefinition.TYPE.ALL);
                            in.skipValue();
                        }
                    }
                    else if ("mods".equals(name))
                    {
                        in.beginArray();
                        while (in.hasNext())
                        {
                            definition.addMod(in.nextString());
                        }
                        in.endArray();
                    }
                    else
                    {
                        in.skipValue();
                    }
                }
                in.endObject();
            }
            else
            {
                in.skipValue();
            }

            return definition;
        }
    }

    public static class BlockSetDefinition
    {
        public String id;
        public SetRequiredModsDefinition requiredMods = new SetRequiredModsDefinition();
        public int unlockCost = 0;
        public UnlockConditionGroup unlockConditions;

        // New format: separate lists for block-elements and mob-elements
        public List<BlockElementDefinition> blocks = new ArrayList<>();
        public List<MobElementDefinition> mobs = new ArrayList<>();

        // transient cache for computed levels
        public transient java.util.Map<Integer, SetLevelDefinition> computedLevels = null;

        public boolean isAvailable()
        {
            return requiredMods.isAvailable();
        }

        public boolean hasUnlockRequirementsMet(IOneBlockPlayerData data)
        {
            return hasUnlockRequirementsMet(data, null);
        }

        public boolean hasUnlockRequirementsMet(IOneBlockPlayerData data, TileEntityOneBlockGenerator generator)
        {
            if (unlockConditions == null || unlockConditions.conditions == null || unlockConditions.conditions.isEmpty())
            {
                return true;
            }

            boolean requireAll = "all".equalsIgnoreCase(unlockConditions.mode);
            if (requireAll)
            {
                for (UnlockConditionDefinition condition : unlockConditions.conditions)
                {
                    if (condition == null || !condition.isSatisfied(data, generator))
                    {
                        return false;
                    }
                }
                return true;
            }

            for (UnlockConditionDefinition condition : unlockConditions.conditions)
            {
                if (condition != null && condition.isSatisfied(data, generator))
                {
                    return true;
                }
            }
            return false;
        }

        public SetLevelDefinition getLevel(int level)
        {
            // Ensure computedLevels built
            ensureComputedLevels();
            return computedLevels.get(level);
        }

        public int getMaxLevel()
        {
            ensureComputedLevels();
            int max = 0;
            for (Integer lvl : computedLevels.keySet()) if (lvl > max) max = lvl;
            return max;
        }

        /**
         * Returns the level definition for generation/rendering, clamping levels that exceed the
         * current maximum (e.g. a set was upgraded higher, then mod settings reduced the computed levels)
         * to the highest available definition so nothing breaks.
         */
        public SetLevelDefinition getLevelClamped(int level)
        {
            ensureComputedLevels();
            if (computedLevels == null || computedLevels.isEmpty())
            {
                return null;
            }
            int max = getMaxLevel();
            if (level > max)
            {
                return computedLevels.get(max);
            }
            return computedLevels.get(level);
        }

        public void ensureComputedLevels()
        {
            if (computedLevels != null) return;

            OneBlockUltima.getLogger().info("[Config] ensureComputedLevels called for set: {}", id);
            OneBlockUltima.getLogger().info("[Config] blocks size: {}", blocks.size());
            OneBlockUltima.getLogger().info("[Config] mobs size: {}", mobs.size());

            computedLevels = new java.util.HashMap<>();

            // If still no elements, nothing to compute
            if (blocks.isEmpty() && mobs.isEmpty()) return;

            // Build levels iteratively until stabilization
            java.util.List<InternalElement> elems = new ArrayList<>();
            for (BlockElementDefinition be : blocks) {
                if (be == null || !isBlockAvailable(be.registry)) continue;
                List<Integer> metaValues = be.getMetaValues();
                if (metaValues == null || metaValues.isEmpty())
                {
                    metaValues = Collections.singletonList(be.meta);
                }

                for (Integer metaValue : metaValues)
                {
                    InternalElement ie = new InternalElement();
                    ie.registry = be.registry;
                    ie.meta = metaValue == null ? 0 : metaValue;
                    ie.baseLevel = be.baseLevel;
                    ie.baseChance = be.baseChance;
                    ie.dropItem = be.dropItem;
                    ie.count = 1;
                    ie.isMob = false;
                    Set<String> keys = be.nbtTags.getKeySet();
                    for (String key : keys) {
                        NBTBase tag = be.nbtTags.getTag(key);
                        ie.nbtTags.setTag(key, tag.copy());
                    }
                    elems.add(ie);
                }
            }
            for (MobElementDefinition me : mobs) {
                if (me == null || !isMobAvailable(me.registry)) continue;
                InternalElement ie = new InternalElement();
                ie.registry = me.registry;
                ie.meta = 0;
                ie.baseLevel = me.baseLevel;
                ie.baseChance = me.baseChance;
                ie.dropItem = null;
                ie.count = me.count;
                ie.isMob = true;
                Set<String> keys = me.nbtTags.getKeySet();
                for (String key : keys) {
                    NBTBase tag = me.nbtTags.getTag(key);
                    ie.nbtTags.setTag(key, tag.copy());
                }
                elems.add(ie);
            }
            // determine minimal baseLevel and iterate
            int minLevel = Integer.MAX_VALUE;
            for (InternalElement e : elems) if (e.baseLevel < minLevel) minLevel = e.baseLevel;
            if (minLevel == Integer.MAX_VALUE) minLevel = 1;

            java.util.List<InternalElement> blockElems = new ArrayList<>();
            java.util.List<InternalElement> mobElems = new ArrayList<>();
            for (InternalElement e : elems)
            {
                if (e.isMob)
                {
                    mobElems.add(e);
                }
                else
                {
                    blockElems.add(e);
                }
            }

            java.util.Map<String, Double> prevPercBlocks = new java.util.HashMap<>();
            java.util.Map<String, Double> prevPercMobs = new java.util.HashMap<>();

            int maxBaseLevel = 0;
            for (InternalElement e : elems) if (e.baseLevel > maxBaseLevel) maxBaseLevel = e.baseLevel;

            int hardCap = Math.max(200, maxBaseLevel + 200);
            SetLevelDefinition previous = null;
            int level = minLevel;
            while (level <= hardCap)
            {
                java.util.List<InternalElement> availBlocks = new ArrayList<>();
                java.util.List<InternalElement> availMobs = new ArrayList<>();
                for (InternalElement e : blockElems) if (e.baseLevel <= level) availBlocks.add(e);
                for (InternalElement e : mobElems) if (e.baseLevel <= level) availMobs.add(e);

                SetLevelDefinition lvlDef = new SetLevelDefinition();
                lvlDef.level = level;
                int baseOpenLevel = minLevel <= 0 ? 1 : minLevel;
                lvlDef.upgradeCost = computeUpgradeCost(unlockCost, level, baseOpenLevel);

                boolean changed;
                if (availBlocks.isEmpty() && availMobs.isEmpty())
                {
                    // No new elements at this level: carry over the previous level's content so the level stays usable
                    if (previous != null)
                    {
                        lvlDef.blocks = new java.util.ArrayList<>(previous.blocks);
                        lvlDef.mobs = new java.util.ArrayList<>(previous.mobs);
                    }
                    changed = false;
                }
                else
                {
                    java.util.Map<String, Integer> proposedBlocks = computeLevelPercentages(availBlocks, prevPercBlocks, 100);
                    java.util.Map<String, Integer> proposedMobs = computeLevelPercentages(availMobs, prevPercMobs, Math.max(0, ModSettings.get().getMaxMobSpawnPercent()));

                    lvlDef.blocks = new java.util.ArrayList<>();
                    lvlDef.mobs = new java.util.ArrayList<>();

                    for (InternalElement e : availBlocks)
                    {
                        String key = elementKey(e);
                        int percent = proposedBlocks.getOrDefault(key, 0);
                        BlockEntryDefinition b = new BlockEntryDefinition();
                        b.registry = e.registry;
                        b.meta = e.meta;
                        b.chance = percent;
                        b.dropItem = e.dropItem;
                        Set<String> keys = e.nbtTags.getKeySet();
                        for (String nbtKey : keys) {
                            NBTBase tag = e.nbtTags.getTag(nbtKey);
                            b.nbtTags.setTag(nbtKey, tag.copy());
                        }
                        lvlDef.blocks.add(b);
                    }

                    for (InternalElement e : availMobs)
                    {
                        String key = elementKey(e);
                        int percent = proposedMobs.getOrDefault(key, 0);
                        MobEntryDefinition m = new MobEntryDefinition();
                        m.registry = e.registry;
                        m.chance = percent;
                        m.count = e.count;
                        Set<String> keys = e.nbtTags.getKeySet();
                        for (String nbtKey : keys) {
                            NBTBase tag = e.nbtTags.getTag(nbtKey);
                            m.nbtTags.setTag(nbtKey, tag.copy());
                        }
                        lvlDef.mobs.add(m);
                    }

                    changed = hasSignificantChange(availBlocks, prevPercBlocks, proposedBlocks)
                            || hasSignificantChange(availMobs, prevPercMobs, proposedMobs);

                    for (String k : proposedBlocks.keySet()) prevPercBlocks.put(k, proposedBlocks.get(k).doubleValue());
                    for (String k : proposedMobs.keySet()) prevPercMobs.put(k, proposedMobs.get(k).doubleValue());

                    previous = lvlDef;
                }

                computedLevels.put(level, lvlDef);

                level++;
                // Stop only once every element is included (level past the highest baseLevel) and percentages stabilized
                if (!changed && level > maxBaseLevel)
                {
                    break;
                }
            }
        }
    }

    private static int computeUpgradeCost(int unlockCost, int level, int baseOpenLevel)
    {
        SettingsDefinition settings = get().getSettings();
        if (settings == null)
        {
            return Math.max(0, unlockCost + 50 * (level - baseOpenLevel));
        }

        int steps = Math.max(0, level - baseOpenLevel);
        String mode = settings.setCostIncreaseMode == null ? "fixed" : settings.setCostIncreaseMode;
        double value = settings.setCostIncreaseValue;

        if ("multiplier".equalsIgnoreCase(mode))
        {
            if (value <= 0)
            {
                return Math.max(0, unlockCost);
            }
            double cost = unlockCost * Math.pow(value, steps);
            return Math.max(0, (int) Math.round(cost));
        }

        if (value <= 0)
        {
            return Math.max(0, unlockCost);
        }
        return Math.max(0, unlockCost + (int) Math.round(value * steps));
    }

    private static java.util.Map<String, Integer> computeLevelPercentages(java.util.List<InternalElement> avail, java.util.Map<String, Double> prevPerc, int maximumTotal)
    {
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        if (avail == null || avail.isEmpty())
        {
            return result;
        }

        java.util.Map<String, Double> prevForThis = new java.util.HashMap<>();
        for (InternalElement e : avail)
        {
            String key = elementKey(e);
            prevForThis.put(key, prevPerc.getOrDefault(key, (double)Math.max(1, e.baseChance)));
        }

        double mean = 0.0;
        for (Double v : prevForThis.values()) mean += v;
        mean = mean / prevForThis.size();

        java.util.Map<String, Double> proposed = new java.util.HashMap<>();
        double totalChange = 0.0;
        for (InternalElement e : avail)
        {
            String key = elementKey(e);
            double prev = prevForThis.get(key);
            double alpha = 0.24 / (1.0 + e.baseLevel * 0.15) / (1.0 + (e.baseChance / 100.0));
            double next = prev + (mean - prev) * alpha;
            proposed.put(key, next);
            totalChange += Math.abs(next - prev);
        }

        boolean newAdded = false;
        for (InternalElement e : avail)
        {
            String key = elementKey(e);
            if (!prevPerc.containsKey(key)) { newAdded = true; break; }
        }

        if (!newAdded)
        {
            if (totalChange < 1.0)
            {
                double factor = totalChange <= 0.0 ? 0.0 : (1.0 / totalChange);
                for (String k : proposed.keySet())
                {
                    double adj = prevForThis.get(k) + (proposed.get(k) - prevForThis.get(k)) * factor;
                    proposed.put(k, adj);
                }
            }
            else if (totalChange > 3.0)
            {
                double factor = 3.0 / totalChange;
                for (String k : proposed.keySet())
                {
                    double adj = prevForThis.get(k) + (proposed.get(k) - prevForThis.get(k)) * factor;
                    proposed.put(k, adj);
                }
            }
        }

        double sum = 0.0;
        for (Double v : proposed.values()) sum += v;
        if (sum <= 0.0) sum = proposed.size();
        double scale = (double) maximumTotal / sum;
        proposed.replaceAll((k, v) -> v * scale);

        java.util.Map<String, Integer> rounded = new java.util.HashMap<>();
        java.util.List<java.util.Map.Entry<String, Double>> remainderList = new ArrayList<>();
        int totalRounded = 0;
        for (String k : proposed.keySet())
        {
            double value = proposed.get(k);
            int floorValue = (int) Math.floor(value);
            rounded.put(k, floorValue);
            totalRounded += floorValue;
            remainderList.add(new java.util.AbstractMap.SimpleEntry<>(k, value - floorValue));
        }

        int diff = maximumTotal - totalRounded;
        if (diff > 0)
        {
            remainderList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < remainderList.size() && diff > 0; i++, diff--)
            {
                String key = remainderList.get(i).getKey();
                rounded.put(key, rounded.get(key) + 1);
            }
        }
        else if (diff < 0)
        {
            remainderList.sort(Comparator.comparingDouble(Map.Entry::getValue));
            for (int i = 0; i < remainderList.size() && diff < 0; i++, diff++)
            {
                String key = remainderList.get(i).getKey();
                rounded.compute(key, (k, current) -> Math.max(0, current == null ? 0 : current - 1));
            }
        }

        return rounded;
    }

    private static boolean hasSignificantChange(java.util.List<InternalElement> avail, java.util.Map<String, Double> prevPerc, java.util.Map<String, ? extends Number> proposed)
    {
        if (avail == null || avail.isEmpty())
        {
            return false;
        }
        for (InternalElement e : avail)
        {
            String key = elementKey(e);
            double prev = prevPerc.getOrDefault(key, 0.0);
            Number nextValue = proposed.get(key);
            double next = nextValue == null ? 0.0 : nextValue.doubleValue();
            if (Math.abs(next - prev) >= 0.5)
            {
                return true;
            }
        }
        return false;
    }

    private static String elementKey(InternalElement e)
    {
        return e.registry + "@" + e.meta + "@" + e.baseLevel + (e.isMob ? "#m" : "");
    }

    public static class SetLevelDefinition
    {
        public int level;
        public int upgradeCost;
        public List<BlockEntryDefinition> blocks = new ArrayList<>();
        public List<MobEntryDefinition> mobs = new ArrayList<>();

        private transient MobEntryDefinition[] weightedMobs;
        private transient int totalMobChance = -1;

        public MobEntryDefinition pickMob(Random random)
        {
            if (mobs == null || mobs.isEmpty())
            {
                return null;
            }

            if (weightedMobs == null)
            {
                List<MobEntryDefinition> filtered = new ArrayList<>();
                int total = 0;
                for (MobEntryDefinition entry : mobs)
                {
                    if (entry != null && entry.getChance() > 0 && isMobAvailable(entry.registry))
                    {
                        filtered.add(entry);
                        total += entry.getChance();
                    }
                }
                weightedMobs = filtered.toArray(new MobEntryDefinition[0]);
                totalMobChance = total;
            }

            if (weightedMobs.length == 0 || totalMobChance <= 0)
            {
                return null;
            }

            int roll = random.nextInt(Math.max(100, totalMobChance));
            int current = 0;
            for (MobEntryDefinition entry : weightedMobs)
            {
                current += entry.getChance();
                if (roll < current)
                {
                    return entry;
                }
            }

            return null;
        }
    }

    public static class BlockEntryDefinition
    {
        public int id;
        public String registry;
        public int meta;
        public int chance;
        public String dropItem = null;
        public NBTTagCompound nbtTags = new NBTTagCompound();

        private transient Block resolvedBlockCache;
        private transient boolean resolvedBlockCacheSet;
        private transient ItemStack cachedPickStack;
        private transient boolean pickStackCached;
        private transient int classification = -1;

        private static final int CLASS_FLUID = 1;
        private static final int CLASS_CHEST = 2;
        private static final int CLASS_SAPLING = 4;

        public int getChance()
        {
            return chance > 0 ? chance : 1;
        }

        public Block resolveBlock()
        {
            if (!resolvedBlockCacheSet)
            {
                resolvedBlockCacheSet = true;
                if (registry == null || registry.isEmpty())
                {
                    resolvedBlockCache = null;
                }
                else
                {
                    resolvedBlockCache = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registry));
                }
            }
            return resolvedBlockCache;
        }

        private void ensureClassification()
        {
            if (classification != -1)
            {
                return;
            }
            classification = 0;
            if (registry != null && !registry.isEmpty())
            {
                String lowered = registry.toLowerCase(Locale.ROOT);
                if (lowered.contains("chest") || lowered.contains("barrel"))
                {
                    classification |= CLASS_CHEST;
                }
                if (lowered.contains("sapling"))
                {
                    classification |= CLASS_SAPLING;
                }
            }
            Block block = resolveBlock();
            if (block instanceof IFluidBlock || (block != null && FluidRegistry.lookupFluidForBlock(block) != null))
            {
                classification |= CLASS_FLUID;
            }
        }

        public boolean isFluid() {
            ensureClassification();
            return (classification & CLASS_FLUID) != 0;
        }

        public boolean isChestEntry() {
            ensureClassification();
            return (classification & CLASS_CHEST) != 0;
        }

        public boolean isSaplingEntry() {
            ensureClassification();
            return (classification & CLASS_SAPLING) != 0;
        }

        public net.minecraft.item.ItemStack getPickBlock()
        {
            if (!pickStackCached)
            {
                pickStackCached = true;
                cachedPickStack = computePickBlock();
            }
            return cachedPickStack;
        }

        private net.minecraft.item.ItemStack computePickBlock()
        {
            // If dropItem is specified, use it
            if (dropItem != null && !dropItem.isEmpty())
            {
                try
                {
                    net.minecraft.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(dropItem));
                    if (item != null && item != net.minecraft.init.Items.AIR)
                    {
                        return new net.minecraft.item.ItemStack(item, 1, 0);
                    }
                }
                catch (Exception ignored) {}
            }

            Block block = resolveBlock();
            if (block != null)
            {
                try
                {
                    //noinspection deprecation
                    IBlockState state = block.getStateFromMeta(meta);
                    //noinspection DataFlowIssue
                    net.minecraft.item.ItemStack pickStack = block.getPickBlock(state, null, null, null, null);
                    if (!pickStack.isEmpty())
                    {
                        return applyNbtToStack(pickStack);
                    }
                }
                catch (Exception ignored) {}

                net.minecraft.item.Item blockItem = net.minecraft.item.Item.getItemFromBlock(block);
                if (blockItem != net.minecraft.init.Items.AIR)
                {
                    try
                    {
                        return applyNbtToStack(new net.minecraft.item.ItemStack(blockItem, 1, meta));
                    }
                    catch (Exception ignored) {}
                }
            }

            // GUI fallback: try the registry as a placeable item (wheat, carrots, reeds, etc.)
            if (registry != null && !registry.isEmpty())
            {
                try
                {
                    net.minecraft.item.Item registryItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(registry));
                    if (registryItem != null && registryItem != net.minecraft.init.Items.AIR)
                    {
                        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(registryItem, 1, meta);
                        if (nbtTags != null && !nbtTags.hasNoTags())
                        {
                            stack.setTagCompound(nbtTags.copy());
                        }
                        return stack;
                    }
                }
                catch (Exception ignored) {}
            }

            return net.minecraft.item.ItemStack.EMPTY;
        }

        private net.minecraft.item.ItemStack applyNbtToStack(net.minecraft.item.ItemStack stack)
        {
            if (stack != null && !stack.isEmpty() && nbtTags != null && !nbtTags.hasNoTags())
            {
                net.minecraft.item.ItemStack copy = stack.copy();
                copy.setTagCompound(nbtTags.copy());
                return copy;
            }
            return stack;
        }
    }

    public static class MobEntryDefinition
    {
        public String registry;
        public int chance;
        public int count = 1;
        public NBTTagCompound nbtTags = new NBTTagCompound();

        public int getChance()
        {
            return chance;
        }
    }

    public static BlockSetDefinition copyBlockSetDefinition(BlockSetDefinition source)
    {
        if (source == null)
        {
            return null;
        }

        BlockSetDefinition copy = new BlockSetDefinition();
        copy.id = source.id;
        copy.unlockCost = source.unlockCost;
        copy.requiredMods = copyRequiredModsDefinition(source.requiredMods);
        copy.unlockConditions = copyUnlockConditionGroup(source.unlockConditions);
        copy.blocks = copyBlockElements(source.blocks);
        copy.mobs = copyMobElements(source.mobs);
        copy.computedLevels = null;
        return copy;
    }

    private static SetRequiredModsDefinition copyRequiredModsDefinition(SetRequiredModsDefinition source)
    {
        if (source == null)
        {
            return new SetRequiredModsDefinition();
        }

        return new SetRequiredModsDefinition(source.getType(), new ArrayList<>(source.getMods()));
    }

    private static UnlockConditionGroup copyUnlockConditionGroup(UnlockConditionGroup source)
    {
        if (source == null)
        {
            return null;
        }

        UnlockConditionGroup copy = new UnlockConditionGroup();
        copy.mode = source.mode;
        copy.conditions = new ArrayList<>();
        if (source.conditions != null)
        {
            for (UnlockConditionDefinition condition : source.conditions)
            {
                if (condition == null)
                {
                    continue;
                }
                UnlockConditionDefinition conditionCopy = new UnlockConditionDefinition();
                conditionCopy.type = condition.type;
                conditionCopy.setId = condition.setId;
                conditionCopy.level = condition.level;
                conditionCopy.count = condition.count;
                copy.conditions.add(conditionCopy);
            }
        }
        return copy;
    }

    private static List<BlockElementDefinition> copyBlockElements(List<BlockElementDefinition> source)
    {
        List<BlockElementDefinition> copy = new ArrayList<>();
        if (source == null)
        {
            return copy;
        }

        for (BlockElementDefinition element : source)
        {
            if (element == null)
            {
                continue;
            }
            BlockElementDefinition elementCopy = new BlockElementDefinition();
            elementCopy.registry = element.registry;
            elementCopy.meta = element.meta;
            elementCopy.metas = element.metas != null ? new ArrayList<>(element.metas) : new ArrayList<>();
            elementCopy.baseLevel = element.baseLevel;
            elementCopy.baseChance = element.baseChance;
            elementCopy.dropItem = element.dropItem;
            elementCopy.nbtTags = copyNbtCompound(element.nbtTags);
            copy.add(elementCopy);
        }
        return copy;
    }

    private static List<MobElementDefinition> copyMobElements(List<MobElementDefinition> source)
    {
        List<MobElementDefinition> copy = new ArrayList<>();
        if (source == null)
        {
            return copy;
        }

        for (MobElementDefinition element : source)
        {
            if (element == null)
            {
                continue;
            }
            MobElementDefinition elementCopy = new MobElementDefinition();
            elementCopy.registry = element.registry;
            elementCopy.baseLevel = element.baseLevel;
            elementCopy.baseChance = element.baseChance;
            elementCopy.count = element.count;
            elementCopy.nbtTags = copyNbtCompound(element.nbtTags);
            copy.add(elementCopy);
        }
        return copy;
    }

    private static NBTTagCompound copyNbtCompound(NBTTagCompound source)
    {
        if (source == null || source.hasNoTags())
        {
            return new NBTTagCompound();
        }
        return source.copy();
    }
}
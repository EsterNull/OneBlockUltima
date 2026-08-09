package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.BlockSetConfig.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class BlockSetConfigSyncTest {

    private BlockSetConfig previousConfig;

    @BeforeClass
    public static void initMinecraftBootstrap() {
        Bootstrap.register();
    }

    private void saveAndApply(BlockSetDefinition... sets) {
        previousConfig = BlockSetConfig.get();
        List<BlockSetDefinition> list = new ArrayList<>();
        Collections.addAll(list, sets);
        BlockSetConfig.applySets(list);
    }

    private void restore() {
        BlockSetConfig.applySets(previousConfig != null ? previousConfig.getSets() : Collections.emptyList());
    }

    private void quietLoadFromServerJson(String json) {
        String loggerName = OneBlockUltima.MODID;
        org.apache.logging.log4j.core.LoggerContext context =
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        Level previousLevel = context.getConfiguration().getLoggerConfig(loggerName).getLevel();
        Configurator.setLevel(loggerName, Level.OFF);
        try {
            BlockSetConfig.loadFromServerJson(json);
        } finally {
            Configurator.setLevel(loggerName, previousLevel);
        }
    }

    @Test
    public void toJsonReturnsNonNullNonEmpty() {
        String json = BlockSetConfig.get().toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    public void toJsonContainsSetIds() {
        BlockSetConfig config = BlockSetConfig.get();
        String json = config.toJson();
        for (BlockSetDefinition set : config.getSets()) {
            if (set.id != null) {
                assertTrue("JSON should contain set id: " + set.id, json.contains(set.id));
            }
        }
    }

    @Test
    public void toJsonRoundTripPreservesSetCount() {
        BlockSetConfig original = BlockSetConfig.get();
        String json = original.toJson();

        BlockSetConfig.loadFromServerJson(json);
        BlockSetConfig restored = BlockSetConfig.get();

        assertEquals(original.getSets().size(), restored.getSets().size());
    }

    @Test
    public void toJsonRoundTripPreservesSetIds() {
        BlockSetConfig original = BlockSetConfig.get();
        String json = original.toJson();

        BlockSetConfig.loadFromServerJson(json);
        BlockSetConfig restored = BlockSetConfig.get();

        for (BlockSetDefinition set : original.getSets()) {
            assertNotNull("Restored config should contain set: " + set.id, restored.getSet(set.id));
        }
    }

    @Test
    public void toJsonRoundTripPreservesBlockEntries() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.meta = 0;
        block.baseLevel = 1;
        block.baseChance = 100;

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "roundtrip_blocks";
        set.blocks.add(block);

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            BlockSetDefinition restoredSet = restored.getSet("roundtrip_blocks");
            assertNotNull(restoredSet);
            assertEquals(1, restoredSet.blocks.size());
            assertEquals("minecraft:stone", restoredSet.blocks.get(0).registry);
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesMobEntries() {
        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";
        mob.count = 3;
        mob.baseLevel = 2;
        mob.baseChance = 50;

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "roundtrip_mobs";
        set.mobs.add(mob);

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            BlockSetDefinition restoredSet = restored.getSet("roundtrip_mobs");
            assertNotNull(restoredSet);
            assertEquals(1, restoredSet.mobs.size());
            assertEquals("minecraft:pig", restoredSet.mobs.get(0).registry);
            assertEquals(3, restoredSet.mobs.get(0).count);
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesSettings() {
        BlockSetConfig original = BlockSetConfig.get();
        boolean wasFluid = original.getSettings().disableFluidGeneration;
        boolean wasMob = original.getSettings().disableMobGeneration;

        original.getSettings().disableFluidGeneration = true;
        original.getSettings().disableMobGeneration = true;

        try {
            String json = original.toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            assertTrue(restored.getSettings().disableFluidGeneration);
            assertTrue(restored.getSettings().disableMobGeneration);
        } finally {
            original.getSettings().disableFluidGeneration = wasFluid;
            original.getSettings().disableMobGeneration = wasMob;
        }
    }

    @Test
    public void toJsonRoundTripPreservesUnlockCost() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "cost_test";
        set.unlockCost = 500;

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            assertEquals(500, restored.getSet("cost_test").unlockCost);
        } finally {
            restore();
        }
    }

    @Test
    public void loadFromServerJsonReplacesInstance() {
        BlockSetConfig.get();

        BlockSetDefinition customSet = new BlockSetDefinition();
        customSet.id = "server_only_set";
        saveAndApply(customSet);

        String serverJson = BlockSetConfig.get().toJson();

        try {
            BlockSetConfig.loadFromServerJson(serverJson);
            BlockSetConfig after = BlockSetConfig.get();

            assertEquals(1, after.getSets().size());
            assertNotNull(after.getSet("server_only_set"));
        } finally {
            restore();
        }
    }

    @Test
    public void loadFromServerJsonInvalidJsonDoesNotCrash() {
        BlockSetConfig original = BlockSetConfig.get();
        int countBefore = original.getSets().size();

        quietLoadFromServerJson("not valid json {{{");
        BlockSetConfig after = BlockSetConfig.get();

        assertEquals(countBefore, after.getSets().size());
    }

    @Test
    public void loadFromServerJsonEmptyJsonDoesNotCrash() {
        BlockSetConfig original = BlockSetConfig.get();
        int countBefore = original.getSets().size();

        quietLoadFromServerJson("");
        BlockSetConfig after = BlockSetConfig.get();

        assertEquals(countBefore, after.getSets().size());
    }

    @Test
    public void loadFromServerJsonNullJsonDoesNotCrash() {
        BlockSetConfig original = BlockSetConfig.get();
        int countBefore = original.getSets().size();

        BlockSetConfig.loadFromServerJson(null);
        BlockSetConfig after = BlockSetConfig.get();

        assertEquals(countBefore, after.getSets().size());
    }

    @Test
    public void loadFromServerJsonBuildsIndexForLookup() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "indexed_set";
        saveAndApply(set);

        String serverJson = BlockSetConfig.get().toJson();

        try {
            BlockSetConfig.loadFromServerJson(serverJson);
            BlockSetConfig after = BlockSetConfig.get();

            assertNotNull(after.getSet("indexed_set"));
            assertNull(after.getSet("nonexistent"));
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesUnlockConditions() {
        UnlockConditionDefinition cond = new UnlockConditionDefinition();
        cond.type = "broken_blocks_total";
        cond.count = 200;

        UnlockConditionGroup group = new UnlockConditionGroup();
        group.mode = "any";
        group.conditions.add(cond);

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "unlock_test";
        set.unlockConditions = group;

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            BlockSetDefinition restoredSet = restored.getSet("unlock_test");
            assertNotNull(restoredSet);
            assertNotNull(restoredSet.unlockConditions);
            assertEquals("any", restoredSet.unlockConditions.mode);
            assertEquals(1, restoredSet.unlockConditions.conditions.size());
            assertEquals("broken_blocks_total", restoredSet.unlockConditions.conditions.get(0).type);
            assertEquals(200, restoredSet.unlockConditions.conditions.get(0).count);
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesMultipleSets() {
        List<BlockSetDefinition> sets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BlockSetDefinition set = new BlockSetDefinition();
            set.id = "set_" + i;
            set.unlockCost = i * 100;
            sets.add(set);
        }

        saveAndApply(sets.toArray(new BlockSetDefinition[0]));

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            assertEquals(5, restored.getSets().size());
            for (int i = 0; i < 5; i++) {
                assertNotNull(restored.getSet("set_" + i));
                assertEquals(i * 100, restored.getSet("set_" + i).unlockCost);
            }
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonPreservesBlockMeta() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:planks";
        block.meta = 4;
        block.baseLevel = 1;
        block.baseChance = 100;

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "meta_test";
        set.blocks.add(block);

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            assertEquals(4, restored.getSet("meta_test").blocks.get(0).meta);
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesBlockMetas() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stained_hardened_clay";
        block.meta = 0;
        block.metas = new ArrayList<>(java.util.Arrays.asList(1, 2, 3, 4));
        block.baseLevel = 1;
        block.baseChance = 100;

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "metas_test";
        set.blocks.add(block);

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            List<Integer> restoredMetas = restored.getSet("metas_test").blocks.get(0).getMetaValues();
            assertEquals(4, restoredMetas.size());
        } finally {
            restore();
        }
    }

    @Test
    public void mergeKeepsSameMetaDifferentNbtSeparate() {
        BlockElementDefinition a = new BlockElementDefinition();
        a.registry = "minecraft:stone";
        a.meta = 0;
        a.baseLevel = 1;
        a.baseChance = 100;
        a.nbtTags.setString("Type", "A");

        BlockElementDefinition b = new BlockElementDefinition();
        b.registry = "minecraft:stone";
        b.meta = 0;
        b.baseLevel = 1;
        b.baseChance = 100;
        b.nbtTags.setString("Type", "B");

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "nbt_variants";
        set.blocks.add(a);
        set.blocks.add(b);

        saveAndApply(set);

        try {
            assertEquals(2, BlockSetConfig.get().getSet("nbt_variants").blocks.size());
        } finally {
            restore();
        }
    }

    @Test
    public void mergeCombinesSameMetaSameNbt() {
        BlockElementDefinition a = new BlockElementDefinition();
        a.registry = "minecraft:stone";
        a.meta = 0;
        a.baseLevel = 1;
        a.baseChance = 100;
        a.nbtTags.setString("Type", "A");

        BlockElementDefinition b = new BlockElementDefinition();
        b.registry = "minecraft:stone";
        b.meta = 1;
        b.baseLevel = 1;
        b.baseChance = 100;
        b.nbtTags.setString("Type", "A");

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "merge_same_nbt";
        set.blocks.add(a);
        set.blocks.add(b);

        saveAndApply(set);

        try {
            List<BlockElementDefinition> blocks = BlockSetConfig.get().getSet("merge_same_nbt").blocks;
            assertEquals(1, blocks.size());
            assertEquals(2, blocks.get(0).getMetaValues().size());
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesRequiredMods() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        rm.setType(SetRequiredModsDefinition.TYPE.ALL);
        rm.addMod("jei");
        rm.addMod("crafttweaker");

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "mods_test";
        set.requiredMods = rm;

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            SetRequiredModsDefinition restoredRm = restored.getSet("mods_test").requiredMods;
            assertNotNull(restoredRm);
            assertEquals(SetRequiredModsDefinition.TYPE.ALL, restoredRm.getType());
            assertEquals(2, restoredRm.getMods().size());
        } finally {
            restore();
        }
    }

    @Test
    public void toJsonRoundTripPreservesMobBaseChance() {
        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:zombie";
        mob.count = 2;
        mob.baseLevel = 1;
        mob.baseChance = 75;

        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "mob_chance_test";
        set.mobs.add(mob);

        saveAndApply(set);

        try {
            String json = BlockSetConfig.get().toJson();
            BlockSetConfig.loadFromServerJson(json);
            BlockSetConfig restored = BlockSetConfig.get();

            MobElementDefinition restoredMob = restored.getSet("mob_chance_test").mobs.get(0);
            assertEquals(75, restoredMob.baseChance);
            assertEquals(2, restoredMob.count);
        } finally {
            restore();
        }
    }
}

package ru.defea.oneblockultima;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.BlockSetConfig.*;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class BlockSetDefinitionTest {

    private List<BlockSetConfig.BlockSetDefinition> originalSets;

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    @Before
    public void saveConfig() {
        originalSets = new ArrayList<>(BlockSetConfig.get().getSets());
    }

    @After
    public void restoreConfig() {
        BlockSetConfig.applySets(originalSets);
    }

    private OneBlockPlayerData newData() {
        return new OneBlockPlayerData();
    }

    @Test
    public void defaultRequiredModsIsAvailable() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        assertTrue(rm.isAvailable());
    }

    @Test
    public void requiredModsAllTypeWithMissingModReturnsFalse() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        rm.setType(SetRequiredModsDefinition.TYPE.ALL);
        rm.addMod("nonexistent_mod_123");
        assertFalse(rm.isAvailable());
    }

    @Test
    public void requiredModsAnyTypeWithNoLoadedModsReturnsFalse() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        rm.setType(SetRequiredModsDefinition.TYPE.ANY);
        rm.addMod("nonexistent_mod_123");
        assertFalse(rm.isAvailable());
    }

    @Test
    public void requiredModsAllTypeWithEmptyModsReturnsTrue() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        rm.setType(SetRequiredModsDefinition.TYPE.ALL);
        assertTrue(rm.isAvailable());
    }

    @Test
    public void unlockConditionBrokenBlocksTotalChecked() {
        UnlockConditionDefinition cond = new UnlockConditionDefinition();
        cond.type = "broken_blocks_total";
        cond.count = 100;

        OneBlockPlayerData data = newData();
        data.addBrokenBlocks("classic", 50);
        assertFalse(cond.isSatisfied(data));

        data.addBrokenBlocks("classic", 50);
        assertTrue(cond.isSatisfied(data));
    }

    @Test
    public void unlockConditionBrokenBlocksPerSetChecked() {
        UnlockConditionDefinition cond = new UnlockConditionDefinition();
        cond.type = "broken_blocks";
        cond.setId = "classic";
        cond.count = 10;

        OneBlockPlayerData data = newData();
        data.addBrokenBlocks("classic", 5);
        assertFalse(cond.isSatisfied(data));

        data.addBrokenBlocks("classic", 5);
        assertTrue(cond.isSatisfied(data));
    }

    @Test
    public void unlockConditionSetLevelChecked() {
        UnlockConditionDefinition cond = new UnlockConditionDefinition();
        cond.type = "set_level";
        cond.setId = "classic";
        cond.level = 3;

        OneBlockPlayerData data = newData();
        assertFalse(cond.isSatisfied(data));

        data.upgradeSet("classic", 0, 10);
        data.upgradeSet("classic", 0, 10);
        assertTrue(cond.isSatisfied(data));
    }

    @Test
    public void unlockConditionNullDataReturnsFalse() {
        UnlockConditionDefinition cond = new UnlockConditionDefinition();
        cond.type = "broken_blocks_total";
        cond.count = 0;
        assertFalse(cond.isSatisfied(null));
    }

    @Test
    public void unlockConditionUnknownTypeReturnsFalse() {
        UnlockConditionDefinition cond = new UnlockConditionDefinition();
        cond.type = "unknown";
        OneBlockPlayerData data = newData();
        assertFalse(cond.isSatisfied(data));
    }

    @Test
    public void unlockConditionsAllModeRequiresAllMet() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.unlockConditions = new UnlockConditionGroup();
        set.unlockConditions.mode = "all";

        UnlockConditionDefinition c1 = new UnlockConditionDefinition();
        c1.type = "broken_blocks_total";
        c1.count = 100;

        UnlockConditionDefinition c2 = new UnlockConditionDefinition();
        c2.type = "set_level";
        c2.setId = "classic";
        c2.level = 3;

        set.unlockConditions.conditions.add(c1);
        set.unlockConditions.conditions.add(c2);

        OneBlockPlayerData data = newData();
        data.addBrokenBlocks("classic", 50);
        assertFalse(set.hasUnlockRequirementsMet(data));

        data.addBrokenBlocks("classic", 50);
        data.upgradeSet("classic", 0, 10);
        data.upgradeSet("classic", 0, 10);
        assertTrue(set.hasUnlockRequirementsMet(data));
    }

    @Test
    public void unlockConditionsAnyModeRequiresOneMet() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.unlockConditions = new UnlockConditionGroup();
        set.unlockConditions.mode = "any";

        UnlockConditionDefinition c1 = new UnlockConditionDefinition();
        c1.type = "broken_blocks_total";
        c1.count = 100;

        UnlockConditionDefinition c2 = new UnlockConditionDefinition();
        c2.type = "set_level";
        c2.setId = "classic";
        c2.level = 3;

        set.unlockConditions.conditions.add(c1);
        set.unlockConditions.conditions.add(c2);

        OneBlockPlayerData data = newData();
        assertFalse(set.hasUnlockRequirementsMet(data));

        data.addBrokenBlocks("classic", 100);
        assertTrue(set.hasUnlockRequirementsMet(data));
    }

    @Test
    public void hasUnlockRequirementsWithNoConditionsReturnsTrue() {
        BlockSetDefinition set = new BlockSetDefinition();
        OneBlockPlayerData data = newData();
        assertTrue(set.hasUnlockRequirementsMet(data));
    }

    @Test
    public void ensureComputedLevelsCreatesLevels() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "test";

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        set.ensureComputedLevels();
        assertNotNull(set.getLevel(1));
    }

    @Test
    public void computedLevelHasCorrectUpgradeCost() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "test";
        set.unlockCost = 100;

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        set.ensureComputedLevels();
        SetLevelDefinition lvl = set.getLevel(1);
        assertNotNull(lvl);
        assertEquals(100, lvl.upgradeCost);
    }

    @Test
    public void computedLevelMaxLevelIsReasonable() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "test";

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        set.ensureComputedLevels();
        assertTrue(set.getMaxLevel() > 0);
    }

    @Test
    public void maxLevelCoversHighestElementBaseLevel() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "max_level_test";

        BlockElementDefinition low = new BlockElementDefinition();
        low.registry = "minecraft:stone";
        low.baseLevel = 1;
        low.baseChance = 100;
        set.blocks.add(low);

        BlockElementDefinition high = new BlockElementDefinition();
        high.registry = "minecraft:diamond_ore";
        high.baseLevel = 6;
        high.baseChance = 100;
        set.blocks.add(high);

        set.ensureComputedLevels();
        assertTrue("Max level must reach the highest element base level", set.getMaxLevel() >= 6);
        assertNotNull(set.getLevel(6));
    }

    @Test
    public void gapLevelsGetPreviousContent() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "gap_test";

        BlockElementDefinition low = new BlockElementDefinition();
        low.registry = "minecraft:stone";
        low.baseLevel = 1;
        low.baseChance = 100;
        set.blocks.add(low);

        BlockElementDefinition high = new BlockElementDefinition();
        high.registry = "minecraft:diamond_ore";
        high.baseLevel = 5;
        high.baseChance = 100;
        set.blocks.add(high);

        set.ensureComputedLevels();

        // Level 4 is a gap (no element starts there) but must still resolve
        SetLevelDefinition gap = set.getLevel(4);
        assertNotNull(gap);
        boolean hasStone = false;
        boolean hasDiamond = false;
        for (BlockEntryDefinition b : gap.blocks) {
            if ("minecraft:stone".equals(b.registry)) hasStone = true;
            if ("minecraft:diamond_ore".equals(b.registry)) hasDiamond = true;
        }
        assertTrue(hasStone);
        assertFalse("Elements above the gap level must not appear yet", hasDiamond);
    }

    @Test
    public void getLevelClampedReturnsMaxDefinitionForHighLevels() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "clamp_test";

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        set.ensureComputedLevels();

        int max = set.getMaxLevel();
        SetLevelDefinition maxDef = set.getLevel(max);
        SetLevelDefinition clamped = set.getLevelClamped(999);
        assertNotNull(clamped);
        assertSame(maxDef, clamped);
    }

    @Test
    public void changingMobSpawnPercentDoesNotReduceMaxLevel() {
        ModSettings settings = ModSettings.get();
        int original = settings.getMaxMobSpawnPercent();
        try {
            settings.setMaxMobSpawnPercent(0);
            BlockSetDefinition set = new BlockSetDefinition();
            set.id = "mob_percent_test";

            BlockElementDefinition block = new BlockElementDefinition();
            block.registry = "minecraft:stone";
            block.baseLevel = 1;
            block.baseChance = 100;
            set.blocks.add(block);

            MobElementDefinition mob = new MobElementDefinition();
            mob.registry = "minecraft:pig";
            mob.baseLevel = 6;
            mob.baseChance = 50;
            set.mobs.add(mob);

            set.ensureComputedLevels();
            assertTrue("Max level must cover mob base level even with mob spawn percent 0", set.getMaxLevel() >= 6);
            assertNotNull(set.getLevel(6));
        } finally {
            settings.setMaxMobSpawnPercent(original);
        }
    }

    @Test
    public void blockSetDefinitionDefaultsAreValid() {
        BlockSetDefinition set = new BlockSetDefinition();
        assertNotNull(set.blocks);
        assertNotNull(set.mobs);
        assertTrue(set.blocks.isEmpty());
        assertTrue(set.mobs.isEmpty());
        assertEquals(0, set.unlockCost);
        assertNull(set.unlockConditions);
    }

    @Test
    public void addBlockToSet() {
        BlockSetDefinition set = new BlockSetDefinition();
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        set.blocks.add(block);
        assertEquals(1, set.blocks.size());
    }

    @Test
    public void removeBlockFromSet() {
        BlockSetDefinition set = new BlockSetDefinition();
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        set.blocks.add(block);
        set.blocks.remove(0);
        assertEquals(0, set.blocks.size());
    }

    @Test
    public void addMobToSet() {
        BlockSetDefinition set = new BlockSetDefinition();
        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";
        set.mobs.add(mob);
        assertEquals(1, set.mobs.size());
    }

    @Test
    public void removeMobToSet() {
        BlockSetDefinition set = new BlockSetDefinition();
        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";
        set.mobs.add(mob);
        set.mobs.remove(0);
        assertEquals(0, set.mobs.size());
    }

    @Test
    public void editBlockProperties() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";

        block.meta = 2;
        block.baseLevel = 5;

        assertEquals(2, block.meta);
        assertEquals(5, block.baseLevel);
    }

    @Test
    public void editMobProperties() {
        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";

        mob.count = 3;
        mob.baseLevel = 5;
        mob.baseChance = 20;

        assertEquals(3, mob.count);
        assertEquals(5, mob.baseLevel);
        assertEquals(20, mob.baseChance);
    }

    @Test
    public void changeSetIdAndCost() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "test";
        set.unlockCost = 500;

        assertEquals("test", set.id);
        assertEquals(500, set.unlockCost);
    }

    @Test
    public void requiredModsAddAndClear() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        rm.addMod("foo");
        assertEquals(1, rm.getMods().size());
        rm.clear();
        assertTrue(rm.getMods().isEmpty());
    }

    @Test
    public void requiredModsTypeToggle() {
        SetRequiredModsDefinition rm = new SetRequiredModsDefinition();
        rm.setType(SetRequiredModsDefinition.TYPE.ANY);
        assertEquals(SetRequiredModsDefinition.TYPE.ANY, rm.getType());
    }

    @Test
    public void getChanceZeroReturnsOne() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.chance = 0;
        assertEquals(1, entry.getChance());
    }

    @Test
    public void getChanceNegativeReturnsOne() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.chance = -5;
        assertEquals(1, entry.getChance());
    }

    @Test
    public void getChancePositiveReturnsValue() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.chance = 75;
        assertEquals(75, entry.getChance());
    }

    @Test
    public void resolveBlockReturnsBlockForValidRegistry() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        assertNotNull(entry.resolveBlock());
    }

    @Test
    public void resolveBlockReturnsNullForEmptyRegistry() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "";
        assertNull(entry.resolveBlock());
    }

    @Test
    public void resolveBlockReturnsNullForNullRegistry() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = null;
        assertNull(entry.resolveBlock());
    }

    @Test
    public void resolveBlockReturnsCachedInstance() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        Block first = entry.resolveBlock();
        Block second = entry.resolveBlock();
        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    public void resolveBlockCachesResultForUnknownRegistry() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "nonexistent_mod:missing_block";
        Block first = entry.resolveBlock();
        Block second = entry.resolveBlock();
        assertNotNull("unknown registry resolves to air in 1.21", first);
        assertSame("unknown registry resolves to air sentinel", Blocks.AIR, first);
        assertSame(first, second);
    }

    @Test
    public void isFluidReturnsTrueForWater() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:water";
        assertTrue(entry.isFluid());
    }

    @Test
    public void isFluidReturnsFalseForStone() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        assertFalse(entry.isFluid());
    }

    @Test
    public void isFluidReturnsFalseForNullRegistry() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = null;
        assertFalse(entry.isFluid());
    }

    @Test
    public void getPickBlockWithDropItemReturnsItem() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        entry.dropItem = "minecraft:diamond";
        ItemStack stack = entry.getPickBlock();
        assertFalse(stack.isEmpty());
        assertEquals(Items.DIAMOND, stack.getItem());
    }

    @Test
    public void getPickBlockWithNullDropItemReturnsBlockItem() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        entry.dropItem = null;
        entry.meta = 0;
        ItemStack stack = entry.getPickBlock();
        assertFalse(stack.isEmpty());
    }

    @Test
    public void getPickBlockWithInvalidDropItemFallsBackToBlock() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        entry.dropItem = "nonexistent:item";
        entry.meta = 0;
        ItemStack stack = entry.getPickBlock();
        assertFalse(stack.isEmpty());
    }

    @Test
    public void getPickBlockWithNullRegistryReturnsEmpty() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = null;
        entry.dropItem = null;
        ItemStack stack = entry.getPickBlock();
        assertTrue(stack.isEmpty());
    }

    @Test
    public void getPickBlockDropItemTakesPriorityOverBlock() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        entry.dropItem = "minecraft:gold_ingot";
        entry.meta = 0;
        ItemStack stack = entry.getPickBlock();
        assertEquals(Items.GOLD_INGOT, stack.getItem());
    }

    @Test
    public void getPickBlockAppliesNbtTags() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        entry.meta = 0;
        entry.nbtTags.putString("CustomColor", "blue");
        ItemStack stack = entry.getPickBlock();
        assertFalse(stack.isEmpty());
        assertTrue("stack should carry custom data", stack.get(DataComponents.CUSTOM_DATA) != null);
        assertEquals("blue", stack.get(DataComponents.CUSTOM_DATA).getUnsafe().getString("CustomColor"));
    }

    @Test
    public void getPickBlockWithoutNbtLeavesStackWithoutTag() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = "minecraft:stone";
        entry.meta = 0;
        ItemStack stack = entry.getPickBlock();
        assertFalse(stack.isEmpty());
        assertFalse(stack.get(DataComponents.CUSTOM_DATA) != null);
    }

    @Test
    public void mobEntryDefinitionDefaultFields() {
        MobEntryDefinition mob = new MobEntryDefinition();
        assertEquals(1, mob.count);
        assertEquals(0, mob.chance);
        assertNotNull(mob.nbtTags);
        assertNull(mob.registry);
    }

    @Test
    public void mobEntryDefinitionGetChanceReturnsRawValue() {
        MobEntryDefinition mob = new MobEntryDefinition();
        mob.chance = 0;
        assertEquals(0, mob.getChance());
        mob.chance = -10;
        assertEquals(-10, mob.getChance());
        mob.chance = 50;
        assertEquals(50, mob.getChance());
    }

    @Test
    public void pickMobReturnsNullForEmptyMobs() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        lvl.mobs = new ArrayList<>();
        assertNull(lvl.pickMob(RandomSource.create()));
    }

    @Test
    public void pickMobReturnsNullForNullMobs() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        lvl.mobs = null;
        assertNull(lvl.pickMob(RandomSource.create()));
    }

    @Test
    public void pickMobReturnsNullForUnavailableMobs() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        MobEntryDefinition mob = new MobEntryDefinition();
        mob.registry = "nonexistent:fake_entity";
        mob.chance = 100;
        lvl.mobs = Collections.singletonList(mob);
        assertNull(lvl.pickMob(RandomSource.create()));
    }

    @Test
    public void pickMobReturnsEntryForAvailableMob() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        MobEntryDefinition mob = new MobEntryDefinition();
        mob.registry = "minecraft:pig";
        mob.chance = 100;
        lvl.mobs = Collections.singletonList(mob);
        MobEntryDefinition result = lvl.pickMob(RandomSource.create());
        assertNotNull(result);
        assertEquals("minecraft:pig", result.registry);
    }

    @Test
    public void pickMobRespectsWeightedChance() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        MobEntryDefinition pig = new MobEntryDefinition();
        pig.registry = "minecraft:pig";
        pig.chance = 90;
        MobEntryDefinition cow = new MobEntryDefinition();
        cow.registry = "minecraft:cow";
        cow.chance = 10;
        lvl.mobs = Arrays.asList(pig, cow);

        int pigCount = 0;
        int cowCount = 0;
        int nullCount = 0;
        for (int i = 0; i < 1000; i++) {
            MobEntryDefinition picked = lvl.pickMob(RandomSource.create(42 + i));
            if (picked == null) nullCount++;
            else if ("minecraft:pig".equals(picked.registry)) pigCount++;
            else if ("minecraft:cow".equals(picked.registry)) cowCount++;
        }
        assertTrue("pig should be picked more often than cow", pigCount > cowCount);
        assertEquals("with totalChance=100, no nulls expected from min(100,totalChance)", 0, nullCount);
    }

    @Test
    public void pickMobMinRollFloorCausesNullsWhenTotalChanceBelow100() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        MobEntryDefinition mob = new MobEntryDefinition();
        mob.registry = "minecraft:pig";
        mob.chance = 50;
        lvl.mobs = Collections.singletonList(mob);

        int nullCount = 0;
        for (int i = 0; i < 1000; i++) {
            MobEntryDefinition picked = lvl.pickMob(RandomSource.create(i));
            if (picked == null) nullCount++;
        }
        assertTrue("with totalChance=50 and min(100,totalChance)=100, ~50% should be null", nullCount > 300);
    }

    @Test
    public void blockElementDefinitionGetMetaValuesEmptyMetasReturnsSingleton() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.meta = 3;
        block.metas = new ArrayList<>();
        List<Integer> values = block.getMetaValues();
        assertEquals(1, values.size());
        assertEquals(Integer.valueOf(3), values.get(0));
    }

    @Test
    public void blockElementDefinitionGetMetaValuesWithMetasReturnsList() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.meta = 3;
        block.metas = Arrays.asList(1, 2, 3);
        List<Integer> values = block.getMetaValues();
        assertEquals(3, values.size());
    }

    @Test
    public void blockElementDefinitionGetMetaValuesNullMetasReturnsSingleton() {
        BlockElementDefinition block = new BlockElementDefinition();
        block.meta = 5;
        block.metas = null;
        List<Integer> values = block.getMetaValues();
        assertEquals(1, values.size());
        assertEquals(Integer.valueOf(5), values.get(0));
    }

    @Test
    public void applySetsReplacesExistingSets() {
        List<BlockSetDefinition> sets = new ArrayList<>();
        BlockSetDefinition s1 = new BlockSetDefinition();
        s1.id = "set_a";
        sets.add(s1);
        BlockSetConfig.applySets(sets);

        BlockSetConfig config = BlockSetConfig.get();
        assertNotNull(config.getSet("set_a"));
        assertEquals(1, config.getSets().size());
    }

    @Test
    public void applySetsWithNullCreatesEmpty() {
        BlockSetConfig.applySets(null);
        BlockSetConfig config = BlockSetConfig.get();
        assertTrue(config.getSets().isEmpty());
    }

    @Test
    public void applySetsReplacesPreviousSets() {
        List<BlockSetDefinition> first = new ArrayList<>();
        BlockSetDefinition s1 = new BlockSetDefinition();
        s1.id = "first";
        first.add(s1);
        BlockSetConfig.applySets(first);

        List<BlockSetDefinition> second = new ArrayList<>();
        BlockSetDefinition s2 = new BlockSetDefinition();
        s2.id = "second";
        second.add(s2);
        BlockSetConfig.applySets(second);

        BlockSetConfig config = BlockSetConfig.get();
        assertNull(config.getSet("first"));
        assertNotNull(config.getSet("second"));
        assertEquals(1, config.getSets().size());
    }

    @Test
    public void applySetsGetSetReturnsNullForUnknownId() {
        List<BlockSetDefinition> sets = new ArrayList<>();
        BlockSetDefinition s1 = new BlockSetDefinition();
        s1.id = "known";
        sets.add(s1);
        BlockSetConfig.applySets(sets);

        BlockSetConfig config = BlockSetConfig.get();
        assertNull(config.getSet("unknown"));
    }

    @Test
    public void copyBlockSetDefinitionCreatesDeepCopy() {
        BlockSetDefinition original = new BlockSetDefinition();
        original.id = "test";
        original.unlockCost = 100;

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        original.blocks.add(block);

        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";
        mob.count = 3;
        original.mobs.add(mob);

        BlockSetDefinition copy = BlockSetConfig.copyBlockSetDefinition(original);

        assertEquals(original.id, copy.id);
        assertEquals(original.unlockCost, copy.unlockCost);
        assertEquals(1, copy.blocks.size());
        assertEquals(1, copy.mobs.size());
    }

    @Test
    public void copyBlockSetDefinitionModifyingCopyDoesNotAffectOriginal() {
        BlockSetDefinition original = new BlockSetDefinition();
        original.id = "test";
        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        original.blocks.add(block);

        BlockSetDefinition copy = BlockSetConfig.copyBlockSetDefinition(original);
        copy.id = "modified";
        copy.blocks.get(0).registry = "minecraft:diamond";

        assertEquals("test", original.id);
        assertEquals("minecraft:stone", original.blocks.get(0).registry);
    }

    @Test
    public void copyBlockSetDefinitionWithNullReturnsNull() {
        assertNull(BlockSetConfig.copyBlockSetDefinition(null));
    }

    @Test
    public void blockEntryDefinitionDefaultNbtTagsNotEmpty() {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        assertNotNull(entry.nbtTags);
        assertTrue(entry.nbtTags.isEmpty());
    }

    @Test
    public void mobEntryDefinitionDefaultNbtTagsNotEmpty() {
        MobEntryDefinition mob = new MobEntryDefinition();
        assertNotNull(mob.nbtTags);
        assertTrue(mob.nbtTags.isEmpty());
    }

    @Test
    public void setLevelDefinitionDefaultFields() {
        SetLevelDefinition lvl = new SetLevelDefinition();
        assertNotNull(lvl.blocks);
        assertNotNull(lvl.mobs);
        assertTrue(lvl.blocks.isEmpty());
        assertTrue(lvl.mobs.isEmpty());
    }

    @Test
    public void blockSetDefinitionIdAndCostSettable() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "mySet";
        set.unlockCost = 250;
        assertEquals("mySet", set.id);
        assertEquals(250, set.unlockCost);
    }

    @Test
    public void settingsDefinitionDefaultsAreAllFalse() {
        SettingsDefinition settings = new SettingsDefinition();
        assertFalse(settings.disableFluidGeneration);
        assertFalse(settings.disableMobGeneration);
        assertFalse(settings.disableChestGeneration);
        assertFalse(settings.disableSaplingGeneration);
    }

    @Test
    public void settingsDefinitionFieldsAreIndependent() {
        SettingsDefinition settings = new SettingsDefinition();
        assertFalse(settings.disableMobGeneration);
        assertFalse(settings.disableChestGeneration);
        assertFalse(settings.disableSaplingGeneration);

        settings.disableMobGeneration = true;
        settings.disableFluidGeneration = false;
    }

    @Test
    public void getSettingsReturnsNonNull() {
        BlockSetConfig config = BlockSetConfig.get();
        assertNotNull(config.getSettings());
    }

    @Test
    public void ensureComputedLevelsWithMobsCreatesMobLevels() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "mob_test";

        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";
        mob.baseLevel = 1;
        mob.baseChance = 100;
        mob.count = 2;
        set.mobs.add(mob);

        set.ensureComputedLevels();

        SetLevelDefinition lvl = set.getLevel(1);
        assertNotNull(lvl);
        assertFalse(lvl.mobs.isEmpty());
        assertEquals("minecraft:pig", lvl.mobs.get(0).registry);
        assertEquals(2, lvl.mobs.get(0).count);
    }

    @Test
    public void ensureComputedLevelsWithBlocksAndMobs() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "mixed_test";

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 50;
        set.blocks.add(block);

        MobElementDefinition mob = new MobElementDefinition();
        mob.registry = "minecraft:pig";
        mob.baseLevel = 2;
        mob.baseChance = 50;
        mob.count = 1;
        set.mobs.add(mob);

        set.ensureComputedLevels();

        SetLevelDefinition lvl1 = set.getLevel(1);
        assertNotNull(lvl1);
        assertFalse(lvl1.blocks.isEmpty());

        SetLevelDefinition lvl2 = set.getLevel(2);
        assertNotNull(lvl2);
        assertFalse(lvl2.mobs.isEmpty());
    }

    @Test
    public void ensureComputedLevelsSkipsUnavailableBlocks() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "unavail_test";

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        BlockElementDefinition fakeBlock = new BlockElementDefinition();
        fakeBlock.registry = "nonexistent:fake_block";
        fakeBlock.baseLevel = 1;
        fakeBlock.baseChance = 100;
        set.blocks.add(fakeBlock);

        set.ensureComputedLevels();

        SetLevelDefinition lvl = set.getLevel(1);
        assertNotNull(lvl);
        assertEquals(1, lvl.blocks.size());
        assertEquals("minecraft:stone", lvl.blocks.get(0).registry);
    }

    @Test
    public void ensureComputedLevelsUpgradeCost() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "cost_test";
        set.unlockCost = 50;

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        set.ensureComputedLevels();

        SetLevelDefinition lvl1 = set.getLevel(1);
        assertNotNull(lvl1);
        assertEquals(50, lvl1.upgradeCost);

        SetLevelDefinition lvl2 = set.getLevel(2);
        assertNotNull(lvl2);
        assertEquals(100, lvl2.upgradeCost);
    }

    @Test
    public void ensureComputedLevelsMultiplierCost() {
        BlockSetConfig.SettingsDefinition settings = BlockSetConfig.get().getSettings();
        String oldMode = settings.setCostIncreaseMode;
        double oldValue = settings.setCostIncreaseValue;
        try {
            settings.setCostIncreaseMode = "multiplier";
            settings.setCostIncreaseValue = 2.0;

            BlockSetDefinition set = new BlockSetDefinition();
            set.id = "mult_cost";
            set.unlockCost = 100;

            BlockElementDefinition block = new BlockElementDefinition();
            block.registry = "minecraft:stone";
            block.baseLevel = 1;
            block.baseChance = 100;
            set.blocks.add(block);

            set.ensureComputedLevels();

            SetLevelDefinition lvl1 = set.getLevel(1);
            assertNotNull(lvl1);
            assertEquals(100, lvl1.upgradeCost);

            SetLevelDefinition lvl2 = set.getLevel(2);
            assertNotNull(lvl2);
            assertEquals(200, lvl2.upgradeCost);
        } finally {
            settings.setCostIncreaseMode = oldMode;
            settings.setCostIncreaseValue = oldValue;
            BlockSetConfig.invalidateComputedLevels();
        }
    }

    @Test
    public void ensureComputedLevelsWithMultipleBlocksDistributesChance() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "dist_test";

        BlockElementDefinition stone = new BlockElementDefinition();
        stone.registry = "minecraft:stone";
        stone.baseLevel = 1;
        stone.baseChance = 75;
        set.blocks.add(stone);

        BlockElementDefinition dirt = new BlockElementDefinition();
        dirt.registry = "minecraft:dirt";
        dirt.baseLevel = 1;
        dirt.baseChance = 25;
        set.blocks.add(dirt);

        set.ensureComputedLevels();

        SetLevelDefinition lvl = set.getLevel(1);
        assertNotNull(lvl);
        assertEquals(2, lvl.blocks.size());

        int stoneChance = 0;
        int dirtChance = 0;
        for (BlockEntryDefinition entry : lvl.blocks) {
            if ("minecraft:stone".equals(entry.registry)) stoneChance = entry.chance;
            if ("minecraft:dirt".equals(entry.registry)) dirtChance = entry.chance;
        }
        assertTrue("stone should have higher chance than dirt", stoneChance > dirtChance);
        assertEquals(100, stoneChance + dirtChance);
    }

    @Test
    public void ensureComputedLevelsIdempotent() {
        BlockSetDefinition set = new BlockSetDefinition();
        set.id = "idempotent_test";

        BlockElementDefinition block = new BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.baseLevel = 1;
        block.baseChance = 100;
        set.blocks.add(block);

        set.ensureComputedLevels();
        SetLevelDefinition first = set.getLevel(1);
        assertNotNull(first);

        set.ensureComputedLevels();
        SetLevelDefinition second = set.getLevel(1);
        assertSame("second call should return same object (cached)", first, second);
    }
}
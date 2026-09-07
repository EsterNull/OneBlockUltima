package ru.defea.oneblockultima;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static net.minecraft.nbt.Tag.*;
import static org.junit.Assert.*;

public class ContainerSetsConfigNbtTest {

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

    private ContainerSetsConfig newContainer() {
        ContainerSetsConfig container = new ContainerSetsConfig();
        container.addNewSet();
        BlockSetConfig.BlockSetDefinition set = container.getEditingSet();
        set.id = "test_nbt";
        set.unlockCost = 0;
        set.blocks = new ArrayList<>();
        set.mobs = new ArrayList<>();
        set.requiredMods = new BlockSetConfig.SetRequiredModsDefinition();
        set.unlockConditions = new BlockSetConfig.UnlockConditionGroup();
        set.unlockConditions.conditions = new ArrayList<>();

        BlockSetConfig.BlockElementDefinition block = new BlockSetConfig.BlockElementDefinition();
        block.registry = "minecraft:stone";
        block.meta = 0;
        block.metas = new ArrayList<>(Arrays.asList(0, 1));
        block.baseLevel = 1;
        block.baseChance = 50;
        block.dropItem = "minecraft:cobblestone";
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CustomName", "Magic Stone");
        block.nbtTags = nbt;
        set.blocks.add(block);

        BlockSetConfig.MobElementDefinition mob = new BlockSetConfig.MobElementDefinition();
        mob.registry = "minecraft:zombie";
        mob.baseLevel = 2;
        mob.baseChance = 5;
        CompoundTag mobNbt = new CompoundTag();
        mobNbt.putString("CustomName", "Zombie King");
        mob.nbtTags = mobNbt;
        set.mobs.add(mob);
        return container;
    }

    private ContainerSetsConfig newBlockEntry() {
        ContainerSetsConfig container = newContainer();
        container.editEntry(0, ContainerSetsConfig.EntryType.BLOCK);
        return container;
    }

    private ContainerSetsConfig.NbtTagEntry findTag(List<ContainerSetsConfig.NbtTagEntry> tags, String key) {
        for (ContainerSetsConfig.NbtTagEntry tag : tags) {
            if (tag.key.equals(key)) return tag;
        }
        return null;
    }

    private void cycleAddType(ContainerSetsConfig container, int targetType) {
        for (int i = 0; i < 12; i++) {
            if (container.getNbtEditorAddType() == targetType) return;
            container.cycleNbtEditorAddType();
        }
    }

    @Test
    public void getEditingEntryNbtReturnsIndependentCopy() {
        ContainerSetsConfig container = newBlockEntry();

        CompoundTag snapshot = container.getEditingEntryNbt();
        assertEquals("Magic Stone", snapshot.getString("CustomName"));
        snapshot.putString("CustomName", "Mutated");
        snapshot.putInt("Purity", 9);

        CompoundTag actual = container.getEditingEntryNbt();
        assertEquals("Magic Stone", actual.getString("CustomName"));
        assertFalse(actual.contains("Purity"));
    }

    @Test
    public void getNbtTagsListsCompoundEntries() {
        ContainerSetsConfig container = newBlockEntry();

        List<ContainerSetsConfig.NbtTagEntry> tags = container.getNbtTags();
        assertEquals(1, tags.size());
        ContainerSetsConfig.NbtTagEntry tag = tags.get(0);
        assertEquals("CustomName", tag.key);
        assertEquals(-1, tag.index);
        assertEquals(TAG_STRING, tag.getTypeId());
        assertTrue(tag.isScalar());
        assertFalse(tag.isCompound());
        assertFalse(tag.isList());
    }

    @Test
    public void nbtEditorPathNavigationIntoCompound() {
        ContainerSetsConfig container = newBlockEntry();
        CompoundTag info = new CompoundTag();
        info.putString("Name", "Secret");
        container.getEditingSet().blocks.get(0).nbtTags.put("Info", info);

        assertTrue(container.nbtEditorAtRoot());
        List<ContainerSetsConfig.NbtTagEntry> tags = container.getNbtTags();
        ContainerSetsConfig.NbtTagEntry infoTag = findTag(tags, "Info");
        assertNotNull(infoTag);
        assertTrue(infoTag.isCompound());

        container.nbtEditorPush("Info");
        assertFalse(container.nbtEditorAtRoot());
        assertEquals("Info", container.nbtEditorPathLabel());

        List<ContainerSetsConfig.NbtTagEntry> inner = container.getNbtTags();
        assertEquals(1, inner.size());
        assertEquals("Name", inner.get(0).key);

        container.nbtEditorPop();
        assertTrue(container.nbtEditorAtRoot());
    }

    @Test
    public void nbtEditorPathNavigationIntoList() {
        ContainerSetsConfig container = newBlockEntry();
        ListTag list = new ListTag();
        CompoundTag e0 = new CompoundTag();
        e0.putInt("Level", 1);
        CompoundTag e1 = new CompoundTag();
        e1.putInt("Level", 2);
        list.add(e0);
        list.add(e1);
        container.getEditingSet().blocks.get(0).nbtTags.put("L", list);

        container.nbtEditorPush("L");
        assertTrue(container.nbtEditorIsListContext());

        List<ContainerSetsConfig.NbtTagEntry> entries = container.getNbtTags();
        assertEquals(2, entries.size());
        assertEquals(0, entries.get(0).index);
        assertEquals(1, entries.get(1).index);
        assertTrue(entries.get(0).isCompound());

        container.nbtEditorPushIndex(1);
        assertEquals("L / [1]", container.nbtEditorPathLabel());

        List<ContainerSetsConfig.NbtTagEntry> inner = container.getNbtTags();
        assertEquals(1, inner.size());
        assertEquals("Level", inner.get(0).key);
    }

    @Test
    public void nbtEditorApplyAddsStringTag() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();

        assertTrue(container.nbtEditorApply("Level", "3"));
        List<ContainerSetsConfig.NbtTagEntry> tags = container.getNbtTags();
        assertEquals(2, tags.size());
        ContainerSetsConfig.NbtTagEntry level = findTag(tags, "Level");
        assertNotNull(level);
        assertEquals(TAG_STRING, level.getTypeId());
        assertEquals("3", container.getEditingEntryNbt().getString("Level"));
    }

    @Test
    public void nbtEditorApplyAddsTypedScalar() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_INT);

        assertTrue(container.nbtEditorApply("Count", "7"));
        assertEquals(7, container.getEditingEntryNbt().getInt("Count"));
    }

    @Test
    public void nbtEditorApplyAddsEmptyCompoundThenFillsIt() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_COMPOUND);

        assertTrue(container.nbtEditorApply("Inner", ""));
        Tag inner = container.getEditingEntryNbt().get("Inner");
        assertTrue(inner instanceof CompoundTag);
        assertTrue(((CompoundTag) inner).getAllKeys().isEmpty());

        container.nbtEditorPush("Inner");
        container.nbtEditorStartAdd();
        assertTrue(container.nbtEditorApply("X", "1"));
        assertEquals("1", container.getEditingEntryNbt().getCompound("Inner").getString("X"));
    }

    @Test
    public void nbtEditorApplyEditsExistingScalar() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartEdit("CustomName");

        assertTrue(container.nbtEditorApply("CustomName", "New Name"));
        assertEquals("New Name", container.getEditingEntryNbt().getString("CustomName"));
        assertFalse(container.nbtEditorIsEditing());
    }

    @Test
    public void nbtEditorApplyRejectsEmptyKey() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();

        assertFalse(container.nbtEditorApply("   ", "x"));
        assertEquals(1, container.getNbtTags().size());
    }

    @Test
    public void nbtEditorApplyRejectsInvalidNumber() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_INT);

        assertFalse(container.nbtEditorApply("N", "abc"));
        assertNull(findTag(container.getNbtTags(), "N"));
    }

    @Test
    public void nbtEditorRemoveRemovesCompoundTag() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        assertTrue(container.nbtEditorApply("Temp", "1"));

        assertTrue(container.nbtEditorRemove("Temp"));
        assertFalse(container.getEditingEntryNbt().contains("Temp"));
        assertFalse(container.nbtEditorRemove("Missing"));
    }

    @Test
    public void nbtEditorListAppendAndSet() {
        ContainerSetsConfig container = newBlockEntry();
        ListTag list = new ListTag();
        list.add(ByteTag.valueOf((byte) 1));
        list.add(ByteTag.valueOf((byte) 2));
        container.getEditingSet().blocks.get(0).nbtTags.put("L", list);

        container.nbtEditorPush("L");
        assertTrue(container.nbtEditorListTypeIsFixed());
        assertEquals(TAG_BYTE, container.getNbtEditorListElementType());

        assertTrue(container.nbtEditorApply("", "3"));
        ListTag afterAppend = (ListTag) container.getEditingEntryNbt().get("L");
        assertEquals(3, afterAppend.size());
        assertEquals(3, ((ByteTag) afterAppend.get(2)).getAsByte());

        container.nbtEditorStartEditIndex(0);
        assertTrue(container.nbtEditorIsEditingListElement());
        assertEquals("1", container.nbtEditorGetValue());
        assertTrue(container.nbtEditorApply("", "9"));
        ListTag afterSet = (ListTag) container.getEditingEntryNbt().get("L");
        assertEquals(9, ((ByteTag) afterSet.get(0)).getAsByte());
        assertEquals(3, afterSet.size());
    }

    @Test
    public void nbtEditorRemoveIndexRemovesListElement() {
        ContainerSetsConfig container = newBlockEntry();
        ListTag list = new ListTag();
        list.add(ByteTag.valueOf((byte) 1));
        list.add(ByteTag.valueOf((byte) 2));
        container.getEditingSet().blocks.get(0).nbtTags.put("L", list);

        container.nbtEditorPush("L");
        assertTrue(container.nbtEditorRemoveIndex(0));
        ListTag after = (ListTag) container.getEditingEntryNbt().get("L");
        assertEquals(1, after.size());
        assertEquals(2, ((ByteTag) after.get(0)).getAsByte());
        assertFalse(container.nbtEditorRemoveIndex(5));
    }

    @Test
    public void nbtEditorAppendToEmptyListCreatesFreshNode() {
        ContainerSetsConfig container = newBlockEntry();
        ListTag list = new ListTag();
        container.getEditingSet().blocks.get(0).nbtTags.put("L", list);

        container.nbtEditorPush("L");
        assertFalse(container.nbtEditorListTypeIsFixed());
        assertTrue(container.nbtEditorApply("", "5"));
        ListTag after = (ListTag) container.getEditingEntryNbt().get("L");
        assertEquals(1, after.size());
        assertEquals(5, ((ByteTag) after.get(0)).getAsByte());
    }

    @Test
    public void nbtEditorGetValueRoundtrips() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartEdit("CustomName");
        assertEquals("Magic Stone", container.nbtEditorGetValue());
        assertEquals("CustomName", container.nbtEditorGetEditingKey());
        assertEquals(TAG_STRING, container.nbtEditorGetEditingTypeId());
    }

    @Test
    public void nbtEditorStartEditPrefillsTexts() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartEdit("CustomName");
        assertEquals("Magic Stone", container.nbtEditorGetValueText());
        assertEquals("CustomName", container.nbtEditorGetKeyText());
    }

    @Test
    public void nbtEditorApplyEditCanRenameKey() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartEdit("CustomName");

        assertTrue(container.nbtEditorApply("Renamed", "X"));
        CompoundTag nbt = container.getEditingEntryNbt();
        assertFalse(nbt.contains("CustomName"));
        assertEquals("X", nbt.getString("Renamed"));
        assertEquals("", container.nbtEditorGetValueText());
        assertEquals("", container.nbtEditorGetKeyText());
    }

    @Test
    public void nbtEditorApplyEditRejectsEmptyKey() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartEdit("CustomName");

        assertFalse(container.nbtEditorApply("   ", "X"));
        CompoundTag nbt = container.getEditingEntryNbt();
        assertEquals("Magic Stone", nbt.getString("CustomName"));
        assertTrue(container.nbtEditorIsEditing());
    }

    @Test
    public void nbtEditorApplyAddClearsPendingTexts() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        container.setNbtEditorKeyText("K");
        container.setNbtEditorValueText("V");

        assertTrue(container.nbtEditorApply("K", "V"));
        assertEquals("", container.nbtEditorGetKeyText());
        assertEquals("", container.nbtEditorGetValueText());
    }

    @Test
    public void nbtEditorSetKeyTextStored() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        container.setNbtEditorKeyText("hello");
        assertEquals("hello", container.nbtEditorGetKeyText());
        container.nbtEditorStartAdd();
        assertEquals("", container.nbtEditorGetKeyText());
    }

    @Test
    public void nbtValuePreviewTruncatesLongStrings() {
        CompoundTag c = new CompoundTag();
        c.putString("Long", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

        String preview = ContainerSetsConfig.nbtValuePreview(c.get("Long"));
        assertEquals(48, preview.length());
        assertTrue(preview.endsWith("..."));
    }

    @Test
    public void nbtValuePreviewShowsCountsForContainers() {
        CompoundTag c = new CompoundTag();
        c.putString("A", "1");
        c.putString("B", "2");

        assertEquals("{2}", ContainerSetsConfig.nbtValuePreview(c));

        ListTag l = new ListTag();
        l.add(ByteTag.valueOf((byte) 1));
        l.add(ByteTag.valueOf((byte) 2));
        l.add(ByteTag.valueOf((byte) 3));
        assertEquals("[3]", ContainerSetsConfig.nbtValuePreview(l));
    }

    @Test
    public void nbtTypeKeyMapping() {
        assertEquals("byte", ContainerSetsConfig.nbtTypeKey(TAG_BYTE));
        assertEquals("int", ContainerSetsConfig.nbtTypeKey(TAG_INT));
        assertEquals("list", ContainerSetsConfig.nbtTypeKey(TAG_LIST));
        assertEquals("compound", ContainerSetsConfig.nbtTypeKey(TAG_COMPOUND));
        assertEquals("end", ContainerSetsConfig.nbtTypeKey(TAG_END));
    }

    @Test
    public void nbtEditorValueTextStored() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        assertEquals("", container.nbtEditorGetValueText());
        container.setNbtEditorValueText("hello");
        assertEquals("hello", container.nbtEditorGetValueText());
        container.nbtEditorStartAdd();
        assertEquals("", container.nbtEditorGetValueText());
    }

    @Test
    public void saveCurrencyMetaSplitPreservesNbtAndDropItem() {
        ContainerSetsConfig container = newContainer();
        container.setSelectedBlockMeta(1);
        container.editEntry(0, ContainerSetsConfig.EntryType.BLOCK);

        assertTrue(container.saveCurrency(5, 10));

        List<BlockSetConfig.BlockElementDefinition> blocks = container.getEditingSet().blocks;
        assertEquals(2, blocks.size());

        BlockSetConfig.BlockElementDefinition original = blocks.get(0);
        BlockSetConfig.BlockElementDefinition split = blocks.get(1);

        assertEquals(0, original.meta);
        assertEquals(Collections.singletonList(0), original.metas);
        assertEquals(1, original.baseLevel);
        assertEquals(50, original.baseChance);
        assertEquals("Magic Stone", original.nbtTags.getString("CustomName"));
        assertEquals("minecraft:cobblestone", original.dropItem);

        assertEquals(1, split.meta);
        assertEquals(Collections.singletonList(1), split.metas);
        assertEquals(5, split.baseLevel);
        assertEquals(10, split.baseChance);
        assertEquals("Magic Stone", split.nbtTags.getString("CustomName"));
        assertEquals("minecraft:cobblestone", split.dropItem);
        assertNotSame(original.nbtTags, split.nbtTags);
    }

    @Test
    public void editEntryWithOutOfRangeIndexIsSafe() {
        ContainerSetsConfig container = newContainer();
        container.editEntry(99, ContainerSetsConfig.EntryType.BLOCK);
        assertTrue(container.getEditingEntryNbt().getAllKeys().isEmpty());
        assertTrue(container.getNbtTags().isEmpty());
        container.nbtEditorStartAdd();
        assertFalse(container.nbtEditorApply("K", "v"));
    }

    @Test
    public void nbtEditorApplyAddsEmptyByteArrayAsContainer() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_BYTE_ARRAY);

        assertTrue(container.nbtEditorApply("B", ""));
        CompoundTag nbt = container.getEditingEntryNbt();
        assertTrue(nbt.get("B") instanceof ByteArrayTag);
        assertEquals(0, ((ByteArrayTag) nbt.get("B")).getAsByteArray().length);
    }

    @Test
    public void nbtEditorApplyAddsEmptyIntArrayAsContainer() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_INT_ARRAY);

        assertTrue(container.nbtEditorApply("I", ""));
        CompoundTag nbt = container.getEditingEntryNbt();
        assertTrue(nbt.get("I") instanceof IntArrayTag);
        assertEquals(0, ((IntArrayTag) nbt.get("I")).getAsIntArray().length);
    }

    @Test
    public void nbtEditorStartEditRejectsContainerArray() {
        ContainerSetsConfig container = newBlockEntry();
        ByteArrayTag arr = new ByteArrayTag(new byte[] { 1 });
        container.getEditingSet().blocks.get(0).nbtTags.put("B", arr);

        container.nbtEditorStartEdit("B");
        assertFalse(container.nbtEditorIsEditing());
    }

    @Test
    public void nbtEditorByteArrayContainerAppendSetEditRemove() {
        ContainerSetsConfig container = newBlockEntry();
        ByteArrayTag arr = new ByteArrayTag(new byte[] { 1, 2 });
        container.getEditingSet().blocks.get(0).nbtTags.put("B", arr);

        container.nbtEditorPush("B");
        assertTrue(container.nbtEditorIsArrayContext());
        assertEquals(TAG_BYTE, container.nbtEditorGetArrayElementType());
        assertEquals(2, container.getNbtTags().size());

        assertTrue(container.nbtEditorApply("", "3"));
        ByteArrayTag afterAppend = (ByteArrayTag) container.getEditingEntryNbt().get("B");
        assertEquals(3, afterAppend.getAsByteArray().length);
        assertEquals(3, afterAppend.getAsByteArray()[2]);

        container.nbtEditorStartEditIndex(0);
        assertTrue(container.nbtEditorIsEditing());
        assertEquals("1", container.nbtEditorGetValue());
        assertTrue(container.nbtEditorApply("", "9"));
        ByteArrayTag afterSet = (ByteArrayTag) container.getEditingEntryNbt().get("B");
        assertEquals(9, afterSet.getAsByteArray()[0]);
        assertEquals(3, afterSet.getAsByteArray().length);

        assertTrue(container.nbtEditorRemoveIndex(0));
        ByteArrayTag afterRemove = (ByteArrayTag) container.getEditingEntryNbt().get("B");
        assertEquals(2, afterRemove.getAsByteArray().length);
        assertEquals(2, afterRemove.getAsByteArray()[0]);
        assertEquals(3, afterRemove.getAsByteArray()[1]);
        assertFalse(container.nbtEditorRemoveIndex(9));
    }

    @Test
    public void nbtEditorIntArrayElementTypeIsInt() {
        ContainerSetsConfig container = newBlockEntry();
        IntArrayTag arr = new IntArrayTag(new int[] { 5 });
        container.getEditingSet().blocks.get(0).nbtTags.put("I", arr);

        container.nbtEditorPush("I");
        assertTrue(container.nbtEditorIsArrayContext());
        assertEquals(TAG_INT, container.nbtEditorGetArrayElementType());

        assertTrue(container.nbtEditorApply("", "7"));
        IntArrayTag after = (IntArrayTag) container.getEditingEntryNbt().get("I");
        assertEquals(2, after.getAsIntArray().length);
        assertEquals(7, after.getAsIntArray()[1]);
    }

    @Test
    public void nbtEditorPushIndexEntersArrayInsideList() {
        ContainerSetsConfig container = newBlockEntry();
        ListTag list = new ListTag();
        list.add(new IntArrayTag(new int[] { 1, 2 }));
        list.add(new IntArrayTag(new int[] { 3, 4 }));
        container.getEditingSet().blocks.get(0).nbtTags.put("L", list);

        container.nbtEditorPush("L");
        container.nbtEditorPushIndex(1);
        assertTrue(container.nbtEditorIsArrayContext());
        assertEquals(TAG_INT, container.nbtEditorGetArrayElementType());
        assertEquals(2, container.getNbtTags().size());
    }

    @Test
    public void addEntryToCurrentSetCopiesNbtFromSearchResult() {
        ContainerSetsConfig container = newBlockEntry();
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag tag = new CompoundTag();
        tag.putString("CustomColor", "blue");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ContainerSetsConfig.SearchResult result = new ContainerSetsConfig.SearchResult("minecraft:stone", "Stone", "minecraft", stack);

        container.addEntryToCurrentSet(ContainerSetsConfig.EntryType.BLOCK, result, 1, 50);

        assertEquals(2, container.getEditingSet().blocks.size());
        assertEquals("blue", container.getEditingSet().blocks.get(1).nbtTags.getString("CustomColor"));
    }

    @Test
    public void addEntryToCurrentSetWithoutStackNbtStoresEmptyTag() {
        ContainerSetsConfig container = newBlockEntry();
        ItemStack stack = new ItemStack(Items.STONE);
        ContainerSetsConfig.SearchResult result = new ContainerSetsConfig.SearchResult("minecraft:stone", "Stone", "minecraft", stack);

        container.addEntryToCurrentSet(ContainerSetsConfig.EntryType.BLOCK, result, 1, 50);

        assertEquals(2, container.getEditingSet().blocks.size());
        assertNotNull(container.getEditingSet().blocks.get(1).nbtTags);
        assertTrue(container.getEditingSet().blocks.get(1).nbtTags.isEmpty());
    }

    @Test
    public void getItemStackFromEntryDoesNotBakeNbtTags() {
        ContainerSetsConfig container = newBlockEntry();
        BlockSetConfig.BlockElementDefinition entry = container.getEditingSet().blocks.get(0);
        entry.nbtTags.putString("CustomColor", "blue");

        ItemStack stack = container.getItemStackFromEntry(entry, 0);

        assertFalse(stack.isEmpty());
        assertEquals(Items.STONE, stack.getItem());
        assertFalse("nbt is applied at block placement, not baked into the preview stack",
                stack.get(DataComponents.CUSTOM_DATA) != null);
        assertEquals("blue", entry.nbtTags.getString("CustomColor"));
    }

    @Test
    public void getExistingBlockKeysExcludesNbtTaggedBlocks() {
        ContainerSetsConfig container = newContainer();
        Set<String> keys = container.getExistingBlockKeys();
        assertFalse(keys.contains("minecraft:stone@0"));
        assertFalse(keys.contains("minecraft:stone@1"));
    }

    @Test
    public void getExistingBlockKeysIncludesPlainAddedBlocks() {
        ContainerSetsConfig container = newContainer();
        ItemStack stack = new ItemStack(Items.STONE);
        container.addEntryToCurrentSet(ContainerSetsConfig.EntryType.BLOCK,
                new ContainerSetsConfig.SearchResult("minecraft:stone", "Stone", "minecraft", stack), 1, 50);

        Set<String> keys = container.getExistingBlockKeys();
        assertTrue(keys.contains("minecraft:stone@0"));
    }

    @Test
    public void getExistingBlockKeysCoversAllMetaValues() {
        ContainerSetsConfig container = newContainer();
        BlockSetConfig.BlockElementDefinition block = new BlockSetConfig.BlockElementDefinition();
        block.registry = "minecraft:wool";
        block.meta = 0;
        block.metas = new ArrayList<>(Arrays.asList(0, 1, 2));
        block.nbtTags = new CompoundTag();
        container.getEditingSet().blocks.add(block);

        Set<String> keys = container.getExistingBlockKeys();
        assertTrue(keys.contains("minecraft:wool@0"));
        assertTrue(keys.contains("minecraft:wool@1"));
        assertTrue(keys.contains("minecraft:wool@2"));
    }

    @Test
    public void addBrokenBlocksTotalRemovesItFromAvailableTypes() {
        ContainerSetsConfig container = newContainer();
        container.initUnlockConditionsEditor();
        container.addUnlockCondition("broken_blocks_total", "", "", "5");

        assertFalse(container.getAvailableUnlockConditionTypes().contains("broken_blocks_total"));
        assertNotEquals("broken_blocks_total", container.getNewConditionTypeToAdd());
    }

    @Test
    public void cycleUnlockConditionTypeSkipsAlreadyUsedBrokenBlocksTotal() {
        ContainerSetsConfig container = newContainer();
        container.initUnlockConditionsEditor();
        container.addUnlockCondition("broken_blocks_total", "", "", "5");

        for (int i = 0; i < 6; i++)
        {
            container.cycleUnlockConditionType();
            assertNotEquals("broken_blocks_total", container.getNewConditionTypeToAdd());
        }
    }

    @Test
    public void initUnlockConditionsEditorMovesOffUsedBrokenBlocksTotal() {
        ContainerSetsConfig container = newContainer();
        BlockSetConfig.UnlockConditionDefinition cond = new BlockSetConfig.UnlockConditionDefinition();
        cond.type = "broken_blocks_total";
        cond.count = 5;
        container.getEditingSet().unlockConditions.conditions.add(cond);

        container.initUnlockConditionsEditor();

        assertNotEquals("broken_blocks_total", container.getNewConditionTypeToAdd());
    }

    @Test
    public void saveCurrencyRejectsNegativeLevel() {
        ContainerSetsConfig container = newBlockEntry();
        BlockSetConfig.BlockElementDefinition entry = container.getEditingSet().blocks.get(0);

        assertFalse(container.saveCurrency(-1, 50));
        assertEquals(1, entry.baseLevel);
        assertEquals(50, entry.baseChance);
    }

    @Test
    public void saveCurrencyRejectsNegativeChance() {
        ContainerSetsConfig container = newBlockEntry();
        BlockSetConfig.BlockElementDefinition entry = container.getEditingSet().blocks.get(0);

        assertFalse(container.saveCurrency(5, -10));
        assertEquals(1, entry.baseLevel);
        assertEquals(50, entry.baseChance);
    }

    @Test
    public void saveCurrencyRejectsZeroLevel() {
        ContainerSetsConfig container = newBlockEntry();
        BlockSetConfig.BlockElementDefinition entry = container.getEditingSet().blocks.get(0);

        assertFalse(container.saveCurrency(0, 50));
        assertEquals(1, entry.baseLevel);
    }

    @Test
    public void addUnlockConditionClampsNegativeLevelAndCount() {
        ContainerSetsConfig container = newContainer();
        container.initUnlockConditionsEditor();
        container.addUnlockCondition("broken_blocks", "test_nbt", "-3", "-7");

        BlockSetConfig.UnlockConditionDefinition cond = container.getUnlockConditionsEditorConditions().get(0);
        assertEquals(1, cond.level);
        assertEquals(0, cond.count);
    }
}
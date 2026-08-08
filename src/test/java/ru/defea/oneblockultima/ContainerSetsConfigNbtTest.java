package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static net.minecraftforge.common.util.Constants.NBT.*;
import static org.junit.Assert.*;

public class ContainerSetsConfigNbtTest {

    private List<BlockSetConfig.BlockSetDefinition> originalSets;

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
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
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("CustomName", "Magic Stone");
        block.nbtTags = nbt;
        set.blocks.add(block);

        BlockSetConfig.MobElementDefinition mob = new BlockSetConfig.MobElementDefinition();
        mob.registry = "minecraft:zombie";
        mob.baseLevel = 2;
        mob.baseChance = 5;
        NBTTagCompound mobNbt = new NBTTagCompound();
        mobNbt.setString("CustomName", "Zombie King");
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

        NBTTagCompound snapshot = container.getEditingEntryNbt();
        assertEquals("Magic Stone", snapshot.getString("CustomName"));
        snapshot.setString("CustomName", "Mutated");
        snapshot.setInteger("Purity", 9);

        NBTTagCompound actual = container.getEditingEntryNbt();
        assertEquals("Magic Stone", actual.getString("CustomName"));
        assertFalse(actual.hasKey("Purity"));
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
        NBTTagCompound info = new NBTTagCompound();
        info.setString("Name", "Secret");
        container.getEditingSet().blocks.get(0).nbtTags.setTag("Info", info);

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
        NBTTagList list = new NBTTagList();
        NBTTagCompound e0 = new NBTTagCompound();
        e0.setInteger("Level", 1);
        NBTTagCompound e1 = new NBTTagCompound();
        e1.setInteger("Level", 2);
        list.appendTag(e0);
        list.appendTag(e1);
        container.getEditingSet().blocks.get(0).nbtTags.setTag("L", list);

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
        assertEquals(7, container.getEditingEntryNbt().getInteger("Count"));
    }

    @Test
    public void nbtEditorApplyAddsEmptyCompoundThenFillsIt() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_COMPOUND);

        assertTrue(container.nbtEditorApply("Inner", ""));
        NBTBase inner = container.getEditingEntryNbt().getTag("Inner");
        assertTrue(inner instanceof NBTTagCompound);
        assertTrue(((NBTTagCompound) inner).getKeySet().isEmpty());

        container.nbtEditorPush("Inner");
        container.nbtEditorStartAdd();
        assertTrue(container.nbtEditorApply("X", "1"));
        assertEquals("1", container.getEditingEntryNbt().getCompoundTag("Inner").getString("X"));
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
        assertFalse(container.getEditingEntryNbt().hasKey("Temp"));
        assertFalse(container.nbtEditorRemove("Missing"));
    }

    @Test
    public void nbtEditorListAppendAndSet() {
        ContainerSetsConfig container = newBlockEntry();
        NBTTagList list = new NBTTagList();
        list.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 1));
        list.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 2));
        container.getEditingSet().blocks.get(0).nbtTags.setTag("L", list);

        container.nbtEditorPush("L");
        assertTrue(container.nbtEditorListTypeIsFixed());
        assertEquals(TAG_BYTE, container.getNbtEditorListElementType());

        assertTrue(container.nbtEditorApply("", "3"));
        NBTTagList afterAppend = (NBTTagList) container.getEditingEntryNbt().getTag("L");
        assertEquals(3, afterAppend.tagCount());
        assertEquals(3, ((net.minecraft.nbt.NBTTagByte) afterAppend.get(2)).getByte());

        container.nbtEditorStartEditIndex(0);
        assertTrue(container.nbtEditorIsEditingListElement());
        assertEquals("1", container.nbtEditorGetValue());
        assertTrue(container.nbtEditorApply("", "9"));
        NBTTagList afterSet = (NBTTagList) container.getEditingEntryNbt().getTag("L");
        assertEquals(9, ((net.minecraft.nbt.NBTTagByte) afterSet.get(0)).getByte());
        assertEquals(3, afterSet.tagCount());
    }

    @Test
    public void nbtEditorRemoveIndexRemovesListElement() {
        ContainerSetsConfig container = newBlockEntry();
        NBTTagList list = new NBTTagList();
        list.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 1));
        list.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 2));
        container.getEditingSet().blocks.get(0).nbtTags.setTag("L", list);

        container.nbtEditorPush("L");
        assertTrue(container.nbtEditorRemoveIndex(0));
        NBTTagList after = (NBTTagList) container.getEditingEntryNbt().getTag("L");
        assertEquals(1, after.tagCount());
        assertEquals(2, ((net.minecraft.nbt.NBTTagByte) after.get(0)).getByte());
        assertFalse(container.nbtEditorRemoveIndex(5));
    }

    @Test
    public void nbtEditorAppendToEmptyListCreatesFreshNode() {
        ContainerSetsConfig container = newBlockEntry();
        NBTTagList list = new NBTTagList();
        container.getEditingSet().blocks.get(0).nbtTags.setTag("L", list);

        container.nbtEditorPush("L");
        assertFalse(container.nbtEditorListTypeIsFixed());
        assertTrue(container.nbtEditorApply("", "5"));
        NBTTagList after = (NBTTagList) container.getEditingEntryNbt().getTag("L");
        assertEquals(1, after.tagCount());
        assertEquals(5, ((net.minecraft.nbt.NBTTagByte) after.get(0)).getByte());
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
        NBTTagCompound nbt = container.getEditingEntryNbt();
        assertFalse(nbt.hasKey("CustomName"));
        assertEquals("X", nbt.getString("Renamed"));
        assertEquals("", container.nbtEditorGetValueText());
        assertEquals("", container.nbtEditorGetKeyText());
    }

    @Test
    public void nbtEditorApplyEditRejectsEmptyKey() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartEdit("CustomName");

        assertFalse(container.nbtEditorApply("   ", "X"));
        NBTTagCompound nbt = container.getEditingEntryNbt();
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
        NBTTagCompound c = new NBTTagCompound();
        c.setString("Long", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

        String preview = ContainerSetsConfig.nbtValuePreview(c.getTag("Long"));
        assertEquals(24, preview.length());
        assertTrue(preview.endsWith("..."));
    }

    @Test
    public void nbtValuePreviewShowsCountsForContainers() {
        NBTTagCompound c = new NBTTagCompound();
        c.setString("A", "1");
        c.setString("B", "2");

        assertEquals("{2}", ContainerSetsConfig.nbtValuePreview(c));

        NBTTagList l = new NBTTagList();
        l.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 1));
        l.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 2));
        l.appendTag(new net.minecraft.nbt.NBTTagByte((byte) 3));
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
        assertTrue(container.getEditingEntryNbt().getKeySet().isEmpty());
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
        NBTTagCompound nbt = container.getEditingEntryNbt();
        assertTrue(nbt.getTag("B") instanceof net.minecraft.nbt.NBTTagByteArray);
        assertEquals(0, ((net.minecraft.nbt.NBTTagByteArray) nbt.getTag("B")).getByteArray().length);
    }

    @Test
    public void nbtEditorApplyAddsEmptyIntArrayAsContainer() {
        ContainerSetsConfig container = newBlockEntry();
        container.nbtEditorStartAdd();
        cycleAddType(container, TAG_INT_ARRAY);

        assertTrue(container.nbtEditorApply("I", ""));
        NBTTagCompound nbt = container.getEditingEntryNbt();
        assertTrue(nbt.getTag("I") instanceof net.minecraft.nbt.NBTTagIntArray);
        assertEquals(0, ((net.minecraft.nbt.NBTTagIntArray) nbt.getTag("I")).getIntArray().length);
    }

    @Test
    public void nbtEditorStartEditRejectsContainerArray() {
        ContainerSetsConfig container = newBlockEntry();
        net.minecraft.nbt.NBTTagByteArray arr = new net.minecraft.nbt.NBTTagByteArray(new byte[] { 1 });
        container.getEditingSet().blocks.get(0).nbtTags.setTag("B", arr);

        container.nbtEditorStartEdit("B");
        assertFalse(container.nbtEditorIsEditing());
    }

    @Test
    public void nbtEditorByteArrayContainerAppendSetEditRemove() {
        ContainerSetsConfig container = newBlockEntry();
        net.minecraft.nbt.NBTTagByteArray arr = new net.minecraft.nbt.NBTTagByteArray(new byte[] { 1, 2 });
        container.getEditingSet().blocks.get(0).nbtTags.setTag("B", arr);

        container.nbtEditorPush("B");
        assertTrue(container.nbtEditorIsArrayContext());
        assertEquals(TAG_BYTE, container.nbtEditorGetArrayElementType());
        assertEquals(2, container.getNbtTags().size());

        assertTrue(container.nbtEditorApply("", "3"));
        net.minecraft.nbt.NBTTagByteArray afterAppend = (net.minecraft.nbt.NBTTagByteArray) container.getEditingEntryNbt().getTag("B");
        assertEquals(3, afterAppend.getByteArray().length);
        assertEquals(3, afterAppend.getByteArray()[2]);

        container.nbtEditorStartEditIndex(0);
        assertTrue(container.nbtEditorIsEditing());
        assertEquals("1", container.nbtEditorGetValue());
        assertTrue(container.nbtEditorApply("", "9"));
        net.minecraft.nbt.NBTTagByteArray afterSet = (net.minecraft.nbt.NBTTagByteArray) container.getEditingEntryNbt().getTag("B");
        assertEquals(9, afterSet.getByteArray()[0]);
        assertEquals(3, afterSet.getByteArray().length);

        assertTrue(container.nbtEditorRemoveIndex(0));
        net.minecraft.nbt.NBTTagByteArray afterRemove = (net.minecraft.nbt.NBTTagByteArray) container.getEditingEntryNbt().getTag("B");
        assertEquals(2, afterRemove.getByteArray().length);
        assertEquals(2, afterRemove.getByteArray()[0]);
        assertEquals(3, afterRemove.getByteArray()[1]);
        assertFalse(container.nbtEditorRemoveIndex(9));
    }

    @Test
    public void nbtEditorIntArrayElementTypeIsInt() {
        ContainerSetsConfig container = newBlockEntry();
        net.minecraft.nbt.NBTTagIntArray arr = new net.minecraft.nbt.NBTTagIntArray(new int[] { 5 });
        container.getEditingSet().blocks.get(0).nbtTags.setTag("I", arr);

        container.nbtEditorPush("I");
        assertTrue(container.nbtEditorIsArrayContext());
        assertEquals(TAG_INT, container.nbtEditorGetArrayElementType());

        assertTrue(container.nbtEditorApply("", "7"));
        net.minecraft.nbt.NBTTagIntArray after = (net.minecraft.nbt.NBTTagIntArray) container.getEditingEntryNbt().getTag("I");
        assertEquals(2, after.getIntArray().length);
        assertEquals(7, after.getIntArray()[1]);
    }

    @Test
    public void nbtEditorPushIndexEntersArrayInsideList() {
        ContainerSetsConfig container = newBlockEntry();
        NBTTagList list = new NBTTagList();
        list.appendTag(new net.minecraft.nbt.NBTTagIntArray(new int[] { 1, 2 }));
        list.appendTag(new net.minecraft.nbt.NBTTagIntArray(new int[] { 3, 4 }));
        container.getEditingSet().blocks.get(0).nbtTags.setTag("L", list);

        container.nbtEditorPush("L");
        container.nbtEditorPushIndex(1);
        assertTrue(container.nbtEditorIsArrayContext());
        assertEquals(TAG_INT, container.nbtEditorGetArrayElementType());
        assertEquals(2, container.getNbtTags().size());
    }

    @Test
    public void addEntryToCurrentSetCopiesNbtFromSearchResult() {
        ContainerSetsConfig container = newBlockEntry();
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.STONE);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setString("CustomColor", "blue");
        stack.setTagCompound(tag);
        ContainerSetsConfig.SearchResult result = new ContainerSetsConfig.SearchResult("minecraft:stone", "Stone", "minecraft", stack);

        container.addEntryToCurrentSet(ContainerSetsConfig.EntryType.BLOCK, result, 1, 50);

        assertEquals(2, container.getEditingSet().blocks.size());
        assertEquals("blue", container.getEditingSet().blocks.get(1).nbtTags.getString("CustomColor"));
    }

    @Test
    public void addEntryToCurrentSetWithoutStackNbtStoresEmptyTag() {
        ContainerSetsConfig container = newBlockEntry();
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.STONE);
        ContainerSetsConfig.SearchResult result = new ContainerSetsConfig.SearchResult("minecraft:stone", "Stone", "minecraft", stack);

        container.addEntryToCurrentSet(ContainerSetsConfig.EntryType.BLOCK, result, 1, 50);

        assertEquals(2, container.getEditingSet().blocks.size());
        assertNotNull(container.getEditingSet().blocks.get(1).nbtTags);
        assertTrue(container.getEditingSet().blocks.get(1).nbtTags.hasNoTags());
    }

    @Test
    public void getItemStackFromEntryAppliesNbt() {
        ContainerSetsConfig container = newBlockEntry();
        BlockSetConfig.BlockElementDefinition entry = container.getEditingSet().blocks.get(0);
        entry.nbtTags.setString("CustomColor", "blue");

        net.minecraft.item.ItemStack stack = container.getItemStackFromEntry(entry, 0);

        assertFalse(stack.isEmpty());
        assertTrue(stack.hasTagCompound());
        assert stack.getTagCompound() != null;
        assertEquals("blue", stack.getTagCompound().getString("CustomColor"));
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
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.STONE);
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
        block.nbtTags = new NBTTagCompound();
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

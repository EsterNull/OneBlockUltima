package ru.defea.oneblockultima;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.testutil.TestBootstrap;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry.GeneratedBlockEntry;

import static org.junit.Assert.*;

public class GeneratedBlockRegistryTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private GeneratedBlockRegistry newRegistry() {
        return new GeneratedBlockRegistry();
    }

    @Test
    public void freshRegistryIsEmpty() {
        GeneratedBlockRegistry reg = newRegistry();
        assertFalse(reg.isGenerated(BlockPos.ZERO));
        assertNull(reg.getEntry(BlockPos.ZERO));
    }

    @Test
    public void markGeneratedMakesBlockTracked() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(1, 64, 2);
        BlockPos genPos = new BlockPos(1, 63, 2);
        reg.markGenerated(pos, genPos, "classic", 10, 1, "minecraft:stone", 0);

        assertTrue(reg.isGenerated(pos));
    }

    @Test
    public void getEntryReturnsCorrectData() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(5, 70, 5);
        BlockPos genPos = new BlockPos(5, 69, 5);
        reg.markGenerated(pos, genPos, "nether", 25, 3, "minecraft:netherrack", 0);

        GeneratedBlockEntry entry = reg.getEntry(pos);
        assertNotNull(entry);
        assertEquals(genPos, entry.generatorPos);
        assertEquals("nether", entry.setId);
        assertEquals(25, entry.currency);
        assertEquals(3, entry.level);
        assertEquals("minecraft:netherrack", entry.blockRegistry);
        assertEquals(0, entry.blockMeta);
    }

    @Test
    public void getGeneratorPosReturnsCorrectPos() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(10, 64, 10);
        BlockPos genPos = new BlockPos(10, 63, 10);
        reg.markGenerated(pos, genPos, "classic", 5, 1, "minecraft:dirt", 0);

        assertEquals(genPos, reg.getGeneratorPos(pos));
    }

    @Test
    public void getGeneratorPosReturnsNullForUnknown() {
        GeneratedBlockRegistry reg = newRegistry();
        assertNull(reg.getGeneratorPos(new BlockPos(99, 99, 99)));
    }

    @Test
    public void removeUntracksBlock() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(0, 64, 0);
        reg.markGenerated(pos, BlockPos.ZERO, "classic", 10, 1, "minecraft:stone", 0);
        assertTrue(reg.isGenerated(pos));

        reg.remove(pos);
        assertFalse(reg.isGenerated(pos));
        assertNull(reg.getEntry(pos));
    }

    @Test
    public void removeUnknownPosIsNoOp() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.remove(new BlockPos(99, 99, 99));
        assertFalse(reg.isGenerated(new BlockPos(99, 99, 99)));
    }

    @Test
    public void multipleEntriesAreIndependent() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos1 = new BlockPos(1, 64, 1);
        BlockPos pos2 = new BlockPos(2, 64, 2);
        BlockPos genPos = BlockPos.ZERO;

        reg.markGenerated(pos1, genPos, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(pos2, genPos, "nether", 20, 2, "minecraft:netherrack", 0);

        assertEquals("classic", reg.getEntry(pos1).setId);
        assertEquals("nether", reg.getEntry(pos2).setId);

        reg.remove(pos1);
        assertFalse(reg.isGenerated(pos1));
        assertTrue(reg.isGenerated(pos2));
    }

    @Test
    public void markGeneratedOverwritesExistingEntry() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(1, 64, 1);
        BlockPos genPos = BlockPos.ZERO;

        reg.markGenerated(pos, genPos, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(pos, genPos, "nether", 30, 5, "minecraft:diamond_block", 0);

        GeneratedBlockEntry entry = reg.getEntry(pos);
        assertEquals("nether", entry.setId);
        assertEquals(30, entry.currency);
        assertEquals(5, entry.level);
        assertEquals("minecraft:diamond_block", entry.blockRegistry);
    }

    @Test
    public void nbtRoundtripPreservesAllEntries() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos genPos = BlockPos.ZERO;
        BlockPos pos1 = new BlockPos(1, 64, 1);
        BlockPos pos2 = new BlockPos(2, 65, 2);
        BlockPos pos3 = new BlockPos(3, 66, 3);
        reg.markGenerated(pos1, genPos, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(pos2, genPos, "nether", 25, 3, "minecraft:netherrack", 2);
        reg.markGenerated(pos3, genPos, "end", 0, 1, null, 0);

        CompoundTag nbt = reg.save(new CompoundTag(), RegistryAccess.EMPTY);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.read(nbt);

        assertTrue(loaded.isGenerated(pos1));
        assertTrue(loaded.isGenerated(pos2));
        assertTrue(loaded.isGenerated(pos3));

        GeneratedBlockEntry e1 = loaded.getEntry(pos1);
        assertNotNull(e1);
        assertEquals("classic", e1.setId);
        assertEquals(10, e1.currency);
        assertEquals(1, e1.level);
        assertEquals("minecraft:stone", e1.blockRegistry);
        assertEquals(0, e1.blockMeta);

        GeneratedBlockEntry e2 = loaded.getEntry(pos2);
        assertNotNull(e2);
        assertEquals("nether", e2.setId);
        assertEquals(25, e2.currency);
        assertEquals(3, e2.level);
        assertEquals(2, e2.blockMeta);

        GeneratedBlockEntry e3 = loaded.getEntry(pos3);
        assertNotNull(e3);
        assertEquals("end", e3.setId);
        assertNull(e3.blockRegistry);
    }

    @Test
    public void nbtRoundtripWithEmptyRegistry() {
        GeneratedBlockRegistry reg = newRegistry();
        CompoundTag nbt = reg.save(new CompoundTag(), RegistryAccess.EMPTY);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.read(nbt);

        assertFalse(loaded.isGenerated(BlockPos.ZERO));
    }

    @Test
    public void readFromNbtClearsExistingEntries() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(1, 64, 1);
        reg.markGenerated(pos, BlockPos.ZERO, "classic", 10, 1, "minecraft:stone", 0);
        assertTrue(reg.isGenerated(pos));

        CompoundTag nbt = new CompoundTag();
        reg.read(nbt);

        assertFalse(reg.isGenerated(pos));
    }

    @Test
    public void entryDefaultConstructorSetsDefaults() {
        GeneratedBlockEntry entry = new GeneratedBlockEntry(BlockPos.ZERO, "classic", 10, 1);
        assertEquals(BlockPos.ZERO, entry.generatorPos);
        assertEquals("classic", entry.setId);
        assertEquals(10, entry.currency);
        assertEquals(1, entry.level);
        assertNull(entry.blockRegistry);
        assertEquals(0, entry.blockMeta);
    }

    @Test
    public void entryFullConstructorSetsAllFields() {
        GeneratedBlockEntry entry = new GeneratedBlockEntry(
                new BlockPos(5, 63, 5), "nether", 50, 7, "minecraft:obsidian", 3
        );
        assertEquals(new BlockPos(5, 63, 5), entry.generatorPos);
        assertEquals("nether", entry.setId);
        assertEquals(50, entry.currency);
        assertEquals(7, entry.level);
        assertEquals("minecraft:obsidian", entry.blockRegistry);
        assertEquals(3, entry.blockMeta);
    }

    @Test
    public void nbtRoundtripPreservesGeneratorPos() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos genPos = new BlockPos(10, 63, 10);
        BlockPos blockPos = new BlockPos(10, 64, 10);
        reg.markGenerated(blockPos, genPos, "classic", 5, 1, "minecraft:stone", 0);

        CompoundTag nbt = reg.save(new CompoundTag(), RegistryAccess.EMPTY);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.read(nbt);

        assertEquals(genPos, loaded.getGeneratorPos(blockPos));
    }

    // --- Self-referencing generatorPos (player re-placed blocks) ---

    @Test
    public void selfReferencingEntryIsGenerated() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(0, 65, 0);
        reg.markGenerated(pos, pos, "", 0, 0, "minecraft:dirt", 0);

        assertTrue(reg.isGenerated(pos));
    }

    @Test
    public void selfReferencingEntryGetGeneratorPosReturnsSelf() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(0, 65, 0);
        reg.markGenerated(pos, pos, "", 0, 0, "minecraft:dirt", 0);

        assertEquals(pos, reg.getGeneratorPos(pos));
    }

    @Test
    public void selfReferencingEntryHasEmptySetId() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(0, 65, 0);
        reg.markGenerated(pos, pos, "", 0, 0, "minecraft:dirt", 0);

        GeneratedBlockEntry entry = reg.getEntry(pos);
        assertNotNull(entry);
        assertEquals("", entry.setId);
        assertEquals(0, entry.currency);
        assertEquals(0, entry.level);
        assertEquals("minecraft:dirt", entry.blockRegistry);
        assertEquals(0, entry.blockMeta);
    }

    @Test
    public void selfReferencingEntryDistinguishesFromDirectGeneration() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos genPos = new BlockPos(0, 64, 0);
        BlockPos targetPos = new BlockPos(0, 65, 0);

        // Direct generation: generatorPos points to the generator
        reg.markGenerated(targetPos, genPos, "classic", 10, 1, "minecraft:stone", 0);
        GeneratedBlockEntry direct = reg.getEntry(targetPos);
        assertFalse("Direct generation: generatorPos != pos",
                direct.generatorPos.equals(targetPos));
        assertEquals(genPos, direct.generatorPos);

        // Re-placed by player: generatorPos == pos (self-reference)
        reg.markGenerated(targetPos, targetPos, "", 0, 0, "minecraft:stone", 0);
        GeneratedBlockEntry replaced = reg.getEntry(targetPos);
        assertTrue("Re-placed: generatorPos == pos",
                replaced.generatorPos.equals(targetPos));
    }

    @Test
    public void selfReferencingEntryNbtRoundtrip() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(3, 65, 7);
        reg.markGenerated(pos, pos, "", 0, 0, "minecraft:gold_ore", 0);

        CompoundTag nbt = reg.save(new CompoundTag(), RegistryAccess.EMPTY);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.read(nbt);

        assertTrue(loaded.isGenerated(pos));
        GeneratedBlockEntry entry = loaded.getEntry(pos);
        assertNotNull(entry);
        assertEquals(pos, entry.generatorPos);
        assertEquals("", entry.setId);
        assertEquals("minecraft:gold_ore", entry.blockRegistry);
    }

    @Test
    public void removeSelfReferencingEntryWorks() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos = new BlockPos(0, 65, 0);
        reg.markGenerated(pos, pos, "", 0, 0, "minecraft:dirt", 0);
        assertTrue(reg.isGenerated(pos));

        reg.remove(pos);
        assertFalse(reg.isGenerated(pos));
        assertNull(reg.getEntry(pos));
    }

    @Test
    public void selfReferencingAndDirectEntriesAreIndependent() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos genPos = new BlockPos(0, 64, 0);
        BlockPos posA = new BlockPos(0, 65, 0);
        BlockPos posB = new BlockPos(1, 65, 1);

        reg.markGenerated(posA, genPos, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(posB, posB, "", 0, 0, "minecraft:dirt", 0);

        assertEquals(genPos, reg.getGeneratorPos(posA));
        assertEquals(posB, reg.getGeneratorPos(posB));

        reg.remove(posA);
        assertFalse(reg.isGenerated(posA));
        assertTrue(reg.isGenerated(posB));
    }

    @Test
    public void multipleSelfReferencingEntries() {
        GeneratedBlockRegistry reg = newRegistry();
        BlockPos pos1 = new BlockPos(0, 65, 0);
        BlockPos pos2 = new BlockPos(1, 66, 1);
        BlockPos pos3 = new BlockPos(2, 67, 2);

        reg.markGenerated(pos1, pos1, "", 0, 0, "minecraft:dirt", 0);
        reg.markGenerated(pos2, pos2, "", 0, 0, "minecraft:cobblestone", 0);
        reg.markGenerated(pos3, pos3, "", 0, 0, "minecraft:stone", 0);

        assertTrue(reg.isGenerated(pos1));
        assertTrue(reg.isGenerated(pos2));
        assertTrue(reg.isGenerated(pos3));

        assertEquals(pos1, reg.getGeneratorPos(pos1));
        assertEquals(pos2, reg.getGeneratorPos(pos2));
        assertEquals(pos3, reg.getGeneratorPos(pos3));

        assertEquals("minecraft:dirt", reg.getEntry(pos1).blockRegistry);
        assertEquals("minecraft:cobblestone", reg.getEntry(pos2).blockRegistry);
        assertEquals("minecraft:stone", reg.getEntry(pos3).blockRegistry);
    }

    @Test
    public void flushPendingDirtyIsSafe() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(new BlockPos(1, 64, 1), BlockPos.ZERO, "classic", 10, 1, "minecraft:stone", 0);
        reg.flushPendingDirty();
        assertTrue(reg.isGenerated(new BlockPos(1, 64, 1)));
    }

    @Test
    public void saveFillsEntriesList() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(new BlockPos(1, 64, 1), BlockPos.ZERO, "classic", 10, 1, "minecraft:stone", 0);
        CompoundTag nbt = reg.save(new CompoundTag(), RegistryAccess.EMPTY);
        assertEquals(1, nbt.getList("entries", 10).size());
    }
}
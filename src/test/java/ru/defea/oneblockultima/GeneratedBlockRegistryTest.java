package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry.GeneratedBlockEntry;

import static org.junit.Assert.*;

public class GeneratedBlockRegistryTest {

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    private GeneratedBlockRegistry newRegistry() {
        return new GeneratedBlockRegistry();
    }

    @Test
    public void freshRegistryIsEmpty() {
        GeneratedBlockRegistry reg = newRegistry();
        assertFalse(reg.isGenerated(0, 0, 0));
        assertNull(reg.getEntry(0, 0, 0));
    }

    @Test
    public void markGeneratedMakesBlockTracked() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(1, 64, 2, 1, 63, 2, "classic", 10, 1, "minecraft:stone", 0);

        assertTrue(reg.isGenerated(1, 64, 2));
    }

    @Test
    public void getEntryReturnsCorrectData() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(5, 70, 5, 5, 69, 5, "nether", 25, 3, "minecraft:netherrack", 0);

        GeneratedBlockEntry entry = reg.getEntry(5, 70, 5);
        assertNotNull(entry);
        assertEquals(5, entry.generatorX);
        assertEquals(69, entry.generatorY);
        assertEquals(5, entry.generatorZ);
        assertEquals("nether", entry.setId);
        assertEquals(25, entry.currency);
        assertEquals(3, entry.level);
        assertEquals("minecraft:netherrack", entry.blockRegistry);
        assertEquals(0, entry.blockMeta);
    }

    @Test
    public void getGeneratorPosReturnsCorrectPos() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(10, 64, 10, 10, 63, 10, "classic", 5, 1, "minecraft:dirt", 0);

        assertEquals(10, reg.getGeneratorX(10, 64, 10));
        assertEquals(63, reg.getGeneratorY(10, 64, 10));
        assertEquals(10, reg.getGeneratorZ(10, 64, 10));
    }

    @Test
    public void getGeneratorPosDefaultsToZeroForUnknown() {
        GeneratedBlockRegistry reg = newRegistry();
        assertFalse(reg.isGenerated(99, 99, 99));
        assertNull(reg.getEntry(99, 99, 99));
        assertEquals(0, reg.getGeneratorX(99, 99, 99));
        assertEquals(0, reg.getGeneratorY(99, 99, 99));
        assertEquals(0, reg.getGeneratorZ(99, 99, 99));
    }

    @Test
    public void removeUntracksBlock() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(0, 64, 0, 0, 0, 0, "classic", 10, 1, "minecraft:stone", 0);
        assertTrue(reg.isGenerated(0, 64, 0));

        reg.remove(0, 64, 0);
        assertFalse(reg.isGenerated(0, 64, 0));
        assertNull(reg.getEntry(0, 64, 0));
    }

    @Test
    public void removeUnknownPosIsNoOp() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.remove(99, 99, 99);
        assertFalse(reg.isGenerated(99, 99, 99));
    }

    @Test
    public void multipleEntriesAreIndependent() {
        GeneratedBlockRegistry reg = newRegistry();

        reg.markGenerated(1, 64, 1, 0, 0, 0, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(2, 64, 2, 0, 0, 0, "nether", 20, 2, "minecraft:netherrack", 0);

        assertEquals("classic", reg.getEntry(1, 64, 1).setId);
        assertEquals("nether", reg.getEntry(2, 64, 2).setId);

        reg.remove(1, 64, 1);
        assertFalse(reg.isGenerated(1, 64, 1));
        assertTrue(reg.isGenerated(2, 64, 2));
    }

    @Test
    public void markGeneratedOverwritesExistingEntry() {
        GeneratedBlockRegistry reg = newRegistry();

        reg.markGenerated(1, 64, 1, 0, 0, 0, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(1, 64, 1, 0, 0, 0, "nether", 30, 5, "minecraft:diamond_block", 0);

        GeneratedBlockEntry entry = reg.getEntry(1, 64, 1);
        assertEquals("nether", entry.setId);
        assertEquals(30, entry.currency);
        assertEquals(5, entry.level);
        assertEquals("minecraft:diamond_block", entry.blockRegistry);
    }

    @Test
    public void nbtRoundtripPreservesAllEntries() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(1, 64, 1, 0, 0, 0, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(2, 65, 2, 0, 0, 0, "nether", 25, 3, "minecraft:netherrack", 2);
        reg.markGenerated(3, 66, 3, 0, 0, 0, "end", 0, 1, null, 0);

        NBTTagCompound nbt = new NBTTagCompound();
        reg.writeToNBT(nbt);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.readFromNBT(nbt);

        assertTrue(loaded.isGenerated(1, 64, 1));
        assertTrue(loaded.isGenerated(2, 65, 2));
        assertTrue(loaded.isGenerated(3, 66, 3));

        GeneratedBlockEntry e1 = loaded.getEntry(1, 64, 1);
        assertNotNull(e1);
        assertEquals("classic", e1.setId);
        assertEquals(10, e1.currency);
        assertEquals(1, e1.level);
        assertEquals("minecraft:stone", e1.blockRegistry);
        assertEquals(0, e1.blockMeta);

        GeneratedBlockEntry e2 = loaded.getEntry(2, 65, 2);
        assertNotNull(e2);
        assertEquals("nether", e2.setId);
        assertEquals(25, e2.currency);
        assertEquals(3, e2.level);
        assertEquals(2, e2.blockMeta);

        GeneratedBlockEntry e3 = loaded.getEntry(3, 66, 3);
        assertNotNull(e3);
        assertEquals("end", e3.setId);
        assertNull(e3.blockRegistry);
    }

    @Test
    public void nbtRoundtripWithEmptyRegistry() {
        GeneratedBlockRegistry reg = newRegistry();
        NBTTagCompound nbt = new NBTTagCompound();
        reg.writeToNBT(nbt);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.readFromNBT(nbt);

        assertFalse(loaded.isGenerated(0, 0, 0));
    }

    @Test
    public void readFromNbtClearsExistingEntries() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(1, 64, 1, 0, 0, 0, "classic", 10, 1, "minecraft:stone", 0);
        assertTrue(reg.isGenerated(1, 64, 1));

        NBTTagCompound nbt = new NBTTagCompound();
        reg.readFromNBT(nbt);

        assertFalse(reg.isGenerated(1, 64, 1));
    }

    @Test
    public void entryDefaultConstructorSetsDefaults() {
        GeneratedBlockEntry entry = new GeneratedBlockEntry(0, 0, 0, "classic", 10, 1);
        assertEquals(0, entry.generatorX);
        assertEquals(0, entry.generatorY);
        assertEquals(0, entry.generatorZ);
        assertEquals("classic", entry.setId);
        assertEquals(10, entry.currency);
        assertEquals(1, entry.level);
        assertNull(entry.blockRegistry);
        assertEquals(0, entry.blockMeta);
    }

    @Test
    public void entryFullConstructorSetsAllFields() {
        GeneratedBlockEntry entry = new GeneratedBlockEntry(
                5, 63, 5, "nether", 50, 7, "minecraft:obsidian", 3
        );
        assertEquals(5, entry.generatorX);
        assertEquals(63, entry.generatorY);
        assertEquals(5, entry.generatorZ);
        assertEquals("nether", entry.setId);
        assertEquals(50, entry.currency);
        assertEquals(7, entry.level);
        assertEquals("minecraft:obsidian", entry.blockRegistry);
        assertEquals(3, entry.blockMeta);
    }

    @Test
    public void nbtRoundtripPreservesGeneratorPos() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(10, 64, 10, 10, 63, 10, "classic", 5, 1, "minecraft:stone", 0);

        NBTTagCompound nbt = new NBTTagCompound();
        reg.writeToNBT(nbt);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.readFromNBT(nbt);

        assertEquals(10, loaded.getGeneratorX(10, 64, 10));
        assertEquals(63, loaded.getGeneratorY(10, 64, 10));
        assertEquals(10, loaded.getGeneratorZ(10, 64, 10));
    }

    // --- Self-referencing generatorPos (player re-placed blocks) ---

    @Test
    public void selfReferencingEntryIsGenerated() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(0, 65, 0, 0, 65, 0, "", 0, 0, "minecraft:dirt", 0);

        assertTrue(reg.isGenerated(0, 65, 0));
    }

    @Test
    public void selfReferencingEntryGetGeneratorPosReturnsSelf() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(0, 65, 0, 0, 65, 0, "", 0, 0, "minecraft:dirt", 0);

        assertEquals(0, reg.getGeneratorX(0, 65, 0));
        assertEquals(65, reg.getGeneratorY(0, 65, 0));
        assertEquals(0, reg.getGeneratorZ(0, 65, 0));
    }

    @Test
    public void selfReferencingEntryHasEmptySetId() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(0, 65, 0, 0, 65, 0, "", 0, 0, "minecraft:dirt", 0);

        GeneratedBlockEntry entry = reg.getEntry(0, 65, 0);
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

        // Direct generation: generatorPos points to the generator
        reg.markGenerated(0, 65, 0, 0, 64, 0, "classic", 10, 1, "minecraft:stone", 0);
        GeneratedBlockEntry direct = reg.getEntry(0, 65, 0);
        assertFalse("Direct generation: generatorPos != pos",
                direct.generatorX == 0 && direct.generatorY == 65 && direct.generatorZ == 0);
        assertEquals(0, direct.generatorX);
        assertEquals(64, direct.generatorY);
        assertEquals(0, direct.generatorZ);

        // Re-placed by player: generatorPos == pos (self-reference)
        reg.markGenerated(0, 65, 0, 0, 65, 0, "", 0, 0, "minecraft:stone", 0);
        GeneratedBlockEntry replaced = reg.getEntry(0, 65, 0);
        assertTrue("Re-placed: generatorPos == pos",
                replaced.generatorX == 0 && replaced.generatorY == 65 && replaced.generatorZ == 0);
    }

    @Test
    public void selfReferencingEntryNbtRoundtrip() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(3, 65, 7, 3, 65, 7, "", 0, 0, "minecraft:gold_ore", 0);

        NBTTagCompound nbt = new NBTTagCompound();
        reg.writeToNBT(nbt);
        GeneratedBlockRegistry loaded = newRegistry();
        loaded.readFromNBT(nbt);

        assertTrue(loaded.isGenerated(3, 65, 7));
        GeneratedBlockEntry entry = loaded.getEntry(3, 65, 7);
        assertNotNull(entry);
        assertEquals(3, entry.generatorX);
        assertEquals(65, entry.generatorY);
        assertEquals(7, entry.generatorZ);
        assertEquals("", entry.setId);
        assertEquals("minecraft:gold_ore", entry.blockRegistry);
    }

    @Test
    public void removeSelfReferencingEntryWorks() {
        GeneratedBlockRegistry reg = newRegistry();
        reg.markGenerated(0, 65, 0, 0, 65, 0, "", 0, 0, "minecraft:dirt", 0);
        assertTrue(reg.isGenerated(0, 65, 0));

        reg.remove(0, 65, 0);
        assertFalse(reg.isGenerated(0, 65, 0));
        assertNull(reg.getEntry(0, 65, 0));
    }

    @Test
    public void selfReferencingAndDirectEntriesAreIndependent() {
        GeneratedBlockRegistry reg = newRegistry();

        reg.markGenerated(0, 65, 0, 0, 64, 0, "classic", 10, 1, "minecraft:stone", 0);
        reg.markGenerated(1, 65, 1, 1, 65, 1, "", 0, 0, "minecraft:dirt", 0);

        assertEquals(0, reg.getGeneratorX(0, 65, 0));
        assertEquals(64, reg.getGeneratorY(0, 65, 0));
        assertEquals(0, reg.getGeneratorZ(0, 65, 0));
        assertEquals(1, reg.getGeneratorX(1, 65, 1));
        assertEquals(65, reg.getGeneratorY(1, 65, 1));
        assertEquals(1, reg.getGeneratorZ(1, 65, 1));

        reg.remove(0, 65, 0);
        assertTrue(reg.isGenerated(0, 65, 0) == false);
        assertTrue(reg.isGenerated(1, 65, 1));
    }

    @Test
    public void multipleSelfReferencingEntries() {
        GeneratedBlockRegistry reg = newRegistry();

        reg.markGenerated(0, 65, 0, 0, 65, 0, "", 0, 0, "minecraft:dirt", 0);
        reg.markGenerated(1, 66, 1, 1, 66, 1, "", 0, 0, "minecraft:cobblestone", 0);
        reg.markGenerated(2, 67, 2, 2, 67, 2, "", 0, 0, "minecraft:stone", 0);

        assertTrue(reg.isGenerated(0, 65, 0));
        assertTrue(reg.isGenerated(1, 66, 1));
        assertTrue(reg.isGenerated(2, 67, 2));

        assertEquals(0, reg.getGeneratorX(0, 65, 0));
        assertEquals(65, reg.getGeneratorY(0, 65, 0));
        assertEquals(0, reg.getGeneratorZ(0, 65, 0));
        assertEquals(1, reg.getGeneratorX(1, 66, 1));
        assertEquals(66, reg.getGeneratorY(1, 66, 1));
        assertEquals(1, reg.getGeneratorZ(1, 66, 1));
        assertEquals(2, reg.getGeneratorX(2, 67, 2));
        assertEquals(67, reg.getGeneratorY(2, 67, 2));
        assertEquals(2, reg.getGeneratorZ(2, 67, 2));

        assertEquals("minecraft:dirt", reg.getEntry(0, 65, 0).blockRegistry);
        assertEquals("minecraft:cobblestone", reg.getEntry(1, 66, 1).blockRegistry);
        assertEquals("minecraft:stone", reg.getEntry(2, 67, 2).blockRegistry);
    }

}

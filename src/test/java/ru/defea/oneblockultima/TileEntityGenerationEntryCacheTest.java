package ru.defea.oneblockultima;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig.BlockEntryDefinition;
import ru.defea.oneblockultima.config.BlockSetConfig.SetLevelDefinition;
import ru.defea.oneblockultima.testutil.TestBootstrap;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Regression checks for the generation-entry cache in
 * {@link TileEntityOneBlockGenerator} (private method {@code pickGenerationEntry}):
 * the weighted list is rebuilt only when the level definition or the disable mask changes,
 * never per call.
 *
 * <p>The tile entity cannot be constructed headless (its constructor resolves
 * {@code ModTileEntities.ONE_BLOCK_GENERATOR.get()}, which needs an FML environment), and
 * {@code pickGenerationEntry} reads {@code level.random}. The generator is therefore allocated
 * with {@code sun.misc.Unsafe} (bypassing the constructor) and a {@link org.mockito.Mockito} mock
 * of {@link Level} (with a seeded {@link RandomSource} injected into its {@code random} field) is
 * attached via the public {@code setLevel}. Reflecting the private
 * {@code pickGenerationEntry} method and the {@code cachedWeightedEntries} field asserts that the
 * same weighted array instance is reused while nothing changes and replaced when it does.
 */
public class TileEntityGenerationEntryCacheTest
{
    private static final sun.misc.Unsafe UNSAFE = unsafe();

    @BeforeClass
    public static void setUp()
    {
        TestBootstrap.prepare();
    }

    private static sun.misc.Unsafe unsafe()
    {
        try
        {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Unable to access sun.misc.Unsafe", e);
        }
    }

    @Test
    public void pickReturnsNullForNullLevel()
    {
        assertNull(pick(null));
    }

    @Test
    public void pickReturnsNullForEmptyBlocks()
    {
        SetLevelDefinition level = new SetLevelDefinition();
        level.blocks = new ArrayList<>();
        assertNull(pick(level));
    }

    @Test
    public void pickReturnsNullWhenAllEntriesAreDisabled()
    {
        TileEntityOneBlockGenerator generator = newGenerator();
        setDisableChestGeneration(generator);
        SetLevelDefinition level = levelWith("minecraft:chest", 50, "minecraft:chest", 30);

        assertNull("a level whose every entry is filtered out must yield no entry", pick(generator, level));
    }

    @Test
    public void cachedEntriesAreReusedWhileStateUnchanged()
    {
        SetLevelDefinition level = levelWith("minecraft:stone", 50, "minecraft:dirt", 30, "minecraft:gravel", 20);
        TileEntityOneBlockGenerator generator = newGenerator();

        BlockEntryDefinition first = pick(generator, level);
        assertNotNull(first);

        BlockEntryDefinition[] cachedAfterFirst = cachedEntries(generator);
        assertNotNull(cachedAfterFirst);
        assertEquals("all three entries are allowed, so the weighted list must have three slots",
                3, cachedAfterFirst.length);

        for (int i = 0; i < 100; i++)
        {
            assertNotNull("a repeated pick must still resolve an entry", pick(generator, level));
        }

        assertSame("as long as level and disable mask are unchanged the same weighted array must be reused",
                cachedAfterFirst, cachedEntries(generator));

        System.out.println("[Perf] pickGenerationEntry weighted list rebuilt 0 times over 100 picks (same array instance)");
    }

    @Test
    public void cacheInvalidatesWhenDisableMaskChanges()
    {
        SetLevelDefinition level = levelWith("minecraft:chest", 50, "minecraft:stone", 30, "minecraft:dirt", 20);
        TileEntityOneBlockGenerator generator = newGenerator();

        BlockEntryDefinition[] before = assertCacheBuilt(generator, level, 3);

        setDisableChestGeneration(generator);
        BlockEntryDefinition first = pick(generator, level);
        assertNotNull(first);

        BlockEntryDefinition[] after = cachedEntries(generator);
        assertNotSame("the disable mask changed, so the weighted array must be rebuilt", before, after);
        assertEquals("chests are filtered out, leaving two entries", 2, after.length);

        for (int i = 0; i < 200; i++)
        {
            BlockEntryDefinition rolled = pick(generator, level);
            assertNotNull(rolled);
            assertNotEquals("a disabled chest must never be rolled after the cache rebuild", "minecraft:chest", rolled.registry);
        }
    }

    @Test
    public void cacheInvalidatesWhenLevelDefinitionChanges()
    {
        SetLevelDefinition levelA = levelWith("minecraft:stone", 50, "minecraft:dirt", 50);
        SetLevelDefinition levelB = levelWith("minecraft:stone", 40, "minecraft:dirt", 30, "minecraft:gravel", 30);
        TileEntityOneBlockGenerator generator = newGenerator();

        BlockEntryDefinition[] forLevelA = assertCacheBuilt(generator, levelA, 2);
        BlockEntryDefinition[] forLevelB = assertCacheBuilt(generator, levelB, 3);

        assertNotSame("a different level definition must invalidate the cache", forLevelA, forLevelB);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private TileEntityOneBlockGenerator newGenerator()
    {
        try
        {
            return (TileEntityOneBlockGenerator) UNSAFE.allocateInstance(TileEntityOneBlockGenerator.class);
        }
        catch (InstantiationException e)
        {
            throw new AssertionError("Unable to allocate TileEntityOneBlockGenerator", e);
        }
    }

    /**
     * Sets {@code disableChestGeneration} through the public setter. The level field is cleared
     * first so the {@code setChanged()} call inside the setter cannot blow up on a bare {@link Level}.
     */
    private void setDisableChestGeneration(TileEntityOneBlockGenerator generator)
    {
        generator.setLevel(null);
        generator.setDisableChestGeneration(true);
    }

    private BlockEntryDefinition pick(TileEntityOneBlockGenerator generator, SetLevelDefinition level)
    {
        return invokePick(generator, level);
    }

    private BlockEntryDefinition pick(SetLevelDefinition level)
    {
        return invokePick(newGenerator(), level);
    }

    private BlockEntryDefinition[] assertCacheBuilt(TileEntityOneBlockGenerator generator, SetLevelDefinition level, int expectedSize)
    {
        BlockEntryDefinition picked = pick(generator, level);
        assertNotNull(picked);
        BlockEntryDefinition[] cached = cachedEntries(generator);
        assertNotNull(cached);
        assertEquals(expectedSize, cached.length);
        return cached;
    }

    private static BlockEntryDefinition invokePick(TileEntityOneBlockGenerator generator, SetLevelDefinition level)
    {
        try
        {
            injectLevel(generator);
            Method method = TileEntityOneBlockGenerator.class.getDeclaredMethod("pickGenerationEntry", SetLevelDefinition.class);
            method.setAccessible(true);
            return (BlockEntryDefinition) method.invoke(generator, level);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Unable to invoke pickGenerationEntry", e);
        }
    }

    private static void injectLevel(TileEntityOneBlockGenerator generator) throws ReflectiveOperationException
    {
        if (generator.getLevel() != null)
        {
            return;
        }

        Level level = org.mockito.Mockito.mock(Level.class);
        Field randomField = Level.class.getField("random");
        randomField.setAccessible(true);
        randomField.set(level, RandomSource.create(42));
        generator.setLevel(level);
    }

    private static BlockEntryDefinition[] cachedEntries(TileEntityOneBlockGenerator generator)
    {
        try
        {
            Field field = TileEntityOneBlockGenerator.class.getDeclaredField("cachedWeightedEntries");
            field.setAccessible(true);
            return (BlockEntryDefinition[]) field.get(generator);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Unable to read cachedWeightedEntries", e);
        }
    }

    private static SetLevelDefinition levelWith(Object... registryChancePairs)
    {
        SetLevelDefinition level = new SetLevelDefinition();
        List<BlockEntryDefinition> blocks = new ArrayList<>();
        for (int i = 0; i < registryChancePairs.length; i += 2)
        {
            BlockEntryDefinition entry = new BlockEntryDefinition();
            entry.registry = (String) registryChancePairs[i];
            entry.meta = 0;
            entry.chance = ((Number) registryChancePairs[i + 1]).intValue();
            blocks.add(entry);
        }
        level.blocks = blocks;
        return level;
    }
}
package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.BlockSetConfig.BlockEntryDefinition;
import ru.defea.oneblockultima.config.BlockSetConfig.MobEntryDefinition;
import ru.defea.oneblockultima.config.BlockSetConfig.SetLevelDefinition;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Regression checks for optimizations: caches must not be rebuilt on every call.
 *
 * <p>Memory is measured by the thread's allocation rate (com.sun.management.ThreadMXBean#getThreadAllocatedBytes):
 * a warm cached path must allocate ~0 bytes, whereas rebuilding the cache on every operation
 * allocates megabytes. Speed is checked via comparative timing (cache faster than rebuild) and
 * an absolute throughput ceiling.
 */
public class OptimizationPerformanceTest
{
    private static final int WARMUP = 30_000;
    private static final int MEASURE = 100_000;
    private static final int TIME_ITERATIONS = 200_000;
    private static final int ALLOC_EPSILON_BYTES = 64 * 1024;

    private static final Random RANDOM = new Random(42);

    private static final boolean ALLOC_SUPPORTED = isAllocMeasurementSupported();

    private SetLevelDefinition levelHolder;
    private BlockEntryDefinition blockHolder;

    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
    }

    private static boolean isAllocMeasurementSupported()
    {
        try
        {
            com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            if (!bean.isThreadAllocatedMemorySupported())
            {
                return false;
            }
            bean.setThreadAllocatedMemoryEnabled(true);
            return bean.isThreadAllocatedMemoryEnabled();
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private void assumeAllocSupported()
    {
        Assume.assumeTrue("Thread allocation measurement not supported on this JVM", ALLOC_SUPPORTED);
    }

    // ------------------------------------------------------------------
    // pickMob (SetLevelDefinition): weightedMobs / totalMobChance cache
    // ------------------------------------------------------------------

    @Test
    public void pickMobWarmCallsDoNotAllocate()
    {
        assumeAllocSupported();

        SetLevelDefinition level = newLevelWithMobs();
        long cachedBytes = measureAllocationBytes(MEASURE, () -> level.pickMob(RANDOM));
        System.out.println("[Perf] pickMob cached: " + formatPerCallBytes(cachedBytes, MEASURE));

        assertTrue("cached pickMob must allocate ~0 bytes, got: " + formatBytes(cachedBytes),
                cachedBytes < ALLOC_EPSILON_BYTES);
    }

    @Test
    public void pickMobRebuildPerCallAllocatesFarMoreThanCached()
    {
        assumeAllocSupported();

        SetLevelDefinition level = newLevelWithMobs();
        long cachedBytes = measureAllocationBytes(MEASURE, () -> level.pickMob(RANDOM));

        long freshBytes = measureAllocationBytes(MEASURE, () -> {
            levelHolder = newLevelWithMobs();
            levelHolder.pickMob(RANDOM);
        });
        System.out.println("[Perf] pickMob cached=" + formatPerCallBytes(cachedBytes, MEASURE) + " vs fresh=" + formatPerCallBytes(freshBytes, MEASURE));

        assertTrue("rebuilding the weighted list per call must allocate noticeably more " +
                "(cached=" + formatBytes(cachedBytes) + ", fresh=" + formatBytes(freshBytes) + ")",
                freshBytes > cachedBytes * 10);
    }

    @Test
    public void pickMobCachedPathIsFasterThanRebuilding()
    {
        SetLevelDefinition cachedLevel = newLevelWithMobs();
        for (int i = 0; i < WARMUP; i++)
        {
            cachedLevel.pickMob(RANDOM);
        }

        long cachedMs = measureTimeMs(() -> cachedLevel.pickMob(RANDOM));

        for (int i = 0; i < WARMUP; i++)
        {
            levelHolder = newLevelWithMobs();
            levelHolder.pickMob(RANDOM);
        }
        long freshMs = measureTimeMs(() -> {
            levelHolder = newLevelWithMobs();
            levelHolder.pickMob(RANDOM);
        });
        System.out.println("[Perf] pickMob cached=" + formatPerCallNanos(cachedMs * 1_000_000L) + " vs fresh=" + formatPerCallNanos(freshMs * 1_000_000L));

        assertTrue("cached pickMob must be faster than rebuilding (cached=" + cachedMs + " ms, fresh=" + freshMs + " ms)",
                cachedMs < freshMs);
    }

    @Test
    public void pickMobCachedPathThroughputIsHigh()
    {
        SetLevelDefinition cachedLevel = newLevelWithMobs();
        for (int i = 0; i < WARMUP; i++)
        {
            cachedLevel.pickMob(RANDOM);
        }

        long start = System.nanoTime();
        int calls = 2_000_000;
        for (int i = 0; i < calls; i++)
        {
            cachedLevel.pickMob(RANDOM);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        System.out.println("[Perf] pickMob throughput: " + calls + " calls in " + elapsedMs + " ms (" + formatCallsPerSecond(calls, elapsedMs) + ")");

        assertTrue("2M cached pickMob calls took " + elapsedMs + " ms (limit 8000 ms)", elapsedMs < 8000L);
    }

    // ------------------------------------------------------------------
    // BlockSetConfig.getDefaultSetId: defaultSetId cache
    // ------------------------------------------------------------------

    @Test
    public void getDefaultSetIdIsCachedAndReferenceStable()
    {
        BlockSetConfig cfg = BlockSetConfig.get();
        String first = cfg.getDefaultSetId();
        String second = cfg.getDefaultSetId();

        assertSame("getDefaultSetId must return the same object (cache)", first, second);

        double perCall = measureAvgNanos(cfg::getDefaultSetId);
        System.out.println("[Perf] getDefaultSetId cached: same instance, " + formatAvgNanos(perCall));
    }

    @Test
    public void getDefaultSetIdWarmCallsDoNotAllocate()
    {
        assumeAllocSupported();

        BlockSetConfig cfg = BlockSetConfig.get();
        cfg.getDefaultSetId();

        long bytes = measureAllocationBytes(1_000_000, cfg::getDefaultSetId);
        System.out.println("[Perf] getDefaultSetId cached: " + formatPerCallBytes(bytes, 1_000_000));

        assertTrue("cached getDefaultSetId must allocate ~0 bytes, got: " + formatBytes(bytes),
                bytes < ALLOC_EPSILON_BYTES);
    }

    @Test
    public void getDefaultSetIdCacheInvalidatesAfterApplySets()
    {
        BlockSetConfig cfg = BlockSetConfig.get();
        String before = cfg.getDefaultSetId();

        // applying sets resets the cache and recomputes the value
        BlockSetConfig.applySets(new ArrayList<>(cfg.getSets()));
        String after = cfg.getDefaultSetId();

        assertEquals("after cache invalidation the value must be recomputed correctly", before, after);

        System.out.println("[Perf] getDefaultSetId cache invalidated by applySets and recomputed: " + after);
    }

    // ------------------------------------------------------------------
    // BlockEntryDefinition: classification cache (fluid/chest/sapling)
    // ------------------------------------------------------------------

    @Test
    public void blockEntryClassificationWarmCallsDoNotAllocate()
    {
        assumeAllocSupported();

        BlockEntryDefinition chest = newBlockEntry("minecraft:chest");
        chest.isChestEntry();
        chest.isSaplingEntry();
        chest.isFluid();

        long bytes = measureAllocationBytes(MEASURE, () -> {
            chest.isChestEntry();
            chest.isSaplingEntry();
            chest.isFluid();
        });
        System.out.println("[Perf] blockEntry classification cached: " + formatPerCallBytes(bytes, MEASURE));

        assertTrue("warm classification calls must allocate ~0 bytes, got: " + formatBytes(bytes),
                bytes < ALLOC_EPSILON_BYTES);
    }

    @Test
    public void blockEntryClassificationRebuildPerCallAllocatesFarMoreThanCached()
    {
        assumeAllocSupported();

        BlockEntryDefinition chest = newBlockEntry("minecraft:chest");
        long cachedBytes = measureAllocationBytes(MEASURE, chest::isChestEntry);

        long freshBytes = measureAllocationBytes(MEASURE, () -> {
            blockHolder = newBlockEntry("minecraft:chest");
            blockHolder.isChestEntry();
        });
        System.out.println("[Perf] blockEntry classification cached=" + formatPerCallBytes(cachedBytes, MEASURE) + " vs fresh=" + formatPerCallBytes(freshBytes, MEASURE));

        assertTrue("rebuilding classification per call must allocate noticeably more " +
                "(cached=" + formatBytes(cachedBytes) + ", fresh=" + formatBytes(freshBytes) + ")",
                freshBytes > cachedBytes * 10);
    }

    // ------------------------------------------------------------------
    // BlockEntryDefinition: getPickBlock / resolveBlock cache
    // ------------------------------------------------------------------

    @Test
    public void blockEntryGetPickBlockIsCachedAndReferenceStable()
    {
        BlockEntryDefinition entry = newBlockEntry("minecraft:stone");

        ItemStack a = entry.getPickBlock();
        ItemStack b = entry.getPickBlock();

        assertSame("getPickBlock must return the same ItemStack (cache)", a, b);

        double perCall = measureAvgNanos(entry::getPickBlock);
        System.out.println("[Perf] getPickBlock cached: same ItemStack, " + formatAvgNanos(perCall));
    }

    @Test
    public void blockEntryGetPickBlockWarmCallsDoNotAllocate()
    {
        assumeAllocSupported();

        BlockEntryDefinition entry = newBlockEntry("minecraft:stone");
        entry.getPickBlock();

        long bytes = measureAllocationBytes(MEASURE, entry::getPickBlock);
        System.out.println("[Perf] getPickBlock cached: " + formatPerCallBytes(bytes, MEASURE));

        assertTrue("warm getPickBlock must allocate ~0 bytes, got: " + formatBytes(bytes),
                bytes < ALLOC_EPSILON_BYTES);
    }

    @Test
    public void blockEntryResolveBlockIsCachedAndReferenceStable()
    {
        BlockEntryDefinition entry = newBlockEntry("minecraft:stone");

        assertSame("resolveBlock must return the same Block (cache)",
                entry.resolveBlock(), entry.resolveBlock());

        double perCall = measureAvgNanos(entry::resolveBlock);
        System.out.println("[Perf] resolveBlock cached: same Block, " + formatAvgNanos(perCall));
    }

    // ------------------------------------------------------------------
    // GeneratedBlockRegistry: markDirty throttling
    // ------------------------------------------------------------------

    @Test
    public void generatedBlockRegistryThrottlesDirtyWrites()
    {
        CountingRegistry registry = new CountingRegistry();
        int posX = 1;
        int posY = 64;
        int posZ = 2;

        for (int i = 0; i < 100; i++)
        {
            registry.markGenerated(posX, posY, posZ, posX, posY, posZ, "classic", 10, 1, "minecraft:stone", 0);
        }

        assertEquals("within a 2-second window the actual write must happen only once", 1, registry.markDirtyCount);

        registry.flushPendingDirty();
        assertEquals("flushPendingDirty must forcibly flush the accumulated dirty state", 2, registry.markDirtyCount);

        registry.markGenerated(posX, posY, posZ, posX, posY, posZ, "classic", 10, 1, "minecraft:stone", 0);
        assertEquals("a repeated call within the same window only accumulates", 2, registry.markDirtyCount);

        registry.flushPendingDirty();
        assertEquals(3, registry.markDirtyCount);

        System.out.println("[Perf] GeneratedBlockRegistry.markDirty throttled: 100 markGenerated calls -> "
                + registry.markDirtyCount + " writes (2 via forced flush)");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private long measureAllocationBytes(int iterations, Runnable task)
    {
        for (int i = 0; i < WARMUP; i++)
        {
            task.run();
        }

        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long before = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        for (int i = 0; i < iterations; i++)
        {
            task.run();
        }
        long after = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        return after - before;
    }

    private long measureTimeMs(Runnable task)
    {
        long before = System.nanoTime();
        for (int i = 0; i < OptimizationPerformanceTest.TIME_ITERATIONS; i++)
        {
            task.run();
        }
        return (System.nanoTime() - before) / 1_000_000L;
    }

    private double measureAvgNanos(Runnable task)
    {
        for (int i = 0; i < WARMUP; i++)
        {
            task.run();
        }

        int iterations = 1_000_000;
        long before = System.nanoTime();
        for (int i = 0; i < iterations; i++)
        {
            task.run();
        }
        return (System.nanoTime() - before) / (double) iterations;
    }

    private static String formatBytes(long bytes)
    {
        if (bytes < 1024)
        {
            return bytes + " B";
        }
        String[] units = {"K", "M", "G", "T"};
        double value = bytes;
        int i = -1;
        do
        {
            value /= 1024.0;
            i++;
        }
        while (value >= 1024 && i < units.length - 1);
        return String.format(java.util.Locale.ROOT, "%.1f %sB", value, units[i]);
    }

    private static String formatPerCallBytes(long totalBytes, int iterations)
    {
        double perCall = (double) totalBytes / iterations;
        if (perCall >= 1)
        {
            return formatBytes((long) perCall) + "/call";
        }
        if (perCall >= 0.001)
        {
            return String.format(java.util.Locale.ROOT, "%.3f B/call", perCall);
        }
        return String.format(java.util.Locale.ROOT, "%.6f B/call", perCall);
    }

    private static String formatPerCallNanos(long totalNanos)
    {
        double perCall = (double) totalNanos / OptimizationPerformanceTest.TIME_ITERATIONS;
        if (perCall >= 1000)
        {
            return String.format(java.util.Locale.ROOT, "%.2f µs/call", perCall / 1000.0);
        }
        return String.format(java.util.Locale.ROOT, "%.0f ns/call", perCall);
    }

    private static String formatAvgNanos(double perCall)
    {
        if (perCall >= 1000)
        {
            return String.format(java.util.Locale.ROOT, "%.2f µs/call", perCall / 1000.0);
        }
        return String.format(java.util.Locale.ROOT, "%.1f ns/call", perCall);
    }

    private static String formatCallsPerSecond(int calls, long elapsedMs)
    {
        double perSecond = elapsedMs > 0 ? calls / (elapsedMs / 1000.0) : Double.POSITIVE_INFINITY;
        if (perSecond >= 1_000_000)
        {
            return String.format(java.util.Locale.ROOT, "%.1f M calls/s", perSecond / 1_000_000.0);
        }
        if (perSecond >= 1_000)
        {
            return String.format(java.util.Locale.ROOT, "%.1f K calls/s", perSecond / 1_000.0);
        }
        return String.format(java.util.Locale.ROOT, "%.0f calls/s", perSecond);
    }

    private static SetLevelDefinition newLevelWithMobs()
    {
        SetLevelDefinition level = new SetLevelDefinition();
        List<MobEntryDefinition> mobs = new ArrayList<>();
        mobs.add(newMob("minecraft:pig", 40));
        mobs.add(newMob("minecraft:cow", 30));
        mobs.add(newMob("minecraft:sheep", 15));
        mobs.add(newMob("minecraft:chicken", 10));
        mobs.add(newMob("minecraft:rabbit", 5));
        level.mobs = mobs;
        return level;
    }

    private static MobEntryDefinition newMob(String registry, int chance)
    {
        MobEntryDefinition mob = new MobEntryDefinition();
        mob.registry = registry;
        mob.chance = chance;
        return mob;
    }

    private static BlockEntryDefinition newBlockEntry(String registry)
    {
        BlockEntryDefinition entry = new BlockEntryDefinition();
        entry.registry = registry;
        entry.meta = 0;
        entry.chance = 100;
        return entry;
    }

    private static class CountingRegistry extends GeneratedBlockRegistry
    {
        int markDirtyCount = 0;

        @Override
        public void markDirty()
        {
            markDirtyCount++;
        }
    }
}

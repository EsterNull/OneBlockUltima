package ru.defea.oneblockultima;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;

import java.lang.management.ManagementFactory;

import static org.junit.Assert.*;

/**
 * Regression checks for BlockOneBlockGenerator collision/outline boxes.
 *
 * <p>The block keeps three shared static {@link AxisAlignedBB}s (see
 * {@link BlockOneBlockGenerator#getBoundingBox} and
 * {@link BlockOneBlockGenerator#getCollisionBoundingBox}): every render/collision
 * pass reuses the same instance instead of allocating a new box per block. These
 * tests prove that the instance is stable across calls and that warm calls
 * allocate ~0 bytes.
 */
public class BlockOneBlockGeneratorTest
{
    private static final int WARMUP = 30_000;
    private static final int MEASURE = 100_000;
    private static final long ALLOC_EPSILON_BYTES = 64 * 1024;

    private static final boolean ALLOC_SUPPORTED = isAllocMeasurementSupported();

    private static final BlockPos POS = new BlockPos(1, 63, 2);

    private static BlockOneBlockGenerator block;
    private static IBlockState state;

    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
        block = ModBlocks.ONE_BLOCK_GENERATOR;
        state = block.getDefaultState();
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
    // reference stability (no per-call allocation)
    // ------------------------------------------------------------------

    @Test
    public void getBoundingBoxReturnsSameStaticInstance()
    {
        //noinspection DataFlowIssue
        AxisAlignedBB a = block.getBoundingBox(state, null, POS);
        //noinspection DataFlowIssue
        AxisAlignedBB b = block.getBoundingBox(state, null, POS);

        assertSame("getBoundingBox must return the shared static AABB (not a new box per call)", a, b);

        //noinspection DataFlowIssue
        double perCall = measureAvgNanos(() -> block.getBoundingBox(state, null, POS));
        System.out.println("[Perf] BlockOneBlockGenerator.getBoundingBox cached: same static AABB, " + formatAvgNanos(perCall));
    }

    @Test
    public void getCollisionBoundingBoxReturnsSameStaticInstance()
    {
        //noinspection DataFlowIssue
        AxisAlignedBB a = block.getCollisionBoundingBox(state, null, POS);
        //noinspection DataFlowIssue
        AxisAlignedBB b = block.getCollisionBoundingBox(state, null, POS);

        assertSame("getCollisionBoundingBox must return the shared static AABB (not a new box per call)", a, b);

        //noinspection DataFlowIssue
        double perCall = measureAvgNanos(() -> block.getCollisionBoundingBox(state, null, POS));
        System.out.println("[Perf] BlockOneBlockGenerator.getCollisionBoundingBox cached: same static AABB, " + formatAvgNanos(perCall));
    }

    // ------------------------------------------------------------------
    // allocation rate (warm calls must be allocation-free)
    // ------------------------------------------------------------------

    @Test
    public void getBoundingBoxWarmCallsDoNotAllocate()
    {
        assumeAllocSupported();

        //noinspection DataFlowIssue
        long bytes = measureAllocationBytes(() -> block.getBoundingBox(state, null, POS));
        System.out.println("[Perf] BlockOneBlockGenerator.getBoundingBox cached: " + formatPerCallBytes(bytes));

        assertTrue("warm getBoundingBox must allocate ~0 bytes, got: " + formatBytes(bytes),
                bytes < ALLOC_EPSILON_BYTES);
    }

    @Test
    public void getCollisionBoundingBoxWarmCallsDoNotAllocate()
    {
        assumeAllocSupported();

        //noinspection DataFlowIssue
        long bytes = measureAllocationBytes(() -> block.getCollisionBoundingBox(state, null, POS));
        System.out.println("[Perf] BlockOneBlockGenerator.getCollisionBoundingBox cached: " + formatPerCallBytes(bytes));

        assertTrue("warm getCollisionBoundingBox must allocate ~0 bytes, got: " + formatBytes(bytes),
                bytes < ALLOC_EPSILON_BYTES);
    }

    // ------------------------------------------------------------------
    // geometry
    // ------------------------------------------------------------------

    @Test
    public void getBoundingBoxHasExpectedGeometry()
    {
        //noinspection DataFlowIssue
        AxisAlignedBB box = block.getBoundingBox(state, null, POS);

        assertEquals(0.0D, box.minX, 0.0D);
        assertEquals(0.0D, box.minY, 0.0D);
        assertEquals(0.0D, box.minZ, 0.0D);
        assertEquals(1.0D, box.maxX, 0.0D);
        assertEquals(0.001D, box.maxY, 0.0D);
        assertEquals(1.0D, box.maxZ, 0.0D);
    }

    @Test
    public void getCollisionBoundingBoxHasExpectedGeometry()
    {
        //noinspection DataFlowIssue
        AxisAlignedBB box = block.getCollisionBoundingBox(state, null, POS);

        assert box != null;
        assertEquals(0.0D, box.minX, 0.0D);
        assertEquals(0.0D, box.minY, 0.0D);
        assertEquals(0.0D, box.minZ, 0.0D);
        assertEquals(1.0D, box.maxX, 0.0D);
        assertEquals(2.0D, box.maxY, 0.0D);
        assertEquals(1.0D, box.maxZ, 0.0D);
    }

    @Test
    public void getSelectedBoundingBoxOffsetsByPosition()
    {
        //noinspection DataFlowIssue
        AxisAlignedBB box = block.getSelectedBoundingBox(state, null, POS);

        assertEquals(POS.getX() + 0.0D, box.minX, 0.0D);
        assertEquals(POS.getY() + 1.0D, box.minY, 0.0D);
        assertEquals(POS.getZ() + 0.0D, box.minZ, 0.0D);
        assertEquals(POS.getX() + 1.0D, box.maxX, 0.0D);
        assertEquals(POS.getY() + 2.0D, box.maxY, 0.0D);
        assertEquals(POS.getZ() + 1.0D, box.maxZ, 0.0D);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private long measureAllocationBytes(Runnable task)
    {
        for (int i = 0; i < WARMUP; i++)
        {
            task.run();
        }

        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long before = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        for (int i = 0; i < BlockOneBlockGeneratorTest.MEASURE; i++)
        {
            task.run();
        }
        long after = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        return after - before;
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

    private static String formatPerCallBytes(long totalBytes)
    {
        double perCall = (double) totalBytes / BlockOneBlockGeneratorTest.MEASURE;
        if (perCall >= 1)
        {
            return formatBytes((long) perCall) + "/call";
        }
        if (perCall >= 0.001) {
            return String.format(java.util.Locale.ROOT, "%.3f B/call", perCall);
        }
        return String.format(java.util.Locale.ROOT, "%.6f B/call", perCall);
    }

    private static String formatAvgNanos(double perCall)
    {
        if (perCall >= 1000)
        {
            return String.format(java.util.Locale.ROOT, "%.2f µs/call", perCall / 1000.0);
        }
        return String.format(java.util.Locale.ROOT, "%.1f ns/call", perCall);
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
}

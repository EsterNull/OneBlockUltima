package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;

import static org.junit.Assert.*;

/**
 * Regression checks for BlockOneBlockGenerator collision/outline boxes.
 *
 * <p>The block keeps two shared static {@link AxisAlignedBB} templates (see
 * {@link BlockOneBlockGenerator}): {@code COLLISION_AABB} (0,0,0,1,2,1) and
 * {@code HIGHLIGHT_AABB} (0,1,0,1,2,1). Render bounds produced by
 * {@code setBlockBoundsBasedOnState} are a full cube (0,0,0,1,1,1); the block
 * itself renders nothing in the world (render type -1, matching the 1.12.x
 * INVISIBLE render type), and {@code shouldSideBeRendered} returns true so the
 * neighboring blocks do not leave visible holes. In 1.7.10 the
 * bounding-box methods take plain {@code (x, y, z)} ints and return the
 * template offset to that position, so these tests assert the resulting geometry
 * rather than instance identity.
 */
public class BlockOneBlockGeneratorTest
{
    private static final int X = 1;
    private static final int Y = BlockOneBlockGenerator.GENERATOR_Y;
    private static final int Z = 2;

    private static final AxisAlignedBB COLLISION_AABB = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    private static final AxisAlignedBB HIGHLIGHT_AABB = AxisAlignedBB.getBoundingBox(0.0D, 1.0D, 0.0D, 1.0D, 2.0D, 1.0D);

    private static BlockOneBlockGenerator block;
    private static World world;

    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
        block = ModBlocks.ONE_BLOCK_GENERATOR;
        world = TestDummyWorld.newWorld(false);
    }

    @Test
    public void setBlockBoundsBasedOnStateProducesFullCubeBounds()
    {
        block.setBlockBoundsBasedOnState(world, X, Y, Z);

        assertEquals(0.0D, block.getBlockBoundsMinX(), 0.0D);
        assertEquals(0.0D, block.getBlockBoundsMinY(), 0.0D);
        assertEquals(0.0D, block.getBlockBoundsMinZ(), 0.0D);
        assertEquals(1.0D, block.getBlockBoundsMaxX(), 0.0001D);
        assertEquals(1.0D, block.getBlockBoundsMaxY(), 0.0001D);
        assertEquals(1.0D, block.getBlockBoundsMaxZ(), 0.0001D);
    }

    @Test
    public void getCollisionBoundingBoxHasExpectedGeometry()
    {
        AxisAlignedBB expected = COLLISION_AABB.getOffsetBoundingBox(X, Y, Z);
        AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, X, Y, Z);

        assert box != null;
        assertBoxEquals("getCollisionBoundingBoxFromPool", expected, box);
    }

    @Test
    public void getCollisionBoundingBoxIsStableAcrossCalls()
    {
        AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(world, X, Y, Z);
        AxisAlignedBB b = block.getCollisionBoundingBoxFromPool(world, X, Y, Z);

        assert a != null;
        assert b != null;
        assertBoxEquals("repeated getCollisionBoundingBoxFromPool calls", a, b);
    }

    @Test
    public void getSelectedBoundingBoxOffsetsByPosition()
    {
        AxisAlignedBB expected = HIGHLIGHT_AABB.getOffsetBoundingBox(X, Y, Z);
        AxisAlignedBB box = block.getSelectedBoundingBoxFromPool(world, X, Y, Z);

        assert box != null;
        assertBoxEquals("getSelectedBoundingBoxFromPool", expected, box);
    }

    @Test
    public void collisionRayTraceReportsGeneratorCoordsNotZeroedCoords()
    {
        net.minecraft.util.Vec3 start = net.minecraft.util.Vec3.createVectorHelper(X + 0.5D, Y + 3.0D, Z + 0.5D);
        net.minecraft.util.Vec3 end = net.minecraft.util.Vec3.createVectorHelper(X + 0.5D, Y - 1.0D, Z + 0.5D);

        net.minecraft.util.MovingObjectPosition mop = block.collisionRayTrace(world, X, Y, Z, start, end);

        assertNotNull("a vertical ray through the highlight box must be intercepted", mop);
        assertEquals("blockX must be the generator's own X, not the zeroed intercept coords", X, mop.blockX);
        assertEquals("blockY must be the generator's own Y, not the zeroed intercept coords", Y, mop.blockY);
        assertEquals("blockZ must be the generator's own Z, not the zeroed intercept coords", Z, mop.blockZ);
    }

    private static void assertBoxEquals(String message, AxisAlignedBB expected, AxisAlignedBB actual)
    {
        assertEquals(message + " minX", expected.minX, actual.minX, 0.0D);
        assertEquals(message + " minY", expected.minY, actual.minY, 0.0D);
        assertEquals(message + " minZ", expected.minZ, actual.minZ, 0.0D);
        assertEquals(message + " maxX", expected.maxX, actual.maxX, 0.0D);
        assertEquals(message + " maxY", expected.maxY, actual.maxY, 0.0D);
        assertEquals(message + " maxZ", expected.maxZ, actual.maxZ, 0.0D);
    }
}

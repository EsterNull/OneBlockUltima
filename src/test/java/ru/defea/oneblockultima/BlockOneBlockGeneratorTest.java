package ru.defea.oneblockultima;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.BlockCustomBreakable;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Geometrical regression checks for BlockOneBlockGenerator shapes.
 *
 * <p>The 1.12 original asserted that the block reuses shared static AABBs and that warm
 * calls allocate ~0 bytes. The 1.21 port builds fresh {@link VoxelShape}s (the shared
 * static {@code COLLISION_AABB} still backs the collision box), so those
 * instance/allocation guarantees no longer hold; only the geometry is preserved here.</p>
 *
 * <p>Per design the generator is visually invisible and raycast-transparent
 * ({@code getShape}/{@code getInteractionShape} are empty), while it still has a
 * solid collision box so players cannot walk into it.</p>
 */
public class BlockOneBlockGeneratorTest
{
    private static final BlockPos POS = new BlockPos(1, 63, 2);

    private static BlockOneBlockGenerator block;

    @BeforeClass
    public static void setUp()
    {
        TestBootstrap.prepare();
        if (ModBlocks.ONE_BLOCK_GENERATOR == null)
        {
            TestBootstrap.unfreezeBlockRegistry();
            ModBlocks.ONE_BLOCK_GENERATOR = new BlockOneBlockGenerator();
        }
        block = ModBlocks.ONE_BLOCK_GENERATOR;
    }

    @Test
    public void collisionShapeBoundsMatchCollisionAabb()
    {
        VoxelShape shape = block.getCollisionShape(block.defaultBlockState(), null, POS, CollisionContext.empty());
        AABB bounds = shape.bounds();
        assertEquals(0.0D, bounds.minX, 0.0D);
        assertEquals(0.0D, bounds.minY, 0.0D);
        assertEquals(0.0D, bounds.minZ, 0.0D);
        assertEquals(1.0D, bounds.maxX, 0.0D);
        assertEquals(2.0D, bounds.maxY, 0.0D);
        assertEquals(1.0D, bounds.maxZ, 0.0D);
    }

    @Test
    public void shapeIsEmpty()
    {
        VoxelShape shape = block.getShape(block.defaultBlockState(), null, POS, CollisionContext.empty());
        assertTrue("generator must be raycast-transparent", shape.isEmpty());
    }

    @Test
    public void interactionShapeIsEmpty()
    {
        VoxelShape shape = block.getInteractionShape(block.defaultBlockState(), null, POS);
        assertTrue("generator must be raycast-transparent", shape.isEmpty());
    }

    @Test
    public void collisionShapeTranslatesByPos()
    {
        VoxelShape shape = block.getCollisionShape(block.defaultBlockState(), null, POS, CollisionContext.empty());
        AABB bounds = shape.move(POS.getX(), POS.getY(), POS.getZ()).bounds();
        assertEquals(POS.getX() + 0.0D, bounds.minX, 0.0D);
        assertEquals(POS.getY() + 0.0D, bounds.minY, 0.0D);
        assertEquals(POS.getZ() + 0.0D, bounds.minZ, 0.0D);
        assertEquals(POS.getX() + 1.0D, bounds.maxX, 0.0D);
        assertEquals(POS.getY() + 2.0D, bounds.maxY, 0.0D);
        assertEquals(POS.getZ() + 1.0D, bounds.maxZ, 0.0D);
    }

    @Test
    public void shapeGeometryIsStableAcrossCalls()
    {
        VoxelShape a = block.getCollisionShape(block.defaultBlockState(), null, POS, CollisionContext.empty());
        VoxelShape b = block.getCollisionShape(block.defaultBlockState(), null, POS, CollisionContext.empty());
        assertEquals(a.bounds(), b.bounds());
    }
}
package ru.defea.oneblockultima;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.testutil.TestBootstrap;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.world.OneBlockWorldType;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Guards of {@link ModEvents#onWorldTick} plus the per-tick iteration over
 * {@link TileEntityOneBlockGenerator#getActiveGenerators()}.
 *
 * <p>The world is a {@link org.mockito.Mockito} mock of {@link Level} (with a mocked
 * {@link Holder} backing {@code dimensionTypeRegistration()}), so no running server is required;
 * the guards are exercised by (a) wrong dimension/terrain, (b) client worlds, (c) non-END phases,
 * and the iteration itself is proven by expiring invites and stale-generator cleanup.
 */
public class ModEventsWorldTickTest
{
    private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID INVITEE = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @BeforeClass
    public static void setUp()
    {
        TestBootstrap.registerOneBlockTileEntity();
    }

    @After
    public void clearActiveGenerators()
    {
        TileEntityOneBlockGenerator.getActiveGenerators().clear();
    }

    @Test
    public void endPhaseTickTicksInvitesOfActiveGenerators()
    {
        Level world = level(true, true);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        ModEvents.onWorldTick(new TickEvent.LevelTickEvent.Post(LogicalSide.SERVER, world, () -> true));

        assertTrue("the single-tick invite must expire during the END world tick",
                generator.getPendingInvites().isEmpty());
        assertTrue("a valid generator belonging to this world must stay in the active set",
                TileEntityOneBlockGenerator.getActiveGenerators().contains(generator));
    }

    @Test
    public void worldTickRemovesGeneratorThatDoesNotBelongToTheTickedWorld()
    {
        Level world = level(true, true);
        TileEntityOneBlockGenerator valid = generatorWithWorldAndInvite(world, 100);
        TileEntityOneBlockGenerator stale = generatorWithoutWorld();
        stale.addPendingInvite(INVITEE, OWNER, 100);
        TileEntityOneBlockGenerator.getActiveGenerators().add(stale);

        ModEvents.onWorldTick(new TickEvent.LevelTickEvent.Post(LogicalSide.SERVER, world, () -> true));

        assertTrue(TileEntityOneBlockGenerator.getActiveGenerators().contains(valid));
        assertFalse("a generator whose world is null (or another world) must be removed",
                TileEntityOneBlockGenerator.getActiveGenerators().contains(stale));
    }

    @Test
    public void worldTickSkipsClientWorlds()
    {
        Level world = level(false, true);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        ModEvents.onWorldTick(new TickEvent.LevelTickEvent.Post(LogicalSide.SERVER, world, () -> true));

        assertEquals("client-side worlds must be skipped entirely", 1, generator.getPendingInvites().size());
        assertTrue("a generator in a remote world is not added to the active set",
                TileEntityOneBlockGenerator.getActiveGenerators().isEmpty());
    }

    @Test
    public void worldTickSkipsNonOneBlockTerrain()
    {
        Level world = level(true, false);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        ModEvents.onWorldTick(new TickEvent.LevelTickEvent.Post(LogicalSide.SERVER, world, () -> true));

        assertEquals("worlds with a terrain type other than OneBlock must be skipped", 1, generator.getPendingInvites().size());
    }

    @Test
    public void worldTickSkipsStartPhase()
    {
        Level world = level(true, true);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        ModEvents.onWorldTick(new TickEvent.LevelTickEvent.Pre(LogicalSide.SERVER, world, () -> true));

        assertEquals("only END-phase ticks may process generators", 1, generator.getPendingInvites().size());
    }

    private Level level(boolean serverSide, boolean isOneBlockTerrain)
    {
        Level world = Mockito.mock(Level.class);
        Mockito.when(world.isClientSide()).thenReturn(!serverSide);
        Holder<DimensionType> holder = Mockito.mock(Holder.class);
        Mockito.when(holder.is(OneBlockWorldType.DIMENSION_TYPE)).thenReturn(isOneBlockTerrain);
        Mockito.when(world.dimensionTypeRegistration()).thenReturn(holder);
        return world;
    }

    private TileEntityOneBlockGenerator generatorWithWorldAndInvite(Level world, int inviteTicks)
    {
        TileEntityOneBlockGenerator generator = new TileEntityOneBlockGenerator(
                BlockPos.ZERO, ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());
        generator.setLevel(world);
        generator.setOwnerId(OWNER);
        generator.addPendingInvite(INVITEE, OWNER, inviteTicks);
        generator.onLoad();
        return generator;
    }

    private TileEntityOneBlockGenerator generatorWithoutWorld()
    {
        return new TileEntityOneBlockGenerator(BlockPos.ZERO, ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());
    }
}
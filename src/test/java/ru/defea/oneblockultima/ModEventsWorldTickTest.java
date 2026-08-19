package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.world.World;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.world.OneBlockWorldType;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Guards of {@link ModEvents#onWorldTick} plus the per-tick iteration over
 * {@link TileEntityOneBlockGenerator#getActiveGenerators()}.
 *
 * <p>Uses {@link TestDummyWorld} so no running server is required; the guards are
 * exercised by (a) wrong dimension/terrain, (b) client worlds, (c) non-END phases,
 * and the iteration itself is proven by expiring invites and stale-generator cleanup.
 */
public class ModEventsWorldTickTest
{
    private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID INVITEE = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
    }

    @After
    public void clearActiveGenerators()
    {
        TileEntityOneBlockGenerator.getActiveGenerators().clear();
    }

    @Test
    public void endPhaseTickTicksInvitesOfActiveGenerators()
    {
        World world = TestDummyWorld.newWorld(false, OneBlockWorldType.ONE_BLOCK);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        new ModEvents().onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));

        assertTrue("the single-tick invite must expire during the END world tick",
                generator.getPendingInvites().isEmpty());
        assertTrue("a valid generator belonging to this world must stay in the active set",
                TileEntityOneBlockGenerator.getActiveGenerators().contains(generator));
    }

    @Test
    public void worldTickRemovesGeneratorThatDoesNotBelongToTheTickedWorld()
    {
        World world = TestDummyWorld.newWorld(false, OneBlockWorldType.ONE_BLOCK);
        TileEntityOneBlockGenerator valid = generatorWithWorldAndInvite(world, 100);
        TileEntityOneBlockGenerator stale = new TileEntityOneBlockGenerator();
        stale.addPendingInvite(INVITEE, OWNER, 100);
        TileEntityOneBlockGenerator.getActiveGenerators().add(stale);

        new ModEvents().onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));

        assertTrue(TileEntityOneBlockGenerator.getActiveGenerators().contains(valid));
        assertFalse("a generator whose world is null (or another world) must be removed",
                TileEntityOneBlockGenerator.getActiveGenerators().contains(stale));
    }

    @Test
    public void worldTickSkipsClientWorlds()
    {
        World world = TestDummyWorld.newWorld(true, OneBlockWorldType.ONE_BLOCK);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        new ModEvents().onWorldTick(new TickEvent.WorldTickEvent(Side.CLIENT, TickEvent.Phase.END, world));

        assertEquals("client-side worlds must be skipped entirely", 1, generator.getPendingInvites().size());
        assertTrue("a generator in a remote world is not added to the active set",
                TileEntityOneBlockGenerator.getActiveGenerators().isEmpty());
    }

    @Test
    public void worldTickSkipsNonOneBlockTerrain()
    {
        World world = TestDummyWorld.newWorld(false);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        new ModEvents().onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));

        assertEquals("worlds with a terrain type other than OneBlock must be skipped", 1, generator.getPendingInvites().size());
    }

    @Test
    public void worldTickSkipsStartPhase()
    {
        World world = TestDummyWorld.newWorld(false, OneBlockWorldType.ONE_BLOCK);
        TileEntityOneBlockGenerator generator = generatorWithWorldAndInvite(world, 1);

        new ModEvents().onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.START, world));

        assertEquals("only END-phase ticks may process generators", 1, generator.getPendingInvites().size());
    }

    private TileEntityOneBlockGenerator generatorWithWorldAndInvite(World world, int inviteTicks)
    {
        TileEntityOneBlockGenerator generator = new TileEntityOneBlockGenerator();
        TestDummyWorld.setWorld(generator, world);
        generator.setOwnerId(OWNER);
        generator.addPendingInvite(INVITEE, OWNER, inviteTicks);
        generator.validate();
        return generator;
    }
}

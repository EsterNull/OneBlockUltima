package ru.defea.oneblockultima;

import cpw.mods.fml.common.eventhandler.ASMEventHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.world.WorldEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.world.OneBlockWorldType;
import ru.defea.oneblockultima.world.SpawnConfigData;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards the 1.7.10 event-registration fix for the "generator not placed" bug.
 *
 * <p>In 1.7.10 {@code EventBus.register(SomeClass.class)} is a silent no-op: the bus
 * scans {@code Class.getMethods()} (the methods of {@code java.lang.Class}), so the
 * old 1.12.2-style class registration never delivered a single event. On top of
 * that, the 1.7.10 {@code ASMEventHandler} wrapper always invokes handlers as
 * <em>instance</em> methods (there is no {@code Modifier.isStatic} branch before 1.8),
 * so {@code @SubscribeEvent} methods must be non-static. These tests pin both facts
 * down, then prove end-to-end that {@link ModEvents#onWorldLoad} places the
 * generator, the fluid barrier and the mod spawn when a real {@link WorldEvent.Load}
 * is posted to a bus the handler instance is registered on.
 *
 * <p>One subtlety: the 1.7.10 {@code EventBus.register(instance)} internally does
 * {@code eventType.getConstructor().newInstance()} on the <em>event</em> class to grab
 * its {@code ListenerList}. In the real game the runtime ASM transformer
 * {@code cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer} generates a
 * public no-arg constructor (plus a per-class {@code LISTENER_LIST}) for every Event
 * subclass, so that lookup succeeds. The plain JUnit classloader does not run that
 * transformer, so Forge's own event types (which have no no-arg constructor in
 * bytecode) cannot be registered via {@code bus.register}. {@link #registerEventHandler}
 * below emulates exactly what the transformer-enabled registration achieves, using the
 * same {@code ASMEventHandler} + {@code ListenerList} machinery the bus itself uses.
 */
public class ModEventsWorldLoadTest
{
    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
    }

    @Test
    public void classRegistrationRegistersNothingIn1710()
    {
        EventBus bus = new EventBus();
        TestListener.invoked = false;

        bus.register(TestListener.class);
        bus.post(new TestTriggerEvent());

        assertFalse("1.7.10 EventBus.register(SomeClass.class) must NOT register any handlers",
                TestListener.invoked);
    }

    @Test
    public void instanceRegistrationDispatchesToInstanceHandlers()
    {
        EventBus bus = new EventBus();
        TestListener.invoked = false;

        bus.register(new TestListener());
        bus.post(new TestTriggerEvent());

        assertTrue("1.7.10 EventBus.register(instance) must dispatch to @SubscribeEvent instance methods",
                TestListener.invoked);
    }

    @Test
    public void onWorldLoadPlacesGeneratorBarrierAndSpawn() throws Exception
    {
        World world = TestDummyWorld.newWorld(false, OneBlockWorldType.ONE_BLOCK, tempSaveHandler());
        EventBus bus = new EventBus();
        registerEventHandler(bus, new ModEvents(), "onWorldLoad", WorldEvent.Load.class);

        bus.post(new WorldEvent.Load(world));

        assertEquals(ModBlocks.ONE_BLOCK_GENERATOR,
                world.getBlock(BlockOneBlockGenerator.GENERATOR_X, BlockOneBlockGenerator.GENERATOR_Y, BlockOneBlockGenerator.GENERATOR_Z));
        assertEquals(ModBlocks.FLUID_BARRIER,
                world.getBlock(BlockOneBlockGenerator.GENERATOR_X, BlockOneBlockGenerator.FLUID_BARRIER_Y, BlockOneBlockGenerator.GENERATOR_Z));
        assertEquals(0, world.getWorldInfo().getSpawnX());
        assertEquals(BlockOneBlockGenerator.FLUID_BARRIER_Y, world.getWorldInfo().getSpawnY());
        assertEquals(0, world.getWorldInfo().getSpawnZ());
        assertTrue("the one-time spawn initialization must be recorded", SpawnConfigData.get(world).spawnInitialized);
    }

    @Test
    public void onWorldLoadSkipsWorldsWithOtherTerrain() throws Exception
    {
        World world = TestDummyWorld.newWorld(false, null, tempSaveHandler());
        EventBus bus = new EventBus();
        registerEventHandler(bus, new ModEvents(), "onWorldLoad", WorldEvent.Load.class);

        bus.post(new WorldEvent.Load(world));

        assertNotEquals("worlds without the OneBlock terrain type must be left untouched",
                ModBlocks.ONE_BLOCK_GENERATOR,
                world.getBlock(BlockOneBlockGenerator.GENERATOR_X, BlockOneBlockGenerator.GENERATOR_Y, BlockOneBlockGenerator.GENERATOR_Z));
        assertFalse(SpawnConfigData.get(world).spawnInitialized);
    }

    /**
     * Emulates what the runtime {@code EventSubscriptionTransformer} makes
     * {@code EventBus.register(instance)} do for a single handler method: builds the
     * {@link ASMEventHandler} wrapper and registers it on the bus's {@code ListenerList}
     * under the bus id, exactly as {@code EventBus.register(Class,Object,Method,ModContainer)}
     * would after the transformer supplies the event class a public no-arg constructor.
     */
    private static void registerEventHandler(EventBus bus, Object target, String methodName, Class<?> eventType) throws Exception
    {
        Method method = target.getClass().getMethod(methodName, eventType);
        ASMEventHandler listener = new ASMEventHandler(target, method, null);

        Field busIdField = EventBus.class.getDeclaredField("busID");
        busIdField.setAccessible(true);

        new Event().getListenerList().register(busIdField.getInt(bus), listener.getPriority(), listener);
    }

    private static ISaveHandler tempSaveHandler()
    {
        try
        {
            final File dir = Files.createTempDirectory("obu-test-world").toFile();
            return new ISaveHandler()
            {
                @Override
                public WorldInfo loadWorldInfo()
                {
                    return null;
                }

                @Override
                public void checkSessionLock()
                {
                }

                @Override
                public IChunkLoader getChunkLoader(WorldProvider provider)
                {
                    return null;
                }

                @Override
                public void saveWorldInfoWithPlayer(WorldInfo worldInfo, NBTTagCompound nbtTagCompound)
                {
                }

                @Override
                public void saveWorldInfo(WorldInfo worldInfo)
                {
                }

                @Override
                public IPlayerFileData getPlayerNBTManager()
                {
                    return null;
                }

                @Override
                public void flush()
                {
                }

                @Override
                public File getWorldDirectory()
                {
                    return dir;
                }

                @Override
                public File getMapFileFromName(String name)
                {
                    return new File(dir, name + ".dat");
                }

                @Override
                public String getWorldDirectoryName()
                {
                    return dir.getName();
                }
            };
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static class TestTriggerEvent extends Event
    {
    }

    public static class TestListener
    {
        static boolean invoked;

        @SubscribeEvent
        public void onTrigger(TestTriggerEvent event)
        {
            invoked = true;
        }
    }
}

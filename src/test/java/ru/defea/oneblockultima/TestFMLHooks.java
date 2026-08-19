package ru.defea.oneblockultima;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.MinecraftDummyContainer;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Installs a minimal {@link Loader} into FML's static state so the vanilla
 * registries, the mod's own blocks/items and {@link net.minecraft.init.Bootstrap#register()}
 * can be initialised inside a plain JVM (no {@code LaunchClassLoader}).
 *
 * <p>The stub is created with {@code Unsafe.allocateInstance} so the real
 * constructor (which demands a {@code LaunchClassLoader}) never runs. The
 * fields the game actually consults during registration are left defaulted
 * or primed: {@code activeModContainer()} returns null (which makes
 * {@code GameData.addPrefix} assume the {@code minecraft} prefix),
 * {@code getActiveModList()} returns an empty list, {@code getConfigDir()}
 * points at a temp directory, {@code isModLoaded()} returns false and a
 * {@code LoadController} parked in the {@code NOINIT} state backs
 * {@code Loader.isInState(...)} so {@code GameRegistry} registration calls
 * work without tripping the launchwrapper-only {@code FMLLog} path.</p>
 */
public final class TestFMLHooks
{
    private TestFMLHooks()
    {
    }

    public static void installLoader()
    {
        try
        {
            if (instanceField().get(null) != null)
            {
                Loader.instance();
                return;
            }

            Loader loader = (Loader) unsafe().allocateInstance(Loader.class);
            instanceField().set(null, loader);

            LoadController controller = new LoadController(loader);
            setField(LoadController.class, "state", controller, LoaderState.NOINIT);
            setField(Loader.class, "modController", loader, controller);

            setField(FMLRelaunchLog.class, "side", null, Side.CLIENT);

            setField(Loader.class, "canonicalConfigDir", loader, tempConfigDir());
            setField(Loader.class, "namedMods", loader, new HashMap<String, ModContainer>());
            setField(Loader.class, "minecraft", loader, new MinecraftDummyContainer("1.7.10"));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to install the test FML Loader stub", e);
        }
    }

    private static Field instanceField() throws NoSuchFieldException
    {
        Field field = Loader.class.getDeclaredField("instance");
        field.setAccessible(true);
        return field;
    }

    private static void setField(Class<?> type, String name, Object target, Object value)
            throws NoSuchFieldException, IllegalAccessException
    {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static sun.misc.Unsafe unsafe() throws Exception
    {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    private static File tempConfigDir() throws Exception
    {
        Path dir = Files.createTempDirectory("oneblockultima-fml-config");
        return dir.toFile();
    }
}

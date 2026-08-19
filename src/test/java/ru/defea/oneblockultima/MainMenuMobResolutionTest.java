package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.util.MobIdUtil;
import ru.defea.oneblockultima.util.ModelUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Reproduces the MAIN-MENU config screen path: no world is loaded
 * (Minecraft.theWorld == null), so ModelUtil falls back to its dummy world
 * and mobs are created through it. If the dummy world fails to build, every
 * mob icon in the set config screen silently becomes a gray "M" placeholder.
 */
public class MainMenuMobResolutionTest
{
    private static final String[] REGISTRIES = {
        "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper",
        "minecraft:spider", "minecraft:cave_spider", "minecraft:enderman",
        "minecraft:witch", "minecraft:slime", "minecraft:magma_cube",
        "minecraft:blaze", "minecraft:zombie_pigman", "minecraft:villager_golem",
        "minecraft:bat", "minecraft:sheep", "minecraft:cow", "minecraft:pig",
        "minecraft:chicken", "minecraft:wolf", "minecraft:villager", "minecraft:horse",
        "minecraft:snowman", "minecraft:ocelot", "minecraft:mooshroom", "minecraft:squid"
    };

    private static World world;

    private static sun.misc.Unsafe unsafe() throws Exception
    {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    @BeforeClass
    public static void setUp() throws Exception
    {
        Bootstrap.register();
        Minecraft mc = (Minecraft) unsafe().allocateInstance(Minecraft.class);
        Field f = Minecraft.class.getDeclaredField("theMinecraft");
        f.setAccessible(true);
        f.set(null, mc);
        world = ModelUtil.getWorldOrCreateDummy();
    }

    @Test
    public void dummyWorldIsCreatedWithoutLoadedWorld()
    {
        assertNotNull("dummy world must be created when theWorld is null", world);
    }

    @Test
    public void mobsResolveThroughDummyWorld()
    {
        assertNotNull(world);
        List<String> failed = new ArrayList<>();
        for (String registry : REGISTRIES)
        {
            Entity entity = null;
            try
            {
                entity = MobIdUtil.createEntity(registry, world);
            }
            catch (Throwable t)
            {
                failed.add(registry + " threw " + t);
                continue;
            }
            if (!(entity instanceof EntityLivingBase))
            {
                failed.add(registry + " -> " + (entity == null ? "null" : entity.getClass().getSimpleName()));
            }
        }
        assertTrue("failed to create through dummy world: " + failed, failed.isEmpty());
    }
}

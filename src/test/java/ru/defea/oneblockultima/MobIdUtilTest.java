package ru.defea.oneblockultima;

import net.minecraft.entity.EntityList;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.util.MobIdUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Verifies the 1.12.2-style namespaced mob ids ("minecraft:zombie") used in the
 * shared config resolve to the plain 1.7.10 {@link net.minecraft.entity.EntityList}
 * keys ("Zombie") and back again.
 */
public class MobIdUtilTest
{
    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
    }

    @Test
    public void resolveKey_plainKeysPassThrough()
    {
        assertEquals("Zombie", MobIdUtil.resolveKey("Zombie"));
        assertEquals("PigZombie", MobIdUtil.resolveKey("PigZombie"));
    }

    @Test
    public void resolveKey_namespacedVanillaIds()
    {
        assertEquals("Zombie", MobIdUtil.resolveKey("minecraft:zombie"));
        assertEquals("Skeleton", MobIdUtil.resolveKey("minecraft:skeleton"));
        assertEquals("PigZombie", MobIdUtil.resolveKey("minecraft:zombie_pigman"));
        assertEquals("VillagerGolem", MobIdUtil.resolveKey("minecraft:villager_golem"));
        assertEquals("LavaSlime", MobIdUtil.resolveKey("minecraft:magma_cube"));
        assertEquals("MushroomCow", MobIdUtil.resolveKey("minecraft:mooshroom"));
        assertEquals("Zombie", MobIdUtil.resolveKey("minecraft:zombie_villager"));
        assertNull(MobIdUtil.resolveKey("minecraft:rabbit"));
        assertNull(MobIdUtil.resolveKey("minecraft:endermite"));
        assertNull(MobIdUtil.resolveKey("minecraft:shulker"));
        assertNull(MobIdUtil.resolveKey("minecraft:wither_skeleton"));
    }

    @Test
    public void resolveKey_unknownIdReturnsNull()
    {
        assertNull(MobIdUtil.resolveKey("minecraft:endermite"));
        assertNull(MobIdUtil.resolveKey("minecraft:shulker"));
        assertNull(MobIdUtil.resolveKey(null));
    }

    @Test
    public void toNamespacedRegistry_vanillaKeys()
    {
        assertEquals("minecraft:zombie", MobIdUtil.toNamespacedRegistry("Zombie"));
        assertEquals("minecraft:zombie_pigman", MobIdUtil.toNamespacedRegistry("PigZombie"));
        assertEquals("minecraft:villager_golem", MobIdUtil.toNamespacedRegistry("VillagerGolem"));
        assertEquals("minecraft:magma_cube", MobIdUtil.toNamespacedRegistry("LavaSlime"));
        assertEquals("minecraft:cave_spider", MobIdUtil.toNamespacedRegistry("CaveSpider"));
        assertEquals("minecraft:ocelot", MobIdUtil.toNamespacedRegistry("Ozelot"));
        assertEquals("minecraft:mooshroom", MobIdUtil.toNamespacedRegistry("MushroomCow"));
    }

    @Test
    public void toNamespacedRegistry_alreadyNamespacedPassThrough()
    {
        assertEquals("minecraft:zombie", MobIdUtil.toNamespacedRegistry("minecraft:zombie"));
        assertEquals("divinerpg:eden_cadillion", MobIdUtil.toNamespacedRegistry("divinerpg:eden_cadillion"));
    }

    @Test
    public void roundTrip()
    {
        String registry = MobIdUtil.toNamespacedRegistry("Zombie");
        assertNotNull(registry);
        assertEquals("Zombie", MobIdUtil.resolveKey(registry));

        String mushroom = MobIdUtil.toNamespacedRegistry("MushroomCow");
        assertNotNull(mushroom);
        assertEquals("minecraft:mooshroom", mushroom);
        assertEquals("MushroomCow", MobIdUtil.resolveKey(mushroom));
    }

    @Test
    public void resolveKey_modIdsMatchByClassName()
    {
        EntityList.addMapping(TestMobApalachiaCadillion.class, "ApalachiaCadillion", 250, 0x111111, 0x222222);
        EntityList.addMapping(TestMobBasalz.class, "Basalz", 251, 0x111111, 0x222222);

        assertEquals("ApalachiaCadillion", MobIdUtil.resolveKey("divinerpg:divinerpg._apalachia_cadillion"));
        assertEquals("ApalachiaCadillion", MobIdUtil.resolveKey("divinerpg:apalachia_cadillion"));
        assertEquals("Basalz", MobIdUtil.resolveKey("thermalfoundation:basalz"));
        assertNull(MobIdUtil.resolveKey("thermalfoundation:no_such_mob_xyz"));
    }
}

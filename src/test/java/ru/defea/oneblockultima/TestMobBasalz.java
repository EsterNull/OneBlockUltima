package ru.defea.oneblockultima;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.World;

/**
 * Stand-in for a mod elemental (real-world equivalent: Thermal Foundation's
 * {@code EntityBasalz}) used to verify {@link ru.defea.oneblockultima.util.MobIdUtil}
 * resolves short mod ids against 1.7.10 {@link net.minecraft.entity.EntityList}
 * keys by class name.
 */
public class TestMobBasalz extends EntityZombie
{
    public TestMobBasalz(World world)
    {
        super(world);
    }
}
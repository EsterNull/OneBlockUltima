package ru.defea.oneblockultima;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.World;

/**
 * Stand-in for a mod entity (real-world equivalent: divinerpg's
 * {@code EntityApalachiaCadillion}) used to verify {@link ru.defea.oneblockultima.util.MobIdUtil}
 * resolves 1.12-style mod ids against 1.7.10 {@link net.minecraft.entity.EntityList}
 * keys by class name.
 */
public class TestMobApalachiaCadillion extends EntityZombie
{
    public TestMobApalachiaCadillion(World world)
    {
        super(world);
    }
}
package ru.defea.oneblockultima.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ItemSuperPoison extends CustomPotion {
    public ItemSuperPoison() {
        super("super_poison",
                new MobEffectInstance[]{
                        new MobEffectInstance(MobEffects.POISON, 90 * 20, 3),
                        new MobEffectInstance(MobEffects.WITHER, 30 * 20, 2),
                });
    }
}

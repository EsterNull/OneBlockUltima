package ru.defea.oneblockultima.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ItemNaturalPoison extends CustomPotion {
    public ItemNaturalPoison() {
        super("natural_poison",
                new MobEffectInstance[]{
                        new MobEffectInstance(MobEffects.POISON, 30 * 20, 1)
                });
    }
}

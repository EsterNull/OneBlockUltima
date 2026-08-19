package ru.defea.oneblockultima.item;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class ItemLiquidDeath extends CustomPotion {
    public ItemLiquidDeath() {
        super("liquid_death",
                new PotionEffect[]{
                        new PotionEffect(Potion.harm.id, 600 * 20, 123)
                });
    }
}

package ru.defea.oneblockultima.item;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class ItemNaturalPoison extends CustomPotion {
    public ItemNaturalPoison() {
        super("natural_poison",
                new PotionEffect[]{
                        new PotionEffect(Potion.poison.id, 30 * 20, 1)
                });
    }
}

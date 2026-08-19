package ru.defea.oneblockultima.item;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class ItemSuperPoison extends CustomPotion {
    public ItemSuperPoison() {
        super("super_poison",
                new PotionEffect[]{
                        new PotionEffect(Potion.poison.id, 90 * 20, 3),
                        new PotionEffect(Potion.wither.id, 30 * 20, 2),
                });
    }
}

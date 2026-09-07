package ru.defea.oneblockultima.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.ModTab;

public class ItemSuperMashedVegetables extends Item {
    public ItemSuperMashedVegetables() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()));
    }
}

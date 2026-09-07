package ru.defea.oneblockultima.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.ModTab;

public class ItemMashedVegetables extends Item {
    public ItemMashedVegetables() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    }
}

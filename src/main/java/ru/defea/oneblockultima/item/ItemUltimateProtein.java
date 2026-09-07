package ru.defea.oneblockultima.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.ModTab;

public class ItemUltimateProtein extends Item {
    public ItemUltimateProtein() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(3.0F).build()));
    }
}

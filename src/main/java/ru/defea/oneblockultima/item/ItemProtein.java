package ru.defea.oneblockultima.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.ModTab;

public class ItemProtein extends Item {
    public ItemProtein() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(1.0F).build()));
    }
}

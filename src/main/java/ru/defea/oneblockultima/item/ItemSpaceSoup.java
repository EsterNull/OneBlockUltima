package ru.defea.oneblockultima.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.ModTab;

public class ItemSpaceSoup extends Item {
    public ItemSpaceSoup() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(100).saturationModifier(10.0F).build()));
    }
}

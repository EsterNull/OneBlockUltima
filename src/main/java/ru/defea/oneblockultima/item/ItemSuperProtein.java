package ru.defea.oneblockultima.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import ru.defea.oneblockultima.ModTab;

public class ItemSuperProtein extends Item {
    public ItemSuperProtein() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.5F).build()));
    }
}

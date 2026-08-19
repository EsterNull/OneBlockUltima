package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemMashedVegetables extends ItemFood {
    public ItemMashedVegetables() {
        super(2, 0.3F, false);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("mashed_vegetables");
    }
}

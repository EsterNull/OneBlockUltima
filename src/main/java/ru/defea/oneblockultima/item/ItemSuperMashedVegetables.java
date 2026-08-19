package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemSuperMashedVegetables extends ItemFood {
    public ItemSuperMashedVegetables() {
        super(4, 0.3F, false);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("super_mashed_vegetables");
    }
}

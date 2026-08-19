package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemUltimateMashedVegetables extends ItemFood {
    public ItemUltimateMashedVegetables() {
        super(6, 0.3F, false);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("ultimate_mashed_vegetables");
    }
}

package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemUltimateProtein extends ItemFood {
    public ItemUltimateProtein() {
        super(10, 3F, true);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("ultimate_protein");
    }
}

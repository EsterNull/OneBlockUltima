package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemProtein extends ItemFood {
    public ItemProtein() {
        super(3, 1F, true);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("protein");
    }
}

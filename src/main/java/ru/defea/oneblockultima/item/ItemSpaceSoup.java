package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemSpaceSoup extends ItemFood {
    public ItemSpaceSoup() {
        super(100, 10F, false);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("space_soup");
    }
}

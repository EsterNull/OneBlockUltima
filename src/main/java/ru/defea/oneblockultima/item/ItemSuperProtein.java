package ru.defea.oneblockultima.item;

import net.minecraft.item.ItemFood;
import ru.defea.oneblockultima.OneBlockUltima;

public class ItemSuperProtein extends ItemFood {
    public ItemSuperProtein() {
        super(6, 1.5F, true);
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName("super_protein");
    }
}

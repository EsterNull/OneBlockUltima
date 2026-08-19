package ru.defea.oneblockultima.item;

import net.minecraft.item.Item;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems
{
    @SuppressWarnings("unused")
    public static final class RegisterItem
    {
        private final Item item;
        private String variantIn = "inventory";
        private int meta = 0;

        private RegisterItem(Item item)
        {
            this.item = item;
        }

        private RegisterItem(Item item, String variantIn)
        {
            this.item = item;
            this.variantIn = variantIn;
        }

        private RegisterItem(Item item, int meta)
        {
            this.item = item;
            this.meta = meta;
        }

        private RegisterItem(Item item, String variantIn, int meta)
        {
            this.item = item;
            this.variantIn = variantIn;
            this.meta = meta;
        }

        public Item getItem()
        {
            return this.item;
        }

        public String getVariantIn()
        {
            return this.variantIn;
        }

        public int getMeta()
        {
            return this.meta;
        }
    }

    public static final ItemGraviton GRAVITON = new ItemGraviton();
    public static final ItemDarkMatter DARK_MATTER = new ItemDarkMatter();
    public static final ItemEnergyOrb ENERGY_ORB = new ItemEnergyOrb();
    public static final ItemHiggsBoson HIGGS_BOSON = new ItemHiggsBoson();
    public static final ItemProtein PROTEIN = new ItemProtein();
    public static final ItemSuperProtein SUPER_PROTEIN = new ItemSuperProtein();
    public static final ItemUltimateProtein ULTIMATE_PROTEIN = new ItemUltimateProtein();
    public static final ItemMashedVegetables MASHED_VEGETABLES = new ItemMashedVegetables();
    public static final ItemSuperMashedVegetables SUPER_MASHED_VEGETABLES = new ItemSuperMashedVegetables();
    public static final ItemUltimateMashedVegetables ULTIMATE_MASHED_VEGETABLES = new ItemUltimateMashedVegetables();
    public static final ItemNaturalPoison NATURAL_POISON = new ItemNaturalPoison();
    public static final ItemSuperPoison SUPER_POISON = new ItemSuperPoison();
    public static final ItemLiquidDeath LIQUID_DEATH = new ItemLiquidDeath();
    public static final ItemSpaceSoup SPACE_SOUP = new ItemSpaceSoup();
    public static final ItemCompressedMineral COMPRESSED_MINERAL = new ItemCompressedMineral();
    public static final ItemFarStar FAR_STAR = new ItemFarStar();
    public static final ItemGuideBook GUIDE_BOOK = new ItemGuideBook();

    public static final ItemAdvancementIcon ADV_ICON_BRIAN_FRY = new ItemAdvancementIcon("adv_icon_brian_fry");
    public static final ItemAdvancementIcon ADV_ICON_ROYAL_TASTER = new ItemAdvancementIcon("adv_icon_royal_taster");
    public static final ItemAdvancementIcon ADV_ICON_TASTE_DEATH = new ItemAdvancementIcon("adv_icon_taste_death");

    public static final RegisterItem[] modItems = {
            new RegisterItem(GRAVITON),
            new RegisterItem(DARK_MATTER),
            new RegisterItem(ENERGY_ORB),
            new RegisterItem(HIGGS_BOSON),
            new RegisterItem(PROTEIN),
            new RegisterItem(SUPER_PROTEIN),
            new RegisterItem(ULTIMATE_PROTEIN),
            new RegisterItem(MASHED_VEGETABLES),
            new RegisterItem(SUPER_MASHED_VEGETABLES),
            new RegisterItem(ULTIMATE_MASHED_VEGETABLES),
            new RegisterItem(NATURAL_POISON),
            new RegisterItem(SUPER_POISON),
            new RegisterItem(LIQUID_DEATH),
            new RegisterItem(SPACE_SOUP),
            new RegisterItem(COMPRESSED_MINERAL),
            new RegisterItem(FAR_STAR),
            new RegisterItem(GUIDE_BOOK),
            new RegisterItem(ADV_ICON_BRIAN_FRY),
            new RegisterItem(ADV_ICON_ROYAL_TASTER),
            new RegisterItem(ADV_ICON_TASTE_DEATH)
    };

    private ModItems()
    {
    }

    public static void registerItems()
    {
        for (RegisterItem modItem : modItems)
        {
            Item item = modItem.getItem();
            String name = item.getUnlocalizedName();
            if (name.startsWith("item."))
            {
                name = name.substring("item.".length());
            }
            setItemTexture(item, name);
            GameRegistry.registerItem(item, name);
        }
    }

    private static void setItemTexture(Item item, String name)
    {
        if (item == GUIDE_BOOK)
        {
            item.setTextureName("oneblockultima:obu_guide_book");
        }
        else if (item instanceof ItemAdvancementIcon)
        {
            String iconName = name.substring("adv_icon_".length());
            item.setTextureName("oneblockultima:advancements/adv_" + iconName);
        }
        else
        {
            item.setTextureName("oneblockultima:" + name);
        }
    }
}
package ru.defea.oneblockultima.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import ru.defea.oneblockultima.OneBlockUltima;

import java.util.HashMap;
import java.util.Map;

public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.Keys.ITEMS, OneBlockUltima.MODID);

    public static ItemGraviton GRAVITON;
    public static ItemDarkMatter DARK_MATTER;
    public static ItemEnergyOrb ENERGY_ORB;
    public static ItemHiggsBoson HIGGS_BOSON;
    public static ItemProtein PROTEIN;
    public static ItemSuperProtein SUPER_PROTEIN;
    public static ItemUltimateProtein ULTIMATE_PROTEIN;
    public static ItemMashedVegetables MASHED_VEGETABLES;
    public static ItemSuperMashedVegetables SUPER_MASHED_VEGETABLES;
    public static ItemUltimateMashedVegetables ULTIMATE_MASHED_VEGETABLES;
    public static ItemNaturalPoison NATURAL_POISON;
    public static ItemSuperPoison SUPER_POISON;
    public static ItemLiquidDeath LIQUID_DEATH;
    public static ItemSpaceSoup SPACE_SOUP;
    public static ItemCompressedMineral COMPRESSED_MINERAL;
    public static ItemFarStar FAR_STAR;
    public static ItemGuideBook GUIDE_BOOK;
    public static ItemCase CASE;
    public static ItemAdvancementIcon ADV_ICON_BRIAN_FRY;
    public static ItemAdvancementIcon ADV_ICON_ROYAL_TASTER;
    public static ItemAdvancementIcon ADV_ICON_TASTE_DEATH;

    private static final Map<String, RegistryObject<? extends Item>> ITEM_RO = new HashMap<>();

    static
    {
        ITEM_RO.put("graviton", ITEMS.register("graviton", ItemGraviton::new));
        ITEM_RO.put("dark_matter", ITEMS.register("dark_matter", ItemDarkMatter::new));
        ITEM_RO.put("energy_orb", ITEMS.register("energy_orb", ItemEnergyOrb::new));
        ITEM_RO.put("higgs_boson", ITEMS.register("higgs_boson", ItemHiggsBoson::new));
        ITEM_RO.put("protein", ITEMS.register("protein", ItemProtein::new));
        ITEM_RO.put("super_protein", ITEMS.register("super_protein", ItemSuperProtein::new));
        ITEM_RO.put("ultimate_protein", ITEMS.register("ultimate_protein", ItemUltimateProtein::new));
        ITEM_RO.put("mashed_vegetables", ITEMS.register("mashed_vegetables", ItemMashedVegetables::new));
        ITEM_RO.put("super_mashed_vegetables", ITEMS.register("super_mashed_vegetables", ItemSuperMashedVegetables::new));
        ITEM_RO.put("ultimate_mashed_vegetables", ITEMS.register("ultimate_mashed_vegetables", ItemUltimateMashedVegetables::new));
        ITEM_RO.put("natural_poison", ITEMS.register("natural_poison", ItemNaturalPoison::new));
        ITEM_RO.put("super_poison", ITEMS.register("super_poison", ItemSuperPoison::new));
        ITEM_RO.put("liquid_death", ITEMS.register("liquid_death", ItemLiquidDeath::new));
        ITEM_RO.put("space_soup", ITEMS.register("space_soup", ItemSpaceSoup::new));
        ITEM_RO.put("compressed_mineral", ITEMS.register("compressed_mineral", ItemCompressedMineral::new));
        ITEM_RO.put("far_star", ITEMS.register("far_star", ItemFarStar::new));
        ITEM_RO.put("obu_guide_book", ITEMS.register("obu_guide_book", ItemGuideBook::new));
        ITEM_RO.put("case", ITEMS.register("case", ItemCase::new));
        ITEM_RO.put("adv_icon_brian_fry", ITEMS.register("adv_icon_brian_fry", () -> new ItemAdvancementIcon("adv_icon_brian_fry")));
        ITEM_RO.put("adv_icon_royal_taster", ITEMS.register("adv_icon_royal_taster", () -> new ItemAdvancementIcon("adv_icon_royal_taster")));
        ITEM_RO.put("adv_icon_taste_death", ITEMS.register("adv_icon_taste_death", () -> new ItemAdvancementIcon("adv_icon_taste_death")));
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus)
    {
        ITEMS.register(bus);
        bus.addListener((RegisterEvent event) ->
        {
            if (event.getRegistryKey() == ForgeRegistries.Keys.ITEMS)
            {
                GRAVITON = (ItemGraviton) ITEM_RO.get("graviton").get();
                DARK_MATTER = (ItemDarkMatter) ITEM_RO.get("dark_matter").get();
                ENERGY_ORB = (ItemEnergyOrb) ITEM_RO.get("energy_orb").get();
                HIGGS_BOSON = (ItemHiggsBoson) ITEM_RO.get("higgs_boson").get();
                PROTEIN = (ItemProtein) ITEM_RO.get("protein").get();
                SUPER_PROTEIN = (ItemSuperProtein) ITEM_RO.get("super_protein").get();
                ULTIMATE_PROTEIN = (ItemUltimateProtein) ITEM_RO.get("ultimate_protein").get();
                MASHED_VEGETABLES = (ItemMashedVegetables) ITEM_RO.get("mashed_vegetables").get();
                SUPER_MASHED_VEGETABLES = (ItemSuperMashedVegetables) ITEM_RO.get("super_mashed_vegetables").get();
                ULTIMATE_MASHED_VEGETABLES = (ItemUltimateMashedVegetables) ITEM_RO.get("ultimate_mashed_vegetables").get();
                NATURAL_POISON = (ItemNaturalPoison) ITEM_RO.get("natural_poison").get();
                SUPER_POISON = (ItemSuperPoison) ITEM_RO.get("super_poison").get();
                LIQUID_DEATH = (ItemLiquidDeath) ITEM_RO.get("liquid_death").get();
                SPACE_SOUP = (ItemSpaceSoup) ITEM_RO.get("space_soup").get();
                COMPRESSED_MINERAL = (ItemCompressedMineral) ITEM_RO.get("compressed_mineral").get();
                FAR_STAR = (ItemFarStar) ITEM_RO.get("far_star").get();
                GUIDE_BOOK = (ItemGuideBook) ITEM_RO.get("obu_guide_book").get();
                CASE = (ItemCase) ITEM_RO.get("case").get();
                ADV_ICON_BRIAN_FRY = (ItemAdvancementIcon) ITEM_RO.get("adv_icon_brian_fry").get();
                ADV_ICON_ROYAL_TASTER = (ItemAdvancementIcon) ITEM_RO.get("adv_icon_royal_taster").get();
                ADV_ICON_TASTE_DEATH = (ItemAdvancementIcon) ITEM_RO.get("adv_icon_taste_death").get();
            }
        });
    }

    private ModItems()
    {
    }
}

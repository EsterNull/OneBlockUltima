package ru.defea.oneblockultima.achievement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ModItems;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.7.10 port of the 1.12.2 advancement tree (all "has item" / "consume item"
 * criteria from the original assets/oneblockultima/advancements/*.json).
 * The original used the vanilla advancement system which does not exist in
 * 1.7.10, so every advancement becomes a vanilla {@link Achievement}.
 * <p>
 * All achievements are placed on their own {@link AchievementPage}
 * ("OneBlockUltima"). In 1.7.10 the achievements screen shows one shared map,
 * so a dedicated page keeps the mod tree out of the vanilla page entirely
 * (no overlapping icons) while keeping the original compact coordinates.
 */
public final class ModAchievements
{
    public static Achievement ROOT;
    public static Achievement COMPRESSED_MINERAL;
    public static Achievement GRAVITON;
    public static Achievement DARK_MATTER;
    public static Achievement ENERGY_ORB;
    public static Achievement HIGGS_BOSON;
    public static Achievement SPACE_SOUP;
    public static Achievement FAR_STAR;
    public static Achievement ONE_BLOCK_GENERATOR_CRAFTED;
    public static Achievement MASHED_VEGETABLES;
    public static Achievement SUPER_MASHED_VEGETABLES;
    public static Achievement ULTIMATE_MASHED_VEGETABLES;
    public static Achievement PROTEIN;
    public static Achievement SUPER_PROTEIN;
    public static Achievement ULTIMATE_PROTEIN;
    public static Achievement NATURAL_POISON;
    public static Achievement BRIAN_FRY;
    public static Achievement SUPER_POISON;
    public static Achievement ROYAL_TASTER;
    public static Achievement LIQUID_DEATH;
    public static Achievement TASTE_DEATH;

    private static final Map<Item, Achievement> ITEM_ACHIEVEMENTS = new HashMap<>();
    private static final Map<Item, Achievement> CONSUME_ACHIEVEMENTS = new HashMap<>();

    private static boolean initialized;

    private ModAchievements()
    {
    }

    /**
     * Registers all achievements. Must be called after items/blocks are
     * registered (FMLInitializationEvent), on both client and server.
     */
    public static void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        ROOT = create("root", 0, 0, ModItems.GUIDE_BOOK, null, false);
        COMPRESSED_MINERAL = create("compressed_mineral", 2, 0, ModItems.COMPRESSED_MINERAL, ROOT, false);
        GRAVITON = create("graviton", 3, 0, ModItems.GRAVITON, COMPRESSED_MINERAL, false);
        DARK_MATTER = create("dark_matter", 4, 0, ModItems.DARK_MATTER, GRAVITON, true);
        ENERGY_ORB = create("energy_orb", 2, -1, ModItems.ENERGY_ORB, ROOT, false);
        HIGGS_BOSON = create("higgs_boson", 2, 1, ModItems.HIGGS_BOSON, ROOT, false);
        SPACE_SOUP = create("space_soup", 1, -1, ModItems.SPACE_SOUP, ROOT, false);
        FAR_STAR = create("far_star", 2, -2, ModItems.FAR_STAR, SPACE_SOUP, true);
        ONE_BLOCK_GENERATOR_CRAFTED = create("one_block_generator_crafted", 3, -2,
                Item.getItemFromBlock(ModBlocks.ONE_BLOCK_GENERATOR), FAR_STAR, true);
        MASHED_VEGETABLES = create("mashed_vegetables", -1, -1, ModItems.MASHED_VEGETABLES, ROOT, false);
        SUPER_MASHED_VEGETABLES = create("super_mashed_vegetables", -2, -1,
                ModItems.SUPER_MASHED_VEGETABLES, MASHED_VEGETABLES, false);
        ULTIMATE_MASHED_VEGETABLES = create("ultimate_mashed_vegetables", -3, -1,
                ModItems.ULTIMATE_MASHED_VEGETABLES, SUPER_MASHED_VEGETABLES, false);
        PROTEIN = create("protein", -1, 1, ModItems.PROTEIN, ROOT, false);
        SUPER_PROTEIN = create("super_protein", -2, 1, ModItems.SUPER_PROTEIN, PROTEIN, false);
        ULTIMATE_PROTEIN = create("ultimate_protein", -3, 1, ModItems.ULTIMATE_PROTEIN, SUPER_PROTEIN, false);
        NATURAL_POISON = create("natural_poison", 1, 1, ModItems.NATURAL_POISON, ROOT, false);
        BRIAN_FRY = create("brian_fry", 1, 2, ModItems.ADV_ICON_BRIAN_FRY, NATURAL_POISON, false);
        SUPER_POISON = create("super_poison", 2, 2, ModItems.SUPER_POISON, NATURAL_POISON, false);
        ROYAL_TASTER = create("royal_taster", 2, 3, ModItems.ADV_ICON_ROYAL_TASTER, SUPER_POISON, false);
        LIQUID_DEATH = create("liquid_death", 3, 2, ModItems.LIQUID_DEATH, SUPER_POISON, false);
        TASTE_DEATH = create("taste_death", 3, 3, ModItems.ADV_ICON_TASTE_DEATH, LIQUID_DEATH, false);

        ITEM_ACHIEVEMENTS.clear();
        ITEM_ACHIEVEMENTS.put(ModItems.GUIDE_BOOK, ROOT);
        ITEM_ACHIEVEMENTS.put(ModItems.COMPRESSED_MINERAL, COMPRESSED_MINERAL);
        ITEM_ACHIEVEMENTS.put(ModItems.GRAVITON, GRAVITON);
        ITEM_ACHIEVEMENTS.put(ModItems.DARK_MATTER, DARK_MATTER);
        ITEM_ACHIEVEMENTS.put(ModItems.ENERGY_ORB, ENERGY_ORB);
        ITEM_ACHIEVEMENTS.put(ModItems.HIGGS_BOSON, HIGGS_BOSON);
        ITEM_ACHIEVEMENTS.put(ModItems.SPACE_SOUP, SPACE_SOUP);
        ITEM_ACHIEVEMENTS.put(ModItems.FAR_STAR, FAR_STAR);
        ITEM_ACHIEVEMENTS.put(Item.getItemFromBlock(ModBlocks.ONE_BLOCK_GENERATOR), ONE_BLOCK_GENERATOR_CRAFTED);
        ITEM_ACHIEVEMENTS.put(ModItems.MASHED_VEGETABLES, MASHED_VEGETABLES);
        ITEM_ACHIEVEMENTS.put(ModItems.SUPER_MASHED_VEGETABLES, SUPER_MASHED_VEGETABLES);
        ITEM_ACHIEVEMENTS.put(ModItems.ULTIMATE_MASHED_VEGETABLES, ULTIMATE_MASHED_VEGETABLES);
        ITEM_ACHIEVEMENTS.put(ModItems.PROTEIN, PROTEIN);
        ITEM_ACHIEVEMENTS.put(ModItems.SUPER_PROTEIN, SUPER_PROTEIN);
        ITEM_ACHIEVEMENTS.put(ModItems.ULTIMATE_PROTEIN, ULTIMATE_PROTEIN);
        ITEM_ACHIEVEMENTS.put(ModItems.NATURAL_POISON, NATURAL_POISON);
        ITEM_ACHIEVEMENTS.put(ModItems.SUPER_POISON, SUPER_POISON);
        ITEM_ACHIEVEMENTS.put(ModItems.LIQUID_DEATH, LIQUID_DEATH);

        CONSUME_ACHIEVEMENTS.clear();
        CONSUME_ACHIEVEMENTS.put(ModItems.NATURAL_POISON, BRIAN_FRY);
        CONSUME_ACHIEVEMENTS.put(ModItems.SUPER_POISON, ROYAL_TASTER);
        CONSUME_ACHIEVEMENTS.put(ModItems.LIQUID_DEATH, TASTE_DEATH);

        AchievementPage.registerAchievementPage(new AchievementPage("OneBlockUltima",
                ROOT, COMPRESSED_MINERAL, GRAVITON, DARK_MATTER, ENERGY_ORB, HIGGS_BOSON, SPACE_SOUP,
                FAR_STAR, ONE_BLOCK_GENERATOR_CRAFTED, MASHED_VEGETABLES, SUPER_MASHED_VEGETABLES,
                ULTIMATE_MASHED_VEGETABLES, PROTEIN, SUPER_PROTEIN, ULTIMATE_PROTEIN, NATURAL_POISON,
                BRIAN_FRY, SUPER_POISON, ROYAL_TASTER, LIQUID_DEATH, TASTE_DEATH));
    }

    private static Achievement create(String name, int column, int row, Item icon, Achievement parent, boolean special)
    {
        Achievement achievement = new Achievement("achievement.oneblock." + name, name, column, row, icon, parent);
        if (special)
        {
            achievement.setSpecial();
        }
        return achievement.registerStat();
    }

    /**
     * Grants the achievement the given item maps to. Called from the server-side
     * inventory scan; safe to call repeatedly because already-unlocked
     * achievements are skipped.
     */
    public static void unlockByItem(EntityPlayer player, Item item)
    {
        unlock(player, ITEM_ACHIEVEMENTS.get(item));
    }

    /**
     * Grants the achievement for consuming (drinking) the given potion item.
     */
    public static void unlockByConsume(EntityPlayer player, Item consumed)
    {
        unlock(player, CONSUME_ACHIEVEMENTS.get(consumed));
    }

    /**
     * Returns the achievement granted by holding the given item, or null.
     */
    public static Achievement getAchievementForItem(Item item)
    {
        return ITEM_ACHIEVEMENTS.get(item);
    }

    /**
     * Returns the achievement granted by consuming the given item, or null.
     */
    public static Achievement getAchievementForConsume(Item item)
    {
        return CONSUME_ACHIEVEMENTS.get(item);
    }

    /**
     * Periodic server-side check for "inventory_changed"-style achievements.
     * Runs a cheap scan of the player's inventory; achievements whose parent is
     * not yet unlocked are silently skipped by vanilla and retried next scan.
     */
    public static void tickPlayer(EntityPlayer player)
    {
        if (player == null || player.ticksExisted % 10 != 0 || player.worldObj == null || player.worldObj.isRemote)
        {
            return;
        }
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++)
        {
            net.minecraft.item.ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack != null)
            {
                unlockByItem(player, stack.getItem());
            }
        }
    }

    private static void unlock(EntityPlayer player, Achievement achievement)
    {
        if (player == null || achievement == null || player.worldObj.isRemote)
        {
            return;
        }
        if (player instanceof EntityPlayerMP)
        {
            if (((EntityPlayerMP) player).getStatFile().hasAchievementUnlocked(achievement))
            {
                return;
            }
        }
        player.addStat(achievement, 1);
    }
}

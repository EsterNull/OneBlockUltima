package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraftforge.common.AchievementPage;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.achievement.ModAchievements;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ModItems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Guards the 1.7.10 port of the 1.12.2 advancement tree: every advancement from
 * the original assets/oneblockultima/advancements/*.json must be registered as a
 * vanilla Achievement, with the original parent relationships and item mappings.
 */
public class ModAchievementsTest
{
    private static int baselineAchievementCount;

    @BeforeClass
    public static void setup()
    {
        TestFMLHooks.installLoader();
        Bootstrap.register();
        ModItems.registerItems();
        baselineAchievementCount = AchievementList.achievementList.size();
        ModAchievements.init();
    }

    @Test
    public void registersAllTwentyOneOriginalAdvancements()
    {
        assertEquals(21, AchievementList.achievementList.size() - baselineAchievementCount);
    }

    @Test
    public void rootHasNoParentAndEachOriginalParentLinkIsPreserved()
    {
        assertNull(ModAchievements.ROOT.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.COMPRESSED_MINERAL.parentAchievement);
        assertSame(ModAchievements.COMPRESSED_MINERAL, ModAchievements.GRAVITON.parentAchievement);
        assertSame(ModAchievements.GRAVITON, ModAchievements.DARK_MATTER.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.ENERGY_ORB.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.HIGGS_BOSON.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.SPACE_SOUP.parentAchievement);
        assertSame(ModAchievements.SPACE_SOUP, ModAchievements.FAR_STAR.parentAchievement);
        assertSame(ModAchievements.FAR_STAR, ModAchievements.ONE_BLOCK_GENERATOR_CRAFTED.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.MASHED_VEGETABLES.parentAchievement);
        assertSame(ModAchievements.MASHED_VEGETABLES, ModAchievements.SUPER_MASHED_VEGETABLES.parentAchievement);
        assertSame(ModAchievements.SUPER_MASHED_VEGETABLES, ModAchievements.ULTIMATE_MASHED_VEGETABLES.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.PROTEIN.parentAchievement);
        assertSame(ModAchievements.PROTEIN, ModAchievements.SUPER_PROTEIN.parentAchievement);
        assertSame(ModAchievements.SUPER_PROTEIN, ModAchievements.ULTIMATE_PROTEIN.parentAchievement);
        assertSame(ModAchievements.ROOT, ModAchievements.NATURAL_POISON.parentAchievement);
        assertSame(ModAchievements.NATURAL_POISON, ModAchievements.BRIAN_FRY.parentAchievement);
        assertSame(ModAchievements.NATURAL_POISON, ModAchievements.SUPER_POISON.parentAchievement);
        assertSame(ModAchievements.SUPER_POISON, ModAchievements.ROYAL_TASTER.parentAchievement);
        assertSame(ModAchievements.SUPER_POISON, ModAchievements.LIQUID_DEATH.parentAchievement);
        assertSame(ModAchievements.LIQUID_DEATH, ModAchievements.TASTE_DEATH.parentAchievement);
    }

    @Test
    public void mapsEveryHeldItemToItsOriginalAchievement()
    {
        assertNotNull(ModAchievements.ROOT);
        assertSame(ModAchievements.ROOT, ModAchievements.getAchievementForItem(ModItems.GUIDE_BOOK));
        assertSame(ModAchievements.COMPRESSED_MINERAL, ModAchievements.getAchievementForItem(ModItems.COMPRESSED_MINERAL));
        assertSame(ModAchievements.GRAVITON, ModAchievements.getAchievementForItem(ModItems.GRAVITON));
        assertSame(ModAchievements.DARK_MATTER, ModAchievements.getAchievementForItem(ModItems.DARK_MATTER));
        assertSame(ModAchievements.ENERGY_ORB, ModAchievements.getAchievementForItem(ModItems.ENERGY_ORB));
        assertSame(ModAchievements.HIGGS_BOSON, ModAchievements.getAchievementForItem(ModItems.HIGGS_BOSON));
        assertSame(ModAchievements.SPACE_SOUP, ModAchievements.getAchievementForItem(ModItems.SPACE_SOUP));
        assertSame(ModAchievements.FAR_STAR, ModAchievements.getAchievementForItem(ModItems.FAR_STAR));
        assertSame(ModAchievements.ONE_BLOCK_GENERATOR_CRAFTED,
                ModAchievements.getAchievementForItem(Item.getItemFromBlock(ModBlocks.ONE_BLOCK_GENERATOR)));
        assertSame(ModAchievements.MASHED_VEGETABLES, ModAchievements.getAchievementForItem(ModItems.MASHED_VEGETABLES));
        assertSame(ModAchievements.SUPER_MASHED_VEGETABLES,
                ModAchievements.getAchievementForItem(ModItems.SUPER_MASHED_VEGETABLES));
        assertSame(ModAchievements.ULTIMATE_MASHED_VEGETABLES,
                ModAchievements.getAchievementForItem(ModItems.ULTIMATE_MASHED_VEGETABLES));
        assertSame(ModAchievements.PROTEIN, ModAchievements.getAchievementForItem(ModItems.PROTEIN));
        assertSame(ModAchievements.SUPER_PROTEIN, ModAchievements.getAchievementForItem(ModItems.SUPER_PROTEIN));
        assertSame(ModAchievements.ULTIMATE_PROTEIN, ModAchievements.getAchievementForItem(ModItems.ULTIMATE_PROTEIN));
        assertSame(ModAchievements.NATURAL_POISON, ModAchievements.getAchievementForItem(ModItems.NATURAL_POISON));
        assertSame(ModAchievements.SUPER_POISON, ModAchievements.getAchievementForItem(ModItems.SUPER_POISON));
        assertSame(ModAchievements.LIQUID_DEATH, ModAchievements.getAchievementForItem(ModItems.LIQUID_DEATH));
    }

    @Test
    public void mapsEveryDrinkablePotionToItsOriginalAchievement()
    {
        assertSame(ModAchievements.BRIAN_FRY, ModAchievements.getAchievementForConsume(ModItems.NATURAL_POISON));
        assertSame(ModAchievements.ROYAL_TASTER, ModAchievements.getAchievementForConsume(ModItems.SUPER_POISON));
        assertSame(ModAchievements.TASTE_DEATH, ModAchievements.getAchievementForConsume(ModItems.LIQUID_DEATH));
    }

    @Test
    public void modAchievementsLiveOnTheirOwnAchievementPage()
    {
        Achievement[] modAchievements = new Achievement[] {
                ModAchievements.ROOT,
                ModAchievements.COMPRESSED_MINERAL,
                ModAchievements.GRAVITON,
                ModAchievements.DARK_MATTER,
                ModAchievements.ENERGY_ORB,
                ModAchievements.HIGGS_BOSON,
                ModAchievements.SPACE_SOUP,
                ModAchievements.FAR_STAR,
                ModAchievements.ONE_BLOCK_GENERATOR_CRAFTED,
                ModAchievements.MASHED_VEGETABLES,
                ModAchievements.SUPER_MASHED_VEGETABLES,
                ModAchievements.ULTIMATE_MASHED_VEGETABLES,
                ModAchievements.PROTEIN,
                ModAchievements.SUPER_PROTEIN,
                ModAchievements.ULTIMATE_PROTEIN,
                ModAchievements.NATURAL_POISON,
                ModAchievements.BRIAN_FRY,
                ModAchievements.SUPER_POISON,
                ModAchievements.ROYAL_TASTER,
                ModAchievements.LIQUID_DEATH,
                ModAchievements.TASTE_DEATH
        };

        AchievementPage page = AchievementPage.getAchievementPage("OneBlockUltima");
        assertNotNull("mod achievements must be registered on their own AchievementPage", page);
        assertEquals(21, page.getAchievements().size());

        for (Achievement a : modAchievements)
        {
            assertTrue("achievement " + a.statId + " must be listed on the mod page",
                    page.getAchievements().contains(a));
            assertTrue("achievement " + a.statId + " must be removed from the vanilla page",
                    AchievementPage.isAchievementInPages(a));
        }
    }

    @Test
    public void goalAndChallengeFramesAreMarkedSpecial()
    {
        assertTrue(ModAchievements.DARK_MATTER.getSpecial());
        assertTrue(ModAchievements.FAR_STAR.getSpecial());
        assertTrue(ModAchievements.ONE_BLOCK_GENERATOR_CRAFTED.getSpecial());
    }

    @Test
    public void initIsIdempotent()
    {
        int before = AchievementList.achievementList.size();
        ModAchievements.init();
        assertEquals(before, AchievementList.achievementList.size());
    }
}

package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ItemBlockCompressed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression checks for the 1.7.10 compressed-block tint.
 *
 * <p>1.7.10 has no Forge colour-handler API, so the grayscale tint is applied
 * through the vanilla {@code Block#getRenderColor(int)} /
 * {@code Block#colorMultiplier(IBlockAccess, int, int, int)} hooks (in-world
 * only). The item carries no tint ({@code Item#getColorFromItemStack} stays
 * white), so GUI/inventory icons render at full brightness while placed blocks
 * in the world keep the per-level darkening. The factors mirror the 1.12.2
 * {@code CompressedBlockTints} table, keyed by base name.
 */
public class BlockCompressedBaseTintTest
{
    private static World world;

    @BeforeClass
    public static void setUp()
    {
        Bootstrap.register();
        world = TestDummyWorld.newWorld(false);
    }

    private static BlockCompressedBase[] allBlocks()
    {
        return new BlockCompressedBase[] {
                ModBlocks.COMPRESSED_BEDROCK,
                ModBlocks.COMPRESSED_REDSTONE_BLOCK,
                ModBlocks.COMPRESSED_GOLD_BLOCK,
                ModBlocks.COMPRESSED_IRON_BLOCK,
                ModBlocks.COMPRESSED_DIAMOND_BLOCK,
                ModBlocks.COMPRESSED_STONE_BLOCK,
                ModBlocks.COMPRESSED_EMERALD_BLOCK,
                ModBlocks.COMPRESSED_LAPIS_BLOCK,
                ModBlocks.COMPRESSED_END_STONE,
                ModBlocks.COMPRESSED_NETHERRACK
        };
    }

    @Test
    public void bedrockFirstLevelMatchesFactor()
    {
        assertEquals(0xFFC1C1C1, ModBlocks.COMPRESSED_BEDROCK.getRenderColor(0));
        assertEquals(0xFF8B8B8B, ModBlocks.COMPRESSED_BEDROCK.getRenderColor(1));
        assertEquals(0xFF656565, ModBlocks.COMPRESSED_BEDROCK.getRenderColor(2));
        assertEquals(0xFF4B4B4B, ModBlocks.COMPRESSED_BEDROCK.getRenderColor(3));
    }

    @Test
    public void tintIsGrayscaleOpaqueAndStrictlyDarkeningPerLevel()
    {
        for (BlockCompressedBase block : allBlocks())
        {
            int prev = Integer.MAX_VALUE;
            for (int meta = 0; meta < block.getMaxLevel(); meta++)
            {
                int color = block.getRenderColor(meta);
                int r = (color >> 16) & 255;
                int g = (color >> 8) & 255;
                int b = color & 255;
                assertEquals("alpha for " + block.getUnlocalizedName() + " meta " + meta, 0xFF, (color >> 24) & 255);
                assertEquals("r==g for " + block.getUnlocalizedName() + " meta " + meta, r, g);
                assertEquals("r==b for " + block.getUnlocalizedName() + " meta " + meta, r, b);
                assertTrue("darkening for " + block.getUnlocalizedName() + " meta " + meta, r < prev);
                prev = r;
            }
        }
    }

    @Test
    public void everyLevelIsTintedDark()
    {
        for (BlockCompressedBase block : allBlocks())
        {
            for (int meta = 0; meta < block.getMaxLevel(); meta++)
            {
                int color = block.getRenderColor(meta);
                int r = (color >> 16) & 255;
                assertTrue("level " + meta + " of " + block.getUnlocalizedName() + " must be darker than white", r < 255);
            }
        }
    }

    @Test
    public void colorMultiplierFollowsWorldMetadata()
    {
        int worldColor = ModBlocks.COMPRESSED_DIAMOND_BLOCK.colorMultiplier(world, 5, 70, 5);
        assertEquals(ModBlocks.COMPRESSED_DIAMOND_BLOCK.getRenderColor(world.getBlockMetadata(5, 70, 5)), worldColor);
        assertEquals(0xFFBABABA, worldColor);
    }

    @Test
    public void itemIconIsFullBrightness()
    {
        ItemBlockCompressed item = new ItemBlockCompressed(ModBlocks.COMPRESSED_BEDROCK, "compressed_bedrock", ModBlocks.COMPRESSED_BEDROCK.getMaxLevel());
        for (int meta = 0; meta < ModBlocks.COMPRESSED_BEDROCK.getMaxLevel(); meta++)
        {
            ItemStack stack = new ItemStack(item, 1, meta);
            assertEquals("icon of meta " + meta + " must be full brightness", 0x00FFFFFF, item.getColorFromItemStack(stack, 0));
        }
    }
}

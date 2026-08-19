package ru.defea.oneblockultima.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;

import java.util.Arrays;

public final class ModRecipes
{
    private static final class Material
    {
        private final BlockCompressedBase block;
        private final String baseName;
        private final String capName;
        private final int maxLevel;
        private final ItemStack baseItem;
        private final String baseOre;

        private Material(BlockCompressedBase block, String baseName, String capName, ItemStack baseItem, String baseOre)
        {
            this.block = block;
            this.baseName = baseName;
            this.capName = capName;
            this.maxLevel = block.getMaxLevel();
            this.baseItem = baseItem;
            this.baseOre = baseOre;
        }
    }

    private static final Material[] MATERIALS = {
        new Material(ModBlocks.COMPRESSED_BEDROCK, "bedrock", "Bedrock", new ItemStack(Blocks.bedrock), null),
        new Material(ModBlocks.COMPRESSED_REDSTONE_BLOCK, "redstone_block", "RedstoneBlock", new ItemStack(Blocks.redstone_block), "blockRedstone"),
        new Material(ModBlocks.COMPRESSED_GOLD_BLOCK, "gold_block", "GoldBlock", new ItemStack(Blocks.gold_block), "blockGold"),
        new Material(ModBlocks.COMPRESSED_IRON_BLOCK, "iron_block", "IronBlock", new ItemStack(Blocks.iron_block), "blockIron"),
        new Material(ModBlocks.COMPRESSED_DIAMOND_BLOCK, "diamond_block", "DiamondBlock", new ItemStack(Blocks.diamond_block), "blockDiamond"),
        new Material(ModBlocks.COMPRESSED_STONE_BLOCK, "stone_block", "Stone", new ItemStack(Blocks.stone), "stone"),
        new Material(ModBlocks.COMPRESSED_EMERALD_BLOCK, "emerald_block", "EmeraldBlock", new ItemStack(Blocks.emerald_block), "blockEmerald"),
        new Material(ModBlocks.COMPRESSED_LAPIS_BLOCK, "lapis_block", "LapisBlock", new ItemStack(Blocks.lapis_block), "blockLapis"),
        new Material(ModBlocks.COMPRESSED_END_STONE, "end_stone", "EndStone", new ItemStack(Blocks.end_stone), null),
        new Material(ModBlocks.COMPRESSED_NETHERRACK, "netherrack", "Netherrack", new ItemStack(Blocks.netherrack), null)
    };

    private ModRecipes()
    {
    }

    public static void registerRecipes()
    {
        for (Material mat : MATERIALS)
        {
            for (int level = 1; level <= mat.maxLevel; level++)
            {
                GameRegistry.addRecipe(createCompress(mat, level));
                GameRegistry.addRecipe(createUncompress(mat, level));
            }
        }
    }

    private static ShapelessOreRecipe createCompress(Material mat, int level)
    {
        ItemStack result = new ItemStack(mat.block, 1, level - 1);
        Object[] ingredients = new Object[9];
        if (level == 1)
        {
            Object base = mat.baseOre != null ? mat.baseOre : mat.baseItem;
            Arrays.fill(ingredients, base);
        }
        else
        {
            String ore = "compressed" + (level - 1) + "x" + mat.capName;
            Arrays.fill(ingredients, ore);
        }
        return new ShapelessOreRecipe(result, ingredients);
    }

    private static ShapelessOreRecipe createUncompress(Material mat, int level)
    {
        ItemStack result;
        if (level == 1)
        {
            result = mat.baseItem.copy();
            result.stackSize = 9;
        }
        else
        {
            result = new ItemStack(mat.block, 9, level - 2);
        }
        return new ShapelessOreRecipe(result, "compressed" + level + "x" + mat.capName);
    }
}

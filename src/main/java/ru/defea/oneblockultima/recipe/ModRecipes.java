package ru.defea.oneblockultima.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;

@Mod.EventBusSubscriber(modid = OneBlockUltima.MODID)
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
        new Material(ModBlocks.COMPRESSED_BEDROCK, "bedrock", "Bedrock", new ItemStack(Blocks.BEDROCK), null),
        new Material(ModBlocks.COMPRESSED_REDSTONE_BLOCK, "redstone_block", "RedstoneBlock", new ItemStack(Blocks.REDSTONE_BLOCK), "blockRedstone"),
        new Material(ModBlocks.COMPRESSED_GOLD_BLOCK, "gold_block", "GoldBlock", new ItemStack(Blocks.GOLD_BLOCK), "blockGold"),
        new Material(ModBlocks.COMPRESSED_IRON_BLOCK, "iron_block", "IronBlock", new ItemStack(Blocks.IRON_BLOCK), "blockIron"),
        new Material(ModBlocks.COMPRESSED_DIAMOND_BLOCK, "diamond_block", "DiamondBlock", new ItemStack(Blocks.DIAMOND_BLOCK), "blockDiamond"),
        new Material(ModBlocks.COMPRESSED_STONE_BLOCK, "stone_block", "Stone", new ItemStack(Blocks.STONE), "stone"),
        new Material(ModBlocks.COMPRESSED_EMERALD_BLOCK, "emerald_block", "EmeraldBlock", new ItemStack(Blocks.EMERALD_BLOCK), "blockEmerald"),
        new Material(ModBlocks.COMPRESSED_LAPIS_BLOCK, "lapis_block", "LapisBlock", new ItemStack(Blocks.LAPIS_BLOCK), "blockLapis"),
        new Material(ModBlocks.COMPRESSED_END_STONE, "end_stone", "EndStone", new ItemStack(Blocks.END_STONE), "endstone"),
        new Material(ModBlocks.COMPRESSED_NETHERRACK, "netherrack", "Netherrack", new ItemStack(Blocks.NETHERRACK), "netherrack")
    };

    private ModRecipes()
    {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event)
    {
        for (Material mat : MATERIALS)
        {
            for (int level = 1; level <= mat.maxLevel; level++)
            {
                registerCompress(event, mat, level);
                registerUncompress(event, mat, level);
            }
        }
    }

    private static void registerCompress(RegistryEvent.Register<IRecipe> event, Material mat, int level)
    {
        ItemStack result = new ItemStack(mat.block, 1, level - 1);
        Object[] ingredients = new Object[9];
        if (level == 1)
        {
            Object base = mat.baseOre != null ? (Object) mat.baseOre : mat.baseItem;
            for (int i = 0; i < ingredients.length; i++)
            {
                ingredients[i] = base;
            }
        }
        else
        {
            String ore = "compressed" + (level - 1) + "x" + mat.capName;
            for (int i = 0; i < ingredients.length; i++)
            {
                ingredients[i] = ore;
            }
        }
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(null, result, ingredients);
        recipe.setRegistryName(new ResourceLocation(OneBlockUltima.MODID, "compressed_" + mat.baseName + "_" + level + "x"));
        event.getRegistry().register(recipe);
    }

    private static void registerUncompress(RegistryEvent.Register<IRecipe> event, Material mat, int level)
    {
        ItemStack result;
        if (level == 1)
        {
            result = mat.baseItem.copy();
            result.setCount(9);
        }
        else
        {
            result = new ItemStack(mat.block, 9, level - 2);
        }
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(null, result, "compressed" + level + "x" + mat.capName);
        recipe.setRegistryName(new ResourceLocation(OneBlockUltima.MODID, "uncompress_" + mat.baseName + "_" + level + "x"));
        event.getRegistry().register(recipe);
    }
}

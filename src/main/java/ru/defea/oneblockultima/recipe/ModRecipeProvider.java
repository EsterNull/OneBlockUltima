package ru.defea.oneblockultima.recipe;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends net.minecraft.data.recipes.RecipeProvider
{
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output)
    {
        for (Material mat : buildMaterials())
        {
            for (int level = 1; level <= mat.maxLevel; level++)
            {
                registerCompress(output, mat, level);
                registerUncompress(output, mat, level);
            }
        }
        buildModItemRecipes(output);
    }

    private static final class Material
    {
        private final BlockCompressedBase block;
        private final String baseName;
        private final int maxLevel;
        private final ItemStack baseItem;

        private Material(BlockCompressedBase block, String baseName, ItemStack baseItem)
        {
            this.block = block;
            this.baseName = baseName;
            this.maxLevel = block.getMaxLevel();
            this.baseItem = baseItem;
        }
    }

    private static Material[] buildMaterials()
    {
        return new Material[] {
            new Material(ModBlocks.COMPRESSED_BEDROCK, "bedrock", new ItemStack(Blocks.BEDROCK)),
            new Material(ModBlocks.COMPRESSED_REDSTONE_BLOCK, "redstone_block", new ItemStack(Blocks.REDSTONE_BLOCK)),
            new Material(ModBlocks.COMPRESSED_GOLD_BLOCK, "gold_block", new ItemStack(Blocks.GOLD_BLOCK)),
            new Material(ModBlocks.COMPRESSED_IRON_BLOCK, "iron_block", new ItemStack(Blocks.IRON_BLOCK)),
            new Material(ModBlocks.COMPRESSED_DIAMOND_BLOCK, "diamond_block", new ItemStack(Blocks.DIAMOND_BLOCK)),
            new Material(ModBlocks.COMPRESSED_STONE_BLOCK, "stone_block", new ItemStack(Blocks.STONE)),
            new Material(ModBlocks.COMPRESSED_EMERALD_BLOCK, "emerald_block", new ItemStack(Blocks.EMERALD_BLOCK)),
            new Material(ModBlocks.COMPRESSED_LAPIS_BLOCK, "lapis_block", new ItemStack(Blocks.LAPIS_BLOCK)),
            new Material(ModBlocks.COMPRESSED_END_STONE, "end_stone", new ItemStack(Blocks.END_STONE)),
            new Material(ModBlocks.COMPRESSED_NETHERRACK, "netherrack", new ItemStack(Blocks.NETHERRACK))
        };
    }

    private void registerCompress(RecipeOutput output, Material mat, int level)
    {
        Item result = ModBlocks.getCompressedItem(mat.block, level - 1);
        ItemStack input = (level == 1) ? mat.baseItem : new ItemStack(ModBlocks.getCompressedItem(mat.block, level - 2));
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 1);
        for (int i = 0; i < 9; i++) builder.requires(Ingredient.of(input));
        builder.unlockedBy("has_" + mat.baseName, InventoryChangeTrigger.TriggerInstance.hasItems(mat.baseItem.getItem()));
        builder.save(output, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "compress_" + mat.baseName + "_" + level));
    }

    private void registerUncompress(RecipeOutput output, Material mat, int level)
    {
        ItemStack result = (level == 1) ? mat.baseItem.copy() : new ItemStack(ModBlocks.getCompressedItem(mat.block, level - 2));
        result.setCount(9);
        Item input = ModBlocks.getCompressedItem(mat.block, level - 1);
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result.getItem(), 9);
        for (int i = 0; i < 9; i++) builder.requires(Ingredient.of(input));
        builder.unlockedBy("has_" + mat.baseName + "_" + level, InventoryChangeTrigger.TriggerInstance.hasItems(input));
        builder.save(output, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "uncompress_" + mat.baseName + "_" + level));
    }

    private Item mod(String name)
    {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, name));
        if (item == null) throw new IllegalStateException("Recipe item not found: " + name);
        return item;
    }

    private Ingredient ing(String name)
    {
        return Ingredient.of(mod(name));
    }

    private Ingredient ing(net.minecraft.world.item.Item item)
    {
        return Ingredient.of(item);
    }

    private void shaped(RecipeOutput output, String id, Item result, String[] pattern, Object... keys)
    {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result);
        for (String p : pattern) builder.pattern(p);
        for (int i = 0; i < keys.length; i += 2)
        {
            builder.define((Character) keys[i], (Ingredient) keys[i + 1]);
        }
        builder.unlockedBy("has_" + id, InventoryChangeTrigger.TriggerInstance.hasItems(result));
        builder.save(output, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, id));
    }

    private void shapeless(RecipeOutput output, String id, Item result, Ingredient... ings)
    {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result);
        for (Ingredient i : ings) builder.requires(i);
        builder.unlockedBy("has_" + id, InventoryChangeTrigger.TriggerInstance.hasItems(result));
        builder.save(output, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, id));
    }

    private void shapelessMulti(RecipeOutput output, String id, Item result, Ingredient ing, int count)
    {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result);
        for (int i = 0; i < count; i++) builder.requires(ing);
        builder.unlockedBy("has_" + id, InventoryChangeTrigger.TriggerInstance.hasItems(result));
        builder.save(output, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, id));
    }

    private void buildModItemRecipes(RecipeOutput output)
    {
        // compressed_mineral: one of each high-tier compressed block
        shapeless(output, "compressed_mineral", mod("compressed_mineral"),
                ing("compressed_redstone_block_5"), ing("compressed_lapis_block_5"),
                ing("compressed_diamond_block_4"), ing("compressed_gold_block_4"),
                ing("compressed_iron_block_4"), ing("compressed_end_stone_5"),
                ing("compressed_emerald_block_4"), ing("compressed_stone_block_5"),
                ing("compressed_netherrack_5"));

        shapelessMulti(output, "compressed_mineral_block", mod("compressed_mineral_block"), ing("compressed_mineral"), 9);

        shapelessMulti(output, "dark_matter", mod("dark_matter"), ing("graviton"), 9);

        shaped(output, "energy_orb", mod("energy_orb"),
                new String[] { "RRR", "RTR", "RRR" },
                'R', ing("compressed_redstone_block_5"), 'T', ing(Items.TNT));

        shaped(output, "far_star_1", mod("far_star"),
                new String[] { "EHE", "HSH", "EHE" },
                'E', ing("energy_orb"), 'H', ing("higgs_boson"), 'S', ing(Items.NETHER_STAR));
        shaped(output, "far_star_2", mod("far_star"),
                new String[] { "HEH", "ESE", "HEH" },
                'E', ing("energy_orb"), 'H', ing("higgs_boson"), 'S', ing(Items.NETHER_STAR));

        shaped(output, "graviton", mod("graviton"),
                new String[] { "BBB", "BMB", "BBB" },
                'B', ing("compressed_bedrock_3"), 'M', ing("compressed_mineral_block"));

        shaped(output, "higgs_boson", mod("higgs_boson"),
                new String[] { "DGD", "GDG", "DGD" },
                'D', ing("compressed_diamond_block_4"), 'G', ing("compressed_gold_block_4"));

        shaped(output, "liquid_death", mod("liquid_death"),
                new String[] { "SSS", "SRS", "SSS" },
                'S', ing("super_poison"), 'R', ing(Items.BLAZE_ROD));

        shapeless(output, "mashed_vegetables", mod("mashed_vegetables"),
                ing(Items.BREAD), ing(Items.MUSHROOM_STEW), ing(Items.GOLDEN_APPLE), ing(Items.BEETROOT_SOUP),
                ing(Items.COOKIE), ing(Items.MELON_SLICE), ing(Items.CARROT), ing(Items.BAKED_POTATO),
                ing(Items.PUMPKIN_PIE));

        shaped(output, "natural_poison_1", mod("natural_poison"),
                new String[] { "EFE", "PIP", "EFE" },
                'E', ing(Items.FERMENTED_SPIDER_EYE), 'F', ing(Items.ROTTEN_FLESH),
                'P', ing(Items.POISONOUS_POTATO), 'I', ing(Items.PUFFERFISH));
        shaped(output, "natural_poison_2", mod("natural_poison"),
                new String[] { "EPE", "FIF", "EPE" },
                'E', ing(Items.FERMENTED_SPIDER_EYE), 'F', ing(Items.ROTTEN_FLESH),
                'P', ing(Items.POISONOUS_POTATO), 'I', ing(Items.PUFFERFISH));

        shaped(output, "one_block_generator_1", mod("one_block_generator"),
                new String[] { "SDS", "DFD", "SDS" },
                'S', ing("space_soup"), 'D', ing("dark_matter"), 'F', ing("far_star"));
        shaped(output, "one_block_generator_2", mod("one_block_generator"),
                new String[] { "DSD", "SFS", "DSD" },
                'S', ing("space_soup"), 'D', ing("dark_matter"), 'F', ing("far_star"));

        shapeless(output, "protein", mod("protein"),
                ing(Items.COOKED_COD), ing(Items.COOKED_SALMON), ing(Items.COOKED_PORKCHOP),
                ing(Items.COOKED_BEEF), ing(Items.COOKED_CHICKEN), ing(Items.COOKED_MUTTON),
                ing(Items.COOKED_RABBIT), ing(Items.MILK_BUCKET), ing(Items.PUFFERFISH));

        shaped(output, "space_soup_1", mod("space_soup"),
                new String[] { "LPL", "VCV", "LPL" },
                'L', ing("liquid_death"), 'V', ing("ultimate_mashed_vegetables"),
                'P', ing("ultimate_protein"), 'C', ing(Items.CAKE));
        shaped(output, "space_soup_2", mod("space_soup"),
                new String[] { "LVL", "PCP", "LVL" },
                'L', ing("liquid_death"), 'V', ing("ultimate_mashed_vegetables"),
                'P', ing("ultimate_protein"), 'C', ing(Items.CAKE));

        shapelessMulti(output, "super_mashed_vegetables", mod("super_mashed_vegetables"), ing("mashed_vegetables"), 9);
        shapelessMulti(output, "super_protein", mod("super_protein"), ing("protein"), 9);

        shaped(output, "super_poison", mod("super_poison"),
                new String[] { "NNN", "NPN", "NNN" },
                'N', ing("natural_poison"), 'P', ing(Items.NETHER_WART));

        shapelessMulti(output, "ultimate_mashed_vegetables", mod("ultimate_mashed_vegetables"), ing("super_mashed_vegetables"), 9);
        shapelessMulti(output, "ultimate_protein", mod("ultimate_protein"), ing("super_protein"), 9);
    }
}

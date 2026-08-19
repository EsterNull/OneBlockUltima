package ru.defea.oneblockultima.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import ru.defea.oneblockultima.OneBlockUltima;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JsonRecipeLoader
{
    private static final String RECIPES_BASE = "/assets/oneblockultima/recipes/";

    private static final String[] RECIPE_FILES = {
            "compressed_mineral.json",
            "compressed_mineral_block.json",
            "dark_matter.json",
            "energy_orb.json",
            "far_star_1.json",
            "far_star_2.json",
            "graviton.json",
            "higgs_boson.json",
            "liquid_death.json",
            "mashed_vegetables.json",
            "natural_poison_1.json",
            "natural_poison_2.json",
            "one_block_generator_1.json",
            "one_block_generator_2.json",
            "protein.json",
            "space_soup_1.json",
            "space_soup_2.json",
            "super_mashed_vegetables.json",
            "super_poison.json",
            "super_protein.json",
            "ultimate_mashed_vegetables.json",
            "ultimate_protein.json"
    };

    private JsonRecipeLoader()
    {
    }

    public static void registerRecipes()
    {
        for (String fileName : RECIPE_FILES)
        {
            try
            {
                registerRecipeFromFile(fileName);
            }
            catch (Exception e)
            {
                OneBlockUltima.getRawLogger().warn("[Recipes] Failed to load recipe file {}: {}", fileName, e.toString());
            }
        }
    }

    private static void registerRecipeFromFile(String fileName)
    {
        InputStream in = JsonRecipeLoader.class.getResourceAsStream(RECIPES_BASE + fileName);
        if (in == null)
        {
            OneBlockUltima.getRawLogger().warn("[Recipes] Recipe file not found in classpath: {}", fileName);
            return;
        }

        JsonObject root;
        try
        {
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                root = new JsonParser().parse(reader).getAsJsonObject();
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getRawLogger().warn("[Recipes] Recipe file {} is not valid JSON: {}", fileName, e.toString());
            return;
        }

        ItemStack result = resolveStack(root.get("result"));
        if (result == null)
        {
            OneBlockUltima.getRawLogger().warn("[Recipes] Recipe {} has unresolvable result, skipping", fileName);
            return;
        }

        String type = getString(root, "type");
        if ("minecraft:crafting_shapeless".equals(type))
        {
            JsonArray ingredients = root.getAsJsonArray("ingredients");
            List<Object> inputs = new ArrayList<>();
            for (JsonElement el : ingredients)
            {
                Object ingredient = resolveIngredient(el);
                if (ingredient == null)
                {
                    OneBlockUltima.getRawLogger().warn("[Recipes] Recipe {} has unresolvable ingredient {}, skipping", fileName, el);
                    return;
                }
                inputs.add(ingredient);
            }
            GameRegistry.addRecipe(new ShapelessOreRecipe(result, inputs.toArray()));
        }
        else if ("minecraft:crafting_shaped".equals(type))
        {
            JsonArray pattern = root.getAsJsonArray("pattern");
            JsonObject key = root.getAsJsonObject("key");
            List<Object> inputs = new ArrayList<>();
            for (JsonElement row : pattern)
            {
                inputs.add(row.getAsString());
            }
            for (Map.Entry<String, JsonElement> entry : key.entrySet())
            {
                Object ingredient = resolveIngredient(entry.getValue());
                if (ingredient == null)
                {
                    OneBlockUltima.getRawLogger().warn("[Recipes] Recipe {} has unresolvable key {}={}, skipping", fileName, entry.getKey(), entry.getValue());
                    return;
                }
                inputs.add(entry.getKey().charAt(0));
                inputs.add(ingredient);
            }
            GameRegistry.addRecipe(new ShapedOreRecipe(result, inputs.toArray()));
        }
        else
        {
            OneBlockUltima.getRawLogger().warn("[Recipes] Recipe {} has unknown type {}, skipping", fileName, type);
            return;
        }

        OneBlockUltima.getRawLogger().info("[Recipes] Registered recipe from {}", fileName);
    }

    private static Object resolveIngredient(JsonElement el)
    {
        if (el == null || !el.isJsonObject())
        {
            return null;
        }
        JsonObject obj = el.getAsJsonObject();
        if ("forge:ore_dict".equals(getString(obj, "type")))
        {
            String ore = getString(obj, "ore");
            return ore == null || ore.isEmpty() ? null : ore;
        }
        return resolveStack(obj);
    }

    private static ItemStack resolveStack(JsonElement el)
    {
        if (el == null || !el.isJsonObject())
        {
            return null;
        }
        JsonObject obj = el.getAsJsonObject();
        String key = getString(obj, "item");
        if (key == null || key.isEmpty())
        {
            return null;
        }
        int data = obj.has("data") ? obj.get("data").getAsInt() : 0;

        Item item = (Item) Item.itemRegistry.getObject(key);
        if (item != null)
        {
            return new ItemStack(item, 1, data);
        }
        Block block = (Block) Block.blockRegistry.getObject(key);
        if (block != null && block != Blocks.air)
        {
            Item blockItem = Item.getItemFromBlock(block);
            if (blockItem != null)
            {
                return new ItemStack(blockItem, 1, data);
            }
        }
        return null;
    }

    private static String getString(JsonObject obj, String name)
    {
        if (obj != null && obj.has(name) && !obj.get(name).isJsonNull())
        {
            return obj.get(name).getAsString();
        }
        return null;
    }
}
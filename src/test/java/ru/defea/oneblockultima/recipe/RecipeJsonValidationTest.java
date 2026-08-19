package ru.defea.oneblockultima.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ModItems;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Guards every recipe JSON in assets/oneblockultima/recipes/ against the 1.7.10 registries:
 * all ingredients and results must reference blocks/items that actually exist in vanilla 1.7.10
 * or among the mod's registered items/blocks. A recipe referencing a 1.8+ item (e.g.
 * cooked_mutton) would silently fail to register at runtime, so this test catches that.
 */
public class RecipeJsonValidationTest
{
    private static final String RECIPES_BASE = "/assets/oneblockultima/recipes/";

    private static final List<String> recipeFiles = new ArrayList<>();

    @BeforeClass
    public static void setup() throws Exception
    {
        Bootstrap.register();
        Field filesField = JsonRecipeLoader.class.getDeclaredField("RECIPE_FILES");
        filesField.setAccessible(true);
        String[] files = (String[]) filesField.get(null);
        for (String file : files)
        {
            recipeFiles.add(file);
        }
    }

    @Test
    public void everyRecipeFileLoadsFromClasspath()
    {
        for (String file : recipeFiles)
        {
            try (InputStream in = JsonRecipeLoader.class.getResourceAsStream(RECIPES_BASE + file))
            {
                assertTrue("recipe file " + file + " must exist in the classpath", in != null);
            }
            catch (Exception e)
            {
                fail("recipe file " + file + " could not be read: " + e);
            }
        }
    }

    @Test
    public void everyRecipeResultResolvesInThe17_10Registries()
    {
        for (String file : recipeFiles)
        {
            JsonObject root = parse(file);
            if (root == null)
            {
                continue;
            }
            String key = getResultKey(root);
            assertTrue("recipe " + file + " has a result that does not resolve in 1.7.10: " + key,
                    resolves(key));
        }
    }

    @Test
    public void everyShapelessIngredientResolvesInThe17_10Registries()
    {
        for (String file : recipeFiles)
        {
            JsonObject root = parse(file);
            if (root == null || !"minecraft:crafting_shapeless".equals(getString(root, "type")))
            {
                continue;
            }
            JsonArray ingredients = root.getAsJsonArray("ingredients");
            for (JsonElement el : ingredients)
            {
                assertIngredientResolves(file, el);
            }
        }
    }

    @Test
    public void everyShapedKeyResolvesInThe17_10Registries()
    {
        for (String file : recipeFiles)
        {
            JsonObject root = parse(file);
            if (root == null || !"minecraft:crafting_shaped".equals(getString(root, "type")))
            {
                continue;
            }
            JsonObject key = root.getAsJsonObject("key");
            for (java.util.Map.Entry<String, JsonElement> entry : key.entrySet())
            {
                assertIngredientResolves(file + " (key " + entry.getKey() + ")", entry.getValue());
            }
        }
    }

    @Test
    public void everyShapedPatternCharacterIsDeclaredInKeys()
    {
        for (String file : recipeFiles)
        {
            JsonObject root = parse(file);
            if (root == null || !"minecraft:crafting_shaped".equals(getString(root, "type")))
            {
                continue;
            }
            JsonArray pattern = root.getAsJsonArray("pattern");
            JsonObject key = root.getAsJsonObject("key");
            Set<Character> declared = new HashSet<>();
            for (java.util.Map.Entry<String, JsonElement> entry : key.entrySet())
            {
                if (!entry.getKey().isEmpty())
                {
                    declared.add(entry.getKey().charAt(0));
                }
            }
            for (JsonElement row : pattern)
            {
                String chars = row.getAsString();
                for (int i = 0; i < chars.length(); i++)
                {
                    char c = chars.charAt(i);
                    if (c != ' ' && !declared.contains(c))
                    {
                        fail("recipe " + file + " uses pattern character '" + c + "' that is not declared in key");
                    }
                }
            }
        }
    }

    private static void assertIngredientResolves(String context, JsonElement el)
    {
        if (el == null || !el.isJsonObject())
        {
            fail("recipe " + context + " has a malformed ingredient: " + el);
            return;
        }
        JsonObject obj = el.getAsJsonObject();
        if ("forge:ore_dict".equals(getString(obj, "type")))
        {
            String ore = getString(obj, "ore");
            assertTrue("recipe " + context + " has an empty ore dict tag", ore != null && !ore.isEmpty());
            return;
        }
        String key = getString(obj, "item");
        assertTrue("recipe " + context + " has an ingredient that does not resolve in 1.7.10: " + key,
                resolves(key));
    }

    private static boolean resolves(String key)
    {
        if (key == null || key.isEmpty())
        {
            return false;
        }
        if (key.startsWith("minecraft:"))
        {
            String bare = key.substring("minecraft:".length());
            if (Item.itemRegistry.getObject(key) != null || Item.itemRegistry.getObject(bare) != null)
            {
                return true;
            }
            Block block = (Block) Block.blockRegistry.getObject(key);
            if (block == null || block == Blocks.air)
            {
                block = (Block) Block.blockRegistry.getObject(bare);
            }
            return block != null && block != Blocks.air;
        }
        if (key.startsWith("oneblockultima:"))
        {
            String name = key.substring("oneblockultima:".length());
            return modItemNamed(name) || modBlockNamed(name);
        }
        return false;
    }

    private static boolean modItemNamed(String name)
    {
        for (ModItems.RegisterItem reg : ModItems.modItems)
        {
            if (stripPrefix(reg.getItem().getUnlocalizedName(), "item.").equals(name))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean modBlockNamed(String name)
    {
        for (ModBlocks.RegisterBlock reg : ModBlocks.modBlocks)
        {
            if (stripPrefix(reg.getBlock().getUnlocalizedName(), "tile.").equals(name))
            {
                return true;
            }
        }
        return false;
    }

    private static String stripPrefix(String value, String prefix)
    {
        return value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static JsonObject parse(String file)
    {
        try (InputStream in = JsonRecipeLoader.class.getResourceAsStream(RECIPES_BASE + file))
        {
            if (in == null)
            {
                fail("recipe file " + file + " missing from classpath");
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                assertTrue("recipe " + file + " must declare a type", root.has("type"));
                return root;
            }
        }
        catch (Exception e)
        {
            fail("recipe file " + file + " is not valid JSON: " + e);
            return null;
        }
    }

    private static String getResultKey(JsonObject root)
    {
        if (root != null && root.has("result") && root.get("result").isJsonObject())
        {
            return getString(root.getAsJsonObject("result"), "item");
        }
        return getString(root, "result");
    }

    private static String getString(JsonObject obj, String... names)
    {
        for (String name : names)
        {
            if (obj != null && obj.has(name) && !obj.get(name).isJsonNull())
            {
                return obj.get(name).getAsString();
            }
        }
        return null;
    }
}
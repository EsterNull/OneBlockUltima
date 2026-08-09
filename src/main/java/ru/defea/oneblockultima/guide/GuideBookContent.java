package ru.defea.oneblockultima.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GuideBookContent
{
    public static final class CommandInfo
    {
        public final String name;
        public final String usage;
        public final String descriptionKey;
        public final boolean cheat;

        public CommandInfo(String name, String usage, String descriptionKey)
        {
            this(name, usage, descriptionKey, false);
        }

        public CommandInfo(String name, String usage, String descriptionKey, boolean cheat)
        {
            this.name = name;
            this.usage = usage;
            this.descriptionKey = descriptionKey;
            this.cheat = cheat;
        }
    }

    public static final class Recipe
    {
        public final String id;
        public final ItemStack result;
        public final ItemStack[] grid;
        public final boolean shaped;

        public Recipe(String id, ItemStack result, ItemStack[] grid, boolean shaped)
        {
            this.id = id;
            this.result = result;
            this.grid = grid;
            this.shaped = shaped;
        }
    }

    private GuideBookContent()
    {
    }

    public static List<CommandInfo> getCommands()
    {
        List<CommandInfo> commands = new ArrayList<>();
        commands.add(new CommandInfo("obuSell", "/obuSell", "book.oneblockultima.command.obuSell.desc"));
        commands.add(new CommandInfo("obuSellAll", "/obuSellAll", "book.oneblockultima.command.obuSellAll.desc"));
        commands.add(new CommandInfo("inviteGeneratorMember", "/inviteGeneratorMember <playerName>", "book.oneblockultima.command.inviteGeneratorMember.desc"));
        commands.add(new CommandInfo("acceptGeneratorInvite", "/acceptGeneratorInvite", "book.oneblockultima.command.acceptGeneratorInvite.desc"));
        commands.add(new CommandInfo("declineGeneratorInvite", "/declineGeneratorInvite", "book.oneblockultima.command.declineGeneratorInvite.desc"));
        commands.add(new CommandInfo("addUltimaBalance", "/addUltimaBalance <player> <amount>", "book.oneblockultima.command.addUltimaBalance.desc", true));
        commands.add(new CommandInfo("setOwner", "/setOwner <x> <y> <z> <playerName>", "book.oneblockultima.command.setOwner.desc", true));
        return commands;
    }

    public static List<Recipe> getRecipes()
    {
        List<Recipe> recipes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try
        {
            for (ResourceLocation key : ForgeRegistries.RECIPES.getKeys())
            {
                if (!OneBlockUltima.MODID.equals(key.getResourceDomain()))
                {
                    continue;
                }
                if (!ForgeRegistries.RECIPES.containsKey(key))
                {
                    continue;
                }
                Recipe recipe = parseRecipe(key);
                if (recipe == null)
                {
                    recipe = parseRecipeFromIRecipe(key);
                }
                if (recipe != null && !recipe.result.isEmpty())
                {
                    String ingredientKey = ingredientKey(recipe.grid);
                    if (seen.add(ingredientKey))
                    {
                        recipes.add(recipe);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        recipes.sort(Comparator.comparing(r -> r.result.getDisplayName()));
        return recipes;
    }

    private static String ingredientKey(ItemStack[] grid)
    {
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : grid)
        {
            if (stack != null && !stack.isEmpty())
            {
                ResourceLocation name = stack.getItem().getRegistryName();
                parts.add(name == null ? "?" : name + "@" + stack.getMetadata());
            }
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }

    public static List<BlockSetConfig.BlockSetDefinition> getSets()
    {
        List<BlockSetConfig.BlockSetDefinition> result = new ArrayList<>();
        for (BlockSetConfig.BlockSetDefinition set : BlockSetConfig.get().getSets())
        {
            if (set != null)
            {
                result.add(set);
            }
        }
        return result;
    }

    private static Recipe parseRecipe(ResourceLocation key)
    {
        try
        {
            ResourceLocation jsonLocation = new ResourceLocation(
                    key.getResourceDomain(),
                    "recipes/" + key.getResourcePath() + ".json"
            );
            JsonObject root = readJson(jsonLocation);
            if (root == null)
            {
                return null;
            }

            String type = root.has("type") ? root.get("type").getAsString() : "";
            boolean shaped = type != null && type.contains("crafting_shaped");

            JsonObject resultObj = root.getAsJsonObject("result");
            ItemStack result = resolveItem(resultObj);
            if (result.isEmpty())
            {
                return null;
            }
            if (resultObj.has("count"))
            {
                result.setCount(Math.max(1, resultObj.get("count").getAsInt()));
            }

            ItemStack[] grid = new ItemStack[9];
            Arrays.fill(grid, ItemStack.EMPTY);

            if (shaped)
            {
                JsonArray pattern = root.getAsJsonArray("pattern");
                List<String> rows = new ArrayList<>();
                for (JsonElement element : pattern)
                {
                    rows.add(element.getAsString());
                }
                JsonObject keyObj = root.getAsJsonObject("key");
                int gridW = rows.isEmpty() ? 0 : rows.get(0).length();
                int gridH = rows.size();
                int startRow = (3 - gridH) / 2;
                int startCol = (3 - gridW) / 2;
                for (int r = 0; r < gridH; r++)
                {
                    String row = rows.get(r);
                    for (int c = 0; c < row.length(); c++)
                    {
                        char symbol = row.charAt(c);
                        if (symbol == ' ')
                        {
                            continue;
                        }
                        JsonElement keyEntry = keyObj.get(String.valueOf(symbol));
                        if (keyEntry == null || !keyEntry.isJsonObject())
                        {
                            continue;
                        }
                        int gr = startRow + r;
                        int gc = startCol + c;
                        if (gr < 0 || gr > 2 || gc < 0 || gc > 2)
                        {
                            continue;
                        }
                        grid[gr * 3 + gc] = resolveItem(keyEntry.getAsJsonObject());
                    }
                }
            }
            else
            {
                JsonArray ingredients = root.getAsJsonArray("ingredients");
                int index = 0;
                for (JsonElement element : ingredients)
                {
                    if (index >= 9)
                    {
                        break;
                    }
                    if (element.isJsonObject())
                    {
                        grid[index] = resolveItem(element.getAsJsonObject());
                    }
                    index++;
                }
            }

            return new Recipe(key.getResourcePath(), result, grid, shaped);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static Recipe parseRecipeFromIRecipe(ResourceLocation key)
    {
        try
        {
            IRecipe recipe = ForgeRegistries.RECIPES.getValue(key);
            if (recipe == null)
            {
                return null;
            }
            ItemStack result = recipe.getRecipeOutput();
            if (result == null || result.isEmpty())
            {
                return null;
            }
            ItemStack[] grid = new ItemStack[9];
            Arrays.fill(grid, ItemStack.EMPTY);
            int index = 0;
            for (Ingredient ingredient : recipe.getIngredients())
            {
                if (index >= 9)
                {
                    break;
                }
                if (ingredient == null || ingredient == Ingredient.EMPTY)
                {
                    continue;
                }
                ItemStack display = ItemStack.EMPTY;
                for (ItemStack stack : ingredient.getMatchingStacks())
                {
                    if (stack != null && !stack.isEmpty())
                    {
                        display = stack.copy();
                        break;
                    }
                }
                grid[index] = display;
                index++;
            }
            return new Recipe(key.getResourcePath(), result, grid, false);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static ItemStack resolveItem(JsonObject entry)
    {
        if (entry == null)
        {
            return ItemStack.EMPTY;
        }
        String type = entry.has("type") ? entry.get("type").getAsString() : "";
        if ("forge:ore_dict".equals(type))
        {
            if (!entry.has("ore"))
            {
                return ItemStack.EMPTY;
            }
            List<ItemStack> ores = net.minecraftforge.oredict.OreDictionary.getOres(entry.get("ore").getAsString());
            for (ItemStack ore : ores)
            {
                if (ore != null && !ore.isEmpty())
                {
                    return ore.copy();
                }
            }
            return ItemStack.EMPTY;
        }
        if (!entry.has("item"))
        {
            return ItemStack.EMPTY;
        }
        String name = entry.get("item").getAsString();
        int meta = entry.has("data") ? entry.get("data").getAsInt() : 0;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
        if (item == null)
        {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, meta);
    }

    private static JsonObject readJson(ResourceLocation location)
    {
        try
        {
            net.minecraft.client.resources.IResource resource =
                    Minecraft.getMinecraft().getResourceManager().getResource(location);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)))
            {
                JsonElement element = new JsonParser().parse(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
}

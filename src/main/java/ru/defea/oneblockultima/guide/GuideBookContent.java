package ru.defea.oneblockultima.guide;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;

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
            for (Object obj : CraftingManager.getInstance().getRecipeList())
            {
                if (!(obj instanceof IRecipe))
                {
                    continue;
                }
                Recipe recipe = parseRecipeFromIRecipe((IRecipe) obj);
                if (recipe != null && recipe.result != null && involvesModItem(recipe))
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

    private static boolean involvesModItem(Recipe recipe)
    {
        if (isModItem(recipe.result.getItem()))
        {
            return true;
        }
        for (ItemStack stack : recipe.grid)
        {
            if (stack != null && isModItem(stack.getItem()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isModItem(Item item)
    {
        if (item == null)
        {
            return false;
        }
        String name = Item.itemRegistry.getNameForObject(item);
        return name != null && name.startsWith(OneBlockUltima.MODID + ":");
    }

    private static String ingredientKey(ItemStack[] grid)
    {
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : grid)
        {
            if (stack != null)
            {
                String name = Item.itemRegistry.getNameForObject(stack.getItem());
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

    private static Recipe parseRecipeFromIRecipe(IRecipe recipe)
    {
        try
        {
            ItemStack result = recipe.getRecipeOutput();
            if (result == null)
            {
                return null;
            }
            ItemStack[] grid = new ItemStack[9];
            int index = 0;
            for (Object input : getRecipeInputs(recipe))
            {
                if (index >= 9)
                {
                    break;
                }
                ItemStack display = resolveInput(input);
                if (display == null)
                {
                    continue;
                }
                grid[index] = display;
                index++;
            }
            return new Recipe("", result, grid, false);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static List<Object> getRecipeInputs(IRecipe recipe)
    {
        List<Object> inputs = new ArrayList<>();
        if (recipe instanceof ShapedRecipes)
        {
            ItemStack[] items = ((ShapedRecipes) recipe).recipeItems;
            if (items != null)
            {
                Collections.addAll(inputs, items);
            }
        }
        else if (recipe instanceof ShapelessRecipes)
        {
            List list = ((ShapelessRecipes) recipe).recipeItems;
            if (list != null)
            {
                inputs.addAll(list);
            }
        }
        else if (recipe instanceof ShapedOreRecipe)
        {
            Object[] array = ((ShapedOreRecipe) recipe).getInput();
            if (array != null)
            {
                Collections.addAll(inputs, array);
            }
        }
        else if (recipe instanceof ShapelessOreRecipe)
        {
            ArrayList<Object> list = ((ShapelessOreRecipe) recipe).getInput();
            if (list != null)
            {
                inputs.addAll(list);
            }
        }
        return inputs;
    }

    private static ItemStack resolveInput(Object input)
    {
        if (input == null)
        {
            return null;
        }
        if (input instanceof ItemStack)
        {
            return ((ItemStack) input).copy();
        }
        if (input instanceof List)
        {
            for (Object o : (List<?>) input)
            {
                if (o instanceof ItemStack)
                {
                    return ((ItemStack) o).copy();
                }
            }
            return null;
        }
        if (input instanceof String)
        {
            for (ItemStack ore : OreDictionary.getOres((String) input))
            {
                if (ore != null)
                {
                    return ore.copy();
                }
            }
            return null;
        }
        return null;
    }
}

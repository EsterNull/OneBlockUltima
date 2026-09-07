package ru.defea.oneblockultima.guide;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return recipes;
        }
        RecipeManager rm = mc.level.getRecipeManager();
        try
        {
            for (RecipeHolder<?> holder : rm.getRecipes())
            {
                ResourceLocation key = holder.id();
                if (!OneBlockUltima.MODID.equals(key.getNamespace()))
                {
                    continue;
                }
                Recipe recipe = parseRecipe(holder);
                if (recipe != null && !recipe.result.isEmpty())
                {
                    String ingredientKeyStr = ingredientKey(recipe.grid);
                    if (seen.add(ingredientKeyStr))
                    {
                        recipes.add(recipe);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        recipes.sort(Comparator.comparing(r -> r.result.getHoverName().getString()));
        return recipes;
    }

    private static String ingredientKey(ItemStack[] grid)
    {
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : grid)
        {
            if (stack != null && !stack.isEmpty())
            {
                ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());
                parts.add(name == null ? "?" : name + "@" + ModBlocks.metaOf(stack));
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

    private static Recipe parseRecipe(RecipeHolder<?> holder)
    {
        try
        {
            net.minecraft.world.item.crafting.Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
            if (result == null || result.isEmpty())
            {
                return null;
            }
            if (result.getCount() < 1)
            {
                result.setCount(1);
            }

            ItemStack[] grid = new ItemStack[9];
            Arrays.fill(grid, ItemStack.EMPTY);

            List<Ingredient> ingredients = recipe.getIngredients();
            boolean shaped = recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe;
            if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe s)
            {
                int w = s.getWidth();
                int h = s.getHeight();
                int startCol = (3 - w) / 2;
                int startRow = (3 - h) / 2;
                int idx = 0;
                for (int r = 0; r < h && idx < ingredients.size(); r++)
                {
                    for (int c = 0; c < w && idx < ingredients.size(); c++)
                    {
                        Ingredient ing = ingredients.get(idx++);
                        int gr = startRow + r;
                        int gc = startCol + c;
                        if (gr >= 0 && gr < 3 && gc >= 0 && gc < 3)
                        {
                            grid[gr * 3 + gc] = firstStack(ing);
                        }
                    }
                }
            }
            else
            {
                int i = 0;
                for (Ingredient ing : ingredients)
                {
                    if (i >= 9)
                    {
                        break;
                    }
                    grid[i++] = firstStack(ing);
                }
            }

            return new Recipe(holder.id().getPath(), result, grid, shaped);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static ItemStack firstStack(Ingredient ing)
    {
        if (ing == null || ing == Ingredient.EMPTY)
        {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : ing.getItems())
        {
            if (stack != null && !stack.isEmpty())
            {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }
}

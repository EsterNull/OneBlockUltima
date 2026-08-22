package ru.defea.oneblockultima.util;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.item.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CaseUtil
{
    public static final String NBT_CASE_SET_ID = "obuCaseSetId";
    public static final String NBT_CASE_CONTENTS = "obuCaseContents";
    public static final String NBT_CASE_STACK = "stack";
    public static final String NBT_CASE_WEIGHT = "weight";

    private CaseUtil()
    {
    }

    public static boolean isCaseItem(ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.getItem() == ModItems.CASE;
    }

    public static ItemStack createCaseItem(World world, BlockSetConfig.BlockSetDefinition set)
    {
        if (world == null || set == null || !set.hasCaseEntries())
        {
            return ItemStack.EMPTY;
        }

        // Deterministic resolution per set so all cases of the same set share identical NBT and stack together
        Random seededRandom = new Random(set.id == null ? 0L : set.id.hashCode());
        List<WeightedStack> weighted = resolveContents(world, set, seededRandom);
        if (weighted.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        ItemStack caseStack = new ItemStack(ModItems.CASE);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString(NBT_CASE_SET_ID, set.id == null ? "" : set.id);
        NBTTagList contents = new NBTTagList();
        for (WeightedStack ws : weighted)
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(NBT_CASE_STACK, ws.stack.writeToNBT(new NBTTagCompound()));
            tag.setInteger(NBT_CASE_WEIGHT, ws.weight);
            contents.appendTag(tag);
        }
        nbt.setTag(NBT_CASE_CONTENTS, contents);
        caseStack.setTagCompound(nbt);
        return caseStack;
    }

    public static List<WeightedStack> readContents(ItemStack caseStack)
    {
        List<WeightedStack> result = new ArrayList<>();
        if (caseStack == null || caseStack.isEmpty() || !caseStack.hasTagCompound())
        {
            return result;
        }

        NBTTagCompound nbt = caseStack.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_CASE_CONTENTS, 9))
        {
            return result;
        }

        NBTTagList contents = nbt.getTagList(NBT_CASE_CONTENTS, 10);
        for (int i = 0; i < contents.tagCount(); i++)
        {
            NBTTagCompound tag = contents.getCompoundTagAt(i);
            if (tag.hasKey(NBT_CASE_STACK, 10))
            {
                ItemStack stack = new ItemStack(tag.getCompoundTag(NBT_CASE_STACK));
                if (!stack.isEmpty())
                {
                    result.add(new WeightedStack(stack, tag.getInteger(NBT_CASE_WEIGHT)));
                }
            }
        }
        return result;
    }

    public static int rollIndex(List<WeightedStack> contents, Random random)
    {
        if (contents == null || contents.isEmpty())
        {
            return -1;
        }

        int total = 0;
        for (WeightedStack ws : contents)
        {
            total += Math.max(0, ws.weight);
        }
        if (total <= 0)
        {
            return random.nextInt(contents.size());
        }

        int roll = random.nextInt(total);
        int current = 0;
        for (int i = 0; i < contents.size(); i++)
        {
            current += Math.max(0, contents.get(i).weight);
            if (roll < current)
            {
                return i;
            }
        }
        return contents.size() - 1;
    }

    private static List<WeightedStack> resolveContents(World world, BlockSetConfig.BlockSetDefinition set, Random random)
    {
        List<WeightedStack> weighted = new ArrayList<>();
        for (BlockSetConfig.CaseEntryDefinition entry : set.caseInfo.entries)
        {
            if (entry == null || entry.weight <= 0)
            {
                continue;
            }

            if (entry.item != null && !entry.item.isEmpty())
            {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.item));
                if (item != null && item != Items.AIR)
                {
                    ItemStack stack = new ItemStack(item, Math.max(1, entry.count), Math.max(0, entry.meta));
                    weighted.add(new WeightedStack(stack, entry.weight));
                }
            }
            else if (entry.lootTable != null && !entry.lootTable.isEmpty())
            {
                resolveLootTable(world, entry, random, weighted);
            }
        }
        return weighted;
    }

    private static void resolveLootTable(World world, BlockSetConfig.CaseEntryDefinition entry, Random random, List<WeightedStack> weighted)
    {
        try
        {
            ResourceLocation location = new ResourceLocation(entry.lootTable);
            if (world instanceof WorldServer)
            {
                LootTable table = world.getLootTableManager().getLootTableFromLocation(location);
                if (location.equals(LootTableList.EMPTY))
                {
                    return;
                }
                LootContext context = new LootContext.Builder((WorldServer) world).build();
                List<ItemStack> stacks = table.generateLootForPools(random, context);
                for (ItemStack stack : stacks)
                {
                    if (stack != null && !stack.isEmpty())
                    {
                        weighted.add(new WeightedStack(stack, entry.weight));
                    }
                }
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().warn("Failed to resolve loot table {} for case", entry.lootTable, e);
        }
    }

    public static class WeightedStack
    {
        public final ItemStack stack;
        public final int weight;

        public WeightedStack(ItemStack stack, int weight)
        {
            this.stack = stack;
            this.weight = weight;
        }
    }
}
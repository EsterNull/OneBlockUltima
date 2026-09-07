package ru.defea.oneblockultima.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.item.ModItems;

import java.util.ArrayList;
import java.util.List;

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

    public static ItemStack createCaseItem(Level world, BlockSetConfig.BlockSetDefinition set)
    {
        if (world == null || set == null || !set.hasCaseEntries())
        {
            return ItemStack.EMPTY;
        }

        ItemStack caseStack = new ItemStack(ModItems.CASE);
        RandomSource seededRandom = RandomSource.create(set.id == null ? 0L : set.id.hashCode());
        List<WeightedStack> weighted = resolveContents(world, set, seededRandom);
        if (weighted.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_CASE_SET_ID, set.id == null ? "" : set.id);
        ListTag contents = new ListTag();
        for (WeightedStack ws : weighted)
        {
            CompoundTag tag = new CompoundTag();
            tag.put(NBT_CASE_STACK, ws.stack.save(world.registryAccess()));
            tag.putInt(NBT_CASE_WEIGHT, ws.weight);
            contents.add(tag);
        }
        nbt.put(NBT_CASE_CONTENTS, contents);
        caseStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
        return caseStack;
    }

    public static List<WeightedStack> readContents(ItemStack caseStack, net.minecraft.core.RegistryAccess registryAccess)
    {
        List<WeightedStack> result = new ArrayList<>();
        if (caseStack == null || caseStack.isEmpty() || !caseStack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA))
        {
            return result;
        }

        CompoundTag nbt = caseStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).getUnsafe();
        if (nbt == null || !nbt.contains(NBT_CASE_CONTENTS, Tag.TAG_LIST))
        {
            return result;
        }

        ListTag contents = nbt.getList(NBT_CASE_CONTENTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < contents.size(); i++)
        {
            CompoundTag tag = contents.getCompound(i);
            if (tag.contains(NBT_CASE_STACK, Tag.TAG_COMPOUND))
            {
                ItemStack stack = ItemStack.parse(registryAccess, tag.getCompound(NBT_CASE_STACK)).orElse(ItemStack.EMPTY);
                if (!stack.isEmpty())
                {
                    result.add(new WeightedStack(stack, tag.getInt(NBT_CASE_WEIGHT)));
                }
            }
        }
        return result;
    }

    public static int rollIndex(List<WeightedStack> contents, RandomSource random)
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

    private static List<WeightedStack> resolveContents(Level world, BlockSetConfig.BlockSetDefinition set, RandomSource random)
    {
        List<WeightedStack> weighted = new ArrayList<>();
        if (set.caseInfo == null) return weighted;
        for (BlockSetConfig.CaseEntryDefinition entry : set.caseInfo.entries)
        {
            if (entry == null || entry.weight <= 0)
            {
                continue;
            }

            if (entry.item != null && !entry.item.isEmpty())
            {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item));
                if (item != Items.AIR)
                {
                    ItemStack stack = ModBlocks.stackFromItemAndMeta(item, entry.meta);
                    if (!stack.isEmpty())
                    {
                        stack.setCount(Math.max(1, entry.count));
                        weighted.add(new WeightedStack(stack, entry.weight));
                    }
                }
            }
            else if (entry.lootTable != null && !entry.lootTable.isEmpty())
            {
                resolveLootTable(world, entry, random, weighted);
            }
        }
        return weighted;
    }

    private static void resolveLootTable(Level world, BlockSetConfig.CaseEntryDefinition entry, RandomSource random, List<WeightedStack> weighted)
    {
        try
        {
            ResourceLocation location = ResourceLocation.parse(entry.lootTable);
            if (world instanceof ServerLevel)
            {
                ServerLevel server = (ServerLevel) world;
                LootTable table = server.getServer().getServerResources().managers().fullRegistries()
                        .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, location));
                if (table == null || table == LootTable.EMPTY || location.equals(BuiltInLootTables.EMPTY.location()))
                {
                    return;
                }
                LootParams params = new LootParams.Builder(server)
                        .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                        .withLuck(0)
                        .create(LootContextParamSets.CHEST);
                List<ItemStack> stacks = table.getRandomItems(params, random);
                for (ItemStack stack : stacks)
                {
                    if (stack != null && !stack.isEmpty())
                    {
                        weighted.add(new WeightedStack(stack.copy(), entry.weight));
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

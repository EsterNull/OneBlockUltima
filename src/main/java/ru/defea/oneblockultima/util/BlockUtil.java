package ru.defea.oneblockultima.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import cpw.mods.fml.common.registry.GameRegistry;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCustomBreakable;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public final class BlockUtil
{
    private BlockUtil()
    {
    }

    public static String getRegistryString(Block block)
    {
        if (block == null)
        {
            return "null";
        }
        try
        {
            GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
            if (uid != null)
            {
                return uid.modId + ":" + uid.name;
            }
        }
        catch (Exception ignored)
        {
        }
        return block.getUnlocalizedName();
    }

    public static String getRegistryString(Item item)
    {
        if (item == null)
        {
            return "null";
        }
        try
        {
            GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(item);
            if (uid != null)
            {
                return uid.modId + ":" + uid.name;
            }
        }
        catch (Exception ignored)
        {
        }
        return item.getUnlocalizedName();
    }

    public static Block getReplacementBlockForGeneratorPlacement(Block block, World world, int x, int y, int z)
    {
        if (block == null || world == null || world.getBlock(x, y - 1, z) != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            return block;
        }

        return toBreakableIfUnbreakable(block);
    }

    /**
     * Replaces an unbreakable block (hardness &lt; 0) with a breakable copy {@link BlockCustomBreakable},
     * so it can be mined like obsidian. All other blocks are returned unchanged.
     */
    @Nullable
    public static Block toBreakableIfUnbreakable(Block block)
    {
        if (block == null || isBreakable(block))
        {
            return block;
        }

        BlockCustomBreakable substitute = ModBlocks.getBreakableFor(block);
        return substitute == null ? block : substitute;
    }

    public static boolean isBreakable(Block block)
    {
        if (block == null || block == Blocks.air || block instanceof BlockCustomBreakable)
        {
            return true;
        }
        if (block == ModBlocks.ONE_BLOCK_GENERATOR || block == ModBlocks.FLUID_BARRIER)
        {
            return true;
        }
        try
        {
            return block.getBlockHardness(null, 0, 0, 0) >= 0.0F;
        }
        catch (Exception ex)
        {
            return true;
        }
    }

    /**
     * Places a block with NBT tags applied. Tags are applied AFTER the block is placed,
     * by writing them into the block's TileEntity when present.
     */
    public static void placeBlockWithNBT(World world, int x, int y, int z, Block block, int meta, @Nullable NBTTagCompound nbtTags)
    {
        if (world == null || block == null)
        {
            return;
        }

        if (block.getMaterial().isLiquid())
        {
            block = normalizeLiquidBlock(block);
            if (block == null)
            {
                return;
            }
        }

        block = getReplacementBlockForGeneratorPlacement(block, world, x, y, z);

        // Place the block
        world.setBlock(x, y, z, block, meta, 3);

        // If NBT tags exist, apply them after placement
        if (nbtTags != null && !nbtTags.hasNoTags())
        {
            applyNbtToBlock(world, x, y, z, nbtTags);
        }
    }

    public static String getDisplayName(Block block, NBTTagCompound nbtTagCompound)
    {
        NBTTagCompound nbttagcompound = nbtTagCompound != null && nbtTagCompound.hasKey("display", Constants.NBT.TAG_COMPOUND) ? nbtTagCompound.getCompoundTag("display") : null;

        if (nbttagcompound != null)
        {
            if (nbttagcompound.hasKey("Name", Constants.NBT.TAG_STRING))
            {
                return nbttagcompound.getString("Name");
            }

            if (nbttagcompound.hasKey("LocName", Constants.NBT.TAG_STRING))
            {
                return StatCollector.translateToLocal(nbttagcompound.getString("LocName"));
            }
        }

        return StatCollector.translateToLocal(StatCollector.translateToLocal(block.getUnlocalizedName()) + ".name").trim();
    }

    public static List<String> getTooltip(BlockSetConfig.BlockEntryDefinition hoveredEntry, boolean isAdvanced) {
        java.util.List<String> tooltip = new java.util.ArrayList<>();
        Block resolvedBlock = hoveredEntry.resolveBlock();
        boolean hasTagCompound = hoveredEntry.nbtTags != null;
        NBTTagCompound nbtTagCompound = hasTagCompound && hoveredEntry.nbtTags.hasKey("display", Constants.NBT.TAG_COMPOUND) ? hoveredEntry.nbtTags.getCompoundTag("display") : null;
        boolean hasDisplayName = nbtTagCompound != null && nbtTagCompound.hasKey("Name", Constants.NBT.TAG_STRING);

        String s = getDisplayName(resolvedBlock, nbtTagCompound);
        if (s == null || s.isEmpty()) {
            s = hoveredEntry.registry;
        }

        if (isAdvanced)
        {
            String s1 = "";

            if (!s.isEmpty())
            {
                s = s + " (";
                s1 = ")";
            }

            int i = Block.getIdFromBlock(resolvedBlock);
            int meta = hoveredEntry.meta;

            if (meta > 0)
            {
                s = s + String.format("#%04d/%d%s", i, meta, s1);
            }
            else
            {
                s = s + String.format("#%04d%s", i, s1);
            }
        }
        else if (!hasDisplayName)
        {
            s = s + " #" + hoveredEntry.meta;
        }

        tooltip.add(s);

        int i1 = 0;

        try {
            assert nbtTagCompound != null;
            if (nbtTagCompound.hasKey("HideFlags")) {
                i1 = nbtTagCompound.getInteger("HideFlags");
            }
        } catch (Exception ignored) {}

        if ((i1 & 1) == 0) {
            try {
                NBTTagList nbttaglist = nbtTagCompound.hasKey("ench", Constants.NBT.TAG_COMPOUND) ? nbtTagCompound.getTagList("ench", Constants.NBT.TAG_COMPOUND) : new NBTTagList();

                for (int j = 0; j < nbttaglist.tagCount(); ++j) {
                    NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(j);
                    int k = nbttagcompound.getShort("id");
                    int l = nbttagcompound.getShort("lvl");
                    Enchantment enchantment = (k >= 0 && k < Enchantment.enchantmentsList.length) ? Enchantment.enchantmentsList[k] : null;

                    if (enchantment != null) {
                        tooltip.add(enchantment.getTranslatedName(l));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (hasDisplayName)
        {
            NBTTagCompound nbttagcompound1 = nbtTagCompound.getCompoundTag("display");

            if (nbttagcompound1.hasKey("color", Constants.NBT.TAG_INT))
            {
                if (isAdvanced)
                {
                    tooltip.add(StatCollector.translateToLocalFormatted("block.color", String.format("#%06X", nbttagcompound1.getInteger("color"))));
                }
                else
                {
                    tooltip.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("block.dyed"));
                }
            }

            if (nbttagcompound1.getTagId("Lore") == 9)
            {
                NBTTagList nbttaglist3 = nbttagcompound1.getTagList("Lore", 8);

                if (nbttaglist3.tagCount() > 0)
                {
                    for (int l1 = 0; l1 < nbttaglist3.tagCount(); ++l1)
                    {
                        tooltip.add(EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.ITALIC + nbttaglist3.getStringTagAt(l1));
                    }
                }
            }
        }

        if (isAdvanced)
        {
            tooltip.add(EnumChatFormatting.DARK_GRAY + getRegistryString(resolvedBlock));

            if (hasDisplayName)
            {
                tooltip.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocalFormatted("block.nbt_tags", nbtTagCompound.getKeySet().size()));
            }
        }

        return tooltip;
    }

    /**
     * Normalizes liquids (water and lava) to their still states
     */
    private static Block normalizeLiquidBlock(Block block)
    {
        if (block == null || !block.getMaterial().isLiquid())
        {
            return block;
        }

        if (block == Blocks.flowing_water)
        {
            return Blocks.water;
        }

        if (block == Blocks.flowing_lava)
        {
            return Blocks.lava;
        }

        if (block instanceof IFluidBlock)
        {
            Fluid fluid = ((IFluidBlock) block).getFluid();
            if (fluid != null)
            {
                Block stillBlock = fluid.getBlock();
                if (stillBlock != null && stillBlock != block)
                {
                    return stillBlock;
                }
            }
        }

        Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
        if (fluid != null)
        {
            Block stillBlock = fluid.getBlock();
            if (stillBlock != null && stillBlock != block)
            {
                return stillBlock;
            }
        }

        return block;
    }

    /**
     * Universal application of NBT tags to the block at the given position
     */
    public static void applyNbtToBlock(World world, int x, int y, int z, NBTTagCompound nbtTags)
    {
        if (world == null || nbtTags == null || nbtTags.hasNoTags())
        {
            return;
        }

        try
        {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            if (tileEntity != null)
            {
                // Read the current TileEntity state
                NBTTagCompound tileNbt = new NBTTagCompound();
                tileEntity.writeToNBT(tileNbt);

                // Add all tags from nbtTags into tileNbt (overwrite if already present)
                for (Object keyObj : nbtTags.getKeySet())
                {
                    String key = keyObj.toString();
                    NBTBase tag = nbtTags.getTag(key);
                    // noinspection ConstantConditions
                    if (tag != null)
                    {
                        tileNbt.setTag(key, tag.copy());
                    }
                }

                // Apply the updated tags
                tileEntity.readFromNBT(tileNbt);
                tileEntity.markDirty();

                // 1.7.10 chests ignore the 1.12-style LootTable tag, so generate
                // the contents manually through Forge's ChestGenHooks.
                if (tileEntity instanceof TileEntityChest && nbtTags.hasKey("LootTable"))
                {
                    fillChestLoot(world, (TileEntityChest) tileEntity, nbtTags.getString("LootTable"));
                }

                // Update the block
                world.markBlockForUpdate(x, y, z);

                OneBlockUltima.getLogger().info("[Generator] Applied NBT tags to TileEntity at ({}, {}, {}): {}", x, y, z, nbtTags);
            }
            else
            {
                OneBlockUltima.getLogger().debug("[Generator] No TileEntity found at ({}, {}, {}) for NBT application", x, y, z);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Generator] Failed to apply NBT tags to block at ({}, {}, {})", x, y, z, e);
        }
    }

    /**
     * Maps a 1.12-style loot table path ("minecraft:chests/simple_dungeon") to
     * the category key Forge's {@link ChestGenHooks} knows in 1.7.10. Unknown
     * tables (woodland_mansion, end_city_treasure, nether_bridge) fall back to
     * the dungeon chest loot.
     */
    private static String mapLootTableToCategory(String lootTable)
    {
        if (lootTable == null)
        {
            return ChestGenHooks.DUNGEON_CHEST;
        }
        String key = lootTable.trim().toLowerCase(Locale.ROOT);
        if (key.endsWith("desert_pyramid"))
        {
            return ChestGenHooks.PYRAMID_DESERT_CHEST;
        }
        if (key.endsWith("village_blacksmith"))
        {
            return ChestGenHooks.VILLAGE_BLACKSMITH;
        }
        if (key.endsWith("jungle_temple"))
        {
            return ChestGenHooks.PYRAMID_JUNGLE_CHEST;
        }
        if (key.endsWith("abandoned_mineshaft"))
        {
            return ChestGenHooks.MINESHAFT_CORRIDOR;
        }
        if (key.endsWith("stronghold_corridor"))
        {
            return ChestGenHooks.STRONGHOLD_CORRIDOR;
        }
        if (key.endsWith("stronghold_crossing"))
        {
            return ChestGenHooks.STRONGHOLD_CROSSING;
        }
        if (key.endsWith("stronghold_library"))
        {
            return ChestGenHooks.STRONGHOLD_LIBRARY;
        }
        // simple_dungeon and everything without a 1.7.10 equivalent
        return ChestGenHooks.DUNGEON_CHEST;
    }

    /**
     * Fills a chest with loot generated by {@link ChestGenHooks}, merging into
     * any existing stacks and placing the rest into random empty slots.
     */
    private static void fillChestLoot(World world, TileEntityChest chest, String lootTable)
    {
        try
        {
            String category = mapLootTableToCategory(lootTable);
            WeightedRandomChestContent[] weightedContents = ChestGenHooks.getItems(category, world.rand);
            if (weightedContents == null || weightedContents.length == 0)
            {
                return;
            }

            int size = chest.getSizeInventory();
            if (size <= 0)
            {
                return;
            }

            for (WeightedRandomChestContent content : weightedContents)
            {
                if (content == null || content.theItemId == null)
                {
                    continue;
                }

                ItemStack[] generated = ChestGenHooks.generateStacks(world.rand, content.theItemId,
                        content.theMinimumChanceToGenerateItem, content.theMaximumChanceToGenerateItem);

                for (ItemStack stack : generated)
                {
                    if (stack == null || stack.stackSize <= 0)
                    {
                        continue;
                    }

                    int remaining = stack.stackSize;

                    // Merge into existing stacks of the same item first
                    for (int i = 0; i < size && remaining > 0; i++)
                    {
                        ItemStack existing = chest.getStackInSlot(i);
                        if (existing != null && existing.isItemEqual(stack)
                                && ItemStack.areItemStackTagsEqual(existing, stack)
                                && existing.stackSize < existing.getMaxStackSize())
                        {
                            int move = Math.min(existing.getMaxStackSize() - existing.stackSize, remaining);
                            existing.stackSize += move;
                            chest.setInventorySlotContents(i, existing);
                            remaining -= move;
                        }
                    }

                    // Place the rest into random empty slots (bounded to avoid endless loops)
                    int tries = size * 3;
                    while (remaining > 0 && tries-- > 0)
                    {
                        int slot = world.rand.nextInt(size);
                        if (chest.getStackInSlot(slot) == null)
                        {
                            ItemStack placed = stack.copy();
                            placed.stackSize = Math.min(remaining, stack.getMaxStackSize());
                            chest.setInventorySlotContents(slot, placed);
                            remaining -= placed.stackSize;
                        }
                    }
                }
            }

            chest.markDirty();
            world.markBlockForUpdate(chest.xCoord, chest.yCoord, chest.zCoord);
            OneBlockUltima.getLogger().info("[Generator] Filled chest at ({}, {}, {}) with loot from {}", chest.xCoord, chest.yCoord, chest.zCoord, lootTable);
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().warn("[Generator] Failed to fill chest loot for {}: {}", lootTable, e);
        }
    }

    public static boolean isFullBlock(Block block, @SuppressWarnings("unused") int meta)
    {
        try
        {
            Item item = Item.getItemFromBlock(block);
            if (item == null || block == null || block == Blocks.air)
            {
                return false;
            }

            return block.isFullBlock() && block.isOpaqueCube();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Recursive merging of NBT tags
     */
    private static void mergeNbtTags(NBTTagCompound target, NBTTagCompound source)
    {
        if (source == null || source.hasNoTags())
        {
            return;
        }

        for (Object keyObj : source.getKeySet())
        {
            String key = keyObj.toString();
            NBTBase sourceTag = source.getTag(key);
            // noinspection ConstantConditions
            if (sourceTag == null)
            {
                continue;
            }

            // If the target already has this key and both are CompoundTags, merge recursively
            if (target.hasKey(key))
            {
                NBTBase targetTag = target.getTag(key);
                if (targetTag instanceof NBTTagCompound && sourceTag instanceof NBTTagCompound)
                {
                    mergeNbtTags((NBTTagCompound) targetTag, (NBTTagCompound) sourceTag);
                    continue;
                }
            }

            // Otherwise just copy (replace)
            target.setTag(key, sourceTag.copy());
        }
    }

    /**
     * Translates 1.12.2-style entity NBT used by the shared config into the tag
     * names that 1.7.10 mobs actually read. 1.7.10 {@code EntityLiving} restores
     * equipment from a 5-slot "Equipment" list with numeric item ids; the config
     * stores "HandItems"/"ArmorItems" (1.12.2 layout) with namespaced string ids.
     * Returns the same instance when no translation is needed.
     */
    private static NBTTagCompound translateLegacyEntityNbtFor17(NBTTagCompound source)
    {
        if (source == null || source.hasNoTags())
        {
            return source;
        }
        if (!source.hasKey("HandItems", Constants.NBT.TAG_LIST) && !source.hasKey("ArmorItems", Constants.NBT.TAG_LIST))
        {
            return source;
        }

        NBTTagCompound result = (NBTTagCompound) source.copy();

        NBTTagList equipment = new NBTTagList();
        for (int i = 0; i < 5; i++)
        {
            equipment.appendTag(new NBTTagCompound());
        }

        if (result.hasKey("HandItems", Constants.NBT.TAG_LIST))
        {
            NBTTagList hand = result.getTagList("HandItems", Constants.NBT.TAG_COMPOUND);
            if (hand.tagCount() > 0)
            {
                equipment.setTag(0, translateLegacyItemStackNbtFor17(hand.getCompoundTagAt(0)));
            }
            result.removeTag("HandItems");
        }

        if (result.hasKey("ArmorItems", Constants.NBT.TAG_LIST))
        {
            NBTTagList armor = result.getTagList("ArmorItems", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < Math.min(4, armor.tagCount()); i++)
            {
                // 1.12.2 armor order (boots, legs, chest, helm) -> 1.7.10 slots 1..4
                equipment.setTag(1 + i, translateLegacyItemStackNbtFor17(armor.getCompoundTagAt(i)));
            }
            result.removeTag("ArmorItems");
        }

        result.setTag("Equipment", equipment);
        return result;
    }

    /**
     * Converts a namespaced string item id ("minecraft:bow") into the numeric id
     * that 1.7.10 {@code ItemStack.readFromNBT} expects. Returns a copy.
     */
    private static NBTTagCompound translateLegacyItemStackNbtFor17(NBTTagCompound stackNbt)
    {
        NBTTagCompound copy = (NBTTagCompound) stackNbt.copy();
        NBTBase idTag = copy.getTag("id");
        if (idTag instanceof NBTTagString)
        {
            String id = ((NBTTagString) idTag).getString();
            Item item = lookupItemById(id);
            if (item == null && id.indexOf(':') >= 0)
            {
                item = lookupItemById(id.substring(id.indexOf(':') + 1));
            }
            if (item != null)
            {
                copy.setShort("id", (short) Item.getIdFromItem(item));
            }
        }
        return copy;
    }

    private static Item lookupItemById(String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }
        try
        {
            Object value = Item.itemRegistry.getObject(id);
            if (value instanceof Item)
            {
                return (Item) value;
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    public static boolean canReplaceForGeneration(World world, int x, int y, int z)
    {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.getMaterial().isReplaceable() || block.getMaterial().isLiquid())
        {
            return true;
        }

        return GeneratedBlockRegistry.get(world).isGenerated(x, y, z);
    }

    @Nullable
    public static Block resolveBlock(BlockSetConfig.BlockEntryDefinition entry)
    {
        if (entry == null)
        {
            return null;
        }

        Block block = entry.resolveBlock();
        if (block == null || block == Blocks.air)
        {
            // Special handling for Forestry
            if (entry.registry != null && entry.registry.toLowerCase().contains("forestry")) {
                // Try to find the block via ItemBlock
                try {
                    Item item = (Item) Item.itemRegistry.getObject(entry.registry);
                    if (item instanceof ItemBlock) {
                        Block forestryBlock = ((ItemBlock) item).blockInstance;
                        // noinspection ConstantConditions
                        if (forestryBlock != null && forestryBlock != Blocks.air) {
                            OneBlockUltima.getLogger().info("[BlockUtil] Found Forestry block via ItemBlock: {}", getRegistryString(forestryBlock));
                            block = forestryBlock;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (block == null || block == Blocks.air) {
                block = resolveSpecialPlantBlock(entry.registry);
            }

            if (block == null || block == Blocks.air)
            {
                OneBlockUltima.getLogger().warn("[BlockUtil] Could not resolve block for registry: {}", entry.registry);
                return null;
            }
        }

        return block;
    }

    public static int resolveMeta(BlockSetConfig.BlockEntryDefinition entry, Block block)
    {
        if (block instanceof BlockCrops)
        {
            return 0;
        }
        return entry.meta & 15;
    }

    private static Block resolveSpecialPlantBlock(String registry)
    {
        if (registry == null || registry.isEmpty())
        {
            return null;
        }

        String normalized = registry.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "minecraft:carrot":
            case "carrot":
                return (Block) Block.blockRegistry.getObject("minecraft:carrots");
            case "minecraft:potato":
            case "potato":
                return (Block) Block.blockRegistry.getObject("minecraft:potatoes");
            case "minecraft:wheat_seeds":
            case "wheat_seeds":
            case "minecraft:wheat":
            case "wheat":
                return (Block) Block.blockRegistry.getObject("minecraft:wheat");
            case "minecraft:reeds":
            case "reeds":
            case "minecraft:sugar_cane":
            case "sugar_cane":
                Block reeds = (Block) Block.blockRegistry.getObject("minecraft:reeds");
                if (reeds != null) {
                    return reeds;
                }
                return (Block) Block.blockRegistry.getObject("minecraft:sugar_cane");
        }

        if (normalized.contains("forestry") && normalized.contains("sapling"))
        {
            OneBlockUltima.getLogger().info("[BlockUtil] Trying to resolve Forestry sapling: {}", registry);

            // The most reliable way - via ItemBlock
            try {
                Item item = (Item) Item.itemRegistry.getObject(registry);
                if (item instanceof ItemBlock) {
                    Block block = ((ItemBlock) item).blockInstance;
                    if (block != Blocks.air) {
                        OneBlockUltima.getLogger().info("[BlockUtil] Found Forestry sapling block via ItemBlock: {}", getRegistryString(block));
                        return block;
                    }
                }
            } catch (Exception ignored) {}

            // If ItemBlock lookup failed, try direct lookup
            try {
                Block b = (Block) Block.blockRegistry.getObject("forestry:sapling");
                if (b != null && b != Blocks.air) {
                    OneBlockUltima.getLogger().info("[BlockUtil] Found Forestry sapling block via direct lookup: {}", getRegistryString(b));
                    return b;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Applies NBT tags to an entity (mob)
     * Used when spawning mobs to apply custom properties
     */
    public static void applyNbtToEntity(net.minecraft.entity.Entity entity, @Nullable NBTTagCompound nbtTags)
    {
        if (entity == null || nbtTags == null || nbtTags.hasNoTags())
        {
            return;
        }

        try
        {
            // Get the entity's current NBT tags
            NBTTagCompound entityNbt = new NBTTagCompound();
            entity.writeToNBT(entityNbt);

            // Translate 1.12.2-style tags (HandItems/ArmorItems, string item ids)
            // to the tag names 1.7.10 actually reads (Equipment with numeric ids)
            mergeNbtTags(entityNbt, translateLegacyEntityNbtFor17(nbtTags));

            // Apply the updated tags
            entity.readFromNBT(entityNbt);

            OneBlockUltima.getLogger().info("[Mob Spawn] Applied NBT tags to entity: {}", entity.getCommandSenderName());
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Mob Spawn] Failed to apply NBT tags to entity: {}", entity.getCommandSenderName(), e);
        }
    }
}

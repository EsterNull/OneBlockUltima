package ru.defea.oneblockultima.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCustomBreakable;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.minecraft.item.ItemStack.DECIMALFORMAT;

public final class BlockUtil
{
    private BlockUtil()
    {
    }

    public static IBlockState getReplacementStateForGeneratorPlacement(IBlockState state, IBlockState belowState)
    {
        if (state == null || belowState == null || belowState.getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            return state;
        }

        return toBreakableIfUnbreakable(state);
    }

    /**
     * Replaces an unbreakable block (hardness &lt; 0) with a breakable copy {@link BlockCustomBreakable},
     * so it can be mined like obsidian. All other blocks are returned unchanged.
     */
    @Nullable
    public static IBlockState toBreakableIfUnbreakable(IBlockState state)
    {
        if (state == null || isBreakable(state.getBlock()))
        {
            return state;
        }

        BlockCustomBreakable substitute = ModBlocks.getBreakableFor(state.getBlock());
        if (substitute == null)
        {
            return state;
        }

        int meta;
        try
        {
            meta = state.getBlock().getMetaFromState(state) & 15;
        }
        catch (Exception ex)
        {
            meta = 0;
        }
        return substitute.getDefaultState().withProperty(BlockCustomBreakable.ORIGINAL_META, meta);
    }

    public static boolean isBreakable(Block block)
    {
        if (block == null || block == Blocks.AIR || block instanceof BlockCustomBreakable)
        {
            return true;
        }
        if (block == ModBlocks.ONE_BLOCK_GENERATOR || block == ModBlocks.FLUID_BARRIER)
        {
            return true;
        }
        try
        {
            //noinspection DataFlowIssue
            return !(block.getDefaultState().getBlockHardness(null, null) < 0.0F);
        }
        catch (Exception ex)
        {
            return true;
        }
    }

    /**
     * Places a block with NBT tags applied atomically.
     * Tags are applied BEFORE placing the block for BlockContainer blocks.
     */
    public static void placeBlockWithNBT(World world, BlockPos pos, IBlockState state, @javax.annotation.Nullable NBTTagCompound nbtTags)
    {
        if (world == null || pos == null || state == null)
        {
            return;
        }

        // Handle liquids
        if (state.getMaterial().isLiquid())
        {
            state = normalizeLiquidState(state);
        }

        state = getReplacementStateForGeneratorPlacement(state, world.getBlockState(pos.down()));
        Block block = state.getBlock();
        
        // For BlockContainer blocks with NBT tags - create the TileEntity BEFORE placing
        TileEntity preCreatedTileEntity = null;
        if (nbtTags != null && !nbtTags.hasNoTags() && block instanceof net.minecraft.block.BlockContainer)
        {
            try
            {
                // Create a TileEntity with full NBT data BEFORE placing the block
                TileEntity tileEntity = ((net.minecraft.block.BlockContainer) block).createNewTileEntity(world, block.getMetaFromState(state));
                
                if (tileEntity != null)
                {
                    for (String key : nbtTags.getKeySet())
                    {
                        NBTBase tag = nbtTags.getTag(key);
                        tileEntity.getTileData().setTag(key, tag.copy());
                    }

                    tileEntity.setPos(pos);
                    world.setTileEntity(pos, tileEntity);

                    if (tileEntity instanceof IInventory) {
                        IInventory inv = (IInventory) tileEntity;
                        NBTTagCompound nbt = tileEntity.getTileData();
                        String lootTableKey = "LootTable";

                        if (nbt.hasKey(lootTableKey, Constants.NBT.TAG_STRING)) {
                            String lootTableId = nbt.getString(lootTableKey);
                            ResourceLocation loc = new ResourceLocation(lootTableId);

                            LootTableManager manager = world.getLootTableManager();
                            LootTable table = manager.getLootTableFromLocation(loc);

                            LootContext.Builder contextBuilder = new LootContext.Builder((net.minecraft.world.WorldServer) world);
                            LootContext context = contextBuilder.build();

                            table.fillInventory(inv, world.rand, context);
                            nbt.removeTag(lootTableKey);
                            preCreatedTileEntity = tileEntity;
                            tileEntity.markDirty();
                        }
                    }

                    OneBlockUltima.getLogger().info("[Generator] Pre-configured TileEntity at {} with NBT tags", pos);
                }
                else
                {
                    OneBlockUltima.getLogger().warn("[Generator] createNewTileEntity returned null for block {}", block.getRegistryName());
                }
            }
            catch (Exception e)
            {
                OneBlockUltima.getLogger().error("[Generator] Failed to pre-configure TileEntity for block at {}", pos, e);
            }
        }
        
        // Place the block
        world.setBlockState(pos, state, 3);
        if (preCreatedTileEntity != null)
        {
            // Remove the old TileEntity if present
            world.removeTileEntity(pos);
            // Set ours
            world.setTileEntity(pos, preCreatedTileEntity);
            preCreatedTileEntity.setPos(pos);
            preCreatedTileEntity.markDirty();
        }

        // If NBT tags exist but the block is not a BlockContainer, try applying them after placement
        if (nbtTags != null && !nbtTags.hasNoTags() && !(block instanceof net.minecraft.block.BlockContainer))
        {
            applyNbtToBlock(world, pos, nbtTags);
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
                return net.minecraft.util.text.translation.I18n.translateToLocal(nbttagcompound.getString("LocName"));
            }
        }

        return net.minecraft.util.text.translation.I18n.translateToLocal(net.minecraft.util.text.translation.I18n.translateToLocal(block.getUnlocalizedName()) + ".name").trim();
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
            if (nbtTagCompound.hasKey("HideFlags", Constants.NBT.TAG_ANY_NUMERIC)) {
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
                    Enchantment enchantment = Enchantment.getEnchantmentByID(k);

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
                    tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("block.color", String.format("#%06X", nbttagcompound1.getInteger("color"))));
                }
                else
                {
                    tooltip.add(TextFormatting.ITALIC + net.minecraft.util.text.translation.I18n.translateToLocal("block.dyed"));
                }
            }

            if (nbttagcompound1.getTagId("Lore") == 9)
            {
                NBTTagList nbttaglist3 = nbttagcompound1.getTagList("Lore", 8);

                if (!nbttaglist3.hasNoTags())
                {
                    for (int l1 = 0; l1 < nbttaglist3.tagCount(); ++l1)
                    {
                        tooltip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + nbttaglist3.getStringTagAt(l1));
                    }
                }
            }
        }

        for (EntityEquipmentSlot entityequipmentslot : EntityEquipmentSlot.values())
        {
            Multimap<String, AttributeModifier> multimap = getAttributeModifiers(entityequipmentslot, nbtTagCompound);

            if (!multimap.isEmpty() && (i1 & 2) == 0)
            {
                tooltip.add("");
                tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("block.modifiers." + entityequipmentslot.getName()));

                for (Map.Entry<String, AttributeModifier> entry : multimap.entries())
                {
                    AttributeModifier attributemodifier = entry.getValue();
                    double d0 = attributemodifier.getAmount();

                    double d1;

                    if (attributemodifier.getOperation() != 1 && attributemodifier.getOperation() != 2)
                    {
                        d1 = d0;
                    }
                    else
                    {
                        d1 = d0 * 100.0D;
                    }

                    if (d0 > 0.0D)
                    {
                        tooltip.add(TextFormatting.BLUE + " " + net.minecraft.util.text.translation.I18n.translateToLocalFormatted("attribute.modifier.plus." + attributemodifier.getOperation(), DECIMALFORMAT.format(d1), net.minecraft.util.text.translation.I18n.translateToLocal("attribute.name." + entry.getKey())));
                    }
                    else if (d0 < 0.0D)
                    {
                        d1 = d1 * -1.0D;
                        tooltip.add(TextFormatting.RED + " " + net.minecraft.util.text.translation.I18n.translateToLocalFormatted("attribute.modifier.take." + attributemodifier.getOperation(), DECIMALFORMAT.format(d1), net.minecraft.util.text.translation.I18n.translateToLocal("attribute.name." + entry.getKey())));
                    }
                }
            }
        }

        if (isAdvanced)
        {

            tooltip.add(TextFormatting.DARK_GRAY + Block.REGISTRY.getNameForObject(resolvedBlock).toString());

            if (hasDisplayName)
            {
                tooltip.add(TextFormatting.DARK_GRAY + net.minecraft.util.text.translation.I18n.translateToLocalFormatted("block.nbt_tags", nbtTagCompound.getKeySet().size()));
            }
        }

        return tooltip;
    }

    public static Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot equipmentSlot, NBTTagCompound nbtTagCompound)
    {
        Multimap<String, AttributeModifier> multimap;
        boolean hasTagCompound = nbtTagCompound != null;

        if (hasTagCompound && nbtTagCompound.hasKey("AttributeModifiers", Constants.NBT.TAG_LIST))
        {
            multimap = HashMultimap.create();
            NBTTagList nbttaglist = nbtTagCompound.getTagList("AttributeModifiers", Constants.NBT.TAG_LIST);

            for (int i = 0; i < nbttaglist.tagCount(); ++i)
            {
                NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
                AttributeModifier attributemodifier = SharedMonsterAttributes.readAttributeModifierFromNBT(nbttagcompound);

                if (attributemodifier != null && (!nbttagcompound.hasKey("Slot", Constants.NBT.TAG_STRING) || nbttagcompound.getString("Slot").equals(equipmentSlot.getName())) && attributemodifier.getID().getLeastSignificantBits() != 0L && attributemodifier.getID().getMostSignificantBits() != 0L)
                {
                    multimap.put(nbttagcompound.getString("AttributeName"), attributemodifier);
                }
            }
        }
        else
        {
            multimap = HashMultimap.create();
        }

        return multimap;
    }

    /**
     * Normalizes liquids (water and lava) to their still states
     */
    private static IBlockState normalizeLiquidState(IBlockState state)
    {
        if (!state.getMaterial().isLiquid())
        {
            return state;
        }

        Block block = state.getBlock();
        if (block == Blocks.FLOWING_WATER)
        {
            return Blocks.WATER.getDefaultState();
        }

        if (block == Blocks.FLOWING_LAVA)
        {
            return Blocks.LAVA.getDefaultState();
        }

        if (block instanceof IFluidBlock)
        {
            Fluid fluid = ((IFluidBlock) block).getFluid();
            if (fluid != null)
            {
                Block stillBlock = fluid.getBlock();
                if (stillBlock != null && stillBlock != block)
                {
                    return stillBlock.getDefaultState();
                }
            }
        }

        Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
        if (fluid != null)
        {
            Block stillBlock = fluid.getBlock();
            if (stillBlock != null && stillBlock != block)
            {
                return stillBlock.getDefaultState();
            }
        }

        return state;
    }

    /**
     * Universal application of NBT tags to the block at the given position
     */
    public static void applyNbtToBlock(World world, BlockPos pos, NBTTagCompound nbtTags)
    {
        if (world == null || pos == null || nbtTags == null || nbtTags.hasNoTags())
        {
            return;
        }

        try
        {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null)
            {
                // Read the current TileEntity state
                NBTTagCompound tileNbt = new NBTTagCompound();
                tileEntity.writeToNBT(tileNbt);

                // Add all tags from nbtTags into tileNbt (overwrite if already present)
                for (String key : nbtTags.getKeySet())
                {
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

                // Update the block
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 3);

                OneBlockUltima.getLogger().info("[Generator] Applied NBT tags to TileEntity at {}: {}", pos, nbtTags);
            }
            else
            {
                OneBlockUltima.getLogger().debug("[Generator] No TileEntity found at {} for NBT application", pos);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Generator] Failed to apply NBT tags to block at {}", pos, e);
        }
    }

    public static boolean isFullBlock(net.minecraft.block.Block block, int meta)
    {
        try
        {
            net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
            if (item == Items.AIR)
            {
                return false;
            }

            net.minecraft.block.state.IBlockState state = null;
            try
            {
                state = block.getStateFromMeta(meta);
            }
            catch (Exception ex)
            {
                try
                {
                    state = block.getDefaultState();
                }
                catch (Exception ignored) {}
            }

            if (state == null) return false;

            return block.isFullBlock(state) && block.isFullCube(state);
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

        for (String key : source.getKeySet())
        {
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

    public static boolean canReplaceForGeneration(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial().isReplaceable())
        {
            return true;
        }

        return GeneratedBlockRegistry.get(world).isGenerated(pos);
    }

    public static IBlockState toState(BlockSetConfig.BlockEntryDefinition entry)
    {
        Block block = entry.resolveBlock();
        if (block == null || block == Blocks.AIR)
        {
            // Special handling for Forestry
            if (entry.registry != null && entry.registry.toLowerCase().contains("forestry")) {
                // Try to find the block via ItemBlock
                try {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.registry));
                    if (item instanceof ItemBlock) {
                        Block forestryBlock = ((ItemBlock) item).getBlock();
                        // noinspection ConstantConditions
                        if (forestryBlock != null && forestryBlock != Blocks.AIR) {
                            OneBlockUltima.getLogger().info("[BlockUtil] Found Forestry block via ItemBlock: {}", forestryBlock.getRegistryName());
                            block = forestryBlock;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (block == null || block == Blocks.AIR) {
                block = resolveSpecialPlantBlock(entry.registry);
            }

            if (block == null || block == Blocks.AIR)
            {
                OneBlockUltima.getLogger().warn("[BlockUtil] Could not resolve block for registry: {}", entry.registry);
                return null;
            }
        }

        try
        {
            IBlockState state;

            // For Forestry saplings always use the default state (meta is ignored)
            if (entry.registry != null && entry.registry.toLowerCase().contains("forestry") &&
                    entry.registry.toLowerCase().contains("sapling"))
            {
                state = block.getDefaultState();
                OneBlockUltima.getLogger().info("[BlockUtil] Using default state for Forestry sapling: {}", state);
            }
            else
            {
                state = block.getStateFromMeta(entry.meta);
            }

            if (state.getBlock() == Blocks.AIR)
            {
                state = block.getDefaultState();
            }
            if (state.getBlock() == Blocks.AIR)
            {
                return null;
            }

            if (block instanceof BlockCrops && state.getBlock() == block)
            {
                return block.getDefaultState();
            }

            OneBlockUltima.getLogger().debug("[BlockUtil] Resolved block: {} -> {} with meta: {}", entry.registry, block.getRegistryName(), entry.meta);
            return state;
        }
        catch (Exception ex)
        {
            OneBlockUltima.getLogger().debug("[BlockUtil] Exception getting state from meta for {}, using default state", entry.registry, ex);
            IBlockState defaultState = block.getDefaultState();
            return defaultState.getBlock() == Blocks.AIR ? null : defaultState;
        }
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
                return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:carrots"));
            case "minecraft:potato":
            case "potato":
                return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:potatoes"));
            case "minecraft:wheat_seeds":
            case "wheat_seeds":
            case "minecraft:wheat":
            case "wheat":
                return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:wheat"));
            case "minecraft:beetroot_seeds":
            case "beetroot_seeds":
            case "minecraft:beetroot":
            case "beetroot":
                return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:beetroots"));
            case "minecraft:reeds":
            case "reeds":
            case "minecraft:sugar_cane":
            case "sugar_cane":
                Block reeds = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:reeds"));
                if (reeds != null) {
                    return reeds;
                }
                return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:sugar_cane"));
        }

        if (normalized.contains("forestry") && normalized.contains("sapling"))
        {
            OneBlockUltima.getLogger().info("[BlockUtil] Trying to resolve Forestry sapling: {}", registry);

            // The most reliable way - via ItemBlock
            try {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(registry));
                if (item instanceof ItemBlock) {
                    Block block = ((ItemBlock) item).getBlock();
                    if (block != Blocks.AIR) {
                        OneBlockUltima.getLogger().info("[BlockUtil] Found Forestry sapling block via ItemBlock: {}", block.getRegistryName());
                        return block;
                    }
                }
            } catch (Exception ignored) {}

            // If ItemBlock lookup failed, try direct lookup
            try {
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("forestry:sapling"));
                if (b != null && b != Blocks.AIR) {
                    OneBlockUltima.getLogger().info("[BlockUtil] Found Forestry sapling block via direct lookup: {}", b.getRegistryName());
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

            // Merge tags recursively
            mergeNbtTags(entityNbt, nbtTags);

            // Apply the updated tags
            entity.readFromNBT(entityNbt);

            OneBlockUltima.getLogger().info("[Mob Spawn] Applied NBT tags to entity: {}", entity.getName());
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Mob Spawn] Failed to apply NBT tags to entity: {}", entity.getName(), e);
        }
    }
}

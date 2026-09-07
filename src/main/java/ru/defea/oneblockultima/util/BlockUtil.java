package ru.defea.oneblockultima.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCustomBreakable;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;
import static net.minecraft.nbt.Tag.TAG_STRING;
import static net.minecraft.nbt.Tag.TAG_INT;

public final class BlockUtil
{
    private BlockUtil()
    {
    }

    private static final DecimalFormat DECIMALFORMAT = new DecimalFormat("########0.########");

    private static final Map<String, String> LEGACY_ID_RENAMES = new java.util.HashMap<>();
    private static final Map<String, String> LEGACY_META_BLOCKS = new java.util.HashMap<>();
    static
    {
        // 1.12 -> 1.21 block renames (base sets reference these legacy ids)
        LEGACY_ID_RENAMES.put("minecraft:grass", "minecraft:grass_block");
        LEGACY_ID_RENAMES.put("minecraft:log", "minecraft:oak_log");
        LEGACY_ID_RENAMES.put("minecraft:log2", "minecraft:acacia_log");
        LEGACY_ID_RENAMES.put("minecraft:planks", "minecraft:oak_planks");
        LEGACY_ID_RENAMES.put("minecraft:stonebrick", "minecraft:stone_bricks");
        LEGACY_ID_RENAMES.put("minecraft:red_flower", "minecraft:poppy");
        LEGACY_ID_RENAMES.put("minecraft:yellow_flower", "minecraft:dandelion");
        LEGACY_ID_RENAMES.put("minecraft:tallgrass", "minecraft:short_grass");
        LEGACY_ID_RENAMES.put("minecraft:double_plant", "minecraft:sunflower");
        LEGACY_ID_RENAMES.put("minecraft:reeds", "minecraft:sugar_cane");
        LEGACY_ID_RENAMES.put("minecraft:nether_brick", "minecraft:nether_bricks");
        LEGACY_ID_RENAMES.put("minecraft:magma", "minecraft:magma_block");
        LEGACY_ID_RENAMES.put("minecraft:quartz_ore", "minecraft:nether_quartz_ore");
        LEGACY_ID_RENAMES.put("minecraft:wool", "minecraft:white_wool");
        LEGACY_ID_RENAMES.put("minecraft:carpet", "minecraft:white_carpet");
        LEGACY_ID_RENAMES.put("minecraft:stained_hardened_clay", "minecraft:white_terracotta");
        // 1.12 -> 1.21 mob renames
        LEGACY_ID_RENAMES.put("minecraft:zombie_pigman", "minecraft:zombified_piglin");
        LEGACY_ID_RENAMES.put("minecraft:villager_golem", "minecraft:iron_golem");
        LEGACY_ID_RENAMES.put("minecraft:carrot", "minecraft:carrots");
        LEGACY_ID_RENAMES.put("minecraft:potato", "minecraft:potatoes");
        // block renames that also carry a color/variant meta
        LEGACY_ID_RENAMES.put("minecraft:stained_glass", "minecraft:white_stained_glass");
        LEGACY_ID_RENAMES.put("minecraft:stained_glass_pane", "minecraft:white_stained_glass_pane");
        LEGACY_ID_RENAMES.put("minecraft:golden_rail", "minecraft:powered_rail");
        LEGACY_ID_RENAMES.put("minecraft:unlit_redstone_torch", "minecraft:redstone_torch");
        LEGACY_ID_RENAMES.put("minecraft:lit_redstone_lamp", "minecraft:redstone_lamp");
        LEGACY_ID_RENAMES.put("minecraft:lit_furnace", "minecraft:furnace");
        LEGACY_ID_RENAMES.put("minecraft:lit_redstone_ore", "minecraft:redstone_ore");
        LEGACY_ID_RENAMES.put("minecraft:lit_pumpkin", "minecraft:jack_o_lantern");
        LEGACY_ID_RENAMES.put("minecraft:hardened_clay", "minecraft:terracotta");
        LEGACY_ID_RENAMES.put("minecraft:end_bricks", "minecraft:end_stone_bricks");
        LEGACY_ID_RENAMES.put("minecraft:slime", "minecraft:slime_block");
        LEGACY_ID_RENAMES.put("minecraft:melon_block", "minecraft:melon");
        LEGACY_ID_RENAMES.put("minecraft:waterlily", "minecraft:lily_pad");
        LEGACY_ID_RENAMES.put("minecraft:noteblock", "minecraft:note_block");
        LEGACY_ID_RENAMES.put("minecraft:mob_spawner", "minecraft:spawner");
        LEGACY_ID_RENAMES.put("minecraft:snow", "minecraft:snow_block");
        LEGACY_ID_RENAMES.put("minecraft:snow_layer", "minecraft:snow");
        LEGACY_ID_RENAMES.put("minecraft:fence_gate", "minecraft:oak_fence_gate");
        LEGACY_ID_RENAMES.put("minecraft:wooden_door", "minecraft:oak_door");
        LEGACY_ID_RENAMES.put("minecraft:wooden_trapdoor", "minecraft:oak_trapdoor");
        LEGACY_ID_RENAMES.put("minecraft:wooden_slab", "minecraft:oak_slab");
        LEGACY_ID_RENAMES.put("minecraft:wooden_stairs", "minecraft:oak_stairs");
        LEGACY_ID_RENAMES.put("minecraft:stone_slab", "minecraft:stone_slab");
        LEGACY_ID_RENAMES.put("minecraft:double_stone_slab", "minecraft:stone_slab");
        LEGACY_ID_RENAMES.put("minecraft:cobblestone_wall", "minecraft:cobblestone_wall");
        LEGACY_ID_RENAMES.put("minecraft:anvil", "minecraft:anvil");
        // 1.12 meta (variant/color) -> 1.21 identifier, "id:meta" -> "new:id"
        putMetaColors("minecraft:wool",
                "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool", "minecraft:light_blue_wool",
                "minecraft:yellow_wool", "minecraft:lime_wool", "minecraft:pink_wool", "minecraft:gray_wool",
                "minecraft:light_gray_wool", "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
                "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool", "minecraft:black_wool");
        putMetaColors("minecraft:carpet",
                "minecraft:white_carpet", "minecraft:orange_carpet", "minecraft:magenta_carpet", "minecraft:light_blue_carpet",
                "minecraft:yellow_carpet", "minecraft:lime_carpet", "minecraft:pink_carpet", "minecraft:gray_carpet",
                "minecraft:light_gray_carpet", "minecraft:cyan_carpet", "minecraft:purple_carpet", "minecraft:blue_carpet",
                "minecraft:brown_carpet", "minecraft:green_carpet", "minecraft:red_carpet", "minecraft:black_carpet");
        putMetaColors("minecraft:stained_glass",
                "minecraft:white_stained_glass", "minecraft:orange_stained_glass", "minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass",
                "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass", "minecraft:pink_stained_glass", "minecraft:gray_stained_glass",
                "minecraft:light_gray_stained_glass", "minecraft:cyan_stained_glass", "minecraft:purple_stained_glass", "minecraft:blue_stained_glass",
                "minecraft:brown_stained_glass", "minecraft:green_stained_glass", "minecraft:red_stained_glass", "minecraft:black_stained_glass");
        putMetaColors("minecraft:stained_glass_pane",
                "minecraft:white_stained_glass_pane", "minecraft:orange_stained_glass_pane", "minecraft:magenta_stained_glass_pane", "minecraft:light_blue_stained_glass_pane",
                "minecraft:yellow_stained_glass_pane", "minecraft:lime_stained_glass_pane", "minecraft:pink_stained_glass_pane", "minecraft:gray_stained_glass_pane",
                "minecraft:light_gray_stained_glass_pane", "minecraft:cyan_stained_glass_pane", "minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane",
                "minecraft:brown_stained_glass_pane", "minecraft:green_stained_glass_pane", "minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane");
        putMetaColors("minecraft:stained_hardened_clay",
                "minecraft:white_terracotta", "minecraft:orange_terracotta", "minecraft:magenta_terracotta", "minecraft:light_blue_terracotta",
                "minecraft:yellow_terracotta", "minecraft:lime_terracotta", "minecraft:pink_terracotta", "minecraft:gray_terracotta",
                "minecraft:light_gray_terracotta", "minecraft:cyan_terracotta", "minecraft:purple_terracotta", "minecraft:blue_terracotta",
                "minecraft:brown_terracotta", "minecraft:green_terracotta", "minecraft:red_terracotta", "minecraft:black_terracotta");
        putMetaColors("minecraft:concrete",
                "minecraft:white_concrete", "minecraft:orange_concrete", "minecraft:magenta_concrete", "minecraft:light_blue_concrete",
                "minecraft:yellow_concrete", "minecraft:lime_concrete", "minecraft:pink_concrete", "minecraft:gray_concrete",
                "minecraft:light_gray_concrete", "minecraft:cyan_concrete", "minecraft:purple_concrete", "minecraft:blue_concrete",
                "minecraft:brown_concrete", "minecraft:green_concrete", "minecraft:red_concrete", "minecraft:black_concrete");
        putMetaColors("minecraft:concrete_powder",
                "minecraft:white_concrete_powder", "minecraft:orange_concrete_powder", "minecraft:magenta_concrete_powder", "minecraft:light_blue_concrete_powder",
                "minecraft:yellow_concrete_powder", "minecraft:lime_concrete_powder", "minecraft:pink_concrete_powder", "minecraft:gray_concrete_powder",
                "minecraft:light_gray_concrete_powder", "minecraft:cyan_concrete_powder", "minecraft:purple_concrete_powder", "minecraft:blue_concrete_powder",
                "minecraft:brown_concrete_powder", "minecraft:green_concrete_powder", "minecraft:red_concrete_powder", "minecraft:black_concrete_powder");
        putMetaColors("minecraft:planks",
                "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks",
                "minecraft:acacia_planks", "minecraft:dark_oak_planks");
        putMetaColors("minecraft:log",
                "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:jungle_log");
        putMetaColors("minecraft:log2",
                "minecraft:acacia_log", "minecraft:dark_oak_log");
        putMetaColors("minecraft:sapling",
                "minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling", "minecraft:jungle_sapling",
                "minecraft:acacia_sapling", "minecraft:dark_oak_sapling");
        putMetaColors("minecraft:stone",
                "minecraft:stone", "minecraft:granite", "minecraft:polished_granite", "minecraft:diorite",
                "minecraft:polished_diorite", "minecraft:andesite", "minecraft:polished_andesite");
        putMetaColors("minecraft:stonebrick",
                "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks");
        putMetaColors("minecraft:sandstone",
                "minecraft:sandstone", "minecraft:chiseled_sandstone", "minecraft:cut_sandstone");
        putMetaColors("minecraft:red_sandstone",
                "minecraft:red_sandstone", "minecraft:chiseled_red_sandstone", "minecraft:cut_red_sandstone");
        putMetaColors("minecraft:quartz_block",
                "minecraft:quartz_block", "minecraft:chiseled_quartz_block", "minecraft:quartz_pillar");
        putMetaColors("minecraft:cobblestone_wall",
                "minecraft:cobblestone_wall", "minecraft:mossy_cobblestone_wall");
        putMetaColors("minecraft:sponge",
                "minecraft:sponge", "minecraft:wet_sponge");
        putMetaColors("minecraft:prismarine",
                "minecraft:prismarine", "minecraft:prismarine_bricks", "minecraft:dark_prismarine");
        putMetaColors("minecraft:monster_egg",
                "minecraft:stone", "minecraft:cobblestone", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks");
        putMetaColors("minecraft:red_flower",
                "minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet",
                "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip",
                "minecraft:oxeye_daisy");
        putMetaColors("minecraft:double_plant",
                "minecraft:sunflower", "minecraft:lilac", "minecraft:tall_grass", "minecraft:large_fern",
                "minecraft:rose_bush", "minecraft:peony");
    }

    private static void putMetaColors(String base, String... ids)
    {
        for (int meta = 0; meta < ids.length; meta++)
        {
            LEGACY_META_BLOCKS.put(base + ":" + meta, ids[meta]);
        }
    }

    public static String normalizeLegacyId(String id)
    {
        return normalizeLegacyId(id, 0);
    }

    public static String normalizeLegacyId(String id, int meta)
    {
        if (id == null) return null;
        int idx = id.lastIndexOf(':');
        if (idx > 0 && idx < id.length() - 1)
        {
            String tail = id.substring(idx + 1);
            if (tail.matches("\\d+"))
            {
                id = id.substring(0, idx);
            }
        }
        if (meta > 0)
        {
            int masked = meta;
            if ("minecraft:leaves".equals(id) || "minecraft:leaves2".equals(id))
            {
                masked = meta & 3;
            }
            String mapped = LEGACY_META_BLOCKS.get(id + ":" + masked);
            if (mapped != null)
            {
                return mapped;
            }
        }
        return LEGACY_ID_RENAMES.getOrDefault(id, id);
    }

    /**
     * Normalizes a block registry id + meta to the modern 1.21 identifier.
     * @return entry whose key is the new registry id and value the adjusted meta
     *         (0 when the meta was consumed by a variant mapping, unchanged otherwise)
     */
    public static java.util.Map.Entry<String, Integer> normalizeBlockRegistryAndMeta(String registry, int meta)
    {
        if (registry == null)
        {
            return null;
        }
        String normalized = normalizeLegacyId(registry, meta);
        int masked = meta;
        if ("minecraft:leaves".equals(registry) || "minecraft:leaves2".equals(registry))
        {
            masked = meta & 3;
        }
        if (LEGACY_META_BLOCKS.containsKey(registry + ":" + masked))
        {
            return new java.util.AbstractMap.SimpleEntry<>(normalized, 0);
        }
        return new java.util.AbstractMap.SimpleEntry<>(normalized, meta);
    }

    public static BlockState getReplacementStateForGeneratorPlacement(BlockState state, BlockState belowState)
    {
        if (state == null || belowState == null || belowState.getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            return state;
        }

        return toBreakableIfUnbreakable(state);
    }

    @Nullable
    public static BlockState toBreakableIfUnbreakable(BlockState state)
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
            meta = Block.getId(state) & 15;
        }
        catch (Exception ex)
        {
            meta = 0;
        }
        return substitute.defaultBlockState().setValue(BlockCustomBreakable.ORIGINAL_META, meta);
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
            return !(block.defaultBlockState().getDestroySpeed(null, null) < 0.0F);
        }
        catch (Exception ex)
        {
            return true;
        }
    }

    public static void placeBlockWithNBT(Level world, BlockPos pos, BlockState state, @javax.annotation.Nullable CompoundTag nbtTags)
    {
        if (world == null || pos == null || state == null)
        {
            return;
        }

        if (!state.getFluidState().isEmpty())
        {
            state = normalizeLiquidState(state);
        }

        state = getReplacementStateForGeneratorPlacement(state, world.getBlockState(pos.below()));
        Block block = state.getBlock();

        BlockEntity preCreatedTileEntity = null;
        if (nbtTags != null && !nbtTags.isEmpty() && block instanceof EntityBlock)
        {
            try
            {
                BlockEntity tileEntity = ((EntityBlock) block).newBlockEntity(pos, state);
                if (tileEntity != null)
                {
                    CompoundTag data = new CompoundTag();
                    for (String key : nbtTags.getAllKeys())
                    {
                        data.put(key, nbtTags.get(key).copy());
                    }
                    tileEntity.loadWithComponents(data, net.minecraft.core.RegistryAccess.EMPTY);

                    world.setBlockEntity(tileEntity);

                    if (tileEntity instanceof Container)
                    {
                        Container inv = (Container) tileEntity;
                        String lootTableKey = "LootTable";

                        if (data.contains(lootTableKey, TAG_STRING))
                        {
                            String lootTableId = data.getString(lootTableKey);
                            ResourceLocation loc = ResourceLocation.parse(lootTableId);

                            if (world instanceof ServerLevel)
                            {
                                LootTable table = ((ServerLevel) world).getServer().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE).get(loc);
                                LootParams params = new LootParams.Builder((ServerLevel) world).create(LootContextParamSets.EMPTY);
                                table.fill(inv, params, world.random.nextLong());
                                data.remove(lootTableKey);
                                preCreatedTileEntity = tileEntity;
                                tileEntity.setChanged();
                            }
                        }
                    }

                    OneBlockUltima.logDebug("[Generator] Pre-configured TileEntity at {} with NBT tags", pos);
                }
                else
                {
                    OneBlockUltima.logDebugWarn("[Generator] newBlockEntity returned null for block {}", block);
                }
            }
            catch (Exception e)
            {
                OneBlockUltima.getLogger().error("[Generator] Failed to pre-configure TileEntity for block at {}", pos, e);
            }
        }

        world.setBlock(pos, state, 3);
        if (preCreatedTileEntity != null)
        {
            world.removeBlockEntity(pos);
            world.setBlockEntity(preCreatedTileEntity);
            preCreatedTileEntity.setChanged();
        }

        if (nbtTags != null && !nbtTags.isEmpty() && !(block instanceof EntityBlock))
        {
            applyNbtToBlock(world, pos, nbtTags);
        }
    }

    public static String getDisplayName(Block block, CompoundTag nbtTagCompound)
    {
        CompoundTag nbttagcompound = nbtTagCompound != null && nbtTagCompound.contains("display", TAG_COMPOUND) ? nbtTagCompound.getCompound("display") : null;

        if (nbttagcompound != null)
        {
            if (nbttagcompound.contains("Name", TAG_STRING))
            {
                return nbttagcompound.getString("Name");
            }

            if (nbttagcompound.contains("LocName", TAG_STRING))
            {
                return net.minecraft.network.chat.Component.translatable(nbttagcompound.getString("LocName")).getString();
            }
        }

        return net.minecraft.network.chat.Component.translatable(block.getDescriptionId()).getString().trim();
    }

    public static List<String> getTooltip(BlockSetConfig.BlockEntryDefinition hoveredEntry, boolean isAdvanced)
    {
        java.util.List<String> tooltip = new java.util.ArrayList<>();
        Block resolvedBlock = hoveredEntry.resolveBlock();
        boolean hasTagCompound = hoveredEntry.nbtTags != null;
        CompoundTag nbtTagCompound = hasTagCompound && hoveredEntry.nbtTags.contains("display", TAG_COMPOUND) ? hoveredEntry.nbtTags.getCompound("display") : null;
        boolean hasDisplayName = nbtTagCompound != null && nbtTagCompound.contains("Name", TAG_STRING);

        String s = getDisplayName(resolvedBlock, nbtTagCompound);
        if (s == null || s.isEmpty())
        {
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

            int i = Block.getId(resolvedBlock.defaultBlockState());
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
        try
        {
            if (nbtTagCompound != null && nbtTagCompound.contains("HideFlags", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC))
            {
                i1 = nbtTagCompound.getInt("HideFlags");
            }
        }
        catch (Exception ignored) {}

        if ((i1 & 1) == 0)
        {
            try
            {
                ListTag nbttaglist = nbtTagCompound != null && nbtTagCompound.contains("ench", TAG_LIST) ? nbtTagCompound.getList("ench", TAG_COMPOUND) : new ListTag();

                for (int j = 0; j < nbttaglist.size(); ++j)
                {
                    CompoundTag nbttagcompound = nbttaglist.getCompound(j);
                    int k = nbttagcompound.getInt("id");
                    int l = nbttagcompound.getInt("lvl");
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.level != null)
                    {
                        net.minecraft.core.Registry<Enchantment> enchReg = mc.level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                        java.util.Optional<net.minecraft.core.Holder.Reference<Enchantment>> holderOpt = enchReg.getHolder(k);
                        if (holderOpt.isPresent())
                        {
                            tooltip.add(Enchantment.getFullname(holderOpt.get(), l).getString());
                        }
                    }
                }
            }
            catch (Exception ignored) {}
        }

        if (hasDisplayName)
        {
            CompoundTag nbttagcompound1 = nbtTagCompound.getCompound("display");

            if (nbttagcompound1.contains("color", TAG_INT))
            {
                if (isAdvanced)
                {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("block.color", String.format("#%06X", nbttagcompound1.getInt("color"))).getString());
                }
                else
                {
                    tooltip.add(ChatFormatting.ITALIC + net.minecraft.network.chat.Component.translatable("block.dyed").getString());
                }
            }

            if (nbttagcompound1.contains("Lore") && nbttagcompound1.getTagType("Lore") == TAG_LIST)
            {
                ListTag nbttaglist3 = nbttagcompound1.getList("Lore", TAG_STRING);

                if (!nbttaglist3.isEmpty())
                {
                    for (int l1 = 0; l1 < nbttaglist3.size(); ++l1)
                    {
                        tooltip.add(ChatFormatting.DARK_PURPLE + "" + ChatFormatting.ITALIC + nbttaglist3.getString(l1));
                    }
                }
            }
        }

        for (EquipmentSlot entityequipmentslot : EquipmentSlot.values())
        {
            Multimap<String, AttributeModifier> multimap = getAttributeModifiers(entityequipmentslot, nbtTagCompound);

            if (!multimap.isEmpty() && (i1 & 2) == 0)
            {
                tooltip.add("");
                tooltip.add(net.minecraft.network.chat.Component.translatable("block.modifiers." + entityequipmentslot.getName()).getString());

                for (Map.Entry<String, AttributeModifier> entry : multimap.entries())
                {
                    AttributeModifier attributemodifier = entry.getValue();
                    double d0 = attributemodifier.amount();

                    double d1;

                    if (attributemodifier.operation() != AttributeModifier.Operation.ADD_VALUE && attributemodifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                    {
                        d1 = d0;
                    }
                    else
                    {
                        d1 = d0 * 100.0D;
                    }

                    if (d0 > 0.0D)
                    {
                        tooltip.add(ChatFormatting.BLUE + " " + net.minecraft.network.chat.Component.translatable("attribute.modifier.plus." + attributemodifier.operation().id(), DECIMALFORMAT.format(d1), net.minecraft.network.chat.Component.translatable("attribute.name." + entry.getKey()).getString()).getString());
                    }
                    else if (d0 < 0.0D)
                    {
                        d1 = d1 * -1.0D;
                        tooltip.add(ChatFormatting.RED + " " + net.minecraft.network.chat.Component.translatable("attribute.modifier.take." + attributemodifier.operation().id(), DECIMALFORMAT.format(d1), net.minecraft.network.chat.Component.translatable("attribute.name." + entry.getKey()).getString()).getString());
                    }
                }
            }
        }

        if (isAdvanced)
        {
            tooltip.add(ChatFormatting.DARK_GRAY + BuiltInRegistries.BLOCK.getKey(resolvedBlock).toString());

            if (hasDisplayName)
            {
                tooltip.add(ChatFormatting.DARK_GRAY + net.minecraft.network.chat.Component.translatable("block.nbt_tags", nbtTagCompound.getAllKeys().size()).getString());
            }
        }

        return tooltip;
    }

    public static Multimap<String, AttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot, CompoundTag nbtTagCompound)
    {
        Multimap<String, AttributeModifier> multimap;
        boolean hasTagCompound = nbtTagCompound != null;

        if (hasTagCompound && nbtTagCompound.contains("AttributeModifiers", TAG_LIST))
        {
            multimap = HashMultimap.create();
            ListTag nbttaglist = nbtTagCompound.getList("AttributeModifiers", TAG_LIST);

            for (int i = 0; i < nbttaglist.size(); ++i)
            {
                CompoundTag nbttagcompound = nbttaglist.getCompound(i);
                double amount = nbttagcompound.getDouble("Amount");
                int opId = nbttagcompound.getInt("Operation");
                AttributeModifier.Operation op = AttributeModifier.Operation.BY_ID.apply(opId);
                ResourceLocation id = ResourceLocation.parse(nbttagcompound.getString("Name"));
                AttributeModifier attributemodifier = new AttributeModifier(id, amount, op);

                if (attributemodifier != null && (!nbttagcompound.contains("Slot", TAG_STRING) || nbttagcompound.getString("Slot").equals(equipmentSlot.getName())))
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

    private static BlockState normalizeLiquidState(BlockState state)
    {
        if (state.getFluidState().isEmpty())
        {
            return state;
        }

        Block block = state.getBlock();
        if (block == Blocks.WATER)
        {
            return Blocks.WATER.defaultBlockState();
        }

        if (block == Blocks.LAVA)
        {
            return Blocks.LAVA.defaultBlockState();
        }

        net.minecraft.world.level.material.FluidState fluidState = state.getFluidState();
        Block stillBlock = net.minecraft.world.level.block.Blocks.WATER; // fallback; still-block resolution handled below
        try
        {
            net.minecraft.world.level.material.Fluid fluid = fluidState.getType();
            if (fluid != null && fluid.defaultFluidState().createLegacyBlock().getBlock() != block)
            {
                return fluid.defaultFluidState().createLegacyBlock();
            }
        }
        catch (Exception ignored) {}

        return state;
    }

    public static void applyNbtToBlock(Level world, BlockPos pos, CompoundTag nbtTags)
    {
        if (world == null || pos == null || nbtTags == null || nbtTags.isEmpty())
        {
            return;
        }

        try
        {
            BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity != null)
            {
                CompoundTag tileNbt = tileEntity.saveWithFullMetadata(net.minecraft.core.RegistryAccess.EMPTY);

                for (String key : nbtTags.getAllKeys())
                {
                    Tag tag = nbtTags.get(key);
                    if (tag != null)
                    {
                        tileNbt.put(key, tag.copy());
                    }
                }

                tileEntity.loadWithComponents(tileNbt, net.minecraft.core.RegistryAccess.EMPTY);
                tileEntity.setChanged();

                BlockState state = world.getBlockState(pos);
                world.sendBlockUpdated(pos, state, state, 3);

                OneBlockUltima.logDebug("[Generator] Applied NBT tags to TileEntity at {}: {}", pos, nbtTags);
            }
            else
            {
                OneBlockUltima.logDebug("[Generator] No TileEntity found at {} for NBT application", pos);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Generator] Failed to apply NBT tags to block at {}", pos, e);
        }
    }

    public static boolean isFullBlock(net.minecraft.world.level.block.Block block, int meta)
    {
        try
        {
            Item item = block.asItem();
            if (item == Items.AIR)
            {
                return false;
            }

            BlockState state = null;
            try
            {
                state = block.defaultBlockState();
            }
            catch (Exception ignored) {}

            if (state == null) return false;

            return state.isSolid();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static void mergeNbtTags(CompoundTag target, CompoundTag source)
    {
        if (source == null || source.isEmpty())
        {
            return;
        }

        for (String key : source.getAllKeys())
        {
            Tag sourceTag = source.get(key);
            if (sourceTag == null)
            {
                continue;
            }

            if (target.contains(key))
            {
                Tag targetTag = target.get(key);
                if (targetTag instanceof CompoundTag && sourceTag instanceof CompoundTag)
                {
                    mergeNbtTags((CompoundTag) targetTag, (CompoundTag) sourceTag);
                    continue;
                }
            }

            target.put(key, sourceTag.copy());
        }
    }

    public static boolean canReplaceForGeneration(Level world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        if (state.canBeReplaced())
        {
            return true;
        }

        return GeneratedBlockRegistry.get(world).isGenerated(pos);
    }

    public static BlockState toState(BlockSetConfig.BlockEntryDefinition entry)
    {
        Block block = entry.resolveBlock();
        if (block == null || block == Blocks.AIR)
        {
            if (entry.registry != null && entry.registry.toLowerCase().contains("forestry"))
            {
                try
                {
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.registry));
                    if (item instanceof BlockItem)
                    {
                        Block forestryBlock = ((BlockItem) item).getBlock();
                        if (forestryBlock != null && forestryBlock != Blocks.AIR)
                        {
                            OneBlockUltima.logDebug("[BlockUtil] Found Forestry block via BlockItem: {}", BuiltInRegistries.BLOCK.getKey(forestryBlock));
                            block = forestryBlock;
                        }
                    }
                }
                catch (Exception ignored) {}
            }

            if (block == null || block == Blocks.AIR)
            {
                block = resolveSpecialPlantBlock(entry.registry);
            }

            if (block == null || block == Blocks.AIR)
            {
                OneBlockUltima.logDebugWarn("[BlockUtil] Could not resolve block for registry: {}", entry.registry);
                return null;
            }
        }

        try
        {
            BlockState state;

            if (entry.registry != null && entry.registry.toLowerCase().contains("forestry") &&
                    entry.registry.toLowerCase().contains("sapling"))
            {
                state = block.defaultBlockState();
                OneBlockUltima.logDebug("[BlockUtil] Using default state for Forestry sapling: {}", state);
            }
            else
            {
                state = block.defaultBlockState();
            }

            if (state.getBlock() == Blocks.AIR)
            {
                state = block.defaultBlockState();
            }
            if (state.getBlock() == Blocks.AIR)
            {
                return null;
            }

            if (block instanceof CropBlock && state.getBlock() == block)
            {
                return block.defaultBlockState();
            }

            OneBlockUltima.logDebug("[BlockUtil] Resolved block: {} -> {} with meta: {}", entry.registry, BuiltInRegistries.BLOCK.getKey(block), entry.meta);
            return state;
        }
        catch (Exception ex)
        {
            OneBlockUltima.logDebug("[BlockUtil] Exception getting state for {}, using default state", entry.registry, ex);
            BlockState defaultState = block.defaultBlockState();
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
                return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:carrots"));
            case "minecraft:potato":
            case "potato":
                return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:potatoes"));
            case "minecraft:wheat_seeds":
            case "wheat_seeds":
            case "minecraft:wheat":
            case "wheat":
                return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:wheat"));
            case "minecraft:beetroot_seeds":
            case "beetroot_seeds":
            case "minecraft:beetroot":
            case "beetroot":
                return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:beetroots"));
            case "minecraft:reeds":
            case "reeds":
            case "minecraft:sugar_cane":
            case "sugar_cane":
                Block reeds = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:reeds"));
                if (reeds != Blocks.AIR)
                {
                    return reeds;
                }
                return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:sugar_cane"));
        }

        if (normalized.contains("forestry") && normalized.contains("sapling"))
        {
            OneBlockUltima.logDebug("[BlockUtil] Trying to resolve Forestry sapling: {}", registry);

            try
            {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(registry));
                if (item instanceof BlockItem)
                {
                    Block block = ((BlockItem) item).getBlock();
                    if (block != Blocks.AIR)
                    {
                        OneBlockUltima.logDebug("[BlockUtil] Found Forestry sapling block via BlockItem: {}", BuiltInRegistries.BLOCK.getKey(block));
                        return block;
                    }
                }
            }
            catch (Exception ignored) {}

            try
            {
                Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forestry:sapling"));
                if (b != Blocks.AIR)
                {
                    OneBlockUltima.logDebug("[BlockUtil] Found Forestry sapling block via direct lookup: {}", BuiltInRegistries.BLOCK.getKey(b));
                    return b;
                }
            }
            catch (Exception ignored) {}
        }

        return null;
    }

    public static void applyNbtToEntity(net.minecraft.world.entity.Entity entity, @Nullable CompoundTag nbtTags)
    {
        if (entity == null || nbtTags == null || nbtTags.isEmpty())
        {
            return;
        }

        try
        {
            CompoundTag entityNbt = new CompoundTag();
            entity.save(entityNbt);

            mergeNbtTags(entityNbt, nbtTags);

            entity.load(entityNbt);

            OneBlockUltima.logDebug("[Mob Spawn] Applied NBT tags to entity: {}", entity.getName().getString());
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Mob Spawn] Failed to apply NBT tags to entity: {}", entity.getName().getString(), e);
        }
    }
}

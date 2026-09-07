package ru.defea.oneblockultima.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.item.ItemBlockCompressed;
import ru.defea.oneblockultima.util.BlockUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.Keys.BLOCKS, OneBlockUltima.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.Keys.ITEMS, OneBlockUltima.MODID);

    public static BlockOneBlockGenerator ONE_BLOCK_GENERATOR;
    public static BlockFluidBarrier FLUID_BARRIER;
    public static BlockCase CASE_BLOCK;
    public static BlockCompressedMineralBlock COMPRESSED_MINERAL_BLOCK;

    public static BlockCompressedBedrock COMPRESSED_BEDROCK;
    public static BlockCompressedRedstoneBlock COMPRESSED_REDSTONE_BLOCK;
    public static BlockCompressedGoldBlock COMPRESSED_GOLD_BLOCK;
    public static BlockCompressedIronBlock COMPRESSED_IRON_BLOCK;
    public static BlockCompressedDiamondBlock COMPRESSED_DIAMOND_BLOCK;
    public static BlockCompressedStoneBlock COMPRESSED_STONE_BLOCK;
    public static BlockCompressedEmeraldBlock COMPRESSED_EMERALD_BLOCK;
    public static BlockCompressedLapisBlock COMPRESSED_LAPIS_BLOCK;
    public static BlockCompressedEndStone COMPRESSED_END_STONE;
    public static BlockCompressedNetherrack COMPRESSED_NETHERRACK;

    public static final int CUSTOM_BREAKABLE_POOL_SIZE = 32;
    public static final List<BlockCustomBreakable> CUSTOM_BREAKABLE_POOL = new ArrayList<>();

    private static final Map<Block, BlockCustomBreakable> BREAKABLE_BY_EMULATED = new HashMap<>();

    private static final Map<String, RegistryObject<? extends Block>> BLOCK_RO = new HashMap<>();
    private static final Map<String, RegistryObject<? extends Item>> ITEM_RO = new HashMap<>();

    private static final List<CompressedReg> COMPRESSED_REGS = new ArrayList<>();

    private record CompressedReg(RegistryObject<? extends Block> blockRO, String name, int maxLevel) {}

    static
    {
        BLOCK_RO.put("one_block_generator", BLOCKS.register("one_block_generator", BlockOneBlockGenerator::new));
        ITEM_RO.put("one_block_generator", ITEMS.register("one_block_generator", () -> new BlockItem(ONE_BLOCK_GENERATOR, new Item.Properties())));

        BLOCK_RO.put("fluid_barrier", BLOCKS.register("fluid_barrier", BlockFluidBarrier::new));

        BLOCK_RO.put("case_block", BLOCKS.register("case_block", BlockCase::new));
        ITEM_RO.put("case_block", ITEMS.register("case_block", () -> new BlockItem(CASE_BLOCK, new Item.Properties())));

        BLOCK_RO.put("compressed_mineral_block", BLOCKS.register("compressed_mineral_block", BlockCompressedMineralBlock::new));
        ITEM_RO.put("compressed_mineral_block", ITEMS.register("compressed_mineral_block", () -> new BlockItem(COMPRESSED_MINERAL_BLOCK, new Item.Properties())));

        registerCompressed("compressed_bedrock", BlockCompressedBedrock::new, BlockCompressedBedrock.MAX_LEVEL);
        registerCompressed("compressed_redstone_block", BlockCompressedRedstoneBlock::new, BlockCompressedRedstoneBlock.MAX_LEVEL);
        registerCompressed("compressed_gold_block", BlockCompressedGoldBlock::new, BlockCompressedGoldBlock.MAX_LEVEL);
        registerCompressed("compressed_iron_block", BlockCompressedIronBlock::new, BlockCompressedIronBlock.MAX_LEVEL);
        registerCompressed("compressed_diamond_block", BlockCompressedDiamondBlock::new, BlockCompressedDiamondBlock.MAX_LEVEL);
        registerCompressed("compressed_stone_block", BlockCompressedStoneBlock::new, BlockCompressedStoneBlock.MAX_LEVEL);
        registerCompressed("compressed_emerald_block", BlockCompressedEmeraldBlock::new, BlockCompressedEmeraldBlock.MAX_LEVEL);
        registerCompressed("compressed_lapis_block", BlockCompressedLapisBlock::new, BlockCompressedLapisBlock.MAX_LEVEL);
        registerCompressed("compressed_end_stone", BlockCompressedEndStone::new, BlockCompressedEndStone.MAX_LEVEL);
        registerCompressed("compressed_netherrack", BlockCompressedNetherrack::new, BlockCompressedNetherrack.MAX_LEVEL);

        for (int i = 0; i < CUSTOM_BREAKABLE_POOL_SIZE; i++)
        {
            final String name = "custom_breakable_" + i;
            BLOCK_RO.put(name, BLOCKS.register(name, () -> new BlockCustomBreakable(name)));
        }
    }

    private static void registerCompressed(String name, Supplier<? extends Block> blockSupplier, int maxLevel)
    {
        RegistryObject<? extends Block> bro = BLOCKS.register(name, blockSupplier);
        BLOCK_RO.put(name, bro);
        COMPRESSED_REGS.add(new CompressedReg(bro, name, maxLevel));
    }

    public static void register(IEventBus bus)
    {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener((RegisterEvent event) ->
        {
            if (event.getRegistryKey() == ForgeRegistries.Keys.BLOCKS)
            {
                ONE_BLOCK_GENERATOR = (BlockOneBlockGenerator) BLOCK_RO.get("one_block_generator").get();
                FLUID_BARRIER = (BlockFluidBarrier) BLOCK_RO.get("fluid_barrier").get();
                CASE_BLOCK = (BlockCase) BLOCK_RO.get("case_block").get();
                COMPRESSED_MINERAL_BLOCK = (BlockCompressedMineralBlock) BLOCK_RO.get("compressed_mineral_block").get();
                COMPRESSED_BEDROCK = (BlockCompressedBedrock) BLOCK_RO.get("compressed_bedrock").get();
                COMPRESSED_REDSTONE_BLOCK = (BlockCompressedRedstoneBlock) BLOCK_RO.get("compressed_redstone_block").get();
                COMPRESSED_GOLD_BLOCK = (BlockCompressedGoldBlock) BLOCK_RO.get("compressed_gold_block").get();
                COMPRESSED_IRON_BLOCK = (BlockCompressedIronBlock) BLOCK_RO.get("compressed_iron_block").get();
                COMPRESSED_DIAMOND_BLOCK = (BlockCompressedDiamondBlock) BLOCK_RO.get("compressed_diamond_block").get();
                COMPRESSED_STONE_BLOCK = (BlockCompressedStoneBlock) BLOCK_RO.get("compressed_stone_block").get();
                COMPRESSED_EMERALD_BLOCK = (BlockCompressedEmeraldBlock) BLOCK_RO.get("compressed_emerald_block").get();
                COMPRESSED_LAPIS_BLOCK = (BlockCompressedLapisBlock) BLOCK_RO.get("compressed_lapis_block").get();
                COMPRESSED_END_STONE = (BlockCompressedEndStone) BLOCK_RO.get("compressed_end_stone").get();
                COMPRESSED_NETHERRACK = (BlockCompressedNetherrack) BLOCK_RO.get("compressed_netherrack").get();
                CUSTOM_BREAKABLE_POOL.clear();
                for (int i = 0; i < CUSTOM_BREAKABLE_POOL_SIZE; i++)
                {
                    CUSTOM_BREAKABLE_POOL.add((BlockCustomBreakable) BLOCK_RO.get("custom_breakable_" + i).get());
                }
            }
            else if (event.getRegistryKey() == ForgeRegistries.Keys.ITEMS)
            {
                for (CompressedReg reg : COMPRESSED_REGS)
                {
                    Block block = reg.blockRO().get();
                    for (int l = 0; l < reg.maxLevel(); l++)
                    {
                        final int level = l;
                        String itemName = reg.name() + "_" + level;
                        event.register(ForgeRegistries.Keys.ITEMS,
                                ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, itemName),
                                () -> new ItemBlockCompressed(block, level));
                    }
                }
            }
        });
    }

    public static void bindBreakablePool()
    {
        for (Block block : BuiltInRegistries.BLOCK)
        {
            if (block == null || block.builtInRegistryHolder() == null || BREAKABLE_BY_EMULATED.containsKey(block))
            {
                continue;
            }
            if (BlockUtil.isBreakable(block))
            {
                continue;
            }
            BlockCustomBreakable slot = null;
            for (BlockCustomBreakable cb : CUSTOM_BREAKABLE_POOL)
            {
                if (cb.getEmulated() == null)
                {
                    slot = cb;
                    break;
                }
            }
            if (slot == null)
            {
                OneBlockUltima.logDebugWarn("[Breakable] Custom breakable pool exhausted, {} stays unbreakable", block);
                continue;
            }
            slot.setEmulated(block);
            BREAKABLE_BY_EMULATED.put(block, slot);
            OneBlockUltima.logDebug("[Breakable] Bound {} -> {}", BuiltInRegistries.BLOCK.getKey(block), BuiltInRegistries.BLOCK.getKey(slot));
        }
    }

    @Nullable
    public static BlockCustomBreakable getBreakableFor(Block block)
    {
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR)
        {
            return null;
        }
        BlockCustomBreakable bound = BREAKABLE_BY_EMULATED.get(block);
        if (bound != null)
        {
            return bound;
        }
        for (BlockCustomBreakable cb : CUSTOM_BREAKABLE_POOL)
        {
            if (cb.getEmulated() == null)
            {
                cb.setEmulated(block);
                BREAKABLE_BY_EMULATED.put(block, cb);
                return cb;
            }
        }
        return null;
    }

    public static void registerOreDict()
    {
        // OreDictionary does not exist in 1.21; tags would be registered via datagen/tags instead.
    }

    public static boolean isCompressedBlock(Block b)
    {
        return b instanceof BlockCompressedBase;
    }

    public static Item getCompressedItem(Block block, int level)
    {
        if (!(block instanceof BlockCompressedBase base)) return Items.AIR;
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, base.getBaseName() + "_" + level);
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        return item != null ? item : Items.AIR;
    }

    public static int metaOf(ItemStack stack)
    {
        if (stack.getItem() instanceof ItemBlockCompressed c) return c.getLevel();
        return stack.getDamageValue();
    }

    public static ItemStack itemStackFor(Block block, int meta)
    {
        if (block instanceof BlockCompressedBase)
        {
            Item item = getCompressedItem(block, meta);
            return item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(block);
        if (meta != 0) stack.setDamageValue(meta);
        return stack;
    }

    public static ItemStack stackFromItemAndMeta(Item item, int meta)
    {
        Block block = Block.byItem(item);
        if (block instanceof BlockCompressedBase)
        {
            Item comp = getCompressedItem(block, meta);
            return comp != Items.AIR ? new ItemStack(comp) : ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (meta != 0) stack.setDamageValue(meta);
        return stack;
    }

    public static List<ItemStack> getCompressedItemStacks()
    {
        List<ItemStack> list = new ArrayList<>();
        for (CompressedReg reg : COMPRESSED_REGS)
        {
            Block block = reg.blockRO().get();
            for (int l = 0; l < reg.maxLevel(); l++)
            {
                Item it = getCompressedItem(block, l);
                if (it != null && it != Items.AIR) list.add(new ItemStack(it));
            }
        }
        return list;
    }

    public static List<ItemStack> getCompressedItemStacksForBlock(Block block)
    {
        List<ItemStack> list = new ArrayList<>();
        if (block instanceof BlockCompressedBase base)
        {
            int maxLevel = base.getMaxLevel();
            for (int l = 0; l < maxLevel; l++)
            {
                Item it = getCompressedItem(block, l);
                if (it != null && it != Items.AIR) list.add(new ItemStack(it));
            }
        }
        return list;
    }

    public static boolean isBlockExcludedFromSearch(String modId, String registryId)
    {
        if (!OneBlockUltima.MODID.equals(modId)) return false;
        return "one_block_generator".equals(registryId)
                || "case".equals(registryId)
                || "case_block".equals(registryId);
    }

    private ModBlocks()
    {
    }
}

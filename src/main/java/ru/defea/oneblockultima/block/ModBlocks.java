package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.item.ItemBlockCompressed;
import ru.defea.oneblockultima.util.BlockUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = OneBlockUltima.MODID)
public final class ModBlocks
{
    @SuppressWarnings("unused")
    public static final class RegisterBlock
    {
        private final Block block;
        private boolean isItem = false;
        private String variantIn = "inventory";
        private int meta = 0;
        private String subBlockName;
        private int subBlockCount = 1;

        private RegisterBlock(Block block)
        {
            this.block = block;
        }

        private RegisterBlock(Block block, boolean isItem)
        {
            this.block = block;
            this.isItem = isItem;
        }

        private RegisterBlock(Block block, String variantIn)
        {
            this.block = block;
            this.variantIn = variantIn;
        }

        private RegisterBlock(Block block, boolean isItem, String variantIn)
        {
            this.block = block;
            this.isItem = isItem;
            this.variantIn = variantIn;
        }

        private RegisterBlock(Block block, int meta)
        {
            this.block = block;
            this.meta = meta;
        }

        private RegisterBlock(Block block, boolean isItem, int meta)
        {
            this.block = block;
            this.isItem = isItem;
            this.meta = meta;
        }

        private RegisterBlock(Block block, String variantIn, int meta)
        {
            this.block = block;
            this.variantIn = variantIn;
            this.meta = meta;
        }

        private RegisterBlock(Block block, boolean isItem, String variantIn, int meta)
        {
            this.block = block;
            this.isItem = isItem;
            this.variantIn = variantIn;
            this.meta = meta;
        }

        private RegisterBlock(Block block, boolean isItem, String variantIn, int meta, String subBlockName, int subBlockCount)
        {
            this.block = block;
            this.isItem = isItem;
            this.variantIn = variantIn;
            this.meta = meta;
            this.subBlockName = subBlockName;
            this.subBlockCount = subBlockCount;
        }

        public Block getBlock()
        {
            return this.block;
        }

        public String getVariantIn()
        {
            return this.variantIn;
        }

        public int getMeta()
        {
            return this.meta;
        }

        public String getSubBlockName()
        {
            return this.subBlockName;
        }

        public int getSubBlockCount()
        {
            return this.subBlockCount;
        }
    }

    public static final BlockOneBlockGenerator ONE_BLOCK_GENERATOR = new BlockOneBlockGenerator();
    public static final BlockFluidBarrier FLUID_BARRIER = new BlockFluidBarrier();
    public static final BlockCompressedMineralBlock COMPRESSED_MINERAL_BLOCK = new BlockCompressedMineralBlock();

    public static final BlockCompressedBedrock COMPRESSED_BEDROCK = new BlockCompressedBedrock();
    public static final BlockCompressedRedstoneBlock COMPRESSED_REDSTONE_BLOCK = new BlockCompressedRedstoneBlock();
    public static final BlockCompressedGoldBlock COMPRESSED_GOLD_BLOCK = new BlockCompressedGoldBlock();
    public static final BlockCompressedIronBlock COMPRESSED_IRON_BLOCK = new BlockCompressedIronBlock();
    public static final BlockCompressedDiamondBlock COMPRESSED_DIAMOND_BLOCK = new BlockCompressedDiamondBlock();
    public static final BlockCompressedCoalBlock COMPRESSED_COAL_BLOCK = new BlockCompressedCoalBlock();
    public static final BlockCompressedEmeraldBlock COMPRESSED_EMERALD_BLOCK = new BlockCompressedEmeraldBlock();
    public static final BlockCompressedLapisBlock COMPRESSED_LAPIS_BLOCK = new BlockCompressedLapisBlock();
    public static final BlockCompressedEndStone COMPRESSED_END_STONE = new BlockCompressedEndStone();
    public static final BlockCompressedNetherrack COMPRESSED_NETHERRACK = new BlockCompressedNetherrack();

    public static RegisterBlock[] modBlocks = {
        new RegisterBlock(ONE_BLOCK_GENERATOR, true),
        new RegisterBlock(FLUID_BARRIER),
        new RegisterBlock(COMPRESSED_MINERAL_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_BEDROCK, true, "normal", 0, "compressed_bedrock", BlockCompressedBedrock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_REDSTONE_BLOCK, true, "normal", 0, "compressed_redstone_block", BlockCompressedRedstoneBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_GOLD_BLOCK, true, "normal", 0, "compressed_gold_block", BlockCompressedGoldBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_IRON_BLOCK, true, "normal", 0, "compressed_iron_block", BlockCompressedIronBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_DIAMOND_BLOCK, true, "normal", 0, "compressed_diamond_block", BlockCompressedDiamondBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_COAL_BLOCK, true, "normal", 0, "compressed_coal_block", BlockCompressedCoalBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_EMERALD_BLOCK, true, "normal", 0, "compressed_emerald_block", BlockCompressedEmeraldBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_LAPIS_BLOCK, true, "normal", 0, "compressed_lapis_block", BlockCompressedLapisBlock.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_END_STONE, true, "normal", 0, "compressed_end_stone", BlockCompressedEndStone.MAX_LEVEL),
        new RegisterBlock(COMPRESSED_NETHERRACK, true, "normal", 0, "compressed_netherrack", BlockCompressedNetherrack.MAX_LEVEL)
    };

    public static final int CUSTOM_BREAKABLE_POOL_SIZE = 128;

    public static final List<BlockCustomBreakable> CUSTOM_BREAKABLE_POOL = new ArrayList<>();

    private static final Map<Block, BlockCustomBreakable> BREAKABLE_BY_EMULATED = new HashMap<>();

    static
    {
        for (int i = 0; i < CUSTOM_BREAKABLE_POOL_SIZE; i++)
        {
            CUSTOM_BREAKABLE_POOL.add(new BlockCustomBreakable("custom_breakable_" + i));
        }
        RegisterBlock[] poolEntries = new RegisterBlock[CUSTOM_BREAKABLE_POOL.size()];
        for (int i = 0; i < poolEntries.length; i++)
        {
            poolEntries[i] = new RegisterBlock(CUSTOM_BREAKABLE_POOL.get(i), "normal");
        }
        RegisterBlock[] combined = Arrays.copyOf(modBlocks, modBlocks.length + poolEntries.length);
        System.arraycopy(poolEntries, 0, combined, modBlocks.length, poolEntries.length);
        modBlocks = combined;
    }

    public static void bindBreakablePool()
    {
        for (Block block : ForgeRegistries.BLOCKS)
        {
            if (block == null || block.getRegistryName() == null || BREAKABLE_BY_EMULATED.containsKey(block))
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
                OneBlockUltima.getLogger().warn("[Breakable] Custom breakable pool exhausted, {} stays unbreakable", block.getRegistryName());
                continue;
            }
            slot.setEmulated(block);
            BREAKABLE_BY_EMULATED.put(block, slot);
            OneBlockUltima.getLogger().info("[Breakable] Bound {} -> {}", block.getRegistryName(), slot.getRegistryName());
        }
    }

    @Nullable
    public static BlockCustomBreakable getBreakableFor(Block block)
    {
        if (block == null || block == Blocks.AIR)
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

    private ModBlocks()
    {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        for (RegisterBlock modBlock : modBlocks)
        {
            event.getRegistry().register(modBlock.block);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        bindBreakablePool();
        for (RegisterBlock modBlock : modBlocks)
        {
            if (modBlock.isItem) {
                Item modItemBlock;
                if (modBlock.getSubBlockCount() > 1)
                {
                    modItemBlock = new ItemBlockCompressed(modBlock.block, modBlock.getSubBlockName(), modBlock.getSubBlockCount());
                }
                else
                {
                    modItemBlock = new ItemBlock(modBlock.block);
                }
                modItemBlock.setRegistryName(Objects.requireNonNull(modBlock.block.getRegistryName()));

                event.getRegistry().register(modItemBlock);
            }
        }
    }
}

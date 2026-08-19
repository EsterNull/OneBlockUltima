package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import ru.defea.oneblockultima.item.ItemBlockCompressed;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final BlockCompressedStoneBlock COMPRESSED_STONE_BLOCK = new BlockCompressedStoneBlock();
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
            new RegisterBlock(COMPRESSED_STONE_BLOCK, true, "normal", 0, "compressed_stone_block", BlockCompressedStoneBlock.MAX_LEVEL),
            new RegisterBlock(COMPRESSED_EMERALD_BLOCK, true, "normal", 0, "compressed_emerald_block", BlockCompressedEmeraldBlock.MAX_LEVEL),
            new RegisterBlock(COMPRESSED_LAPIS_BLOCK, true, "normal", 0, "compressed_lapis_block", BlockCompressedLapisBlock.MAX_LEVEL),
            new RegisterBlock(COMPRESSED_END_STONE, true, "normal", 0, "compressed_end_stone", BlockCompressedEndStone.MAX_LEVEL),
            new RegisterBlock(COMPRESSED_NETHERRACK, true, "normal", 0, "compressed_netherrack", BlockCompressedNetherrack.MAX_LEVEL)
    };

    public static final int CUSTOM_BREAKABLE_POOL_SIZE = 32;

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

    @Nullable
    public static BlockCustomBreakable getBreakableFor(Block block)
    {
        if (block == null || block == Blocks.air)
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
        registerCompressedOres(COMPRESSED_BEDROCK, "Bedrock");
        registerCompressedOres(COMPRESSED_REDSTONE_BLOCK, "RedstoneBlock");
        registerCompressedOres(COMPRESSED_GOLD_BLOCK, "GoldBlock");
        registerCompressedOres(COMPRESSED_IRON_BLOCK, "IronBlock");
        registerCompressedOres(COMPRESSED_DIAMOND_BLOCK, "DiamondBlock");
        registerCompressedOres(COMPRESSED_STONE_BLOCK, "Stone");
        registerCompressedOres(COMPRESSED_EMERALD_BLOCK, "EmeraldBlock");
        registerCompressedOres(COMPRESSED_LAPIS_BLOCK, "LapisBlock");
        registerCompressedOres(COMPRESSED_END_STONE, "EndStone");
        registerCompressedOres(COMPRESSED_NETHERRACK, "Netherrack");
    }

    private static void registerCompressedOres(BlockCompressedBase block, String capName)
    {
        for (int level = 1; level <= block.getMaxLevel(); level++)
        {
            OreDictionary.registerOre("compressed" + level + "x" + capName, new net.minecraft.item.ItemStack(block, 1, level - 1));
        }
    }

    private ModBlocks()
    {
    }

    public static void registerBlocksAndItems()
    {
        for (RegisterBlock modBlock : modBlocks)
        {
            String name = modBlock.block.getUnlocalizedName();
            if (name.startsWith("tile."))
            {
                name = name.substring("tile.".length());
            }
            if (modBlock.isItem)
            {
                if (modBlock.getSubBlockCount() > 1)
                {
                    GameRegistry.registerBlock(modBlock.block, null, name);
                    GameRegistry.registerItem(
                            new ItemBlockCompressed(modBlock.block, modBlock.getSubBlockName(), modBlock.getSubBlockCount()),
                            name);
                }
                else
                {
                    GameRegistry.registerBlock(modBlock.block, name);
                }
            }
            else
            {
                GameRegistry.registerBlock(modBlock.block, null, name);
            }
        }
    }
}
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
    }

    public static final BlockOneBlockGenerator ONE_BLOCK_GENERATOR = new BlockOneBlockGenerator();
    public static final BlockFluidBarrier FLUID_BARRIER = new BlockFluidBarrier();
    public static final BlockCompressedMineralBlock COMPRESSED_MINERAL_BLOCK = new BlockCompressedMineralBlock();

    public static final BlockCompressedBedrock COMPRESSED_BEDROCK = new BlockCompressedBedrock();
    public static final BlockCompressedDoubleBedrock COMPRESSED_DOUBLE_BEDROCK = new BlockCompressedDoubleBedrock();
    public static final BlockCompressedTripleBedrock COMPRESSED_TRIPLE_BEDROCK = new BlockCompressedTripleBedrock();
    public static final BlockCompressedQuadrupleBedrock COMPRESSED_QUADRUPLE_BEDROCK = new BlockCompressedQuadrupleBedrock();

    public static final BlockCompressedRedstoneBlock COMPRESSED_REDSTONE_BLOCK = new BlockCompressedRedstoneBlock();
    public static final BlockCompressedDoubleRedstoneBlock COMPRESSED_DOUBLE_REDSTONE_BLOCK = new BlockCompressedDoubleRedstoneBlock();
    public static final BlockCompressedTripleRedstoneBlock COMPRESSED_TRIPLE_REDSTONE_BLOCK = new BlockCompressedTripleRedstoneBlock();
    public static final BlockCompressedQuadrupleRedstoneBlock COMPRESSED_QUADRUPLE_REDSTONE_BLOCK = new BlockCompressedQuadrupleRedstoneBlock();
    public static final BlockCompressedQuintupleRedstoneBlock COMPRESSED_QUINTUPLE_REDSTONE_BLOCK = new BlockCompressedQuintupleRedstoneBlock();
    public static final BlockCompressedSextupleRedstoneBlock COMPRESSED_SEXTUPLE_REDSTONE_BLOCK = new BlockCompressedSextupleRedstoneBlock();

    public static final BlockCompressedGoldBlock COMPRESSED_GOLD_BLOCK = new BlockCompressedGoldBlock();
    public static final BlockCompressedDoubleGoldBlock COMPRESSED_DOUBLE_GOLD_BLOCK = new BlockCompressedDoubleGoldBlock();
    public static final BlockCompressedTripleGoldBlock COMPRESSED_TRIPLE_GOLD_BLOCK = new BlockCompressedTripleGoldBlock();
    public static final BlockCompressedQuadrupleGoldBlock COMPRESSED_QUADRUPLE_GOLD_BLOCK = new BlockCompressedQuadrupleGoldBlock();
    public static final BlockCompressedQuintupleGoldBlock COMPRESSED_QUINTUPLE_GOLD_BLOCK = new BlockCompressedQuintupleGoldBlock();

    public static final BlockCompressedIronBlock COMPRESSED_IRON_BLOCK = new BlockCompressedIronBlock();
    public static final BlockCompressedDoubleIronBlock COMPRESSED_DOUBLE_IRON_BLOCK = new BlockCompressedDoubleIronBlock();
    public static final BlockCompressedTripleIronBlock COMPRESSED_TRIPLE_IRON_BLOCK = new BlockCompressedTripleIronBlock();
    public static final BlockCompressedQuadrupleIronBlock COMPRESSED_QUADRUPLE_IRON_BLOCK = new BlockCompressedQuadrupleIronBlock();
    public static final BlockCompressedQuintupleIronBlock COMPRESSED_QUINTUPLE_IRON_BLOCK = new BlockCompressedQuintupleIronBlock();

    public static final BlockCompressedDiamondBlock COMPRESSED_DIAMOND_BLOCK = new BlockCompressedDiamondBlock();
    public static final BlockCompressedDoubleDiamondBlock COMPRESSED_DOUBLE_DIAMOND_BLOCK = new BlockCompressedDoubleDiamondBlock();
    public static final BlockCompressedTripleDiamondBlock COMPRESSED_TRIPLE_DIAMOND_BLOCK = new BlockCompressedTripleDiamondBlock();
    public static final BlockCompressedQuadrupleDiamondBlock COMPRESSED_QUADRUPLE_DIAMOND_BLOCK = new BlockCompressedQuadrupleDiamondBlock();
    public static final BlockCompressedQuintupleDiamondBlock COMPRESSED_QUINTUPLE_DIAMOND_BLOCK = new BlockCompressedQuintupleDiamondBlock();

    public static final BlockCompressedCoalBlock COMPRESSED_COAL_BLOCK = new BlockCompressedCoalBlock();
    public static final BlockCompressedDoubleCoalBlock COMPRESSED_DOUBLE_COAL_BLOCK = new BlockCompressedDoubleCoalBlock();
    public static final BlockCompressedTripleCoalBlock COMPRESSED_TRIPLE_COAL_BLOCK = new BlockCompressedTripleCoalBlock();
    public static final BlockCompressedQuadrupleCoalBlock COMPRESSED_QUADRUPLE_COAL_BLOCK = new BlockCompressedQuadrupleCoalBlock();
    public static final BlockCompressedQuintupleCoalBlock COMPRESSED_QUINTUPLE_COAL_BLOCK = new BlockCompressedQuintupleCoalBlock();

    public static final BlockCompressedEmeraldBlock COMPRESSED_EMERALD_BLOCK = new BlockCompressedEmeraldBlock();
    public static final BlockCompressedDoubleEmeraldBlock COMPRESSED_DOUBLE_EMERALD_BLOCK = new BlockCompressedDoubleEmeraldBlock();
    public static final BlockCompressedTripleEmeraldBlock COMPRESSED_TRIPLE_EMERALD_BLOCK = new BlockCompressedTripleEmeraldBlock();
    public static final BlockCompressedQuadrupleEmeraldBlock COMPRESSED_QUADRUPLE_EMERALD_BLOCK = new BlockCompressedQuadrupleEmeraldBlock();
    public static final BlockCompressedQuintupleEmeraldBlock COMPRESSED_QUINTUPLE_EMERALD_BLOCK = new BlockCompressedQuintupleEmeraldBlock();

    public static final BlockCompressedLapisBlock COMPRESSED_LAPIS_BLOCK = new BlockCompressedLapisBlock();
    public static final BlockCompressedDoubleLapisBlock COMPRESSED_DOUBLE_LAPIS_BLOCK = new BlockCompressedDoubleLapisBlock();
    public static final BlockCompressedTripleLapisBlock COMPRESSED_TRIPLE_LAPIS_BLOCK = new BlockCompressedTripleLapisBlock();
    public static final BlockCompressedQuadrupleLapisBlock COMPRESSED_QUADRUPLE_LAPIS_BLOCK = new BlockCompressedQuadrupleLapisBlock();
    public static final BlockCompressedQuintupleLapisBlock COMPRESSED_QUINTUPLE_LAPIS_BLOCK = new BlockCompressedQuintupleLapisBlock();
    public static final BlockCompressedSextupleLapisBlock COMPRESSED_SEXTUPLE_LAPIS_BLOCK = new BlockCompressedSextupleLapisBlock();

    public static final BlockCompressedEndStone COMPRESSED_END_STONE = new BlockCompressedEndStone();
    public static final BlockCompressedDoubleEndStone COMPRESSED_DOUBLE_END_STONE = new BlockCompressedDoubleEndStone();
    public static final BlockCompressedTripleEndStone COMPRESSED_TRIPLE_END_STONE = new BlockCompressedTripleEndStone();
    public static final BlockCompressedQuadrupleEndStone COMPRESSED_QUADRUPLE_END_STONE = new BlockCompressedQuadrupleEndStone();
    public static final BlockCompressedQuintupleEndStone COMPRESSED_QUINTUPLE_END_STONE = new BlockCompressedQuintupleEndStone();

    public static final BlockCompressedNetherrack COMPRESSED_NETHERRACK = new BlockCompressedNetherrack();
    public static final BlockCompressedDoubleNetherrack COMPRESSED_DOUBLE_NETHERRACK = new BlockCompressedDoubleNetherrack();
    public static final BlockCompressedTripleNetherrack COMPRESSED_TRIPLE_NETHERRACK = new BlockCompressedTripleNetherrack();
    public static final BlockCompressedQuadrupleNetherrack COMPRESSED_QUADRUPLE_NETHERRACK = new BlockCompressedQuadrupleNetherrack();
    public static final BlockCompressedQuintupleNetherrack COMPRESSED_QUINTUPLE_NETHERRACK = new BlockCompressedQuintupleNetherrack();

    public static RegisterBlock[] modBlocks = {
        new RegisterBlock(ONE_BLOCK_GENERATOR, true),
        new RegisterBlock(FLUID_BARRIER),
        new RegisterBlock(COMPRESSED_MINERAL_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_BEDROCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_BEDROCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_BEDROCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_BEDROCK, true, "normal"),

        new RegisterBlock(COMPRESSED_REDSTONE_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_REDSTONE_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_REDSTONE_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_REDSTONE_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_REDSTONE_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_SEXTUPLE_REDSTONE_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_GOLD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_GOLD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_GOLD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_GOLD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_GOLD_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_IRON_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_IRON_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_IRON_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_IRON_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_IRON_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_DIAMOND_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_DIAMOND_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_DIAMOND_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_DIAMOND_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_DIAMOND_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_COAL_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_COAL_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_COAL_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_COAL_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_COAL_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_EMERALD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_EMERALD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_EMERALD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_EMERALD_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_EMERALD_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_LAPIS_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_LAPIS_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_LAPIS_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_LAPIS_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_LAPIS_BLOCK, true, "normal"),
        new RegisterBlock(COMPRESSED_SEXTUPLE_LAPIS_BLOCK, true, "normal"),

        new RegisterBlock(COMPRESSED_END_STONE, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_END_STONE, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_END_STONE, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_END_STONE, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_END_STONE, true, "normal"),

        new RegisterBlock(COMPRESSED_NETHERRACK, true, "normal"),
        new RegisterBlock(COMPRESSED_DOUBLE_NETHERRACK, true, "normal"),
        new RegisterBlock(COMPRESSED_TRIPLE_NETHERRACK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUADRUPLE_NETHERRACK, true, "normal"),
        new RegisterBlock(COMPRESSED_QUINTUPLE_NETHERRACK, true, "normal")
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
                ItemBlock modItemBlock = new ItemBlock(modBlock.block);
                modItemBlock.setRegistryName(Objects.requireNonNull(modBlock.block.getRegistryName()));

                event.getRegistry().register(modItemBlock);
            }
        }
    }
}

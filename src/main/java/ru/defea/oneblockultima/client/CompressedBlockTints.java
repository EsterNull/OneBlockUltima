package ru.defea.oneblockultima.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = OneBlockUltima.MODID)
public final class CompressedBlockTints
{
    private static final Map<Block, float[]> FACTORS = new HashMap<>();

    static
    {
        FACTORS.put(ModBlocks.COMPRESSED_BEDROCK, new float[] { 0.7169F, 0.5214F, 0.3868F });
        FACTORS.put(ModBlocks.COMPRESSED_REDSTONE_BLOCK, new float[] { 0.6763F, 0.4551F, 0.3001F, 0.1962F, 0.1237F });
        FACTORS.put(ModBlocks.COMPRESSED_GOLD_BLOCK, new float[] { 0.6871F, 0.4705F, 0.3210F, 0.1475F });
        FACTORS.put(ModBlocks.COMPRESSED_IRON_BLOCK, new float[] { 0.6881F, 0.4723F, 0.3227F, 0.2198F });
        FACTORS.put(ModBlocks.COMPRESSED_DIAMOND_BLOCK, new float[] { 0.6867F, 0.4704F, 0.3210F, 0.2180F });
        FACTORS.put(ModBlocks.COMPRESSED_COAL_BLOCK, new float[] { 0.6410F, 0.4106F, 0.2698F, 0.1407F });
        FACTORS.put(ModBlocks.COMPRESSED_EMERALD_BLOCK, new float[] { 0.6856F, 0.4686F, 0.3191F, 0.2154F });
        FACTORS.put(ModBlocks.COMPRESSED_LAPIS_BLOCK, new float[] { 0.6781F, 0.4566F, 0.3035F, 0.1962F, 0.1230F });
        FACTORS.put(ModBlocks.COMPRESSED_END_STONE, new float[] { 0.6873F, 0.4710F, 0.3219F, 0.2173F });
        FACTORS.put(ModBlocks.COMPRESSED_NETHERRACK, new float[] { 0.6747F, 0.4483F, 0.2931F, 0.1857F });
    }

    private CompressedBlockTints()
    {
    }

    @SubscribeEvent
    public static void registerBlockColors(ColorHandlerEvent.Block event)
    {
        BlockColors blockColors = event.getBlockColors();
        for (Block block : FACTORS.keySet())
        {
            blockColors.registerBlockColorHandler(CompressedBlockTints::blockColor, block);
        }
    }

    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event)
    {
        ItemColors itemColors = event.getItemColors();
        for (Block block : FACTORS.keySet())
        {
            itemColors.registerItemColorHandler(CompressedBlockTints::itemColor, Item.getItemFromBlock(block));
        }
    }

    private static int blockColor(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex)
    {
        if (!(state.getBlock() instanceof BlockCompressedBase))
        {
            return 0xFFFFFFFF;
        }
        int level = ((BlockCompressedBase) state.getBlock()).getLevel(state);
        return tintFor(level, FACTORS.get(state.getBlock()));
    }

    private static int itemColor(ItemStack stack, int tintIndex)
    {
        int meta = stack.getMetadata();
        return tintFor(meta, FACTORS.get(Block.getBlockFromItem(stack.getItem())));
    }

    private static int tintFor(int level, float[] factors)
    {
        if (level <= 0 || factors == null || level > factors.length)
        {
            return 0xFFFFFFFF;
        }
        int t = Math.round(255.0F * factors[level - 1]);
        return 0xFF000000 | (t << 16) | (t << 8) | t;
    }
}

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
        FACTORS.put(ModBlocks.COMPRESSED_BEDROCK, new float[] { 0.7578F, 0.5432F, 0.3951F, 0.2931F });
        FACTORS.put(ModBlocks.COMPRESSED_REDSTONE_BLOCK, new float[] { 0.7244F, 0.4899F, 0.3297F, 0.2174F, 0.1421F, 0.0896F });
        FACTORS.put(ModBlocks.COMPRESSED_GOLD_BLOCK, new float[] { 0.7290F, 0.5008F, 0.3430F, 0.2340F, 0.1075F });
        FACTORS.put(ModBlocks.COMPRESSED_IRON_BLOCK, new float[] { 0.7285F, 0.5013F, 0.3441F, 0.2351F, 0.1602F });
        FACTORS.put(ModBlocks.COMPRESSED_DIAMOND_BLOCK, new float[] { 0.7290F, 0.5005F, 0.3429F, 0.2340F, 0.1589F });
        FACTORS.put(ModBlocks.COMPRESSED_STONE_BLOCK, new float[] { 0.7290F, 0.5011F, 0.3433F, 0.2347F, 0.1584F, 0.0998F });
        FACTORS.put(ModBlocks.COMPRESSED_EMERALD_BLOCK, new float[] { 0.7283F, 0.4994F, 0.3413F, 0.2325F, 0.1569F });
        FACTORS.put(ModBlocks.COMPRESSED_LAPIS_BLOCK, new float[] { 0.7237F, 0.4907F, 0.3304F, 0.2196F, 0.1420F, 0.0890F });
        FACTORS.put(ModBlocks.COMPRESSED_END_STONE, new float[] { 0.7290F, 0.5011F, 0.3433F, 0.2347F, 0.1584F, 0.0998F });
        FACTORS.put(ModBlocks.COMPRESSED_NETHERRACK, new float[] { 0.7206F, 0.4862F, 0.3231F, 0.2113F, 0.1338F, 0.0843F });
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
        if (level < 0 || factors == null || level >= factors.length)
        {
            return 0xFFFFFFFF;
        }
        int t = Math.round(255.0F * factors[level]);
        return 0xFF000000 | (t << 16) | (t << 8) | t;
    }
}

package ru.defea.oneblockultima.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ItemBlockCompressed;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OneBlockUltima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CompressedBlockTints
{
    private static Map<Block, float[]> FACTORS;

    private static Map<Block, float[]> getFactors()
    {
        if (FACTORS == null)
        {
            FACTORS = new HashMap<>();
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
        return FACTORS;
    }

    private CompressedBlockTints()
    {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event)
    {
        for (Block block : getFactors().keySet())
        {
            event.register(CompressedBlockTints::blockColor, block);
        }
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event)
    {
        for (Item item : ForgeRegistries.ITEMS.getValues())
        {
            if (item instanceof ItemBlockCompressed)
            {
                event.register(CompressedBlockTints::itemColor, item);
            }
        }
    }

    private static int blockColor(BlockState state, @Nullable BlockAndTintGetter getter, @Nullable BlockPos pos, int tintIndex)
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
        if (stack.getItem() instanceof ItemBlockCompressed c)
        {
            return tintFor(c.getLevel(), FACTORS.get(Block.byItem(stack.getItem())));
        }
        return 0xFFFFFFFF;
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

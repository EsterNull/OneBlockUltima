package ru.defea.oneblockultima.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCustomBreakable;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ModItems;

import javax.annotation.Nonnull;
import java.util.Objects;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = OneBlockUltima.MODID)
public final class ModModels
{
    private ModModels()
    {
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event)
    {
        for (ModBlocks.RegisterBlock modBlock : ModBlocks.modBlocks)
        {
            registerBlockModel(modBlock.getBlock(), modBlock.getVariantIn(), modBlock.getMeta());
        }
        for (ModItems.RegisterItem modItems : ModItems.modItems)
        {
            registerItemModel(modItems.getItem(), modItems.getVariantIn(), modItems.getMeta());
        }
        registerCustomBreakableStateMappers();
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockModel(net.minecraft.block.Block block, String variantIn, int meta)
    {
        Item item = Item.getItemFromBlock(block);
        registerItemModel(item, variantIn, meta);
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModel(net.minecraft.item.Item item, String variantIn, int meta)
    {
        ModelLoader.setCustomModelResourceLocation(
                item,
                meta,
                new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), variantIn)
        );
    }

    @SideOnly(Side.CLIENT)
    private static void registerCustomBreakableStateMappers()
    {
        for (final BlockCustomBreakable customBreakable : ModBlocks.CUSTOM_BREAKABLE_POOL)
        {
            ModelLoader.setCustomStateMapper(
                    customBreakable,
                    new StateMapperBase()
                    {
                        @Override
                        @Nonnull
                        protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state)
                        {
                            Block emulated = customBreakable.getEmulated();
                            if (emulated == null || emulated.getRegistryName() == null)
                            {
                                return new ModelResourceLocation(new ResourceLocation(OneBlockUltima.MODID, "custom_breakable"), "normal");
                            }
                            IBlockState emuState = customBreakable.getEmulatedState(state);
                            return new ModelResourceLocation(
                                    emulated.getRegistryName(),
                                    BlockCustomBreakable.buildVariantString(emuState == null ? emulated.getDefaultState() : emuState)
                            );
                        }
                    }
            );
        }
    }
}

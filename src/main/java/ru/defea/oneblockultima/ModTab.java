package ru.defea.oneblockultima;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.item.ModItems;

public final class ModTab
{
    public static final CreativeModeTab TAB = CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.oneblockultima"))
            .icon(() -> new ItemStack(ModBlocks.ONE_BLOCK_GENERATOR))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.GRAVITON);
                output.accept(ModItems.DARK_MATTER);
                output.accept(ModItems.ENERGY_ORB);
                output.accept(ModItems.HIGGS_BOSON);
                output.accept(ModItems.PROTEIN);
                output.accept(ModItems.SUPER_PROTEIN);
                output.accept(ModItems.ULTIMATE_PROTEIN);
                output.accept(ModItems.MASHED_VEGETABLES);
                output.accept(ModItems.SUPER_MASHED_VEGETABLES);
                output.accept(ModItems.ULTIMATE_MASHED_VEGETABLES);
                output.accept(ModItems.NATURAL_POISON);
                output.accept(ModItems.SUPER_POISON);
                output.accept(ModItems.LIQUID_DEATH);
                output.accept(ModItems.SPACE_SOUP);
                output.accept(ModItems.COMPRESSED_MINERAL);
                output.accept(new ItemStack(ModBlocks.COMPRESSED_MINERAL_BLOCK));
                for (ItemStack compressed : ModBlocks.getCompressedItemStacks())
                {
                    output.accept(compressed);
                }
                output.accept(ModItems.FAR_STAR);
                output.accept(ModItems.GUIDE_BOOK);
                output.accept(ModItems.CASE);
            })
            .build();

    private ModTab()
    {
    }
}

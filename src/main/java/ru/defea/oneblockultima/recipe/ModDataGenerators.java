package ru.defea.oneblockultima.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.defea.oneblockultima.OneBlockUltima;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = OneBlockUltima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators
{
    private ModDataGenerators()
    {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        net.minecraft.data.DataProvider.Factory<ModRecipeProvider> factory = (output) -> new ModRecipeProvider(output, lookupProvider);
        generator.addProvider(event.includeServer(), factory);
    }
}

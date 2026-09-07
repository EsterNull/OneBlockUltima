package ru.defea.oneblockultima.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.defea.oneblockultima.OneBlockUltima;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OneBlockUltima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModModels
{
    private ModModels()
    {
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event)
    {
        // In 1.21 every registered block/item resolves its model from JSON keyed by its
        // registry name, and tinted/compressed looks are produced via CompressedBlockTints
        // (BlockColor/ItemColor). The legacy per-metadata variant registration is no longer
        // needed because each compressed level is its own registered block/item.
    }
}

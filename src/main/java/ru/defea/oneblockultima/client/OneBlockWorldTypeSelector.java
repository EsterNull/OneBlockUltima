package ru.defea.oneblockultima.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.defea.oneblockultima.OneBlockUltima;

import java.lang.reflect.Field;
import java.util.List;

public class OneBlockWorldTypeSelector
{
    private static final Logger LOG = LogManager.getLogger("OneBlockDiag");
    private static CreateWorldScreen lastScreen = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        CreateWorldScreen screen = Minecraft.getInstance().screen instanceof CreateWorldScreen s ? s : null;
        if (screen == null)
        {
            lastScreen = null;
            return;
        }
        if (screen == lastScreen)
            return;
        lastScreen = screen;

        try
        {
            WorldCreationUiState uiState = screen.getUiState();
            Registry<WorldPreset> presets = uiState.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET);
            ResourceKey<WorldPreset> key = ResourceKey.create(Registries.WORLD_PRESET,
                    ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "oneblock"));

            var holderOpt = presets.getHolder(key);
            if (holderOpt.isEmpty())
            {
                LOG.warn("[OneBlock] oneblockultima:oneblock preset not found in registry");
                return;
            }

            WorldCreationUiState.WorldTypeEntry entry = new WorldCreationUiState.WorldTypeEntry(holderOpt.get());
            ResourceKey<WorldPreset> entryKey = holderOpt.get().unwrapKey().orElse(null);

            Field listField = WorldCreationUiState.class.getDeclaredField("normalPresetList");
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<WorldCreationUiState.WorldTypeEntry> list =
                    (List<WorldCreationUiState.WorldTypeEntry>) listField.get(uiState);

            if (list.stream().noneMatch(e -> e.preset().unwrapKey().equals(entryKey)))
            {
                list.add(entry);
                LOG.info("[OneBlock] added 'One Block' world type to create-world list");
            }
        }
        catch (Throwable t)
        {
            LOG.error("[OneBlock] failed to inject world type", t);
        }
    }
}

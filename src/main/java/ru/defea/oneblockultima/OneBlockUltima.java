package ru.defea.oneblockultima;

import ru.defea.oneblockultima.gui.containers.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.command.CommandAcceptGeneratorInvite;
import ru.defea.oneblockultima.command.CommandAddUltimaBalance;
import ru.defea.oneblockultima.command.CommandDeclineGeneratorInvite;
import ru.defea.oneblockultima.command.CommandInviteGeneratorMember;
import ru.defea.oneblockultima.command.CommandOBUSell;
import ru.defea.oneblockultima.command.CommandOBUSellAll;
import ru.defea.oneblockultima.command.CommandSetOwner;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.item.ModItems;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.tile.ModTileEntities;
import ru.defea.oneblockultima.update.UpdateChecker;
import ru.defea.oneblockultima.world.OneBlockWorldType;

@Mod(OneBlockUltima.MODID)
public class OneBlockUltima
{
    public static final String MODID = "oneblockultima";
    public static final String NAME = "OneBlockUltima";

    public static final net.minecraft.world.item.CreativeModeTab modTab = ModTab.TAB;

    public static CommonProxy proxy;

    private final IEventBus modBus;

    private static Logger proxiedLogger;

    public OneBlockUltima()
    {
                IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        this.modBus = modBus;
                ModBlocks.register(modBus);
        ModItems.register(modBus);
                ModTileEntities.register(modBus);
        ru.defea.oneblockultima.gui.containers.ModMenus.register(modBus);
        ru.defea.oneblockultima.loot.ModLootModifiers.register(modBus);
        
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener((RegisterEvent event) ->
        {
            if (event.getRegistryKey() == net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB)
            {
                event.register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "oneblockultima"),
                        () -> ModTab.TAB);
            }
        });

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        
        proxy = DistExecutor.unsafeRunForDist(() -> ru.defea.oneblockultima.ClientProxy::new, () -> CommonProxy::new);
        
        BlockSetConfig.load(FMLPaths.CONFIGDIR.get().toFile());
                BlockPriceConfig.load(FMLPaths.CONFIGDIR.get().toFile());
        OneBlockWorldType.init();
            }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        ModMessages.register();
        proxy.init();
        ModBlocks.registerOreDict();
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        proxy.preInit();
        proxy.clientSetup(modBus, event);
    }

    private void onRegisterCommands(final RegisterCommandsEvent event)
    {
        CommandAddUltimaBalance.register(event.getDispatcher());
        CommandInviteGeneratorMember.register(event.getDispatcher());
        CommandAcceptGeneratorInvite.register(event.getDispatcher());
        CommandDeclineGeneratorInvite.register(event.getDispatcher());
        CommandSetOwner.register(event.getDispatcher());
        CommandOBUSell.register(event.getDispatcher());
        CommandOBUSellAll.register(event.getDispatcher());
    }

    private void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event)
    {
        UpdateChecker.shutdown();
    }

    public static Logger getLogger()
    {
        if (proxiedLogger == null)
        {
            proxiedLogger = LogManager.getLogger(MODID);
        }
        return proxiedLogger;
    }

    /**
     * Debug-level diagnostics are only logged when the debug mode setting is enabled,
     * so the console stays clean in normal play.
     */
    public static void logDebug(String msg, Object... args)
    {
        if (ModSettings.isDebugEnabled())
        {
            getLogger().info(msg, args);
        }
    }

    public static void logDebugWarn(String msg, Object... args)
    {
        if (ModSettings.isDebugEnabled())
        {
            getLogger().warn(msg, args);
        }
    }
}

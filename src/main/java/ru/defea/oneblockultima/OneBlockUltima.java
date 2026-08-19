package ru.defea.oneblockultima;

import net.minecraft.creativetab.CreativeTabs;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.command.*;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.gui.GuiHandler;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.item.ModItems;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.recipe.ModRecipes;
import ru.defea.oneblockultima.tile.ModTileEntities;
import ru.defea.oneblockultima.update.UpdateChecker;
import ru.defea.oneblockultima.world.OneBlockWorldType;

@Mod(
        modid = OneBlockUltima.MODID,
        useMetadata = true,
        guiFactory = "ru.defea.oneblockultima.ModGuiFactory"
)
public class OneBlockUltima
{
    public static final String MODID = "oneblockultima";
    public static final String NAME = "OneBlockUltima";

    public static final CreativeTabs modTab = new ModTab(NAME);

    @Mod.Instance(MODID)
    public static OneBlockUltima instance;

    @SidedProxy(clientSide = "ru.defea.oneblockultima.ClientProxy", serverSide = "ru.defea.oneblockultima.CommonProxy")
    public static CommonProxy proxy;

    private static Logger proxiedLogger;

    public static Logger getLogger()
    {
        if (proxiedLogger == null)
        {
            final Logger real = LogManager.getLogger(MODID);
            proxiedLogger = (Logger) java.lang.reflect.Proxy.newProxyInstance(
                    Logger.class.getClassLoader(),
                    new Class<?>[]{Logger.class},
                    (proxy, method, args) ->
                    {
                        if (ModSettings.isDebugEnabled())
                        {
                            try
                            {
                                return method.invoke(real, args);
                            }
                            catch (java.lang.reflect.InvocationTargetException e)
                            {
                                throw e.getCause();
                            }
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) return Boolean.FALSE;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                        if (returnType == float.class) return 0.0F;
                        if (returnType == double.class) return 0.0D;
                        return null;
                    });
        }
        return proxiedLogger;
    }

    /**
     * Unconditional logger for diagnostics; unlike {@link #getLogger()} it always
     * writes regardless of the debug setting.
     */
    public static Logger getRawLogger()
    {
        return LogManager.getLogger(MODID);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        proxiedLogger = null;
        BlockSetConfig.load(event.getModConfigurationDirectory());
        BlockPriceConfig.load(event.getModConfigurationDirectory());
        OneBlockPlayerDataProvider.register();
        ModBlocks.registerBlocksAndItems();
        ModItems.registerItems();
        ModRecipes.registerRecipes();
        ModEvents modEvents = new ModEvents();
        MinecraftForge.EVENT_BUS.register(modEvents);
        FMLCommonHandler.instance().bus().register(modEvents);
        ModTileEntities.register();
        ModMessages.register();
        OneBlockWorldType.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        proxy.preInit();

        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide() == cpw.mods.fml.relauncher.Side.CLIENT)
        {
            ContainerSetsConfig.loadStaticCustomNames();
        }
        event.getModLog().info("{} loading...", NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent ignoredEvent)
    {
        proxy.init();
        ModBlocks.registerOreDict();
        ru.defea.oneblockultima.recipe.JsonRecipeLoader.registerRecipes();
        ru.defea.oneblockultima.achievement.ModAchievements.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandAddUltimaBalance());
        event.registerServerCommand(new CommandInviteGeneratorMember());
        event.registerServerCommand(new CommandAcceptGeneratorInvite());
        event.registerServerCommand(new CommandDeclineGeneratorInvite());
        event.registerServerCommand(new CommandSetOwner());
        event.registerServerCommand(new CommandOBUSell());
        event.registerServerCommand(new CommandOBUSellAll());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        UpdateChecker.shutdown();
    }
}
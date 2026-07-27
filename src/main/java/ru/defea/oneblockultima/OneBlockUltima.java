package ru.defea.oneblockultima;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.command.*;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.GuiHandler;
import ru.defea.oneblockultima.gui.GuiSetsConfig;
import ru.defea.oneblockultima.network.ModMessages;
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

    private static Logger logger;

    public static Logger getLogger()
    {
        if (logger == null)
        {
            logger = LogManager.getLogger(MODID);
        }
        return logger;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        BlockSetConfig.load(event.getModConfigurationDirectory());
        BlockPriceConfig.load();
        OneBlockPlayerDataProvider.register();
        ModTileEntities.register();
        ModMessages.register();
        OneBlockWorldType.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        proxy.preInit();

        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide() == net.minecraftforge.fml.relauncher.Side.CLIENT)
        {
            GuiSetsConfig.loadStaticCustomNames();
        }
        logger.info("{} загружается...", NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent ignoredEvent)
    {
        proxy.init();
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

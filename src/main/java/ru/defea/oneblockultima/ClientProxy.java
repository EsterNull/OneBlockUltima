package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.defea.oneblockultima.client.OneBlockWorldTypeSelector;
import ru.defea.oneblockultima.event.ModEventsClient;
import ru.defea.oneblockultima.gui.GuiCaseRoulette;
import ru.defea.oneblockultima.gui.GuiClaimGenerator;
import ru.defea.oneblockultima.gui.GuiGuideBook;
import ru.defea.oneblockultima.gui.GuiOneBlock;
import ru.defea.oneblockultima.gui.ModMainSettings;
import ru.defea.oneblockultima.gui.containers.ModMenus;

public class ClientProxy extends CommonProxy
{
    @Override
    public void clientSetup(IEventBus modBus, FMLClientSetupEvent event)
    {
        MinecraftForge.EVENT_BUS.register(OneBlockWorldTypeSelector.class);
        modBus.addListener(ModEventsClient::registerKeyMappings);

        MenuScreens.register(ModMenus.ONE_BLOCK.get(), GuiOneBlock::new);
        MenuScreens.register(ModMenus.CLAIM_GENERATOR.get(), GuiClaimGenerator::new);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new ModMainSettings(parent)));
    }

    @Override
    public void openGuideBookGui(Player player)
    {
        Minecraft.getInstance().setScreen(new GuiGuideBook());
    }

    @Override
    public void openCaseRouletteGui(Player player, ItemStack caseStack)
    {
        Minecraft.getInstance().setScreen(new GuiCaseRoulette(caseStack));
    }
}

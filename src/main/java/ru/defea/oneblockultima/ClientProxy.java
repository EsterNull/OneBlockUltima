package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.event.ModEventsClient;
import ru.defea.oneblockultima.gui.GuiCaseRoulette;
import ru.defea.oneblockultima.gui.GuiGuideBook;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy
{
    @Override
    public void preInit()
    {
        KeyBinding hidePanel = new KeyBinding("key.oneblockultima.hide_panel", Keyboard.KEY_J, "key.categories.oneblockultima");
        ClientRegistry.registerKeyBinding(hidePanel);
        ModEventsClient.hidePanelKey = hidePanel;
    }

    @Override
    public void openGuideBookGui(EntityPlayer player)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiGuideBook());
    }

    @Override
    public void openCaseRouletteGui(EntityPlayer player, ItemStack caseStack)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiCaseRoulette(caseStack));
    }
}
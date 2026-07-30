package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.ViewFactory;

public class ModMainSettings extends GuiScreen
{
    private static final int BUTTON_CONFIG_EDITOR = 0;
    private static final int BUTTON_MOD_SETTINGS = 1;
    private static final int BUTTON_BLOCK_PRICES = 2;
    private static final int BUTTON_BACK = 3;

    private final GuiScreen parent;
    private ViewFactory factory;

    public ModMainSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    @Override
    public void initGui()
    {
        factory = new ViewFactory(width, height)
            .padding(10)
            .gap(8)
            .align(Alignment.CENTER)
            .centerVertical();

        factory.title("gui.oneblockultima.mod_settings.title");
        factory.button(BUTTON_CONFIG_EDITOR, 200, I18n.format("gui.oneblockultima.settings.open_editor"));
        factory.button(BUTTON_MOD_SETTINGS, 200, I18n.format("gui.oneblockultima.settings.mod_settings"));
        factory.button(BUTTON_BLOCK_PRICES, 200, I18n.format("gui.oneblockultima.mod_settings.block_prices"));
        factory.button(BUTTON_BACK, 200, I18n.format("gui.oneblockultima.cancel"));

        factory.build(buttonList, fontRenderer);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_CONFIG_EDITOR)
        {
            mc.displayGuiScreen(new GuiSetsConfig(this));
        }
        else if (button.id == BUTTON_MOD_SETTINGS)
        {
            mc.displayGuiScreen(new GuiModSettings(this));
        }
        else if (button.id == BUTTON_BLOCK_PRICES)
        {
            mc.displayGuiScreen(new GuiBlockPrices(this));
        }
        else if (button.id == BUTTON_BACK)
        {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return true;
    }
}

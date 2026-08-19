package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;

import java.util.HashMap;
import java.util.Map;

public class ModMainSettings extends GuiScreen
{
    private static final int BUTTON_BACK = 999;
    private static final int BUTTON_CONFIG_EDITOR = 0;
    private static final int BUTTON_UI_SETTINGS = 1;
    private static final int BUTTON_BLOCK_PRICES = 2;
    private static final int BUTTON_MISC_SETTINGS = 3;

    private final GuiScreen parent;

    public ModMainSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    public ModMainSettings()
    {
        this(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui()
    {
        ViewFactory factory = new ViewFactory(width, height)
                .padding(10)
                .gap(8)
                .align(Alignment.CENTER)
                .centerVertical();

        Map<Integer, String> labels = new HashMap<>();
        labels.put(BUTTON_CONFIG_EDITOR, I18n.format("gui.oneblockultima.config.sets_title"));
        labels.put(BUTTON_UI_SETTINGS, I18n.format("gui.oneblockultima.ui_settings.title"));
        labels.put(BUTTON_BLOCK_PRICES, I18n.format("gui.oneblockultima.settings.open_prices"));
        labels.put(BUTTON_MISC_SETTINGS, I18n.format("gui.oneblockultima.misc.title"));
        labels.put(BUTTON_BACK, I18n.format("gui.oneblockultima.back"));

        int maxWidth = 0;
        for (String s : labels.values()) {
            int stringWidth = fontRendererObj.getStringWidth(s);
            if (stringWidth > maxWidth) maxWidth = stringWidth;
        }
        maxWidth += ButtonElement.BUTTON_PADDING;

        factory.title("gui.oneblockultima.ui_settings.title");
        for (int i : labels.keySet())
        {
            factory.button(i, maxWidth, labels.get(i));
        }

        factory.build(buttonList, fontRendererObj);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_CONFIG_EDITOR)
        {
            mc.displayGuiScreen(new GuiSetsConfig(this));
        }
        else if (button.id == BUTTON_UI_SETTINGS)
        {
            mc.displayGuiScreen(new GuiUiSettings(this));
        }
        else if (button.id == BUTTON_BLOCK_PRICES)
        {
            mc.displayGuiScreen(new GuiBlockPrices(this));
        }
        else if (button.id == BUTTON_MISC_SETTINGS)
        {
            mc.displayGuiScreen(new GuiMiscSettings(this));
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
}

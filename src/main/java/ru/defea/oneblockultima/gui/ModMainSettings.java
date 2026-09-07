package ru.defea.oneblockultima.gui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;

import java.util.HashMap;
import java.util.Map;

public class ModMainSettings extends ModScreen
{
    private static final int BUTTON_BACK = 999;
    private static final int BUTTON_CONFIG_EDITOR = 0;
    private static final int BUTTON_UI_SETTINGS = 1;
    private static final int BUTTON_BLOCK_PRICES = 2;
    private static final int BUTTON_MISC_SETTINGS = 3;

    private final Screen parent;

    public ModMainSettings(Screen parent)
    {
        super(Component.literal(I18n.get("gui.oneblockultima.ui_settings.title")));
        this.parent = parent;
    }

    @Override
    public void init()
    {
        ViewFactory factory = new ViewFactory(width, height)
                .padding(10)
                .gap(8)
                .align(Alignment.CENTER)
                .centerVertical();

        Map<Integer, String> labels = new HashMap<>();
        labels.put(BUTTON_CONFIG_EDITOR, I18n.get("gui.oneblockultima.config.sets_title"));
        labels.put(BUTTON_UI_SETTINGS, I18n.get("gui.oneblockultima.ui_settings.title"));
        labels.put(BUTTON_BLOCK_PRICES, I18n.get("gui.oneblockultima.settings.open_prices"));
        labels.put(BUTTON_MISC_SETTINGS, I18n.get("gui.oneblockultima.misc.title"));
        labels.put(BUTTON_BACK, I18n.get("gui.oneblockultima.back"));

        int maxWidth = 0;
        for (String s : labels.values()) {
            int stringWidth = this.font.width(s);
            if (stringWidth > maxWidth) maxWidth = stringWidth;
        }
        maxWidth += ButtonElement.BUTTON_PADDING;

        factory.title("gui.oneblockultima.ui_settings.title");
        factory.button(BUTTON_CONFIG_EDITOR, maxWidth, labels.get(BUTTON_CONFIG_EDITOR))
                .onPress(() -> minecraft.setScreen(new GuiSetsConfig(this)));
        factory.button(BUTTON_UI_SETTINGS, maxWidth, labels.get(BUTTON_UI_SETTINGS))
                .onPress(() -> minecraft.setScreen(new GuiUiSettings(this)));
        factory.button(BUTTON_BLOCK_PRICES, maxWidth, labels.get(BUTTON_BLOCK_PRICES))
                .onPress(() -> minecraft.setScreen(new GuiBlockPrices(this)));
        factory.button(BUTTON_MISC_SETTINGS, maxWidth, labels.get(BUTTON_MISC_SETTINGS))
                .onPress(() -> minecraft.setScreen(new GuiMiscSettings(this)));
        factory.button(BUTTON_BACK, maxWidth, labels.get(BUTTON_BACK))
                .onPress(() -> minecraft.setScreen(parent));

        factory.build(this, this.font);
        for (net.minecraft.client.gui.components.AbstractWidget w : factory.getWidgets()) {
            this.addRenderableWidget(w);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        this.drawModBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
    }
}

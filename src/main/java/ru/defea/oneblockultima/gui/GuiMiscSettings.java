package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.layout.*;

import java.io.IOException;

public class GuiMiscSettings extends GuiScreen {
    private static final int BUTTON_BACK = 0;
    private static final int BUTTON_SAVE = 1;
    private static final int BUTTON_MOB_WORLD_GENERATION = 2;

    private final GuiScreen parent;
    private ViewFactory factory;
    private ModSettings settings;

    private boolean mobWorldGeneration;

    private ButtonToggleElement mobWorldGenerationToggle;

    public GuiMiscSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        buildView();

        settings = ModSettings.get();
        mobWorldGeneration = settings.getMobWorldGeneration();
    }

    private void buildView()
    {
        factory = new ViewFactory(width, height)
                .margin(8).padding(2)
                .gap(6)
                .align(Alignment.CENTER)
                .centerVertical()
                .fitContent();

        factory.title("gui.oneblockultima.misc.title");

        RowElement firstLineControls = factory.row(Alignment.LEFT).gap(8);
        mobWorldGenerationToggle = firstLineControls.buttonToggle(BUTTON_MOB_WORLD_GENERATION, mobWorldGeneration)
                .label(I18n.format("gui.oneblockultima.misc.mob_world_generation"));

        RowElement btnRow = factory.row(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));
        btnRow.button(BUTTON_SAVE, I18n.format("gui.oneblockultima.save"));

        factory.build(buttonList, fontRenderer);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_BACK)
        {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_SAVE) {
            settings.setMobWorldGeneration(mobWorldGeneration);
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_MOB_WORLD_GENERATION) {
            mobWorldGeneration = !mobWorldGeneration;
            mobWorldGenerationToggle.toggle();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (factory != null) factory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        if (factory != null) factory.draw(fontRenderer, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

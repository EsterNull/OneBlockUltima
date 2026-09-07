package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for OneBlockUltima screens.
 *
 * Vanilla {@link Screen#renderBackground(GuiGraphics)} stretches a tiny 32x32 region of the
 * options dirt texture across the whole screen, which looks blurry on screens whose content
 * does not cover the entire viewport. Use {@link #drawModBackground(GuiGraphics)} instead to
 * paint a crisp solid fill.
 */
public abstract class ModScreen extends Screen
{
    public static final int SCREEN_BACKGROUND = 0xFF14161A;

    protected ModScreen(Component title)
    {
        super(title);
    }

    /**
     * Adds a widget both to the render pass and to the interactive children list,
     * so it receives mouse, keyboard and character input. The protected
     * {@link Screen#addRenderableWidget} is otherwise unreachable from the layout package.
     */
    public void registerRenderableWidget(AbstractWidget widget)
    {
        this.addRenderableWidget(widget);
    }

    protected void drawModBackground(GuiGraphics guiGraphics)
    {
        guiGraphics.fill(0, 0, this.width, this.height, SCREEN_BACKGROUND);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
    }
}

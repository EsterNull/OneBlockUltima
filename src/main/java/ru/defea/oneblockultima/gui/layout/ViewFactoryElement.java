package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ViewFactoryElement extends ViewElement<ViewFactoryElement> {
    private final ViewFactory factory;

    public ViewFactoryElement(ViewFactory factory) {
        this.factory = factory;
    }

    public ViewFactory getFactory() {
        return factory;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory outer) {
        factory.build(screen, font, computedX, computedY, computedWidth, computedHeight);
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        factory.draw(g, font, mouseX, mouseY, partialTicks, computedX, computedY, computedWidth, computedHeight);
    }

    @Override
    public boolean actionPerformed(AbstractWidget button) {
        return factory.actionPerformed(button);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return factory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        return factory.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        return factory.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        return factory.handleMouseInput(dWheel);
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        return factory.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateCursorCounter() {
        factory.updateScreen();
    }

    @Override
    public void tick() {
        factory.updateScreen();
    }

    @Override
    public int getPreferredWidth() {
        return getPreferredWidth(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredHeight() {
        return getPreferredHeight(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredWidth(Font font) {
        int natural = factory.computeNaturalContentWidth(font);
        return Math.max(natural, 0);
    }

    @Override
    public int getPreferredHeight(Font font) {
        int natural = factory.computeNaturalContentHeight(font);
        return Math.max(natural, 0);
    }
}

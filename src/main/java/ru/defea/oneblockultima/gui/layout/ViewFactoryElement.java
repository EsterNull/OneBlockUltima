package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

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
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory outer) {
        factory.build(buttonList, fontRenderer, computedX, computedY, computedWidth, computedHeight);
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        factory.draw(fr, mouseX, mouseY, partialTicks, computedX, computedY, computedWidth, computedHeight);
    }

    @Override
    public boolean actionPerformed(GuiButton button) {
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
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return 0;
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        int natural = factory.computeNaturalContentWidth(fr);
        return Math.max(natural, 0);
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
        int natural = factory.computeNaturalContentHeight(fr);
        return Math.max(natural, 0);
    }
}

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.List;

public class ViewSwitcherElement extends ViewElement {
    private final List<ViewElement> views = new ArrayList<>();
    private int currentView = 0;

    public ViewSwitcherElement setView(int index) {
        this.currentView = index;
        return this;
    }

    public int getCurrentView() {
        return currentView;
    }

    public ViewSwitcherElement addView(ViewElement... elements) {
        for (ViewElement e : elements) {
            views.add(e);
        }
        return this;
    }

    public ViewElement getView(int index) {
        if (index >= 0 && index < views.size()) return views.get(index);
        return null;
    }

    public List<ViewElement> getViews() {
        return views;
    }

    public ViewElement currentViewElement() {
        return getView(currentView);
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        ViewElement active = currentViewElement();
        if (active != null) {
            active.setComputedPosition(computedX, computedY);
            active.setComputedSize(computedWidth, computedHeight);
            active.createWidgets(buttonList, fontRenderer, factory);
        }
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        ViewElement active = currentViewElement();
        if (active != null && active.isVisible()) active.draw(fr, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean actionPerformed(GuiButton button) {
        ViewElement active = currentViewElement();
        return active != null && active.actionPerformed(button);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        ViewElement active = currentViewElement();
        return active != null && active.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        ViewElement active = currentViewElement();
        return active != null && active.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        ViewElement active = currentViewElement();
        return active != null && active.handleMouseInput(dWheel);
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        ViewElement active = currentViewElement();
        return active != null && active.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateCursorCounter() {
        ViewElement active = currentViewElement();
        if (active != null) active.updateCursorCounter();
    }

    @Override
    public int getPreferredWidth() {
        ViewElement active = currentViewElement();
        return active != null ? active.getPreferredWidth() : 0;
    }

    @Override
    public int getPreferredHeight() {
        ViewElement active = currentViewElement();
        return active != null ? active.getPreferredHeight() : 0;
    }
}

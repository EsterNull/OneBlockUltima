package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_1;
import static ru.defea.oneblockultima.Constants.TRANSPARENT_DARK_GRAY_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ViewSwitcherElement extends ViewElement<ViewSwitcherElement> {
    private final List<ViewElement<?>> views = new ArrayList<>();
    private int currentView = 0;
    private boolean fitContent = false;
    private int panelColor = TRANSPARENT_DARK_GRAY_COLOR_1;
    private int panelBorderColor = DARK_GRAY_COLOR_1;

    public ViewSwitcherElement setView(int index) {
        this.currentView = index;
        return this;
    }

    public ViewSwitcherElement fitContent(boolean fitContent) {
        this.fitContent = fitContent;
        return this;
    }

    public ViewSwitcherElement panel(int color, int borderColor) {
        this.panelColor = color;
        this.panelBorderColor = borderColor;
        return this;
    }

    public int getCurrentView() {
        return currentView;
    }

    public ViewSwitcherElement addView(ViewElement<?>... elements) {
        Collections.addAll(views, elements);
        return this;
    }

    public ViewSwitcherElement replaceView(int index, ViewElement<?> element) {
        while (views.size() <= index) {
            views.add(null);
        }
        views.set(index, element);
        return this;
    }

    public ViewElement<?> getView(int index) {
        if (index >= 0 && index < views.size()) return views.get(index);
        return null;
    }

    public List<ViewElement<?>> getViews() {
        return views;
    }

    public ViewElement<?> currentViewElement() {
        return getView(currentView);
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        ViewElement<?> active = currentViewElement();
        if (active != null) {
            int x = computedX;
            int y = computedY;
            int w = computedWidth;
            int h = computedHeight;
            if (fitContent) {
                int prefW = active.getPreferredWidth(fontRenderer);
                int prefH = active.getPreferredHeight(fontRenderer);
                w = Math.min(Math.max(prefW, 0), computedWidth);
                h = Math.min(Math.max(prefH, 0), computedHeight);
                x = computedX + (computedWidth - w) / 2;
                y = computedY + (computedHeight - h) / 2;
            }
            active.setComputedPosition(x, y);
            active.setComputedSize(w, h);
            active.createWidgets(buttonList, fontRenderer, factory);
        }
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        ViewElement<?> active = currentViewElement();
        if (active == null || !active.isVisible()) return;
        if (fitContent && panelColor != 0) {
            int px = active.getComputedX() - 4;
            int py = active.getComputedY() - 4;
            int pw = active.getComputedWidth() + 8;
            int ph = active.getComputedHeight() + 8;
            Gui.drawRect(px, py, px + pw, py + ph, panelColor);
            if (panelBorderColor != 0) {
                Gui.drawRect(px, py, px + pw, py + 1, panelBorderColor);
                Gui.drawRect(px, py + ph - 1, px + pw, py + ph, panelBorderColor);
                Gui.drawRect(px, py, px + 1, py + ph, panelBorderColor);
                Gui.drawRect(px + pw - 1, py, px + pw, py + ph, panelBorderColor);
            }
        }
        active.draw(fr, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean actionPerformed(GuiButton button) {
        ViewElement<?> active = currentViewElement();
        return active != null && active.actionPerformed(button);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        ViewElement<?> active = currentViewElement();
        return active != null && active.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        ViewElement<?> active = currentViewElement();
        return active != null && active.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean handleMouseInput(int dWheel) {
        ViewElement<?> active = currentViewElement();
        return active != null && active.handleMouseInput(dWheel);
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        ViewElement<?> active = currentViewElement();
        return active != null && active.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateCursorCounter() {
        ViewElement<?> active = currentViewElement();
        if (active != null) active.updateCursorCounter();
    }

    @Override
    public void tick() {
        ViewElement<?> active = currentViewElement();
        if (active != null) active.tick();
    }

    @Override
    public int getPreferredWidth() {
        ViewElement<?> active = currentViewElement();
        return active != null ? active.getPreferredWidth() : 0;
    }

    @Override
    public int getPreferredHeight() {
        ViewElement<?> active = currentViewElement();
        return active != null ? active.getPreferredHeight() : 0;
    }
}

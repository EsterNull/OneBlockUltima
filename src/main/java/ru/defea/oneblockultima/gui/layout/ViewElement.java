package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class ViewElement<T extends ViewElement<T>> {
    protected int computedX;
    protected int computedY;
    protected int computedWidth;
    protected int computedHeight;
    protected Alignment alignment = null;
    protected boolean visible = true;
    protected boolean flexible = false;
    protected int explicitWidth = -1;
    protected int widthPercent = -1;
    protected int heightPercent = -1;
    protected boolean stretchToContent = false;

    public abstract void createWidgets(Screen screen, Font font, ViewFactory factory);

    public abstract void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks);

    public AbstractWidget getWidget() {
        return null;
    }

    public boolean actionPerformed(AbstractWidget button) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        return false;
    }

    public boolean mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        return false;
    }

    public boolean handleMouseInput(int dWheel) {
        return false;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    public void updateCursorCounter() {
    }

    public void tick() {
    }

    public void setComputedPosition(int x, int y) {
        this.computedX = x;
        this.computedY = y;
    }

    public void setComputedSize(int width, int height) {
        this.computedWidth = width;
        this.computedHeight = height;
    }

    public abstract int getPreferredWidth();

    public abstract int getPreferredHeight();

    public int getPreferredWidth(Font font) {
        return getPreferredWidth();
    }

    public int getPreferredHeight(Font font) {
        return getPreferredHeight();
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public T align(Alignment alignment) {
        this.alignment = alignment;
        return self();
    }

    public boolean isVisible() {
        return visible;
    }

    public T visible(boolean visible) {
        this.visible = visible;
        return self();
    }

    public T flexible(boolean flexible) {
        this.flexible = flexible;
        return self();
    }

    public boolean isFlexible() {
        return flexible;
    }

    public int getWidthPercent() {
        return widthPercent;
    }

    public T width(int width) {
        this.explicitWidth = width;
        return self();
    }

    public int getExplicitWidth() {
        return explicitWidth;
    }

    public T widthPercent(int percent) {
        this.widthPercent = percent;
        return self();
    }

    public int getHeightPercent() {
        return heightPercent;
    }

    public T heightPercent(int percent) {
        this.heightPercent = percent;
        return self();
    }

    public T stretchToContent() {
        this.stretchToContent = true;
        return self();
    }

    public T stretchToContent(boolean stretch) {
        this.stretchToContent = stretch;
        return self();
    }

    public boolean isStretchToContent() {
        return stretchToContent;
    }

    public int getComputedX() {
        return computedX;
    }

    public int getComputedY() {
        return computedY;
    }

    public int getComputedWidth() {
        return computedWidth;
    }

    public int getComputedHeight() {
        return computedHeight;
    }
}

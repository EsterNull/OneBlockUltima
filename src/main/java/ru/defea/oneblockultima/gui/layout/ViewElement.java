package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

public abstract class ViewElement {
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

    public abstract void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory);

    public abstract void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks);

    public boolean actionPerformed(GuiButton button) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int state) {
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

    public int getPreferredWidth(FontRenderer fr) {
        return getPreferredWidth();
    }

    public int getPreferredHeight(FontRenderer fr) {
        return getPreferredHeight();
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public ViewElement align(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public ViewElement visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public ViewElement flexible(boolean flexible) {
        this.flexible = flexible;
        return this;
    }

    public boolean isFlexible() {
        return flexible;
    }

    public int getWidthPercent() {
        return widthPercent;
    }

    public ViewElement width(int width) {
        this.explicitWidth = width;
        return this;
    }

    public int getExplicitWidth() {
        return explicitWidth;
    }

    public ViewElement widthPercent(int percent) {
        this.widthPercent = percent;
        return this;
    }

    public int getHeightPercent() {
        return heightPercent;
    }

    public ViewElement heightPercent(int percent) {
        this.heightPercent = percent;
        return this;
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

package ru.defea.oneblockultima.gui.layout;

import static ru.defea.oneblockultima.Constants.*;

public class ButtonToggleElement extends ButtonElement {
    private static final String CHECKMARK = "\u2714";

    private boolean stateTriggered;

    public ButtonToggleElement(int id, boolean stateTriggered) {
        super(id, stateTriggered ? CHECKMARK : "");
        this.stateTriggered = stateTriggered;
    }

    public ButtonToggleElement(boolean stateTriggered) {
        super(stateTriggered ? CHECKMARK : "");
        this.stateTriggered = stateTriggered;
    }

    public void stateTriggered(boolean stateTriggered) {
        this.stateTriggered = stateTriggered;
        text(stateTriggered ? CHECKMARK : "");
    }

    public boolean isStateTriggered() {
        return this.stateTriggered;
    }

    public void toggle() {
        this.stateTriggered = !stateTriggered;
        this.
        text(stateTriggered ? CHECKMARK : "");
    }

    @Override
    protected int widgetFillColor(boolean hovered) {
        if (stateTriggered) return DARK_GREEN;
        else if (hovered) return GRAY_COLOR_6;
        else return DARK_GRAY_COLOR_1;
    }

    @Override
    protected int widgetTextColor(boolean hovered) {
        if (stateTriggered) return SUCCESS_COLOR;
        else if (hovered) return this.getTextColorHovered();
        else return this.getTextColor();
    }

    public ButtonToggleElement width(int width) {
        this.width = width;
        return this;
    }

    public ButtonToggleElement height(int height) {
        this.height = height;
        return this;
    }

    public ButtonToggleElement enabled(boolean enabled) {
        this.enabled = enabled;
        if (guiButton != null) guiButton.enabled = enabled;
        return this;
    }

    @Override
    public ButtonToggleElement visible(boolean visible) {
        super.visible(visible);
        if (guiButton != null) guiButton.visible = visible;
        return this;
    }

    public ButtonToggleElement text(String text) {
        this.text = text;
        if (guiButton != null) guiButton.displayString = text;
        return this;
    }
}
package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ButtonToggleElement extends ButtonElement<ButtonToggleElement> {
    private static final String CHECKMARK = "\u2714";

    private boolean stateTriggered;
    private String label;
    private LabelPosition labelPosition = LabelPosition.RIGHT;
    private int labelGap = 6;
    private int labelColor = WHITE_COLOR_1;

    public ButtonToggleElement(int id, boolean stateTriggered) {
        super(id, stateTriggered ? CHECKMARK : "");
        this.stateTriggered = stateTriggered;
    }

    public ButtonToggleElement(boolean stateTriggered) {
        super(stateTriggered ? CHECKMARK : "");
        this.stateTriggered = stateTriggered;
    }

    public ButtonToggleElement label(String label) {
        this.label = label;
        sideLabel(label);
        return this;
    }

    public ButtonToggleElement label(String label, LabelPosition position) {
        this.label = label;
        this.labelPosition = position;
        sideLabel(label).sideLabelPosition(position);
        return this;
    }

    public ButtonToggleElement labelPosition(LabelPosition position) {
        this.labelPosition = position;
        sideLabelPosition(position);
        return this;
    }

    public ButtonToggleElement labelColor(int color) {
        this.labelColor = color;
        sideLabelColor(color);
        return this;
    }

    public ButtonToggleElement labelGap(int gap) {
        this.labelGap = gap;
        sideLabelGap(gap);
        return this;
    }

    public String getLabel() {
        return label;
    }

    public LabelPosition getLabelPosition() {
        return labelPosition;
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
        text(stateTriggered ? CHECKMARK : "");
    }

    @Override
    protected int widgetFillColor(boolean hovered) {
        if (!enabled) return DISABLED_BUTTON_FILL;
        if (stateTriggered) return hovered ? SUCCESS_HOVERED_COLOR : DARK_GREEN;
        else if (hovered) return GRAY_COLOR_6;
        else return DARK_GRAY_COLOR_1;
    }

    @Override
    protected int widgetTextColor(boolean hovered) {
        if (!enabled) return DISABLED_BUTTON_TEXT;
        if (stateTriggered) return SUCCESS_COLOR;
        else if (hovered) return this.getTextColorHovered();
        else return this.getTextColor();
    }

    @Override
    public int getPreferredWidth() {
        int boxW = width > 0 ? width : BUTTON_PADDING;
        if (label != null && !label.isEmpty()) {
            return boxW + labelGap + label.length() * 6;
        }
        return boxW;
    }

    @Override
    public int getPreferredWidth(Font font) {
        if (label == null || label.isEmpty()) {
            return super.getPreferredWidth(font);
        }
        int boxW = width > 0 ? width : BUTTON_PADDING;
        return boxW + labelGap + font.width(label);
    }

    @Override
    public int getPreferredHeight() {
        return getPreferredHeight(Minecraft.getInstance().font);
    }
}

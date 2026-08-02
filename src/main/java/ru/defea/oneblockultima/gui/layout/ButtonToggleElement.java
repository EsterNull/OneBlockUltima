package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import javax.annotation.Nonnull;
import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ButtonToggleElement extends ButtonElement<ButtonToggleElement> {
    private static final String CHECKMARK = "\u2714";

    public enum LabelPosition {
        LEFT, RIGHT
    }

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
        return this;
    }

    public ButtonToggleElement label(String label, LabelPosition position) {
        this.label = label;
        this.labelPosition = position;
        return this;
    }

    public ButtonToggleElement labelPosition(LabelPosition position) {
        this.labelPosition = position;
        return this;
    }

    public ButtonToggleElement labelColor(int color) {
        this.labelColor = color;
        return this;
    }

    public ButtonToggleElement labelGap(int gap) {
        this.labelGap = gap;
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
        int boxW = Math.max(width, 0);
        if (label != null && !label.isEmpty()) {
            return boxW + labelGap + label.length() * 6;
        }
        return boxW;
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        if (label == null || label.isEmpty()) {
            return super.getPreferredWidth(fr);
        }
        int boxW = width > 0 ? width : (fr.getStringWidth(text != null ? text : "") + BUTTON_PADDING);
        return boxW + labelGap + fr.getStringWidth(label);
    }

    private int getBoxWidth(FontRenderer fr) {
        if (width > 0) return width;
        if (label == null || label.isEmpty()) return computedWidth;
        return fr.getStringWidth(text != null ? text : "") + BUTTON_PADDING;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        boolean hasLabel = label != null && !label.isEmpty();
        final int boxW = getBoxWidth(fontRenderer);
        final int buttonId = getId();
        final int borderSize = getBorderSize();
        final int borderColor = getBorderColor();

        guiButton = new GuiButton(buttonId, computedX, computedY, computedWidth, computedHeight, text) {
            @Override
            public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                if (!this.visible) {
                    return;
                }
                this.hovered = mouseX >= this.x && mouseY >= this.y &&
                        mouseX < this.x + this.width && mouseY < this.y + this.height;

                int actualBoxW = hasLabel ? Math.min(boxW, this.width) : this.width;
                int boxX = hasLabel && labelPosition == LabelPosition.LEFT ? this.x + (this.width - actualBoxW) : this.x;

                drawRect(boxX, this.y, boxX + actualBoxW, this.y + this.height, widgetFillColor(hovered));
                drawRect(boxX, this.y, boxX + actualBoxW, this.y + borderSize, borderColor);
                drawRect(boxX, this.y + this.height - borderSize, boxX + actualBoxW, this.y + this.height, borderColor);
                drawRect(boxX, this.y, boxX + borderSize, this.y + this.height, borderColor);
                drawRect(boxX + actualBoxW - borderSize, this.y, boxX + actualBoxW, this.y + this.height, borderColor);
                this.drawCenteredString(fontRenderer, this.displayString,
                        boxX + actualBoxW / 2,
                        this.y + (this.height - fontRenderer.FONT_HEIGHT) / 2,
                        widgetTextColor(hovered));

                if (hasLabel) {
                    int labelX = labelPosition == LabelPosition.RIGHT
                            ? this.x + actualBoxW + labelGap
                            : this.x;
                    int labelY = this.y + (this.height - fontRenderer.FONT_HEIGHT) / 2;
                    fontRenderer.drawString(label, labelX, labelY, enabled ? labelColor : DISABLED_BUTTON_TEXT);
                }
            }
        };
        guiButton.enabled = enabled;
        buttonList.add(guiButton);
    }
}

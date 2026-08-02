package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.function.DoubleConsumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class DoubleStepperElement extends ViewElement<DoubleStepperElement> {
    private static final int DEC_BUTTON_ID = -9101;
    private static final int INC_BUTTON_ID = -9102;

    private final RowElement row = new RowElement(Alignment.RIGHT).gap(4);
    private final ButtonElement<?> decButton = new ButtonElement<>(DEC_BUTTON_ID, "-");
    private final ButtonElement<?> incButton = new ButtonElement<>(INC_BUTTON_ID, "+");
    private final TextFieldElement field = new TextFieldElement(60);

    private double value;
    private double min = 0;
    private double max = Double.MAX_VALUE;
    private double step = 1.0;
    private int repeatDelay = 20;
    private int repeatInterval = 5;
    private int doubleEvery = 60;
    private boolean enabled = true;
    private DoubleConsumer onChange;

    private int heldDir = 0;
    private int heldTicks = 0;

    public DoubleStepperElement() {
        row.add(decButton);
        row.add(field);
        row.add(incButton);
    }

    public DoubleStepperElement value(double value) {
        this.value = clamp(value);
        field.setText(format(this.value));
        return this;
    }

    public DoubleStepperElement min(double min) {
        this.min = min;
        return this;
    }

    public DoubleStepperElement max(double max) {
        this.max = max;
        return this;
    }

    public DoubleStepperElement step(double step) {
        this.step = step;
        return this;
    }

    public DoubleStepperElement fieldWidth(int width) {
        field.width(width);
        return this;
    }

    public DoubleStepperElement gap(int gap) {
        row.gap(gap);
        return this;
    }

    public DoubleStepperElement focused(boolean focused) {
        field.focused(focused);
        return this;
    }

    public DoubleStepperElement repeatDelay(int ticks) {
        this.repeatDelay = ticks;
        return this;
    }

    public DoubleStepperElement repeatInterval(int ticks) {
        this.repeatInterval = ticks;
        return this;
    }

    public DoubleStepperElement doubleEvery(int ticks) {
        this.doubleEvery = ticks;
        return this;
    }

    public DoubleStepperElement enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public DoubleStepperElement onChange(DoubleConsumer onChange) {
        this.onChange = onChange;
        return this;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        value(value);
    }

    public void commit() {
        String text = field.getText();
        double parsed;
        try {
            parsed = Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            parsed = value;
        }
        parsed = clamp(parsed);
        if (parsed != value) {
            value = parsed;
            if (onChange != null) onChange.accept(value);
        }
        field.setText(format(value));
    }

    private double clamp(double v) {
        return Math.max(min, Math.min(max, v));
    }

    private static String format(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private void applyStep(int dir, int multiplier) {
        commit();
        double newValue = clamp(value + (double) dir * step * multiplier);
        if (newValue != value) {
            value = newValue;
            field.setText(format(value));
            if (onChange != null) onChange.accept(value);
        }
    }

    private boolean isInside(ViewElement<?> e, int mx, int my) {
        return mx >= e.getComputedX() && mx < e.getComputedX() + e.getComputedWidth()
                && my >= e.getComputedY() && my < e.getComputedY() + e.getComputedHeight();
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        row.setComputedPosition(computedX, computedY);
        row.setComputedSize(computedWidth, computedHeight);
        row.createWidgets(buttonList, fontRenderer, factory);
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        row.draw(fr, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !enabled) return false;
        if (isInside(incButton, mouseX, mouseY)) {
            heldDir = 1;
            heldTicks = 0;
            applyStep(1, 1);
            return true;
        }
        if (isInside(decButton, mouseX, mouseY)) {
            heldDir = -1;
            heldTicks = 0;
            applyStep(-1, 1);
            return true;
        }
        field.mouseClicked(mouseX, mouseY, mouseButton);
        return field.isFocused();
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        heldDir = 0;
        heldTicks = 0;
        return false;
    }

    @Override
    public void tick() {
        if (heldDir == 0) return;
        if (!Mouse.isButtonDown(0)) {
            heldDir = 0;
            heldTicks = 0;
            return;
        }
        heldTicks++;
        if (heldTicks >= repeatDelay && (heldTicks - repeatDelay) % repeatInterval == 0) {
            int multiplier = 1 << (heldTicks / doubleEvery);
            applyStep(heldDir, multiplier);
        }
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        if (field.isFocused() && (keyCode == 28 || keyCode == 156)) {
            commit();
            return true;
        }
        return row.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateCursorCounter() {
        row.updateCursorCounter();
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return row.getPreferredHeight();
    }

    @Override
    public int getPreferredWidth(FontRenderer fr) {
        return row.getPreferredWidth(fr);
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
        return row.getPreferredHeight(fr);
    }
}

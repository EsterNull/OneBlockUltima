package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.function.IntConsumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class StepperElement extends ViewElement<StepperElement> {
    private static final int DEC_BUTTON_ID = -9001;
    private static final int INC_BUTTON_ID = -9002;

    private final RowElement row = new RowElement(Alignment.RIGHT).gap(4);
    private final ButtonElement<?> decButton = new ButtonElement<>(DEC_BUTTON_ID, "-");
    private final ButtonElement<?> incButton = new ButtonElement<>(INC_BUTTON_ID, "+");
    private final TextFieldElement field = new TextFieldElement(60);

    private int value;
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    private int step = 1;
    private int repeatDelay = 20;
    private int repeatInterval = 5;
    private int doubleEvery = 60;
    private boolean enabled = true;
    private IntConsumer onChange;

    private int heldDir = 0;
    private int heldTicks = 0;

    public StepperElement() {
        field.enabled(false);
        row.add(decButton);
        row.add(field);
        row.add(incButton);
    }

    public StepperElement value(int value) {
        this.value = clamp(value);
        field.setText(String.valueOf(this.value));
        return this;
    }

    public StepperElement min(int min) {
        this.min = min;
        return this;
    }

    public StepperElement max(int max) {
        this.max = max;
        return this;
    }

    public StepperElement step(int step) {
        this.step = step;
        return this;
    }

    public StepperElement fieldWidth(int width) {
        field.width(width);
        return this;
    }

    public StepperElement gap(int gap) {
        row.gap(gap);
        return this;
    }

    public StepperElement repeatDelay(int ticks) {
        this.repeatDelay = ticks;
        return this;
    }

    public StepperElement repeatInterval(int ticks) {
        this.repeatInterval = ticks;
        return this;
    }

    public StepperElement doubleEvery(int ticks) {
        this.doubleEvery = ticks;
        return this;
    }

    public StepperElement enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public StepperElement onChange(IntConsumer onChange) {
        this.onChange = onChange;
        return this;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        value(value);
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    private void applyStep(int dir, int multiplier) {
        long target = Math.max(min, Math.min(max, (long) value + (long) dir * step * multiplier));
        int newValue = (int) target;
        if (newValue != value) {
            value = newValue;
            field.setText(String.valueOf(value));
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
        return false;
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

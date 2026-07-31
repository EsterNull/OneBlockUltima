package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

import static ru.defea.oneblockultima.Constants.SUCCESS_COLOR;

public class StatusBarElement extends ViewElement {
    private String text = "";
    private int textColor = SUCCESS_COLOR;
    private int timer = 0;

    public StatusBarElement text(String text, int ticks) {
        this.text = text;
        this.timer = ticks;
        return this;
    }

    public StatusBarElement textColor(int color) {
        this.textColor = color;
        return this;
    }

    public void tick() {
        if (timer > 0) timer--;
    }

    public void clear() {
        this.text = "";
        this.timer = 0;
    }

    public boolean isActive() {
        return timer > 0 && text != null && !text.isEmpty();
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (!isActive()) return;
        fr.drawStringWithShadow(text,
            computedX + (computedWidth - fr.getStringWidth(text)) / 2.0f,
            computedY, textColor);
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return 10;
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
        return fr.FONT_HEIGHT + 2;
    }
}

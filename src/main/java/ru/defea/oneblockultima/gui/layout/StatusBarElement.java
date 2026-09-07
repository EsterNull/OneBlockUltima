package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static ru.defea.oneblockultima.Constants.SUCCESS_COLOR;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class StatusBarElement extends ViewElement<StatusBarElement> {
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
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (!isActive()) return;
        g.drawString(font, text,
                computedX + (computedWidth - font.width(text)) / 2,
                computedY, textColor, true);
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return getPreferredHeight(Minecraft.getInstance().font);
    }

    @Override
    public int getPreferredHeight(Font font) {
        return font.lineHeight + 2;
    }
}

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TitleElement extends ViewElement<TitleElement> {
    private final String text;
    private int color = WHITE_COLOR_1;

    public TitleElement(String key, Object... args) {
        this.text = I18n.get(key, args);
    }

    public TitleElement color(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (text == null || text.isEmpty()) return;
        String t = text;
        int tx = computedX + (computedWidth - font.width(t)) / 2;
        g.drawString(font, t, tx, computedY, color);
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
        return font.lineHeight + 4;
    }
}

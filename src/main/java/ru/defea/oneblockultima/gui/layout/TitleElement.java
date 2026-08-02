package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import java.util.List;

import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TitleElement extends ViewElement<TitleElement> {
    private final String text;
    private int color = WHITE_COLOR_1;

    public TitleElement(String key, Object... args) {
        this.text = I18n.format(key, args);
    }

    public TitleElement color(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (text == null || text.isEmpty()) return;
        String t = text;
        int tx = computedX + (computedWidth - fr.getStringWidth(t)) / 2;
        int ty = computedY;
        fr.drawString(t, tx, ty, color);
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return 12;
    }

    @Override
    public int getPreferredHeight(FontRenderer fr) {
        return fr.FONT_HEIGHT + 4;
    }
}

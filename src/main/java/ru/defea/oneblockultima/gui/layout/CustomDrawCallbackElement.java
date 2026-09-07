package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CustomDrawCallbackElement extends ViewElement<CustomDrawCallbackElement> {
    public interface DrawCallback {
        void draw(GuiGraphics g, int x, int y, int width, int height, Font font, int mouseX, int mouseY, float partialTicks);
    }

    public interface SizeCallback {
        int getWidth();
        int getHeight();
    }

    private final DrawCallback drawCallback;
    private int fixedWidth;
    private int fixedHeight;
    private SizeCallback sizeCallback;

    public CustomDrawCallbackElement(DrawCallback drawCallback, int width, int height) {
        this.drawCallback = drawCallback;
        this.fixedWidth = width;
        this.fixedHeight = height;
    }

    public CustomDrawCallbackElement(DrawCallback drawCallback, SizeCallback sizeCallback) {
        this.drawCallback = drawCallback;
        this.sizeCallback = sizeCallback;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (drawCallback != null) {
            drawCallback.draw(g, computedX, computedY, computedWidth, computedHeight, font, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public int getPreferredWidth() {
        return sizeCallback != null ? sizeCallback.getWidth() : fixedWidth;
    }

    @Override
    public int getPreferredHeight() {
        return sizeCallback != null ? sizeCallback.getHeight() : fixedHeight;
    }
}

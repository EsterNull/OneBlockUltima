package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

public class CustomDrawCallbackElement extends ViewElement {
    public interface DrawCallback {
        void draw(int x, int y, int width, int height, FontRenderer fr, int mouseX, int mouseY, float partialTicks);
    }

    public interface SizeCallback {
        int getWidth();
        int getHeight();
    }

    private DrawCallback drawCallback;
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
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (drawCallback != null) {
            drawCallback.draw(computedX, computedY, computedWidth, computedHeight, fr, mouseX, mouseY, partialTicks);
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

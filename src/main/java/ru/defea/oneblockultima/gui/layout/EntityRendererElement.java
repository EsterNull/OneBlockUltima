package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.List;

import static ru.defea.oneblockultima.Constants.GRAY_COLOR_4;
import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class EntityRendererElement extends ViewElement<EntityRendererElement> {
    private static final int FIT_MARGIN = 1;

    private Entity entity;
    private int scale = 16;

    public EntityRendererElement(Entity entity) {
        this.entity = entity;
    }

    public EntityRendererElement scale(int scale) {
        this.scale = scale;
        return this;
    }

    public EntityRendererElement entity(Entity entity) {
        this.entity = entity;
        return this;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (entity == null || !(entity instanceof EntityLivingBase)) {
            Gui.drawRect(computedX, computedY, computedX + computedWidth, computedY + computedHeight, GRAY_COLOR_4);
            fr.drawString("M", computedX + 2, computedY + 2, WHITE_COLOR_1);
            return;
        }
        float[] units = ModelUtil.getModelUnits(entity);
        int fitW = Math.max(4, computedWidth - 2 * FIT_MARGIN);
        int fitH = Math.max(4, computedHeight - 2 * FIT_MARGIN);
        float finalScale = Math.min(fitW / units[0], fitH / units[1]);
        int ox = computedX + computedWidth / 2 - Math.round(units[2] * finalScale);
        int oy = computedY + computedHeight / 2 + Math.round(units[3] * finalScale);
        ModelUtil.drawEntityOnScreenScaled(ox, oy, entity, finalScale);
    }

    @Override
    public int getPreferredWidth() {
        return scale;
    }

    @Override
    public int getPreferredHeight() {
        return scale;
    }
}

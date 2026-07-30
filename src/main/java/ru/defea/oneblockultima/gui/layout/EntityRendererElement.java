package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.List;

public class EntityRendererElement extends ViewElement {
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
        if (entity == null || !(entity instanceof EntityLivingBase)) return;
        ModelUtil.drawEntityOnScreen(computedX + computedWidth / 2, computedY + computedHeight, entity, scale);
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

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.List;

import static ru.defea.oneblockultima.Constants.GRAY_COLOR_4;
import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class EntityRendererElement extends ViewElement<EntityRendererElement> {
    private static final int FIT_MARGIN = 1;

    private Entity entity;
    private EntityType<?> entityType;
    private int scale = 16;

    public EntityRendererElement(Entity entity) {
        this.entity = entity;
    }

    public EntityRendererElement(Entity entity, EntityType<?> entityType) {
        this.entity = entity;
        this.entityType = entityType;
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
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (!drawEntity(g)) {
            drawFallback(g, font);
        }
    }

    private boolean drawEntity(GuiGraphics g) {
        if (entity == null || !(entity instanceof LivingEntity)) {
            return false;
        }
        try {
            float[] fit = ModelUtil.computeScreenEntityFit(computedWidth, computedHeight, entity);
            float finalScale = fit[0];
            int ox = computedX + computedWidth / 2 - Math.round(fit[1] * finalScale);
            int oy = computedY + computedHeight / 2 + Math.round(fit[2] * finalScale);
            ModelUtil.drawEntityOnScreenScaled(g, ox, oy, entity, finalScale);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void drawFallback(GuiGraphics g, Font font) {
        g.fill(computedX, computedY, computedX + computedWidth, computedY + computedHeight, GRAY_COLOR_4);
        if (entityType != null)
        {
            Item egg = SpawnEggItem.byId(entityType);
            if (egg != null)
            {
                int eggX = computedX + (computedWidth - 16) / 2;
                int eggY = computedY + (computedHeight - 16) / 2;
                g.renderFakeItem(new ItemStack(egg), eggX, eggY);
                return;
            }
        }
        g.drawString(font, "M", computedX + 2, computedY + 2, WHITE_COLOR_1);
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

package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FluidElement extends ViewElement<FluidElement> {
    private Fluid fluid;
    private int width = 16;
    private int height = 16;

    public FluidElement(Fluid fluid) {
        this.fluid = fluid;
    }

    public static FluidElement fromName(String fluidName) {
        Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidName));
        if (f == null || f == net.minecraft.world.level.material.Fluids.EMPTY) return null;
        return new FluidElement(f);
    }

    public FluidElement size(int size) {
        this.width = size;
        this.height = size;
        return this;
    }

    public FluidElement size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public FluidElement fluid(Fluid fluid) {
        this.fluid = fluid;
        return this;
    }

    @Override
    public void createWidgets(Screen screen, Font font, ViewFactory factory) {
    }

    @Override
    public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (fluid == null) return;
        ModelUtil.renderFluidSprite(g, fluid, computedX, computedY, width, height);
    }

    @Override
    public int getPreferredWidth() {
        return width;
    }

    @Override
    public int getPreferredHeight() {
        return height;
    }
}

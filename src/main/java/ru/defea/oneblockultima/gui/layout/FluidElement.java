package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
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
        Fluid f = FluidRegistry.getFluid(fluidName);
        if (f == null) return null;
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
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        if (fluid == null) return;
        ModelUtil.renderFluidSprite(fluid, computedX, computedY, width, height);
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

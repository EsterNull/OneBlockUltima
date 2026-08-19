package ru.defea.oneblockultima.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.defea.oneblockultima.OneBlockUltima;

/**
 * Exception-safe item icon rendering. Some modded items (e.g. CoFH's
 * ItemFishingRodAdv) throw inside {@link ItemStack#writeToNBT} while their
 * icon is being resolved for a GUI, which would crash the whole screen. These
 * helpers guarantee the GL state is reset and a single broken icon is skipped.
 */
public final class RenderUtil
{
    private RenderUtil()
    {
    }

    public static void renderItemIntoGUI(FontRenderer fr, ItemStack stack, int x, int y)
    {
        if (stack == null || stack.stackSize <= 0)
        {
            return;
        }
        setupLighting();
        try
        {
            RenderItem.getInstance().renderItemIntoGUI(fr, Minecraft.getMinecraft().getTextureManager(), stack, x, y);
        }
        catch (Exception ex)
        {
            OneBlockUltima.getLogger().warn("[RenderUtil] Failed to render item icon for {}: {}", stack, ex);
        }
        finally
        {
            resetLighting();
        }
    }

    public static void renderItemAndEffectIntoGUI(FontRenderer fr, ItemStack stack, int x, int y)
    {
        if (stack == null || stack.stackSize <= 0)
        {
            return;
        }
        setupLighting();
        try
        {
            RenderItem.getInstance().renderItemAndEffectIntoGUI(fr, Minecraft.getMinecraft().getTextureManager(), stack, x, y);
            RenderItem.getInstance().renderItemOverlayIntoGUI(fr, Minecraft.getMinecraft().getTextureManager(), stack, x, y, null);
        }
        catch (Exception ex)
        {
            OneBlockUltima.getLogger().warn("[RenderUtil] Failed to render item icon for {}: {}", stack, ex);
        }
        finally
        {
            resetLighting();
        }
    }

    private static void setupLighting()
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

    private static void resetLighting()
    {
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }
}

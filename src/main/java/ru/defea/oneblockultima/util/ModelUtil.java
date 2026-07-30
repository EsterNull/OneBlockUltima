package ru.defea.oneblockultima.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.*;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public final class ModelUtil {
    private ModelUtil() {}

    private static World dummyWorld;

    public static World getWorldOrCreateDummy() {
        World w = Minecraft.getMinecraft().world;
        if (w != null) return w;
        if (dummyWorld == null) {
            try {
                GameProfile profile = new GameProfile(UUID.randomUUID(), "OBUDummy");
                NetworkManager nm = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
                NetHandlerPlayClient handler = new NetHandlerPlayClient(
                    Minecraft.getMinecraft(), null, nm, profile);
                dummyWorld = new WorldClient(handler,
                    new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.DEFAULT),
                    0, EnumDifficulty.PEACEFUL, new Profiler());
            } catch (Exception e) {
                System.err.println("[ModelUtil] Failed to create dummy world: " + e);
            }
        }
        return dummyWorld;
    }

    public static void renderBlockModelToGUI(net.minecraft.block.state.IBlockState state, int x, int y, int size)
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 100.0F);
            GlStateManager.scale(size / 16.0F, size / 16.0F, size / 16.0F);
            GlStateManager.rotate(180F, 1F, 0F, 0F);
            RenderHelper.enableGUIStandardItemLighting();
            blockRenderer.renderBlockBrightness(state, 1.0F);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
        catch (Exception ignored) { }
    }

    public static void renderFluidSprite(net.minecraftforge.fluids.Fluid fluid, int x, int y, int w, int h)
    {
        if (fluid == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        TextureAtlasSprite sprite = null;
        try
        {
            ResourceLocation tex = fluid.getStill();
            if (tex == null) tex = fluid.getFlowing();
            if (tex != null)
            {
                TextureMap map = mc.getTextureMapBlocks();
                sprite = map.getAtlasSprite(tex.toString());
            }
        }
        catch (Exception ignored) { }

        if (sprite == null) return;

        try
        {
            GlStateManager.pushMatrix();
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();

            int fluidColor = fluid.getColor();
            float r = ((fluidColor >> 16) & 0xFF) / 255.0F;
            float g = ((fluidColor >> 8) & 0xFF) / 255.0F;
            float b = (fluidColor & 0xFF) / 255.0F;

            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(r, g, b, 1.0F);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buf.pos(x, y + h, 0.0D).tex(minU, maxV).endVertex();
            buf.pos(x + w, y + h, 0.0D).tex(maxU, maxV).endVertex();
            buf.pos(x + w, y, 0.0D).tex(maxU, minV).endVertex();
            buf.pos(x, y, 0.0D).tex(minU, minV).endVertex();
            tess.draw();

            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
        catch (Exception ignored) { }
    }

    public static void drawEntityOnScreen(int posX, int posY, Entity entity, int baseScale)
    {
        if (!(entity instanceof EntityLivingBase)) return;
        EntityLivingBase ent = (EntityLivingBase) entity;

        float origRenderYawOffset = ent.renderYawOffset;
        float origPrevRotationYaw = ent.prevRotationYaw;
        float origRotationYaw = ent.rotationYaw;
        float origRotationYawHead = ent.rotationYawHead;
        float origPrevRotationYawHead = ent.prevRotationYawHead;
        float origRotationPitch = ent.rotationPitch;
        float origPrevRotationPitch = ent.prevRotationPitch;
        float origLimbSwing = ent.limbSwing;
        float origLimbSwingAmount = ent.limbSwingAmount;
        float origPrevLimbSwingAmount = ent.prevLimbSwingAmount;

        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        float prevPlayerViewY = renderManager.playerViewY;

        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        try
        {
            GlStateManager.translate(posX, posY, 50.0F);

            float finalScale = getScale(baseScale / 2, ent);

            GlStateManager.scale(-finalScale, finalScale, finalScale);
            GlStateManager.rotate(170.0F, 0.3F, 0.0F, 1.0F);

            ent.renderYawOffset = 0.0F;
            ent.prevRotationYaw = 0.0F;
            ent.rotationYaw = 0.0F;
            ent.rotationYawHead = 0.0F;
            ent.prevRotationYawHead = 0.0F;
            ent.rotationPitch = 0.0F;
            ent.prevRotationPitch = 0.0F;
            ent.limbSwing = 0.0F;
            ent.limbSwingAmount = 0.0F;
            ent.prevLimbSwingAmount = 0.0F;

            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            renderManager.setPlayerViewY(180.0F);
            renderManager.setRenderShadow(false);
            renderManager.renderEntity(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        }
        catch (Exception ignored) { }
        finally
        {
            renderManager.setRenderShadow(true);
            renderManager.setPlayerViewY(prevPlayerViewY);

            GlStateManager.disableCull();
            GlStateManager.disableDepth();
            GlStateManager.disableRescaleNormal();

            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.disableColorMaterial();
            GlStateManager.disableLighting();

            ent.renderYawOffset = origRenderYawOffset;
            ent.prevRotationYaw = origPrevRotationYaw;
            ent.rotationYaw = origRotationYaw;
            ent.rotationYawHead = origRotationYawHead;
            ent.prevRotationYawHead = origPrevRotationYawHead;
            ent.rotationPitch = origRotationPitch;
            ent.prevRotationPitch = origPrevRotationPitch;
            ent.limbSwing = origLimbSwing;
            ent.limbSwingAmount = origLimbSwingAmount;
            ent.prevLimbSwingAmount = origPrevLimbSwingAmount;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static float getScale(int scale, EntityLivingBase ent) {
        float heightScale = scale / ent.height;
        float widthScale = scale / ent.width;
        return Math.min(heightScale, widthScale);
    }
}

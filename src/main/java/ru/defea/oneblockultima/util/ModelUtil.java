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
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import ru.defea.oneblockultima.OneBlockUltima;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
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
                //noinspection DataFlowIssue
                NetHandlerPlayClient handler = new NetHandlerPlayClient(
                    Minecraft.getMinecraft(), null, nm, profile);
                dummyWorld = new WorldClient(handler,
                    new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.DEFAULT),
                    0, EnumDifficulty.PEACEFUL, new Profiler());
            } catch (Exception e) {
                OneBlockUltima.getLogger().error("[ModelUtil] Failed to create dummy world: {}", e.toString());
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

            net.minecraft.client.renderer.block.model.IBakedModel model = blockRenderer.getModelForState(state);
            if (model == mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel())
            {
                renderBlockTextureAsIcon(state, x + size / 2, y + size / 2, size);
                return;
            }

            GlStateManager.pushMatrix();
            try
            {
                GlStateManager.enableDepth();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.disableCull();
                GlStateManager.translate(x + size / 2.0F, y + size / 2.0F, 100.0F);
                GlStateManager.scale(size, size, size);
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                blockRenderer.renderBlockBrightness(state, 1.0F);
                GlStateManager.disableRescaleNormal();
                RenderHelper.disableStandardItemLighting();
                GlStateManager.enableCull();
                GlStateManager.disableBlend();
                GlStateManager.disableAlpha();
                GlStateManager.disableDepth();
            }
            catch (Exception ignored)
            {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.enableCull();
                GlStateManager.disableBlend();
                GlStateManager.disableAlpha();
                GlStateManager.disableDepth();
            }
            GlStateManager.popMatrix();
        }
        catch (Exception ignored) { }
    }

    private static void renderBlockTextureAsIcon(net.minecraft.block.state.IBlockState state, int x, int y, int size)
    {
        Minecraft mc = Minecraft.getMinecraft();
        TextureAtlasSprite sprite;
        try
        {
            sprite = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
        }
        catch (Exception ignored)
        {
            return;
        }
        if ("missingno".equals(sprite.getIconName()))
        {
            return;
        }

        float half = 8.0F * size / 16.0F;
        float left = x - half;
        float top = y - half;
        float right = x + half;
        float bottom = y + half;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 100.0F);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(left, bottom, 0.0D).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        buf.pos(right, bottom, 0.0D).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        buf.pos(right, top, 0.0D).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        buf.pos(left, top, 0.0D).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        tess.draw();

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
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

    public static int getBlockSpriteTint(net.minecraft.block.state.IBlockState state, TextureAtlasSprite sprite)
    {
        if (state == null || sprite == null) return 0xFFFFFFFF;
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            net.minecraft.client.renderer.block.model.IBakedModel model =
                mc.getBlockRendererDispatcher().getModelForState(state);

            int tintIndex = -1;
            for (net.minecraft.client.renderer.block.model.BakedQuad quad : model.getQuads(state, null, 0L))
            {
                if (quad.getSprite() == sprite)
                {
                    tintIndex = quad.getTintIndex();
                    break;
                }
            }
            if (tintIndex < 0) return 0xFFFFFFFF;

            net.minecraft.util.math.BlockPos pos = mc.player != null ? mc.player.getPosition() : null;
            int color = mc.getBlockColors().colorMultiplier(state, mc.world, pos, tintIndex);
            if ((color & 0xFF000000) == 0) color |= 0xFF000000;
            return color;
        }
        catch (Exception ignored)
        {
            return 0xFFFFFFFF;
        }
    }

    public static void drawEntityOnScreenScaled(int posX, int posY, Entity entity, float finalScale)
    {
        renderEntityOnScreen(posX, posY, entity, finalScale);
    }

    private static void renderEntityOnScreen(int posX, int posY, Entity entity, float finalScale)
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

    private static final int MEASURE_SIZE = 256;
    private static final float[] MEASURE_SCALES = { 10.0F, 5.0F, 2.5F, 1.25F };
    private static final Map<Class<? extends Entity>, float[]> measuredModelCache = new HashMap<>();

    public static float[] getModelUnits(Entity entity)
    {
        if (!(entity instanceof EntityLivingBase))
        {
            return new float[] { 0.0F, 0.0F, 0.0F, 0.0F };
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        float[] units = measureModelUnits(living);
        if (units[0] <= 0.0F || units[1] <= 0.0F)
        {
            return new float[] { living.width, living.height, 0.0F, 0.0F };
        }
        return units;
    }

    private static float[] measureModelUnits(EntityLivingBase entity)
    {
        float[] cached = measuredModelCache.get(entity.getClass());
        if (cached != null)
        {
            return cached;
        }
        float[] result = measureModelUnitsNow(entity);
        measuredModelCache.put(entity.getClass(), result);
        OneBlockUltima.getLogger().info("[ModelUtil] Measured units for {}: {}x{} (hitbox {}x{})",
            entity.getClass().getSimpleName(), result[0], result[1],
            entity.width, entity.height);
        return result;
    }

    private static float[] measureModelUnitsNow(EntityLivingBase entity)
    {
        int[] bounds = null;
        float usedScale = MEASURE_SCALES[0];
        for (float scale : MEASURE_SCALES)
        {
            usedScale = scale;
            bounds = measurePixels(entity, scale);
            if (bounds != null && !touchesEdge(bounds))
            {
                break;
            }
        }
        if (bounds == null)
        {
            return new float[] { 0.0F, 0.0F, 0.0F, 0.0F };
        }
        float width = (bounds[2] - bounds[0] + 1) / usedScale;
        float height = (bounds[3] - bounds[1] + 1) / usedScale;
        float centerX = (bounds[0] + bounds[2]) / 2.0F;
        float centerY = (bounds[1] + bounds[3]) / 2.0F;
        float offsetX = (centerX - MEASURE_SIZE / 2.0F) / usedScale;
        float offsetY = (centerY - MEASURE_SIZE / 2.0F) / usedScale;
        return new float[] { width, height, offsetX, offsetY };
    }

    private static boolean touchesEdge(int[] bounds)
    {
        return bounds[0] <= 1 || bounds[1] <= 1
            || bounds[2] >= MEASURE_SIZE - 2 || bounds[3] >= MEASURE_SIZE - 2;
    }

    private static int[] measurePixels(EntityLivingBase entity, float scale)
    {
        Framebuffer fb = new Framebuffer(MEASURE_SIZE, MEASURE_SIZE, true);
        try
        {
            fb.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            fb.bindFramebuffer(true);
            GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager.clearDepth(1.0D);
            GlStateManager.clear(16384 | 256);

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, MEASURE_SIZE, MEASURE_SIZE, 0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);

            renderEntityOnScreen(MEASURE_SIZE / 2, MEASURE_SIZE / 2, entity, scale);

            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();

            ByteBuffer pixels = BufferUtils.createByteBuffer(MEASURE_SIZE * MEASURE_SIZE * 4);
            GL11.glReadPixels(0, 0, MEASURE_SIZE, MEASURE_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

            byte[] arr = new byte[MEASURE_SIZE * MEASURE_SIZE * 4];
            pixels.get(arr);
            int minX = MEASURE_SIZE, minY = MEASURE_SIZE, maxX = -1, maxY = -1;
            for (int i = 0; i < MEASURE_SIZE * MEASURE_SIZE; i++)
            {
                int alpha = arr[i * 4 + 3] & 0xFF;
                if (alpha > 16)
                {
                    int px = i % MEASURE_SIZE;
                    int py = i / MEASURE_SIZE;
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
            }
            if (maxX < 0)
            {
                return null;
            }
            return new int[] { minX, minY, maxX, maxY };
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[ModelUtil] measurePixels failed for {}: {}",
                entity.getClass().getSimpleName(), e.toString());
            return null;
        }
        finally
        {
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
            fb.deleteFramebuffer();
            GlStateManager.viewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        }
    }
}

package ru.defea.oneblockultima.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import ru.defea.oneblockultima.OneBlockUltima;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class ModelUtil {
    private ModelUtil() {}

    private static World dummyWorld;

    public static World getWorldOrCreateDummy() {
        World w = Minecraft.getMinecraft().theWorld;
        if (w != null) return w;
        if (dummyWorld == null) {
            dummyWorld = createDummyWorld();
            OneBlockUltima.getRawLogger().info("[ModelUtil] Dummy world created: {}",
                dummyWorld == null ? "null" : dummyWorld.getClass().getSimpleName());
        }
        return dummyWorld;
    }

    private static World createDummyWorld() {
        try {
            return new MinimalDummyWorld();
        } catch (Throwable t) {
            OneBlockUltima.getRawLogger().error("[ModelUtil] Failed to create minimal dummy world", t);
        }
        try {
            NetworkManager nm = new NetworkManager(true);
            NetHandlerPlayClient handler = new NetHandlerPlayClient(
                Minecraft.getMinecraft(), null, nm);
            return new WorldClient(handler,
                new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                0, EnumDifficulty.PEACEFUL, new Profiler());
        } catch (Throwable t) {
            OneBlockUltima.getRawLogger().error("[ModelUtil] Failed to create WorldClient dummy world", t);
        }
        return null;
    }

    private static final class MinimalDummyWorld extends World {
        private MinimalDummyWorld() {
            super(null, "Dummy",
                WorldProvider.getProviderForDimension(0),
                new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                new Profiler());
            this.isRemote = true;
            this.difficultySetting = EnumDifficulty.PEACEFUL;
            this.mapStorage = new MapStorage(null);
            this.provider.registerWorld(this);
            this.chunkProvider = new ChunkProviderClient(this);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return chunkProvider;
        }

        @Override
        protected int getRenderDistanceChunks() {
            return 2;
        }

        @Override
        public Entity getEntityByID(int id) {
            return null;
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
            return (15 << 20) | (15 << 4);
        }

        @Override
        public float getLightBrightness(int x, int y, int z) {
            return 1.0F;
        }
    }

    public static void renderFluidSprite(net.minecraftforge.fluids.Fluid fluid, int x, int y, int w, int h)
    {
        if (fluid == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.util.IIcon icon = null;
        try
        {
            icon = fluid.getStillIcon();
            if (icon == null) icon = fluid.getFlowingIcon();
        }
        catch (Exception ignored) { }

        if (icon == null) return;

        try
        {
            GL11.glPushMatrix();
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            float minU = icon.getMinU();
            float maxU = icon.getMaxU();
            float minV = icon.getMinV();
            float maxV = icon.getMaxV();

            int fluidColor = fluid.getColor();
            float r = ((fluidColor >> 16) & 0xFF) / 255.0F;
            float g = ((fluidColor >> 8) & 0xFF) / 255.0F;
            float b = (fluidColor & 0xFF) / 255.0F;

            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(r, g, b, 1.0F);

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x, y + h, 0.0D, minU, maxV);
            tess.addVertexWithUV(x + w, y + h, 0.0D, maxU, maxV);
            tess.addVertexWithUV(x + w, y, 0.0D, maxU, minV);
            tess.addVertexWithUV(x, y, 0.0D, minU, minV);
            tess.draw();

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            RenderHelper.disableStandardItemLighting();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
        catch (Exception ignored) { }
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

        RenderManager renderManager = RenderManager.instance;
        float prevPlayerViewY = renderManager.playerViewY;

        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        try
        {
            GL11.glTranslatef(posX, posY, 50.0F);

            GL11.glScalef(-finalScale, finalScale, finalScale);
            GL11.glRotatef(170.0F, 0.3F, 0.0F, 1.0F);

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
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            if (renderManager.renderEngine == null)
            {
                Minecraft mc = Minecraft.getMinecraft();
                renderManager.cacheActiveRenderInfo(
                    ent.worldObj, mc.getTextureManager(), mc.fontRendererObj,
                    ent, null, mc.gameSettings, 1.0F);
            }

            renderManager.playerViewY = 180.0F;
            renderManager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        }
        catch (Exception e)
        {
            OneBlockUltima.getRawLogger().error("[ModelUtil] renderEntityOnScreen failed for {}",
                ent.getClass().getSimpleName(), e);
        }
        finally
        {
            renderManager.playerViewY = prevPlayerViewY;

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            GL11.glDisable(GL11.GL_CULL_FACE);

            GL11.glPopMatrix();
            RenderHelper.disableStandardItemLighting();
            GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            GL11.glDisable(GL11.GL_LIGHTING);

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
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
            return new float[] { living.width, living.height, 0.0F, living.height / 2.0F };
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
        OneBlockUltima.getRawLogger().info("[ModelUtil] Measured units for {}: {}x{} (hitbox {}x{})",
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
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClearDepth(1.0D);
            GL11.glClear(16384 | 256);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, MEASURE_SIZE, MEASURE_SIZE, 0.0D, 1000.0D, 3000.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

            renderEntityOnScreen(MEASURE_SIZE / 2, MEASURE_SIZE / 2, entity, scale);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();

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
                OneBlockUltima.getRawLogger().warn("[ModelUtil] measurePixels: no opaque pixels for {} at scale {}",
                    entity.getClass().getSimpleName(), scale);
                return null;
            }
            return new int[] { minX, minY, maxX, maxY };
        }
        catch (Exception e)
        {
            OneBlockUltima.getRawLogger().error("[ModelUtil] measurePixels failed for {}: {}",
                entity.getClass().getSimpleName(), e.toString());
            return null;
        }
        finally
        {
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
            fb.deleteFramebuffer();
            GL11.glViewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        }
    }
}

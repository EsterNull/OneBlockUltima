package ru.defea.oneblockultima.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.defea.oneblockultima.OneBlockUltima;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

public final class ModelUtil
{
    private ModelUtil()
    {
    }

    public static void renderBlockModelToGUI(GuiGraphics guiGraphics, BlockState state, int x, int y, int size)
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            ItemStack stack = new ItemStack(state.getBlock());
            if (mc.getItemRenderer().getModel(stack, null, null, 0).isGui3d() || true)
            {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(x + size / 2.0F, y + size / 2.0F, 100.0F);
                pose.scale(size / 16.0F, size / 16.0F, size / 16.0F);
                guiGraphics.renderItem(stack, -8, -8);
                pose.popPose();
            }
        }
        catch (Exception ignored)
        {
        }
    }

    public static void renderFluidSprite(GuiGraphics guiGraphics, Fluid fluid, int x, int y, int w, int h)
    {
        if (fluid == null)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        TextureAtlasSprite sprite = null;
        int tint = 0xFF3F76E4;
        try
        {
            var fluidExt = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
            ResourceLocation tex = fluidExt.getStillTexture();
            if (tex == null) tex = fluidExt.getFlowingTexture();
            if (tex != null) sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(tex);
            int c = fluidExt.getTintColor();
            if (c != 0) tint = c | 0xFF000000;
        }
        catch (Exception ignored)
        {
        }

        if (sprite == null || sprite.getU0() == sprite.getU1() || sprite.getV0() == sprite.getV1())
        {
            int r = (tint >> 16) & 0xFF;
            int gg = (tint >> 8) & 0xFF;
            int b = tint & 0xFF;
            guiGraphics.fill(x, y, x + w, y + h, tint);
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x66000000 | (r << 16) | (gg << 8) | b);
            return;
        }

        try
        {
            float r = ((tint >> 16) & 0xFF) / 255.0F;
            float gg = ((tint >> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f pose = guiGraphics.pose().last().pose();
            float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
            buf.addVertex(pose, x, y + h, 0.0F).setUv(u0, v1).setColor(r, gg, b, 1.0F);
            buf.addVertex(pose, x + w, y + h, 0.0F).setUv(u1, v1).setColor(r, gg, b, 1.0F);
            buf.addVertex(pose, x + w, y, 0.0F).setUv(u1, v0).setColor(r, gg, b, 1.0F);
            buf.addVertex(pose, x, y, 0.0F).setUv(u0, v0).setColor(r, gg, b, 1.0F);
            BufferUploader.drawWithShader(buf.build());
            guiGraphics.fill(x, y, x + w, y + 1, 0x44000000);
            guiGraphics.fill(x, y + h - (h / 4), x + w, y + h, 0x33000000);
        }
        catch (Exception ignored)
        {
        }
    }

    public static int getBlockSpriteTint(BlockState state, TextureAtlasSprite sprite)
    {
        if (state == null || sprite == null)
        {
            return 0xFFFFFFFF;
        }
        try
        {
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
            var model = blockRenderer.getBlockModel(state);
            int tintIndex = -1;
            for (var quad : model.getQuads(state, null, net.minecraft.util.RandomSource.create(0L)))
            {
                if (quad.getSprite() == sprite)
                {
                    tintIndex = quad.getTintIndex();
                    break;
                }
            }
            if (tintIndex < 0)
            {
                return 0xFFFFFFFF;
            }
            net.minecraft.world.level.Level level = mc.level;
            int color = mc.getBlockColors().getColor(state, level, BlockPos.ZERO, tintIndex);
            if ((color & 0xFF000000) == 0)
            {
                color |= 0xFF000000;
            }
            return color;
        }
        catch (Exception ignored)
        {
            return 0xFFFFFFFF;
        }
    }

    public static void drawEntityOnScreenScaled(GuiGraphics guiGraphics, int posX, int posY, Entity entity, float finalScale)
    {
        renderEntityOnScreen(guiGraphics, posX, posY, entity, finalScale);
    }

    private static void renderEntityOnScreen(GuiGraphics guiGraphics, int posX, int posY, Entity entity, float finalScale)
    {
        if (!(entity instanceof LivingEntity))
        {
            return;
        }
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        try
        {
            pose.last().pose().identity();
            renderEntityApply(pose, posX, posY, entity, finalScale);
        }
        finally
        {
            pose.popPose();
        }
    }

    private static void renderEntityRaw(int posX, int posY, LivingEntity entity, float scale)
    {
        try
        {
            PoseStack pose = new PoseStack();
            pose.last().pose().set(RenderSystem.getModelViewMatrix());
            renderEntityApply(pose, posX, posY, entity, scale);
        }
        catch (Exception ignored)
        {
        }
    }

    private static void renderEntityApply(PoseStack pose, int posX, int posY, Entity entity, float finalScale)
    {
        if (!(entity instanceof LivingEntity))
        {
            return;
        }
        LivingEntity ent = (LivingEntity) entity;

        float origRenderYawOffset = ent.yRotO;
        float origRotationYaw = ent.getYRot();
        float origRotationYawHead = ent.getYHeadRot();
        float origRotationPitch = ent.getXRot();
        float origPrevRotationYawHead = ent.yHeadRotO;

        Minecraft mc = Minecraft.getInstance();
        var renderManager = mc.getEntityRenderDispatcher();

        pose.pushPose();
        boolean depthEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        try
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            pose.translate(posX, posY, 50.0F);
            pose.scale(-finalScale, finalScale, finalScale);
            pose.mulPose(new org.joml.Quaternionf().rotateAxis((float) Math.toRadians(170.0F), new org.joml.Vector3f(0.3F, 0.0F, 1.0F).normalize()));

            ent.setYRot(0.0F);
            ent.setYHeadRot(0.0F);
            ent.setXRot(0.0F);
            ent.yRotO = 0.0F;
            ent.xRotO = 0.0F;
            ent.yHeadRotO = 0.0F;

            renderManager.setRenderShadow(false);

            renderManager.render(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, pose,
                    mc.renderBuffers().bufferSource(), 0xF000F0);
            mc.renderBuffers().bufferSource().endBatch();
        }
        catch (Exception ex)
        {
            OneBlockUltima.logDebugWarn("[ModelUtil] GUI entity draw failed for {}: {}", entity.getClass().getSimpleName(), ex.toString());
            throw new RuntimeException("entity render failed", ex);
        }
        finally
        {
            renderManager.setRenderShadow(true);
            pose.popPose();

            if (!depthEnabled)
            {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            }

            ent.setYRot(origRotationYaw);
            ent.setYHeadRot(origRotationYawHead);
            ent.setXRot(origRotationPitch);
            ent.yRotO = origRenderYawOffset;
            ent.yHeadRotO = origPrevRotationYawHead;
        }
    }

    public static float[] getModelUnits(Entity entity)
    {
        if (!(entity instanceof LivingEntity))
        {
            return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
        }
        LivingEntity living = (LivingEntity) entity;
        float[] cached = measuredModelCache.get(entity.getClass());
        if (cached != null)
        {
            return cached;
        }
        float[] units = measureModelUnity(living);
        measuredModelCache.put(entity.getClass(), units);
        return units;
    }

    private static final Map<Class<? extends Entity>, float[]> measuredModelCache = new HashMap<>();

    private static float[] measureModelUnity(LivingEntity living)
    {
        float bw = living.getBbWidth();
        float bh = living.getBbHeight();

        float[] px = measurePixelsCascade(living);
        if (px != null && px[0] > 0.5F && px[1] > 0.5F)
        {
            OneBlockUltima.logDebug("[ModelUtil] units for {}: {}x{} offX={} offY={} (pixels) hitbox {}x{}",
                    living.getClass().getSimpleName(), px[0], px[1], px[2], px[3], bw, bh);
            return px;
        }

        float[] posed = measureModelUnitsNow(living);
        if (posed != null && posed[0] > 0.01F && posed[1] > 0.01F)
        {
            OneBlockUltima.logDebug("[ModelUtil] units for {}: {}x{} offX={} offY={} (posed) hitbox {}x{}",
                    living.getClass().getSimpleName(), posed[0], posed[1], posed[2], posed[3], bw, bh);
            return posed;
        }

        float[] geo = geometryUnits(living);
        float[] units;
        if (geo != null && geo[0] > 0.01F && geo[1] > 0.01F)
        {
            units = geo;
            OneBlockUltima.logDebug("[ModelUtil] units for {}: {}x{} offX={} offY={} (geometry fallback, hitbox {}x{})",
                    living.getClass().getSimpleName(), geo[0], geo[1], geo[2], geo[3], bw, bh);
        }
        else
        {
            units = new float[]{bw * 16.0F, bh * 16.0F, 0.0F, bh * 8.0F};
            OneBlockUltima.logDebug("[ModelUtil] units for {}: {}x{} offX={} offY={} (hitbox fallback)",
                    living.getClass().getSimpleName(), bw * 16.0F, bh * 16.0F, 0.0F, bh * 8.0F);
        }
        return units;
    }

    private static float[] measurePixelsCascade(LivingEntity living)
    {
        int[] bestBounds = null;
        float bestScale = 0.0F;
        for (float scale : MEASURE_SCALES)
        {
            int[] bounds = measurePixels(living, scale);
            if (bounds == null)
            {
                continue;
            }
            OneBlockUltima.logDebug("[ModelUtil] px {} scale={} bounds=[{},{},{},{}]",
                    living.getClass().getSimpleName(), scale, bounds[0], bounds[1], bounds[2], bounds[3]);
            if (touchesEdge(bounds))
            {
                break;
            }
            bestBounds = bounds;
            bestScale = scale;
        }
        if (bestBounds == null)
        {
            OneBlockUltima.logDebug("[ModelUtil] pixel measure gave no usable bounds for {}",
                    living.getClass().getSimpleName());
            return null;
        }
        float width = (bestBounds[2] - bestBounds[0] + 1) / bestScale;
        float height = (bestBounds[3] - bestBounds[1] + 1) / bestScale;
        float centerX = (bestBounds[0] + bestBounds[2]) / 2.0F;
        float centerY = (bestBounds[1] + bestBounds[3]) / 2.0F;
        float offsetX = (centerX - MEASURE_SIZE / 2.0F) / bestScale;
        float offsetY = (centerY - MEASURE_SIZE / 2.0F) / bestScale;
        return new float[]{width, height, offsetX, offsetY};
    }

    private static final int MEASURE_SIZE = 192;
    private static final float[] MEASURE_SCALES = {1.0F, 2.0F, 3.0F, 4.0F, 6.0F, 8.0F, 10.0F, 12.0F, 14.0F, 18.0F, 24.0F};

    private static float[] measureModelUnitsNow(LivingEntity living)
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            EntityRenderer<?> r = mc.getEntityRenderDispatcher().getRenderer(living);
            if (!(r instanceof LivingEntityRenderer<?, ?> ler))
            {
                return null;
            }
            float[] rs = renderScaleFactor(ler, living);
            if (rs[0] <= 0.0F) rs[0] = 1.0F;
            if (rs[1] <= 0.0F) rs[1] = 1.0F;
            if (rs[2] <= 0.0F) rs[2] = 1.0F;

            Model model = ler.getModel();
            @SuppressWarnings("rawtypes")
            EntityModel em = (EntityModel) model;
            em.attackTime = 0.0F;
            em.riding = false;
            em.young = living.isBaby();
            em.prepareMobModel(living, 0.0F, 0.0F, 1.0F);
            em.setupAnim(living, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

            List<ModelPart> roots = collectModelPartRoots(model);
            final float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
            final float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

            PoseStack m = new PoseStack();
            m.translate(0.0F, 0.0F, 50.0F);
            m.scale(-1.0F, 1.0F, 1.0F);
            m.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(170.0F), new Vector3f(0.3F, 0.0F, 1.0F).normalize()));
            m.scale(rs[0], rs[1], rs[2]);

            for (ModelPart root : roots)
            {
                visitAll(m, root, min, max);
            }

            if (min[0] == Float.MAX_VALUE)
            {
                OneBlockUltima.logDebug("[ModelUtil] posed {}: no cubes found", living.getClass().getSimpleName());
                return null;
            }
            float w = max[0] - min[0];
            float h = max[1] - min[1];
            float centerX = (min[0] + max[0]) / 2.0F;
            float centerY = (min[1] + max[1]) / 2.0F;
            OneBlockUltima.logDebug("[ModelUtil] posed {}: {}x{} center=({},{}) hitbox {}x{}",
                    living.getClass().getSimpleName(), w, h, centerX, centerY,
                    living.getBbWidth(), living.getBbHeight());
            return new float[]{w, h, centerX, centerY};        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[ModelUtil] posed failed for {}: {}", living.getClass().getSimpleName(), e.toString());
            return null;
        }
    }

    private static boolean touchesEdge(int[] b)
    {
        return b == null || b[0] <= 1 || b[1] <= 1 || b[2] >= MEASURE_SIZE - 2 || b[3] >= MEASURE_SIZE - 2;
    }

    private static int[] measurePixels(LivingEntity entity, float scale)
    {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget target = new TextureTarget(MEASURE_SIZE, MEASURE_SIZE, true, Minecraft.ON_OSX);
        try
        {
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.viewport(0, 0, MEASURE_SIZE, MEASURE_SIZE);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(16384 | 256, Minecraft.ON_OSX);

            Matrix4fStack mvStack = RenderSystem.getModelViewStack();
            mvStack.pushMatrix();
            mvStack.identity();
            RenderSystem.applyModelViewMatrix();
            Matrix4f prevProj = new Matrix4f(RenderSystem.getProjectionMatrix());
            VertexSorting prevSort = RenderSystem.getVertexSorting();
            try
            {
                RenderSystem.setProjectionMatrix(
                        new Matrix4f().setOrtho(0.0F, MEASURE_SIZE, MEASURE_SIZE, 0.0F, -3000.0F, 3000.0F),
                        VertexSorting.DISTANCE_TO_ORIGIN);
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                renderEntityApplyNoGui(entity, scale);

                return readBounds();
            }
            finally
            {
                RenderSystem.setProjectionMatrix(prevProj, prevSort);
                mvStack.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[ModelUtil] measurePixels failed for {}: {}",
                    entity.getClass().getSimpleName(), e.toString());
            return null;
        }
        finally
        {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
            target.destroyBuffers();
        }
    }

    private static int[] readBounds()
    {
        int size = MEASURE_SIZE;
        ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(size * size * 4);
        org.lwjgl.opengl.GL11.glReadPixels(0, 0, size, size, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        int minX = size, minY = size, maxX = -1, maxY = -1;
        int maxAlpha = 0;
        for (int i = 0; i < size * size; i++)
        {
            int alpha = pixels.get(i * 4 + 3) & 0xFF;
            if (alpha > maxAlpha) maxAlpha = alpha;
            if (alpha > 12)
            {
                int px = i % size;
                int py = i / size;
                if (px < minX) minX = px;
                if (px > maxX) maxX = px;
                if (py < minY) minY = py;
                if (py > maxY) maxY = py;
            }
        }
        if (maxX < 0)
        {
            OneBlockUltima.logDebug("[ModelUtil] readBounds: no opaque pixels, maxAlpha={}", maxAlpha);
            return null;
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    private static void renderEntityApplyNoGui(LivingEntity entity, float finalScale)
    {
        PoseStack pose = new PoseStack();
        float origRenderYawOffset = entity.yRotO;
        float origRotationYaw = entity.getYRot();
        float origRotationYawHead = entity.getYHeadRot();
        float origRotationPitch = entity.getXRot();
        float origPrevRotationYawHead = entity.yHeadRotO;

        Minecraft mc = Minecraft.getInstance();
        var renderManager = mc.getEntityRenderDispatcher();

        pose.pushPose();
        try
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            pose.translate(MEASURE_SIZE / 2.0F, MEASURE_SIZE / 2.0F, 50.0F);
            pose.scale(-finalScale, finalScale, finalScale);
            pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(170.0F), new Vector3f(0.3F, 0.0F, 1.0F).normalize()));

            entity.setYRot(0.0F);
            entity.setYHeadRot(0.0F);
            entity.setXRot(0.0F);
            entity.yRotO = 0.0F;
            entity.xRotO = 0.0F;
            entity.yHeadRotO = 0.0F;

            renderManager.setRenderShadow(false);
            mc.renderBuffers().bufferSource().endBatch();
            renderManager.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, pose,
                    mc.renderBuffers().bufferSource(), 0xF000F0);
            mc.renderBuffers().bufferSource().endBatch();
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            entity.setYRot(origRotationYaw);
            entity.setYHeadRot(origRotationYawHead);
            entity.setXRot(origRotationPitch);
            entity.yRotO = origRenderYawOffset;
            entity.yHeadRotO = origPrevRotationYawHead;
            pose.popPose();
        }
    }

    private static float[] geometryUnits(LivingEntity living)
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            EntityRenderer<?> r = mc.getEntityRenderDispatcher().getRenderer(living);
            if (r instanceof LivingEntityRenderer<?, ?> ler)
            {
                Model model = ler.getModel();
                final List<ModelPart> roots = collectModelPartRoots(model);
                final float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
                final float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
                PoseStack stack = new PoseStack();
                for (ModelPart root : roots)
                {
                    visitAll(stack, root, min, max);
                }
                if (min[0] != Float.MAX_VALUE)
                {
                    float[] rs = renderScaleFactor(ler, living);
                    float w = (max[0] - min[0]) * rs[0];
                    float h = (max[1] - min[1]) * rs[1];
                    float offX = (min[0] + max[0]) / 2.0F;
                    float offY = (min[1] + max[1]) / 2.0F;
                    OneBlockUltima.logDebug("[ModelUtil] geometry {}: raw {}x{} rendererScale {}x{} paths={}",
                            living.getClass().getSimpleName(), max[0] - min[0], max[1] - min[1], rs[0], rs[1], paths);
                    return new float[]{w, h, offX, offY};
                }
                else
                {
                    OneBlockUltima.logDebug("[ModelUtil] geometry {}: no cubes found (model {})",
                            living.getClass().getSimpleName(), model.getClass().getSimpleName());
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private static float[] renderScaleFactor(LivingEntityRenderer<?, ?> renderer, Entity entity)
    {
        java.lang.reflect.Method m = findScaleMethod(renderer.getClass(), entity);
        if (m == null)
        {
            return new float[]{1.0F, 1.0F, 1.0F};
        }
        try
        {
            PoseStack s = new PoseStack();
            m.setAccessible(true);
            m.invoke(renderer, entity, s, 1.0F);
            Matrix4f mat = new Matrix4f(s.last().pose());
            org.joml.Vector4f b = new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
            mat.transform(b);
            org.joml.Vector4f x = new org.joml.Vector4f(1.0F, 0.0F, 0.0F, 1.0F);
            mat.transform(x);
            org.joml.Vector4f y = new org.joml.Vector4f(0.0F, 1.0F, 0.0F, 1.0F);
            mat.transform(y);
            float sx = Math.max(0.0001F, Math.abs(x.distance(b)));
            float sy = Math.max(0.0001F, Math.abs(y.distance(b)));
            float sz = Math.max(0.0001F, sx);
            return new float[]{sx, sy, sz};
        }
        catch (Exception ex)
        {
            OneBlockUltima.logDebug("[ModelUtil] renderScale failed for {}: {}", renderer.getClass().getSimpleName(), ex.toString());
            return new float[]{1.0F, 1.0F, 1.0F};
        }
    }

    private static java.lang.reflect.Method findScaleMethod(Class<?> rendererClass, Entity entity)
    {
        Class<?> cls = rendererClass;
        while (cls != null && cls != Object.class)
        {
            for (java.lang.reflect.Method m : cls.getDeclaredMethods())
            {
                if (!m.getName().equals("scale")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0].isInstance(entity) && PoseStack.class.isAssignableFrom(p[1]) && p[2] == float.class)
                {
                    return m;
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static int paths;

    private static List<ModelPart> collectModelPartRoots(Model model)
    {
        List<ModelPart> roots = new ArrayList<>();
        paths = 0;
        try
        {
            if (model instanceof HierarchicalModel<?> hier)
            {
                ModelPart root = hier.root();
                if (root != null)
                {
                    roots.add(root);
                    return roots;
                }
            }
        }
        catch (Exception ignored)
        {
        }
        Class<?> cls = model.getClass();
        while (cls != null && cls != Object.class)
        {
            for (java.lang.reflect.Field f : cls.getDeclaredFields())
            {
                if (!ModelPart.class.isAssignableFrom(f.getType())) continue;
                try
                {
                    f.setAccessible(true);
                    if (f.get(model) instanceof ModelPart part)
                    {
                        roots.add(part);
                    }
                }
                catch (IllegalAccessException ignored)
                {
                }
            }
            cls = cls.getSuperclass();
        }
        return roots;
    }

    private static void visitAll(PoseStack stack, ModelPart part, float[] min, float[] max)
    {
        try
        {
            stack.pushPose();
            part.translateAndRotate(stack);
            for (ModelPart.Cube cube : cubesOf(part))
            {
                paths++;
                Matrix4f m = new Matrix4f(stack.last().pose());
                addCubeBounds(m, cube, min, max);
            }
            Map<String, ModelPart> children = childrenOf(part);
            if (children != null)
            {
                for (ModelPart child : children.values())
                {
                    visitAll(stack, child, min, max);
                }
            }
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            stack.popPose();
        }
    }

    private static final java.lang.reflect.Field CHILDREN_FIELD;

    static
    {
        Field c = null;
        try
        {
            c = ModelPart.class.getDeclaredField("children");
            c.setAccessible(true);
        }
        catch (Exception ignored)
        {
        }
        CHILDREN_FIELD = c;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ModelPart> childrenOf(ModelPart part)
    {
        if (CHILDREN_FIELD == null)
        {
            return null;
        }
        try
        {
            return (Map<String, ModelPart>) CHILDREN_FIELD.get(part);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static final java.lang.reflect.Field CUBES_FIELD;

    static
    {
        Field f = null;
        try
        {
            f = ModelPart.class.getDeclaredField("cubes");
            f.setAccessible(true);
        }
        catch (Exception ignored)
        {
        }
        CUBES_FIELD = f;
    }

    @SuppressWarnings("unchecked")
    private static List<ModelPart.Cube> cubesOf(ModelPart part)
    {
        if (CUBES_FIELD == null)
        {
            return new ArrayList<>();
        }
        try
        {
            return (List<ModelPart.Cube>) CUBES_FIELD.get(part);
        }
        catch (Exception ex)
        {
            OneBlockUltima.getLogger().error("[ModelUtil] cubesOf failed: {}", ex.toString());
            return new ArrayList<>();
        }
    }

    private static void addCubeBounds(Matrix4f m, ModelPart.Cube cube, float[] min, float[] max)
    {
        for (float x : new float[]{cube.minX, cube.maxX})
        {
            for (float y : new float[]{cube.minY, cube.maxY})
            {
                for (float z : new float[]{cube.minZ, cube.maxZ})
                {
                    org.joml.Vector4f v = new org.joml.Vector4f(x, y, z, 1.0F);
                    m.transform(v);
                    min[0] = Math.min(min[0], v.x());
                    min[1] = Math.min(min[1], v.y());
                    min[2] = Math.min(min[2], v.z());
                    max[0] = Math.max(max[0], v.x());
                    max[1] = Math.max(max[1], v.y());
                    max[2] = Math.max(max[2], v.z());
                }
            }
        }
    }

    public static float[] computeScreenEntityFit(int boxW, int boxH, Entity entity)
    {
        float[] units = getModelUnits(entity);
        float fitW = Math.max(4.0F, boxW - 2.0F);
        float fitH = Math.max(4.0F, boxH - 2.0F);
        float box = Math.min(fitW, fitH);
        float w = units[0];
        float h = units[1];
        if (!(w > 0.0F) || !(h > 0.0F) || Float.isNaN(w) || Float.isNaN(h))
        {
            return new float[]{1.0F, 0.0F, 0.0F};
        }
        float scale = (box * 0.95F) / Math.max(w, h);
        if (!(scale > 0.0F) || Float.isNaN(scale) || Float.isInfinite(scale))
        {
            scale = 1.0F;
        }
        return new float[]{scale, units[2], units[3]};
    }

    public static int[] fitEntityToScreen(int boxX, int boxY, int boxW, int boxH, Entity entity)
    {
        float[] fit = computeScreenEntityFit(boxW, boxH, entity);
        float scale = fit[0];
        int ox = boxX + boxW / 2 - Math.round(fit[1] * scale);
        int oy = boxY + boxH / 2 - Math.round(fit[2] * scale);
        return new int[]{ox, oy};
    }
}

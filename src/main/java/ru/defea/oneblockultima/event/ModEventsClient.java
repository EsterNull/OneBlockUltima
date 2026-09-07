package ru.defea.oneblockultima.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.Constants;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketRequestSync;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.util.ModelUtil;

import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.defea.oneblockultima.Constants.BLOCK_CENTER_OFFSET;
import static ru.defea.oneblockultima.Constants.COIN_TEXTURE;
import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_1;
import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_2;
import static ru.defea.oneblockultima.Constants.DARK_GRAY_COLOR_3;
import static ru.defea.oneblockultima.Constants.GOLD_COLOR;
import static ru.defea.oneblockultima.Constants.GREENISH_COLOR;
import static ru.defea.oneblockultima.Constants.LIME_COLOR;
import static ru.defea.oneblockultima.Constants.ORANGE_COLOR;
import static ru.defea.oneblockultima.Constants.PANEL_COLOR;
import static ru.defea.oneblockultima.Constants.TRANSPARENT_DARK_GRAY_COLOR_1;
import static ru.defea.oneblockultima.Constants.TRANSPARENT_DARK_GRAY_COLOR_2;
import static ru.defea.oneblockultima.Constants.WHITE_COLOR_1;
import static ru.defea.oneblockultima.Constants.WHITE_COLOR_2;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.GENERATOR_POS;

@Mod.EventBusSubscriber(modid = OneBlockUltima.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ModEventsClient
{
    private ModEventsClient()
    {
    }

    public static KeyMapping hidePanelKey;

    private static final List<BlockSetConfig.BlockEntryDefinition> PANEL_SET_BLOCKS = new ArrayList<>();
    private static final List<BlockSetConfig.BlockEntryDefinition> PANEL_FULL_BLOCKS = new ArrayList<>();
    private static final Map<BlockSetConfig.BlockEntryDefinition, TextureAtlasSprite> PANEL_SPRITE_CACHE = new HashMap<>();
    private static final Map<BlockSetConfig.BlockEntryDefinition, Integer> PANEL_TINT_CACHE = new HashMap<>();
    private static String cachedPanelSetId = null;
    private static long lastPanelErrorLog = 0L;
    private static long lastPanelDebugLog = 0L;

    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        if (hidePanelKey == null)
        {
            hidePanelKey = new KeyMapping("key.oneblockultima.hide_panel",
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.oneblockultima");
        }
        event.register(hidePanelKey);
    }

    @Mod.EventBusSubscriber(modid = OneBlockUltima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static final class KeyRegistration
    {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
        {
            registerKeyMappings(event);
        }

        @SubscribeEvent
        public static void onAddGuiOverlayLayers(AddGuiOverlayLayersEvent event)
        {
            event.getLayeredDraw().add((guiGraphics, deltaTracker) -> renderHud(guiGraphics));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        if (hidePanelKey != null && hidePanelKey.consumeClick())
        {
            ModSettings settings = ModSettings.get();
            settings.setShowInfoPanel(settings.isNotShowInfoPanel());
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        ModMessages.sendToServer(new PacketRequestSync());
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event)
    {
        ModMessages.sendToServer(new PacketRequestSync());
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA))
        {
            return;
        }
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).getUnsafe();
        if (tag != null && tag.getBoolean(Constants.NBT_OBU_GENERATED))
        {
            event.getToolTip().add(Component.translatable("gui.oneblockultima.tooltip.obu_generated").withStyle(ChatFormatting.GREEN));
        }
    }

    public static void renderHud(GuiGraphics g)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
        {
            return;
        }
        drawBalanceHud(g, mc);
    }

    private static void drawBalanceHud(GuiGraphics g, Minecraft mc)
    {
        ModSettings settings = ModSettings.get();
        if (!settings.isShowBalance())
        {
            return;
        }
        IOneBlockPlayerData data = mc.player == null
                ? null : mc.player.getCapability(OneBlockPlayerDataProvider.ONE_BLOCK_PLAYER_DATA).orElse(null);
        double currency = data == null ? 0 : data.getCurrency();
        String value = formatCurrency(currency);

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;
        int coinSize = 8;
        int spaceBetween = 2;
        int vMargin = 5;
        int hMargin = 7;
        int textW = font.width(value);
        int boxW = coinSize + textW + spaceBetween + hMargin * 2;
        int boxH = coinSize + vMargin * 2;

        int[] box = ModSettings.computeBalanceBox(
                settings.getBalancePosition(), settings.getHOffset(), settings.getVOffset(),
                boxW, boxH, sw, sh);
        int boxX = box[0];
        int boxY = box[1];

        int frame = 2;
        drawRoundedRect(g, boxX - frame, boxY - frame, boxX + boxW + frame, boxY + boxH + frame, 6, TRANSPARENT_DARK_GRAY_COLOR_1);
        drawRoundedRect(g, boxX, boxY, boxX + boxW, boxY + boxH, 5, TRANSPARENT_DARK_GRAY_COLOR_2);
        int x = boxX + hMargin;
        int y = boxY + vMargin;
        RenderSystem.setShaderTexture(0, COIN_TEXTURE);
        g.blit(COIN_TEXTURE, x, y, 0, 0, coinSize, coinSize, coinSize, coinSize);
        g.drawString(font, Component.literal(value), x + coinSize + spaceBetween, y, GOLD_COLOR, true);
    }

    private static void drawRoundedRect(GuiGraphics g, int minX, int minY, int maxX, int maxY, int radius, int color)
    {
        int r = Math.min(radius, Math.min((maxX - minX) / 2, (maxY - minY) / 2));
        for (int y = minY; y < maxY; y++)
        {
            int leftInset = 0;
            int rightInset = 0;
            if (y < minY + r)
            {
                int d = (minY + r) - y;
                int hw = (int) Math.round(Math.sqrt((double) r * r - (double) d * d));
                leftInset = r - hw;
                rightInset = r - hw;
            }
            else if (y >= maxY - r)
            {
                int d = y - (maxY - r - 1);
                int hw = (int) Math.round(Math.sqrt((double) r * r - (double) d * d));
                leftInset = r - hw;
                rightInset = r - hw;
            }
            g.fill(minX + leftInset, y, maxX - rightInset, y + 1, color);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        if (world == null || mc.player == null)
        {
            return;
        }
        if (ModSettings.get().isNotShowInfoPanel())
        {
            return;
        }
        if (world.getBlockState(GENERATOR_POS).getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            return;
        }
        double distSq = mc.player.distanceToSqr(
                GENERATOR_POS.getX() + BLOCK_CENTER_OFFSET,
                GENERATOR_POS.getY() + BLOCK_CENTER_OFFSET,
                GENERATOR_POS.getZ() + BLOCK_CENTER_OFFSET);
        if (distSq > 4096.0D)
        {
            return;
        }
        try
        {
            renderInfoPanelWorld(mc, event);
        }
        catch (Exception ex)
        {
            long now = System.currentTimeMillis();
            if (now - lastPanelErrorLog > 5000L)
            {
                lastPanelErrorLog = now;
                OneBlockUltima.getLogger().error("Failed to render info panel over generator", ex);
            }
        }
    }

    private static void renderInfoPanelWorld(Minecraft mc, RenderLevelStageEvent event)
    {
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        double anchorX = GENERATOR_POS.getX() + BLOCK_CENTER_OFFSET;
        double anchorY = GENERATOR_POS.getY() + 4.0D;
        double anchorZ = GENERATOR_POS.getZ() + BLOCK_CENTER_OFFSET;
        double hdX = camPos.x - anchorX;
        double hdZ = camPos.z - anchorZ;
        double hd = Math.sqrt(hdX * hdX + hdZ * hdZ);
        double t = 0.0D;
        if (hd < 2.2D)
        {
            t = (2.2D - hd) / 1.2D;
            if (t < 0.0D) t = 0.0D;
            if (t > 1.0D) t = 1.0D;
        }
        double yawRad = Math.toRadians(camera.getYRot());
        double lx = -Math.sin(yawRad);
        double lz = Math.cos(yawRad);
        double ex = camPos.x + lx * 2.0D;
        double ey = camPos.y + 0.8D;
        double ez = camPos.z + lz * 2.0D;
        double fx = anchorX + (ex - anchorX) * t;
        double fy = anchorY + (ey - anchorY) * t;
        double fz = anchorZ + (ez - anchorZ) * t;
        double dxToCam = camPos.x - fx;
        double dyToCam = camPos.y - fy;
        double dzToCam = camPos.z - fz;
        double distHoriz = Math.max(Math.sqrt(dxToCam * dxToCam + dzToCam * dzToCam), 1.0D);
        float yaw = (float) Math.toDegrees(Math.atan2(-dxToCam, dzToCam));
        double rawPitch = Math.toDegrees(Math.atan2(dyToCam, distHoriz));
        float pitch = (float) Math.max(-35.0D, Math.min(35.0D, rawPitch));

        Matrix4f panelPose = new Matrix4f();
        panelPose.rotate(camera.rotation().conjugate());
        panelPose.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
        panelPose.translate((float) fx, (float) fy, (float) fz);
        panelPose.rotate((float) Math.toRadians(-yaw), new Vector3f(0.0F, 1.0F, 0.0F));
        panelPose.rotate((float) Math.toRadians(pitch), new Vector3f(1.0F, 0.0F, 0.0F));
        panelPose.scale(0.019F, -0.019F, 0.019F);

        logPanelDebug(camera, event.getPoseStack(), anchorX, anchorY, anchorZ);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        int panelW = 176;
        int panelH = 88;
        int x = -panelW / 2;
        int y = 0;

        drawPanelQuad(panelPose, x - 2, y - 2, panelW + 4, panelH + 4, DARK_GRAY_COLOR_1);
        drawPanelQuad(panelPose, x, y, panelW, panelH, PANEL_COLOR);

        String setId = null;
        int setLevel = 0;
        int brokenInSet = 0;
        if (mc.level.getBlockEntity(GENERATOR_POS) instanceof TileEntityOneBlockGenerator generator)
        {
            setId = generator.getSelectedSetId();
            setLevel = generator.getSetLevel(setId);
        }
        BlockSetConfig.BlockSetDefinition setDef = setId == null ? null : BlockSetConfig.get().getSet(setId);
        String setName = setDef != null ? ContainerSetsConfig.getLocalizedSetName(setDef) : (setId != null ? setId : "?");
        IOneBlockPlayerData playerData = mc.player.getCapability(OneBlockPlayerDataProvider.ONE_BLOCK_PLAYER_DATA).orElse(null);
        if (playerData != null && setId != null)
        {
            brokenInSet = playerData.getBrokenBlocksCount(setId);
        }

        Font font = mc.font;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        drawTextScaled(font, I18n.get("gui.oneblockultima.panel.title"),
                x + panelW / 2.0F, y + 1.0F, LIME_COLOR, true, true, false, 1.35F, panelW - 4.0F, panelPose, buffers);

        int bannerX = x + 6;
        int bannerW = panelW - 12;
        int bannerY = y + 14;
        int bannerH = 26;
        collectPanelSetBlocks(setDef);
        drawPanelBanner(panelPose, bannerX, bannerY, bannerW, bannerH);

        drawTextScaled(font, I18n.get("gui.oneblockultima.lv") + " " + setLevel,
                bannerX + bannerW / 2.0F, bannerY + 2.0F, WHITE_COLOR_2, true, false, true, 1.35F, bannerW - 4.0F, panelPose, buffers);
        drawTextScaled(font, I18n.get("gui.oneblockultima.blocks_broken") + ": " + brokenInSet,
                bannerX + bannerW / 2.0F, bannerY + 13.0F, WHITE_COLOR_2, true, false, true, 1.35F, bannerW - 4.0F, panelPose, buffers);

        int nameW = font.width(Component.literal(setName));
        int maxNameW = panelW - 10;
        if (nameW > maxNameW)
        {
            float sc = (float) maxNameW / (float) nameW;
            Matrix4f namePose = new Matrix4f(panelPose);
            namePose.translate(x + panelW / 2.0F, bannerY + bannerH + 5.0F, 0.0F);
            namePose.scale(sc, sc, 1.0F);
            drawTextScaled(font, setName, 0.0F, -4.5F, ORANGE_COLOR, true, false, false, 1.35F, panelW - 10.0F, namePose, buffers);
        }
        else
        {
            drawTextScaled(font, setName,
                    x + panelW / 2.0F, bannerY + bannerH + 1.0F, ORANGE_COLOR, true, false, false, 1.35F, panelW - 10.0F, panelPose, buffers);
        }

        drawPanelQuad(panelPose, x, bannerY + bannerH + 11.0F, panelW, 1.0F, DARK_GRAY_COLOR_3);

        drawTextScaled(font, I18n.get("gui.oneblockultima.panel.upgrade_hint"),
                x + panelW / 2.0F, y + 56.0F, WHITE_COLOR_2, true, false, false, 1.35F, panelW - 4.0F, panelPose, buffers);
        String hideKey = hidePanelKey != null ? hidePanelKey.getTranslatedKeyMessage().getString() : "J";
        drawTextScaled(font, I18n.get("gui.oneblockultima.panel.hide_hint", hideKey),
                x + panelW / 2.0F, y + panelH - 10.0F, GREENISH_COLOR, true, false, false, 1.35F, panelW - 4.0F, panelPose, buffers);

        buffers.endBatch();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void logPanelDebug(Camera camera, Matrix4f view, double fx, double fy, double fz)
    {
        long now = System.currentTimeMillis();
        if (now - lastPanelDebugLog < 1000L)
        {
            return;
        }
        lastPanelDebugLog = now;
        Matrix4f p = new Matrix4f(view);
        OneBlockUltima.logDebug("[Panel] camYaw={} camPitch={} anchor=({},{},{}) view=[{} {} {} {} | {} {} {} {} | {} {} {} {} | {} {} {} {}]",
                camera.getYRot(), camera.getXRot(), fx, fy, fz,
                p.m00(), p.m01(), p.m02(), p.m03(),
                p.m10(), p.m11(), p.m12(), p.m13(),
                p.m20(), p.m21(), p.m22(), p.m23(),
                p.m30(), p.m31(), p.m32(), p.m33());
    }

    private static void drawPanelQuad(Matrix4f m, float x0, float y0, float w, float h, int color)
    {
        float a = (float) ((color >> 24) & 0xFF) / 255.0F;
        float r = (float) ((color >> 16) & 0xFF) / 255.0F;
        float g = (float) ((color >> 8) & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(m, x0, y0 + h, 0.0F).setColor(r, g, b, a);
        buf.addVertex(m, x0 + w, y0 + h, 0.0F).setColor(r, g, b, a);
        buf.addVertex(m, x0 + w, y0, 0.0F).setColor(r, g, b, a);
        buf.addVertex(m, x0, y0, 0.0F).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buf.build());
    }

    private static void drawTextScaled(Font font, String text, float x, float y, int color,
            boolean centered, boolean bold, boolean outlined, float size, float maxWidth,
            Matrix4f base, MultiBufferSource.BufferSource buffers)
    {
        if (text == null)
        {
            return;
        }
        Component main = bold ? Component.literal(text).withStyle(ChatFormatting.BOLD) : Component.literal(text);
        float w = font.width(main);
        if (maxWidth > 0.0F && w * size > maxWidth)
        {
            size = maxWidth / w;
        }
        float tx = centered ? x / size - w / 2.0F : x / size;
        float ty = y / size;
        Matrix4f m = new Matrix4f(base);
        m.scale(size, size, 1.0F);
        int packedLight = 0xF000F0;
        float o = 0.5F / size;
        font.drawInBatch(main, tx, ty, color, outlined, m, buffers, Font.DisplayMode.NORMAL, 0, packedLight);
        if (bold)
        {
            font.drawInBatch(main, tx + o, ty + o, color, outlined, m, buffers, Font.DisplayMode.NORMAL, 0, packedLight);
        }
    }

    private static void drawPanelBanner(Matrix4f pose, int bannerX, int bannerY, int bannerW, int bannerH)
    {
        if (PANEL_SET_BLOCKS.isEmpty() || bannerW <= 0 || bannerH <= 0)
        {
            return;
        }
        int cell = 16;
        int total = PANEL_SET_BLOCKS.size();
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        try
        {
            int cols = (bannerW + cell - 1) / cell;
            int rows = (bannerH + cell - 1) / cell;
            for (int row = 0; row < rows; row++)
            {
                for (int col = 0; col < cols; col++)
                {
                    int idx = ((row + col) * 7 + col * 3) % total;
                    if (idx < 0) idx += total;
                    BlockSetConfig.BlockEntryDefinition entry = PANEL_SET_BLOCKS.get(idx);
                    if (entry == null) continue;

                    if (!PANEL_FULL_BLOCKS.isEmpty())
                    {
                        int fullIdx = (idx * 3 + row + col) % PANEL_FULL_BLOCKS.size();
                        if (fullIdx < 0) fullIdx += PANEL_FULL_BLOCKS.size();
                        BlockSetConfig.BlockEntryDefinition backEntry = PANEL_FULL_BLOCKS.get(fullIdx);
                        if (backEntry != null)
                        {
                            TextureAtlasSprite backSprite = resolvePanelSprite(backEntry);
                            if (backSprite != null)
                            {
                                drawPanelBannerCell(buf, pose, backSprite, bannerX + col * cell, bannerY + row * cell, cell,
                                        bannerX, bannerY, bannerW, bannerH, 255, 255, 255);
                            }
                        }
                    }

                    TextureAtlasSprite sprite = resolvePanelSprite(entry);
                    if (sprite == null || sprite.getU0() == sprite.getU1() || sprite.getV0() == sprite.getV1()) continue;
                    Integer tintBoxed = PANEL_TINT_CACHE.get(entry);
                    if (tintBoxed == null)
                    {
                        int argb = ModelUtil.getBlockSpriteTint(resolvePanelState(entry), sprite);
                        PANEL_TINT_CACHE.put(entry, argb);
                        tintBoxed = argb;
                    }
                    int tint = tintBoxed;
                    drawPanelBannerCell(buf, pose, sprite, bannerX + col * cell, bannerY + row * cell, cell,
                            bannerX, bannerY, bannerW, bannerH,
                            (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF);
                }
            }
            BufferUploader.drawWithShader(buf.build());
        }
        catch (Exception ignored)
        {
        }
    }

    private static void drawPanelBannerCell(BufferBuilder buf, Matrix4f pose, TextureAtlasSprite sprite,
            int cellX, int cellY, int cellSize, int bannerX, int bannerY, int bannerW, int bannerH,
            int cr, int cg, int cb)
    {
        int drawX = Math.max(bannerX, cellX);
        int drawY = Math.max(bannerY, cellY);
        int drawX2 = Math.min(bannerX + bannerW, cellX + cellSize);
        int drawY2 = Math.min(bannerY + bannerH, cellY + cellSize);
        if (drawX >= drawX2 || drawY >= drawY2)
        {
            return;
        }

        float u1 = (drawX - cellX) / (float) cellSize;
        float v1 = (drawY - cellY) / (float) cellSize;
        float u2 = (drawX2 - cellX) / (float) cellSize;
        float v2 = (drawY2 - cellY) / (float) cellSize;

        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        float uMin = minU + (maxU - minU) * u1;
        float uMax = minU + (maxU - minU) * u2;
        float vMin = minV + (maxV - minV) * v1;
        float vMax = minV + (maxV - minV) * v2;

        int quadW = drawX2 - drawX;
        int quadH = drawY2 - drawY;

        float r = cr / 255.0F;
        float g = cg / 255.0F;
        float b = cb / 255.0F;
        buf.addVertex(pose, drawX, drawY + quadH, 0.0F).setUv(uMin, vMax).setColor(r, g, b, 1.0F);
        buf.addVertex(pose, drawX + quadW, drawY + quadH, 0.0F).setUv(uMax, vMax).setColor(r, g, b, 1.0F);
        buf.addVertex(pose, drawX + quadW, drawY, 0.0F).setUv(uMax, vMin).setColor(r, g, b, 1.0F);
        buf.addVertex(pose, drawX, drawY, 0.0F).setUv(uMin, vMin).setColor(r, g, b, 1.0F);
    }

    private static String formatCurrency(double value)
    {
        long rounded = Math.round(value * 100.0);
        double d = rounded / 100.0;
        if (d == (long) d)
        {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private static void collectPanelSetBlocks(BlockSetConfig.BlockSetDefinition setDef)
    {
        String id = setDef == null ? null : setDef.id;
        if (id != null && id.equals(cachedPanelSetId))
        {
            return;
        }
        PANEL_SET_BLOCKS.clear();
        PANEL_FULL_BLOCKS.clear();
        PANEL_SPRITE_CACHE.clear();
        PANEL_TINT_CACHE.clear();
        cachedPanelSetId = id;
        if (setDef == null)
        {
            return;
        }

        setDef.ensureComputedLevels();
        Map<Integer, BlockSetConfig.SetLevelDefinition> levels = setDef.computedLevels;
        if (levels == null)
        {
            return;
        }
        for (BlockSetConfig.SetLevelDefinition level : levels.values())
        {
            if (level.blocks == null) continue;
            for (BlockSetConfig.BlockEntryDefinition block : level.blocks)
            {
                if (block == null) continue;
                if (block.resolveBlock() == null) continue;
                PANEL_SET_BLOCKS.add(block);
                if (BlockUtil.isFullBlock(block.resolveBlock(), block.meta))
                {
                    PANEL_FULL_BLOCKS.add(block);
                }
            }
        }
        if (PANEL_SET_BLOCKS.isEmpty())
        {
            BlockSetConfig.BlockEntryDefinition dirt = createPanelEntry("minecraft:dirt", 0);
            PANEL_SET_BLOCKS.add(dirt);
            PANEL_FULL_BLOCKS.add(dirt);
        }
        if (PANEL_FULL_BLOCKS.isEmpty())
        {
            PANEL_FULL_BLOCKS.add(createPanelEntry("minecraft:dirt", 0));
        }
    }

    private static BlockSetConfig.BlockEntryDefinition createPanelEntry(String registry, int meta)
    {
        BlockSetConfig.BlockEntryDefinition entry = new BlockSetConfig.BlockEntryDefinition();
        entry.registry = registry;
        entry.meta = meta;
        entry.chance = 100;
        return entry;
    }

    private static TextureAtlasSprite resolvePanelSprite(BlockSetConfig.BlockEntryDefinition entry)
    {
        if (entry == null) return null;
        TextureAtlasSprite cached = PANEL_SPRITE_CACHE.get(entry);
        if (cached != null) return cached;
        try
        {
            Minecraft mc = Minecraft.getInstance();
            BlockState state = resolvePanelState(entry);
            if (state == null) return null;

            TextureAtlasSprite sprite = null;
            try
            {
                BakedModel model = mc.getBlockRenderer().getBlockModel(state);
                sprite = model.getParticleIcon();
            }
            catch (Exception ignored) {}

            if (sprite == null)
            {
                try
                {
                    ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(rl.withPrefix("block/"));
                }
                catch (Exception ignored) {}
            }

            if (sprite == null) return null;
            PANEL_SPRITE_CACHE.put(entry, sprite);
            return sprite;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static BlockState resolvePanelState(BlockSetConfig.BlockEntryDefinition entry)
    {
        try
        {
            BlockState state = entry.resolveBlock().defaultBlockState();
            if (state.getBlock() instanceof BlockCompressedBase base)
            {
                int level = entry.meta;
                if (level < 0) level = 0;
                if (level >= base.getMaxLevel()) level = base.getMaxLevel() - 1;
                state = state.setValue(base.getLevelProperty(), level);
            }
            return state;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
}
package ru.defea.oneblockultima.event;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.GuiOneBlock;
import ru.defea.oneblockultima.gui.GuiSetsConfig;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.util.ModelUtil;
import ru.defea.oneblockultima.world.OneBlockWorldType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static ru.defea.oneblockultima.Constants.*;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.GENERATOR_POS;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = OneBlockUltima.MODID)
public final class ModEventsClient
{
    private ModEventsClient()
    {
    }

    public static KeyBinding hidePanelKey;

    private static final List<BlockSetConfig.BlockEntryDefinition> PANEL_SET_BLOCKS = new ArrayList<>();
    private static final List<BlockSetConfig.BlockEntryDefinition> PANEL_FULL_BLOCKS = new ArrayList<>();
    private static final Map<BlockSetConfig.BlockEntryDefinition, TextureAtlasSprite> PANEL_SPRITE_CACHE = new HashMap<>();
    private static final Map<BlockSetConfig.BlockEntryDefinition, Integer> PANEL_TINT_CACHE = new HashMap<>();
    private static String cachedPanelSetId = null;

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        BlockSetConfig.reload();
    }

    private static final Map<UUID, Double> displayedCurrencyMap = new HashMap<>();
    private static final Map<UUID, Double> animStepMap = new HashMap<>();

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Text event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiOneBlock)
        {
            return;
        }

        if (mc.currentScreen instanceof GuiSetsConfig)
        {
            return;
        }

        net.minecraft.world.World world = mc.world;
        if (world == null || !(world.getWorldType() instanceof OneBlockWorldType))
        {
            return;
        }

        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT)
        {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
        {
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            return;
        }

        double currency = getDisplayedCurrency(player);

        String balanceValue = formatCurrency(currency);
        int textWidth = mc.fontRenderer.getStringWidth(balanceValue);
        int coinSize = 8;
        int spaceBetween = 2;
        int radius = 3;
        int vMargin = 5 + radius;
        int hMargin = 8 + radius;

        int screenWidth = event.getResolution().getScaledWidth();
        int screenHeight = event.getResolution().getScaledHeight();
        int bgWidth = coinSize + textWidth + spaceBetween + hMargin * 2;
        int bgHeight = coinSize + vMargin * 2;

        ModSettings settings = ModSettings.get();
        boolean isShowBalance = settings.isShowBalance();

        if (isShowBalance)
        {
            ModSettings.BalancePosition pos = settings.getBalancePosition();
            int hOffset = settings.getHOffset();
            int vOffset = settings.getVOffset();
            int hOffsetPx = screenWidth * hOffset / 100;
            int vOffsetPx = screenHeight * vOffset / 100;

            int bgX;
            int bgY;

            switch (pos) {
                case TOP_LEFT:
                    bgX = hOffsetPx;
                    bgY = vOffsetPx;
                    break;
                case TOP:
                    bgX = screenWidth / 2 - bgWidth / 2 + hOffsetPx;
                    bgY = vOffsetPx;
                    break;
                case TOP_RIGHT:
                    bgX = screenWidth - bgWidth - hOffsetPx;
                    bgY = vOffsetPx;
                    break;
                case LEFT:
                    bgX = hOffsetPx;
                    bgY = screenHeight / 2 - bgHeight / 2 + vOffsetPx;
                    break;
                case RIGHT:
                    bgX = screenWidth - bgWidth - hOffsetPx;
                    bgY = screenHeight / 2 - bgHeight / 2 + vOffsetPx;
                    break;
                case BOTTOM_LEFT:
                    bgX = hOffsetPx;
                    bgY = screenHeight - bgHeight - vOffsetPx;
                    break;
                case BOTTOM:
                    bgX = screenWidth / 2 - bgWidth / 2 + hOffsetPx;
                    bgY = screenHeight - bgHeight - vOffsetPx;
                    break;
                case BOTTOM_RIGHT:
                    bgX = screenWidth - bgWidth - hOffsetPx;
                    bgY = screenHeight - bgHeight - vOffsetPx;
                    break;
                default:
                    bgX = screenWidth - bgWidth - hOffset;
                    bgY = vOffset;
                    break;
            }

            int x = bgX + hMargin;
            int y = bgY + vMargin;

            drawRoundedRect(bgX, bgY, bgWidth, bgHeight, 5, TRANSPARENT_DARK_GRAY_COLOR_2);

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            Minecraft.getMinecraft().getTextureManager().bindTexture(COIN_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, coinSize, coinSize, coinSize, coinSize);
            GlStateManager.disableBlend();
            Minecraft.getMinecraft().fontRenderer.drawString(balanceValue, x + coinSize + spaceBetween, y, GOLD_COLOR);
        }
    }

    public static double getDisplayedCurrency(EntityPlayer player)
    {
        if (player == null)
        {
            return 0;
        }

        UUID playerUUID = player.getUniqueID();
        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        double targetCurrency = data == null ? 0 : data.getCurrency();

        Double lastCurrency = ModEvents.lastDisplayedCurrency.get(playerUUID);
        if (lastCurrency == null)
        {
            displayedCurrencyMap.put(playerUUID, targetCurrency);
            ModEvents.lastDisplayedCurrency.put(playerUUID, targetCurrency);
            return targetCurrency;
        }

        double currentDisplayed = displayedCurrencyMap.getOrDefault(playerUUID, targetCurrency);

        if (lastCurrency != targetCurrency)
        {
            ModEvents.lastDisplayedCurrency.put(playerUUID, targetCurrency);
            double delta = Math.abs(targetCurrency - currentDisplayed);
            long intPart = (long) Math.floor(delta);
            double step = Math.max(1, Math.round(intPart / 20.0));
            animStepMap.put(playerUUID, step);
        }

        double diff = targetCurrency - currentDisplayed;
        if (Math.abs(diff) < 0.001)
        {
            displayedCurrencyMap.put(playerUUID, targetCurrency);
            animStepMap.remove(playerUUID);
            return targetCurrency;
        }

        double step = animStepMap.getOrDefault(playerUUID, 1.0);
        double newDisplayed;
        if (Math.abs(diff) <= step)
        {
            newDisplayed = targetCurrency;
            animStepMap.remove(playerUUID);
        }
        else
        {
            newDisplayed = currentDisplayed + Math.signum(diff) * step;
        }

        displayedCurrencyMap.put(playerUUID, newDisplayed);
        return newDisplayed;
    }

    public static String formatCurrency(double value)
    {
        long rounded = Math.round(value * 100.0);
        double d = rounded / 100.0;
        if (d == (long) d)
        {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawRoundedRect(int x, int y, int width, int height, int radius, int color)
    {
        Gui.drawRect(x + radius, y, x + width - radius, y + height, color);
        Gui.drawRect(x, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++)
        {
            for (int j = 0; j < radius; j++)
            {
                if (i * i + j * j < radius * radius)
                {
                    int right = x + width - radius + i + 1;
                    int left = x + radius - i - 1;
                    int bottom = y + height - radius + j + 1;
                    int top = y + radius - j - 1;
                    Gui.drawRect(left, top, left + 1, top + 1, color);
                    Gui.drawRect(right - 1, top, right, top + 1, color);
                    Gui.drawRect(left, bottom - 1, left + 1, bottom, color);
                    Gui.drawRect(right - 1, bottom - 1, right, bottom, color);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event)
    {
        if (!(event.getGui() instanceof GuiCreateWorld))
        {
            return;
        }

        GuiCreateWorld screen = (GuiCreateWorld) event.getGui();
        WorldType worldType = getCreateWorldType(screen);
        if (worldType != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        String bonusLabel = I18n.format("createWorld.customize.bonusItems");
        String structuresLabel = I18n.format("createWorld.customize.mapFeatures");
        for (GuiButton button : event.getButtonList())
        {
            if (button == null || button.displayString == null)
            {
                continue;
            }
            if (button.displayString.equals(bonusLabel) || button.displayString.equals(structuresLabel))
            {
                button.visible = false;
                button.enabled = false;
            }
        }
    }

    private static Field createWorldTypeField;

    private static WorldType getCreateWorldType(GuiCreateWorld screen)
    {
        if (createWorldTypeField == null)
        {
            createWorldTypeField = findFieldByNames(GuiCreateWorld.class, "worldType", "field_146336_f", "field_146335_a");
            if (createWorldTypeField != null)
            {
                createWorldTypeField.setAccessible(true);
            }
        }

        if (createWorldTypeField == null)
        {
            return null;
        }

        try
        {
            Object value = createWorldTypeField.get(screen);
            if (value instanceof WorldType)
            {
                return (WorldType) value;
            }
        }
        catch (IllegalAccessException ignored)
        {
        }

        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private static Field findFieldByNames(Class<?> clazz, String... names)
    {
        for (String name : names)
        {
            try
            {
                return clazz.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored)
            {
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_OBU_GENERATED) && nbt.getBoolean(NBT_OBU_GENERATED))
        {
            event.getToolTip().add(net.minecraft.util.text.translation.I18n.translateToLocal("gui.oneblockultima.tooltip.obu_generated"));
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (hidePanelKey != null && hidePanelKey.isPressed())
        {
            ModSettings settings = ModSettings.get();
            settings.setShowInfoPanel(settings.isNotShowInfoPanel());
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.world.World world = mc.world;
        if (world == null || !world.isRemote)
        {
            return;
        }
        if (mc.player == null || !(world.getWorldType() instanceof OneBlockWorldType))
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
        double distSq = mc.player.getDistanceSq(
                GENERATOR_POS.getX() + BLOCK_CENTER_OFFSET,
                GENERATOR_POS.getY() + BLOCK_CENTER_OFFSET,
                GENERATOR_POS.getZ() + BLOCK_CENTER_OFFSET);
        if (distSq > 4096.0D)
        {
            return;
        }
        renderInfoPanel(event.getPartialTicks());
    }

    private static void renderInfoPanel(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null)
        {
            return;
        }

        double px = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double py = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double pz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        // When the player stands close to or inside the panel volume, smoothly move it
        // right in front of the camera (along the look direction, at eye level)
        double anchorX = GENERATOR_POS.getX() + BLOCK_CENTER_OFFSET;
        double anchorY = GENERATOR_POS.getY() + 5.25D;
        double anchorZ = GENERATOR_POS.getZ() + BLOCK_CENTER_OFFSET;
        double hdX = px - anchorX;
        double hdZ = pz - anchorZ;
        double hd = Math.sqrt(hdX * hdX + hdZ * hdZ);
        double fx = anchorX;
        double fy = anchorY;
        double fz = anchorZ;
        if (hd < 4.0D)
        {
            double yawRad = Math.toRadians(mc.getRenderManager().playerViewY);
            double lx = -Math.sin(yawRad);
            double lz = Math.cos(yawRad);
            double blend = (4.0D - hd) / 4.0D;
            if (blend > 1.0D) blend = 1.0D;
            double standoff = 2.4D;
            double cx = px + lx * standoff;
            double cz = pz + lz * standoff;
            double cy = py + 2.05D;
            fx = anchorX + (cx - anchorX) * blend;
            fy = anchorY + (cy - anchorY) * blend;
            fz = anchorZ + (cz - anchorZ) * blend;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(fx - px, fy - py, fz - pz);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) (mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.019F, -0.019F, 0.019F);

        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        int panelW = 176;
        int panelH = 88;
        int x = -panelW / 2;
        int y = 0;

        drawPanelQuad(x - 2, y - 2, panelW + 4, panelH + 4, DARK_GRAY_COLOR_1);
        drawPanelQuad(x, y, panelW, panelH, PANEL_COLOR);

        String setId = null;
        int setLevel = 0;
        int brokenInSet = 0;
        net.minecraft.tileentity.TileEntity te = mc.world.getTileEntity(GENERATOR_POS);
        if (te instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) te;
            setId = generator.getSelectedSetId();
            setLevel = generator.getSetLevel(setId);
        }
        BlockSetConfig.BlockSetDefinition setDef = setId == null ? null : BlockSetConfig.get().getSet(setId);
        String setName = setDef != null ? ContainerSetsConfig.getLocalizedSetName(setDef) : (setId != null ? setId : "?");
        IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(mc.player);
        if (playerData != null && setId != null)
        {
            brokenInSet = playerData.getBrokenBlocksCount(setId);
        }

        // Заголовок у самой верхней границы: жирный, лаймовый, с обводкой
        String titleText = I18n.format("gui.oneblockultima.panel.title");
        drawOutlinedString(titleText, x + panelW / 2.0F, y + 1, LIME_COLOR, true, true);

        // Баннер из блоков выбранного набора
        int bannerX = x + 6;
        int bannerW = panelW - 12;
        int bannerY = y + 14;
        int bannerH = 26;
        collectPanelSetBlocks(setDef);
        drawPanelBanner(bannerX, bannerY, bannerW, bannerH);

        // Текст внутри баннера (уровень и сломанные блоки), по центру
        String levelText = I18n.format("gui.oneblockultima.lv") + " " + setLevel;
        String brokenText = I18n.format("gui.oneblockultima.blocks_broken") + ": " + brokenInSet;
        drawOutlinedString(levelText, bannerX + bannerW / 2.0F, bannerY + 2, WHITE_COLOR_2, true, false);
        drawOutlinedString(brokenText, bannerX + bannerW / 2.0F, bannerY + 13, WHITE_COLOR_2, true, false);

        // Название набора под баннером (сжимается, если не влезает в панель)
        int nameW = mc.fontRenderer.getStringWidth(setName);
        int maxNameW = panelW - 10;
        if (nameW > maxNameW)
        {
            float sc = (float) maxNameW / (float) nameW;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + panelW / 2.0F, bannerY + bannerH + 5.5F, 0.0F);
            GlStateManager.scale(sc, sc, 1.0F);
            drawOutlinedString(setName, 0.0F, -4.5F, ORANGE_COLOR, true, false);
            GlStateManager.popMatrix();
        }
        else
        {
            drawOutlinedString(setName, x + panelW / 2.0F, bannerY + bannerH + 1, ORANGE_COLOR, true, false);
        }

        drawPanelQuad(x, bannerY + bannerH + 11, panelW, 1, DARK_GRAY_COLOR_3);

        String upgradeHint = I18n.format("gui.oneblockultima.panel.upgrade_hint");
        drawOutlinedString(upgradeHint, x + panelW / 2.0F, y + 56, WHITE_COLOR_2, true, false);

        String hideHint = I18n.format("gui.oneblockultima.panel.hide_hint",
                hidePanelKey != null ? hidePanelKey.getDisplayName() : "J");
        drawOutlinedString(hideHint, x + panelW / 2.0F, y + panelH - 10, GREENISH_COLOR, true, false);

        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void drawOutlinedString(String text, float x, float y, int color, @SuppressWarnings("SameParameterValue") boolean centered, boolean bold)
    {
        net.minecraft.client.gui.FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String main = bold ? "\u00A7l" + text : text;
        String outline = "\u00A70" + (bold ? "\u00A7l" : "") + text;
        float tx = centered ? x - fr.getStringWidth(main) / 2.0F : x;
        fr.drawString(outline, tx - 1.0F, y, WHITE_COLOR_1, false);
        fr.drawString(outline, tx + 1.0F, y, WHITE_COLOR_1, false);
        fr.drawString(outline, tx, y - 1.0F, WHITE_COLOR_1, false);
        fr.drawString(outline, tx, y + 1.0F, WHITE_COLOR_1, false);
        fr.drawString(main, tx, y, color, true);
        if (bold)
        {
            fr.drawString(main, tx + 1.0F, y, color, true);
            fr.drawString(main, tx + 0.5F, y - 0.5F, color, true);
        }
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
                Block mcBlock = block.resolveBlock();
                if (mcBlock == null) continue;
                if (mcBlock.getDefaultState().getMaterial().isLiquid()) continue;
                if (mcBlock instanceof net.minecraftforge.fluids.IFluidBlock) continue;
                PANEL_SET_BLOCKS.add(block);
                if (BlockUtil.isFullBlock(mcBlock, block.meta))
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

    @SuppressWarnings("SameParameterValue")
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
            Minecraft mc = Minecraft.getMinecraft();
            Block block = entry.resolveBlock();
            if (block == null) return null;
            IBlockState state = null;
            try
            {
                state = block.getStateFromMeta(entry.meta);
            }
            catch (Exception ex)
            {
                try
                {
                    state = block.getDefaultState();
                }
                catch (Exception ignored) {}
            }
            if (state == null) return null;

            TextureAtlasSprite sprite = null;
            try
            {
                sprite = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
            }
            catch (Exception ignored) {}

            if (sprite == null || "missingno".equals(sprite.getIconName()))
            {
                try
                {
                    ResourceLocation registryName = Objects.requireNonNull(block.getRegistryName());
                    sprite = mc.getTextureMapBlocks().getAtlasSprite(registryName.toString());
                    if ("missingno".equals(sprite.getIconName()))
                    {
                        sprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:items/" + registryName.getResourcePath());
                    }
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

    private static net.minecraft.block.state.IBlockState resolvePanelState(BlockSetConfig.BlockEntryDefinition entry)
    {
        try
        {
            net.minecraft.block.Block block = entry.resolveBlock();
            if (block == null) return null;
            try
            {
                return block.getStateFromMeta(entry.meta);
            }
            catch (Exception ex)
            {
                return block.getDefaultState();
            }
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static void drawPanelBanner(int bannerX, int bannerY, int bannerW, int bannerH)
    {
        if (PANEL_SET_BLOCKS.isEmpty() || bannerW <= 0 || bannerH <= 0)
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int cell = 16;
        int total = PANEL_SET_BLOCKS.size();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
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

                    boolean partial = !BlockUtil.isFullBlock(entry.resolveBlock(), entry.meta);
                    if (partial && !PANEL_FULL_BLOCKS.isEmpty())
                    {
                        int fullIdx = (idx * 3 + row + col) % PANEL_FULL_BLOCKS.size();
                        if (fullIdx < 0) fullIdx += PANEL_FULL_BLOCKS.size();
                        TextureAtlasSprite backSprite = resolvePanelSprite(PANEL_FULL_BLOCKS.get(fullIdx));
                        if (backSprite != null)
                        {
                            drawPanelBannerCell(buf, backSprite, bannerX + col * cell, bannerY + row * cell, cell, bannerX, bannerY, bannerW, bannerH, 255, 255, 255);
                        }
                    }

                    TextureAtlasSprite sprite = resolvePanelSprite(entry);
                    if (sprite == null) continue;
                    Integer tintBoxed = PANEL_TINT_CACHE.get(entry);
                    if (tintBoxed == null)
                    {
                        int argb = ModelUtil.getBlockSpriteTint(resolvePanelState(entry), sprite);
                        PANEL_TINT_CACHE.put(entry, argb);
                        tintBoxed = argb;
                    }
                    int tint = tintBoxed;
                    drawPanelBannerCell(buf, sprite, bannerX + col * cell, bannerY + row * cell, cell, bannerX, bannerY, bannerW, bannerH,
                            (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF);
                }
            }
            tess.draw();
        }
        catch (Exception ignored) {}
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
    }

    private static void drawPanelBannerCell(BufferBuilder buf, TextureAtlasSprite sprite,
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

        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        float uMin = minU + (maxU - minU) * u1;
        float uMax = minU + (maxU - minU) * u2;
        float vMin = minV + (maxV - minV) * v1;
        float vMax = minV + (maxV - minV) * v2;

        int quadW = drawX2 - drawX;
        int quadH = drawY2 - drawY;

        buf.pos(drawX, drawY + quadH, 0.0D).tex(uMin, vMax).color(cr, cg, cb, 255).endVertex();
        buf.pos(drawX + quadW, drawY + quadH, 0.0D).tex(uMax, vMax).color(cr, cg, cb, 255).endVertex();
        buf.pos(drawX + quadW, drawY, 0.0D).tex(uMax, vMin).color(cr, cg, cb, 255).endVertex();
        buf.pos(drawX, drawY, 0.0D).tex(uMin, vMin).color(cr, cg, cb, 255).endVertex();
    }

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onRenderLivingSpecialsPost(RenderLivingEvent.Specials.Post event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null)
        {
            return;
        }
        if (!(mc.world.getWorldType() instanceof OneBlockWorldType))
        {
            return;
        }
        if (!(event.getEntity() instanceof EntityPlayer))
        {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player != mc.player)
        {
            return;
        }
        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            return;
        }
        if (player.isInvisible())
        {
            return;
        }
        double distSq = player.getDistanceSq(mc.getRenderManager().renderViewEntity);
        if (distSq > 4096.0D)
        {
            return;
        }

        String name = player.getDisplayName().getFormattedText();
        String count = I18n.format("gui.oneblockultima.panel.nametag_broken", data.getBrokenBlocksCount());
        int nameWidth = mc.fontRenderer.getStringWidth(name);
        int countWidth = mc.fontRenderer.getStringWidth(count);
        int boxWidth = Math.max(nameWidth, countWidth);

        boolean sneaking = player.isSneaking();
        boolean frontal = mc.gameSettings.thirdPersonView == 2;
        float heightOffset = player.height + 0.5F - (sneaking ? 0.25F : 0.0F);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) event.getX(), (float) event.getY() + heightOffset, (float) event.getZ());
        GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) (frontal ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (!sneaking)
        {
            GlStateManager.disableDepth();
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos((double) -boxWidth / 2 - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        buffer.pos((double) -boxWidth / 2 - 1, mc.fontRenderer.FONT_HEIGHT * 2 + 2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        buffer.pos((double) boxWidth / 2 + 1, mc.fontRenderer.FONT_HEIGHT * 2 + 2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        buffer.pos((double) boxWidth / 2 + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        mc.fontRenderer.drawString(name, -nameWidth / 2, 0, 553648127);
        mc.fontRenderer.drawString(count, -countWidth / 2, mc.fontRenderer.FONT_HEIGHT + 1, 553648127);
        if (!sneaking)
        {
            GlStateManager.enableDepth();
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void drawPanelQuad(int x, int y, int w, int h, int color)
    {
        float a = (float) ((color >> 24) & 255) / 255.0F;
        float r = (float) ((color >> 16) & 255) / 255.0F;
        float g = (float) ((color >> 8) & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(x, y + h, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(x + w, y + h, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos(x + w, y, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

}

package ru.defea.oneblockultima.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.GuiOneBlock;
import ru.defea.oneblockultima.gui.GuiSetsConfig;
import ru.defea.oneblockultima.world.OneBlockWorldType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = OneBlockUltima.MODID)
public final class ModEventsClient
{
    private ModEventsClient()
    {
    }

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        BlockSetConfig.reload();
    }

    private static final Map<UUID, Double> displayedCurrencyMap = new HashMap<>();
    private static final Map<UUID, Double> animStepMap = new HashMap<>();

    private static double computeAnimStep(double delta)
    {
        if (delta <= 0) return 0.01;
        return Math.max(0.01, Math.pow(10, Math.floor(Math.log10(delta)) - 1)) / 2;
    }

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
        ModSettings.BalancePosition pos = settings.getBalancePosition();
        int hOffset = settings.getHOffset();
        int vOffset = settings.getVOffset();

        int hOffsetPx = screenWidth * hOffset / 100;
        int vOffsetPx = screenHeight * vOffset / 100;

        int bgX;
        int bgY;

        switch (pos)
        {
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

        drawRoundedRect(bgX, bgY, bgWidth, bgHeight, 5, 0x99333333);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(OneBlockUltima.MODID, "textures/gui/coin.png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, coinSize, coinSize, coinSize, coinSize);
        GlStateManager.disableBlend();
        Minecraft.getMinecraft().fontRenderer.drawString(balanceValue, x + coinSize + spaceBetween, y, 0xFFD700);
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
            animStepMap.put(playerUUID, computeAnimStep(delta));
        }

        double diff = targetCurrency - currentDisplayed;
        if (Math.abs(diff) < 0.01)
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
}

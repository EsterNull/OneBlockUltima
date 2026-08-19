package ru.defea.oneblockultima.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.opengl.GL11;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
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

import static ru.defea.oneblockultima.Constants.*;

public class ModEventsClient
{
    public ModEventsClient()
    {
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        BlockSetConfig.reload();
    }

    private static final Map<UUID, Double> displayedCurrencyMap = new HashMap<>();
    private static final Map<UUID, Double> animStepMap = new HashMap<>();

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Text event)
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

        net.minecraft.world.World world = mc.theWorld;
        if (world == null || !(world.getWorldInfo().getTerrainType() instanceof OneBlockWorldType))
        {
            return;
        }

        if (event.type != RenderGameOverlayEvent.ElementType.TEXT)
        {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
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
        int textWidth = mc.fontRendererObj.getStringWidth(balanceValue);
        int coinSize = 8;
        int spaceBetween = 2;
        int radius = 3;
        int vMargin = 5 + radius;
        int hMargin = 8 + radius;

        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();
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

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Minecraft.getMinecraft().getTextureManager().bindTexture(COIN_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, coinSize, coinSize, coinSize, coinSize);
            GL11.glDisable(GL11.GL_BLEND);
            Minecraft.getMinecraft().fontRendererObj.drawString(balanceValue, x + coinSize + spaceBetween, y, GOLD_COLOR);
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
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            return "0";
        }
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(value);
        bd = bd.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
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
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event)
    {
        if (!(event.gui instanceof GuiCreateWorld))
        {
            return;
        }

        GuiCreateWorld screen = (GuiCreateWorld) event.gui;
        WorldType worldType = getCreateWorldType(screen);
        if (worldType != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        String bonusLabel = I18n.format("createWorld.customize.bonusItems");
        String structuresLabel = I18n.format("createWorld.customize.mapFeatures");
        for (Object obj : event.buttonList)
        {
            GuiButton button = (GuiButton) obj;
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
    public void onItemTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.itemStack;
        if (stack == null || stack.stackSize <= 0) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_OBU_GENERATED) && nbt.getBoolean(NBT_OBU_GENERATED))
        {
            event.toolTip.add(net.minecraft.util.StatCollector.translateToLocal("gui.oneblockultima.tooltip.obu_generated"));
        }
    }
}

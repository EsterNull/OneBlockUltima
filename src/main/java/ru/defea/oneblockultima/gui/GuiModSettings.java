package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.ModSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiModSettings extends GuiScreen
{
    private static final int BUTTON_SAVE = 0;
    private static final int BUTTON_BACK = 1;
    private static final int GRID_BUTTON_BASE = 10;
    private static final int BUTTON_H_OFFSET_DEC = 21;
    private static final int BUTTON_H_OFFSET_INC = 22;
    private static final int BUTTON_V_OFFSET_DEC = 23;
    private static final int BUTTON_V_OFFSET_INC = 24;

    private static final ResourceLocation COIN_TEXTURE = new ResourceLocation(OneBlockUltima.MODID, "textures/gui/coin.png");

    private final GuiScreen parent;
    private ModSettings.BalancePosition currentPos;
    private int hOffset;
    private int vOffset;
    private GuiTextField hOffsetField;
    private GuiTextField vOffsetField;

    private int hFieldY;
    private int vFieldY;
    private int hLabelX;
    private int vLabelX;

    private int previewX;
    private int previewY;
    private int previewWidth;
    private int previewHeight;

    private final int cellSize = 16;
    private final int cellGap = 3;
    private final int titleY = 12;

    private final List<int[]> gridCells = new ArrayList<>();

    public GuiModSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    @Override
    public void initGui()
    {
        ModSettings settings = ModSettings.get();
        currentPos = settings.getBalancePosition();
        hOffset = settings.getHOffset();
        vOffset = settings.getVOffset();

        int centerX = width / 2;
        int padding = 10;

        previewWidth = Math.min(width - padding * 2, 250);
        previewHeight = 80;
        previewX = centerX - previewWidth / 2;
        previewY = titleY + fontRenderer.FONT_HEIGHT * 2 + cellGap * 2;

        int controlsY = previewY + previewHeight + 20;
        int gridTotalW = cellSize * 3 + cellGap * 2;
        int gridLeft = centerX - gridTotalW - 15;

        gridCells.clear();
        ModSettings.BalancePosition[][] grid = {
            {ModSettings.BalancePosition.TOP_LEFT, ModSettings.BalancePosition.TOP, ModSettings.BalancePosition.TOP_RIGHT},
            {ModSettings.BalancePosition.LEFT, null, ModSettings.BalancePosition.RIGHT},
            {ModSettings.BalancePosition.BOTTOM_LEFT, ModSettings.BalancePosition.BOTTOM, ModSettings.BalancePosition.BOTTOM_RIGHT}
        };

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                ModSettings.BalancePosition pos = grid[row][col];
                if (pos == null) continue;
                int bx = gridLeft + col * (cellSize + cellGap);
                int by = controlsY + row * (cellSize + cellGap);
                gridCells.add(new int[]{GRID_BUTTON_BASE + pos.ordinal(), bx, by, pos.ordinal()});
            }
        }

        String hLabel = I18n.format("gui.oneblockultima.mod_settings.h_offset");
        String vLabel = I18n.format("gui.oneblockultima.mod_settings.v_offset");
        int labelWidth = Math.max(fontRenderer.getStringWidth(hLabel), fontRenderer.getStringWidth(vLabel));

        int rightX = centerX + 15;
        int fieldWidth = 40;
        int buttonSize = 14;
        int gap = 3;
        int fieldX = rightX + labelWidth + buttonSize + gap;
        hLabelX = rightX;
        vLabelX = rightX;
        int btnDecX = fieldX - (buttonSize + gap);
        int btnIncX = fieldX + fieldWidth + gap;

        hFieldY = controlsY + 2;
        vFieldY = controlsY + 26;

        hOffsetField = new GuiTextField(100, fontRenderer, fieldX, hFieldY, fieldWidth, 14);
        hOffsetField.setText(String.valueOf(hOffset));
        hOffsetField.setFocused(false);
        hOffsetField.setEnableBackgroundDrawing(false);
        hOffsetField.setEnabled(false);

        vOffsetField = new GuiTextField(101, fontRenderer, fieldX, vFieldY, fieldWidth, 14);
        vOffsetField.setText(String.valueOf(vOffset));
        vOffsetField.setFocused(false);
        vOffsetField.setEnableBackgroundDrawing(false);
        vOffsetField.setEnabled(false);

        buttonList.add(new GuiButton(BUTTON_H_OFFSET_DEC, btnDecX, hFieldY, buttonSize, buttonSize, "-"));
        buttonList.add(new GuiButton(BUTTON_H_OFFSET_INC, btnIncX, hFieldY, buttonSize, buttonSize, "+"));
        buttonList.add(new GuiButton(BUTTON_V_OFFSET_DEC, btnDecX, vFieldY, buttonSize, buttonSize, "-"));
        buttonList.add(new GuiButton(BUTTON_V_OFFSET_INC, btnIncX, vFieldY, buttonSize, buttonSize, "+"));

        int bottomY = controlsY + cellSize * 3 + cellGap * 2 + 20;
        int bottomBtnWidth = 80;

        buttonList.add(new GuiButton(BUTTON_SAVE, centerX - bottomBtnWidth - 4, bottomY, bottomBtnWidth, 20, I18n.format("gui.oneblockultima.save")));
        buttonList.add(new GuiButton(BUTTON_BACK, centerX + 4, bottomY, bottomBtnWidth, 20, I18n.format("gui.oneblockultima.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_SAVE)
        {
            applyOffsets();
            ModSettings.get().setBalancePosition(currentPos);
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_BACK)
        {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_H_OFFSET_DEC)
        {
            hOffset = Math.max(0, hOffset - 1);
            hOffsetField.setText(String.valueOf(hOffset));
        }
        else if (button.id == BUTTON_H_OFFSET_INC)
        {
            hOffset = Math.min(50, hOffset + 1);
            hOffsetField.setText(String.valueOf(hOffset));
        }
        else if (button.id == BUTTON_V_OFFSET_DEC)
        {
            vOffset = Math.max(0, vOffset - 1);
            vOffsetField.setText(String.valueOf(vOffset));
        }
        else if (button.id == BUTTON_V_OFFSET_INC)
        {
            vOffset = Math.min(50, vOffset + 1);
            vOffsetField.setText(String.valueOf(vOffset));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (mouseButton == 0)
        {
            for (int[] cell : gridCells)
            {
            int cx = cell[1], cy = cell[2], ordinal = cell[3];
                if (mouseX >= cx && mouseX < cx + cellSize && mouseY >= cy && mouseY < cy + cellSize)
                {
                    currentPos = ModSettings.BalancePosition.values()[ordinal];
                    return;
                }
            }
        }
        hOffsetField.mouseClicked(mouseX, mouseY, mouseButton);
        vOffsetField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void applyOffsets()
    {
        try { hOffset = Integer.parseInt(hOffsetField.getText()); }
        catch (NumberFormatException e) { hOffset = ModSettings.get().getHOffset(); }
        hOffset = Math.max(0, Math.min(50, hOffset));

        try { vOffset = Integer.parseInt(vOffsetField.getText()); }
        catch (NumberFormatException e) { vOffset = ModSettings.get().getVOffset(); }
        vOffset = Math.max(0, Math.min(50, vOffset));

        ModSettings.get().setHOffset(hOffset);
        ModSettings.get().setVOffset(vOffset);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (hOffsetField.isFocused())
        {
            hOffsetField.textboxKeyTyped(typedChar, keyCode);
            hOffset = parseOffset(hOffsetField.getText(), hOffset);
        }
        else if (vOffsetField.isFocused())
        {
            vOffsetField.textboxKeyTyped(typedChar, keyCode);
            vOffset = parseOffset(vOffsetField.getText(), vOffset);
        }
        super.keyTyped(typedChar, keyCode);
    }

    private int parseOffset(String text, int fallback)
    {
        try { int val = Integer.parseInt(text); return Math.max(0, Math.min(50, val)); }
        catch (NumberFormatException e) { return fallback; }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        hOffsetField.updateCursorCounter();
        vOffsetField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();

        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.mod_settings.title"), width / 2, titleY, 0xFFFFFF);

        drawPreview();

        fontRenderer.drawString(I18n.format("gui.oneblockultima.mod_settings.h_offset"), hLabelX, hFieldY + 3, 0xC0C0C0);
        fontRenderer.drawString(I18n.format("gui.oneblockultima.mod_settings.v_offset"), vLabelX, vFieldY + 3, 0xC0C0C0);

        drawTextFieldBackground(hOffsetField);
        hOffsetField.drawTextBox();
        drawTextFieldBackground(vOffsetField);
        vOffsetField.drawTextBox();

        String posName = I18n.format("gui.oneblockultima.mod_settings.pos." + currentPos.name().toLowerCase());
        drawCenteredString(fontRenderer, posName, width / 2, previewY + previewHeight + 8, 0x55FF55);

        drawGrid(mouseX, mouseY);

        for (GuiButton btn : buttonList)
        {
            btn.drawButton(mc, mouseX, mouseY, partialTicks);
        }
    }

    private void drawGrid(int mouseX, int mouseY)
    {
        for (int[] cell : gridCells)
        {
            int cx = cell[1], cy = cell[2], ordinal = cell[3];
            ModSettings.BalancePosition pos = ModSettings.BalancePosition.values()[ordinal];
            boolean selected = pos == currentPos;
            boolean hovered = mouseX >= cx && mouseX < cx + cellSize && mouseY >= cy && mouseY < cy + cellSize;

            int bgColor;
            if (selected)
            {
                bgColor = 0xFF2A6B35;
            }
            else if (hovered)
            {
                bgColor = 0xFF4A4A5A;
            }
            else
            {
                bgColor = 0xFF3A3A4A;
            }

            Gui.drawRect(cx, cy, cx + cellSize, cy + cellSize, bgColor);
            drawHorizontalLine(cx, cx + cellSize, cy, 0xFF666666);
            drawHorizontalLine(cx, cx + cellSize, cy + cellSize, 0xFF666666);
            drawVerticalLine(cx, cy, cy + cellSize, 0xFF666666);
            drawVerticalLine(cx + cellSize, cy, cy + cellSize, 0xFF666666);

            String label = getPositionLabel(pos);
            int textColor = selected ? 0x55FF55 : 0xFFFFFF;
            int textWidth = fontRenderer.getStringWidth(label);
            fontRenderer.drawStringWithShadow(label, cx + (cellSize - textWidth) / 2.0F, cy + (cellSize - 8) / 2.0F, textColor);
        }
    }

    private String getPositionLabel(ModSettings.BalancePosition pos)
    {
        switch (pos)
        {
            case TOP_LEFT: return "\u2196";
            case TOP: return "\u2191";
            case TOP_RIGHT: return "\u2197";
            case LEFT: return "\u2190";
            case RIGHT: return "\u2192";
            case BOTTOM_LEFT: return "\u2199";
            case BOTTOM: return "\u2193";
            case BOTTOM_RIGHT: return "\u2198";
            default: return "?";
        }
    }

    private void drawTextFieldBackground(GuiTextField field)
    {
        int pad = 2;
        int x = field.x - pad;
        int y = field.y - pad;
        int w = field.width + pad * 2;
        int h = field.height + pad * 2;
        Gui.drawRect(x, y, x + w, y + h, 0xFF1A1D21);
        drawHorizontalLine(x, x + w, y, 0xFF444444);
        drawHorizontalLine(x, x + w, y + h, 0xFF444444);
        drawVerticalLine(x, y, y + h, 0xFF444444);
        drawVerticalLine(x + w, y, y + h, 0xFF444444);
    }

    private void drawPreview()
    {
        drawHorizontalLine(previewX, previewX + previewWidth, previewY, 0xFF555555);
        drawHorizontalLine(previewX, previewX + previewWidth, previewY + previewHeight, 0xFF555555);
        drawVerticalLine(previewX, previewY, previewY + previewHeight, 0xFF555555);
        drawVerticalLine(previewX + previewWidth, previewY, previewY + previewHeight, 0xFF555555);
        Gui.drawRect(previewX + 1, previewY + 1, previewX + previewWidth - 1, previewY + previewHeight - 1, 0xAA111111);

        int chX = previewX + previewWidth / 2;
        int chY = previewY + previewHeight / 2;
        int chLen = 5;
        int chThick = 1;
        Gui.drawRect(chX - chLen, chY, chX + chLen + (chThick + 1) / 2, chY + (chThick + 1) / 2, 0xFFCCCCCC);
        Gui.drawRect(chX, chY - chLen, chX + (chThick + 1) / 2, chY + chLen + (chThick + 1) / 2, 0xFFCCCCCC);

        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.mod_settings.preview"), previewX + previewWidth / 2, previewY - fontRenderer.FONT_HEIGHT - cellGap, 0xA0A0A0);

        int coinSize = 6;
        int spaceBetween = 2;
        int hMargin = 5;
        int vMargin = 3;
        String sampleText = "12345";
        int textWidth = fontRenderer.getStringWidth(sampleText);
        int boxW = coinSize + textWidth + spaceBetween + hMargin * 2;
        int boxH = coinSize + vMargin * 2;

        int innerX = previewX + 1;
        int innerY = previewY + 1;
        int innerW = previewWidth - 2;
        int innerH = previewHeight - 2;

        int boxX;
        int boxY;

        switch (currentPos)
        {
            case TOP_LEFT:
                boxX = innerX + innerW * hOffset / 100;
                boxY = innerY + innerH * vOffset / 100;
                break;
            case TOP:
                boxX = innerX + innerW / 2 - boxW / 2 + innerW * hOffset / 100;
                boxY = innerY + innerH * vOffset / 100;
                break;
            case TOP_RIGHT:
                boxX = innerX + innerW - boxW - innerW * hOffset / 100;
                boxY = innerY + innerH * vOffset / 100;
                break;
            case LEFT:
                boxX = innerX + innerW * hOffset / 100;
                boxY = innerY + innerH / 2 - boxH / 2 + innerH * vOffset / 100;
                break;
            case RIGHT:
                boxX = innerX + innerW - boxW - innerW * hOffset / 100;
                boxY = innerY + innerH / 2 - boxH / 2 + innerH * vOffset / 100;
                break;
            case BOTTOM_LEFT:
                boxX = innerX + innerW * hOffset / 100;
                boxY = innerY + innerH - boxH - innerH * vOffset / 100;
                break;
            case BOTTOM:
                boxX = innerX + innerW / 2 - boxW / 2 + innerW * hOffset / 100;
                boxY = innerY + innerH - boxH - innerH * vOffset / 100;
                break;
            case BOTTOM_RIGHT:
                boxX = innerX + innerW - boxW - innerW * hOffset / 100;
                boxY = innerY + innerH - boxH - innerH * vOffset / 100;
                break;
            default:
                boxX = innerX + innerW - boxW - innerW * hOffset / 100;
                boxY = innerY + innerH * vOffset / 100;
                break;
        }

        boxX = Math.max(innerX, Math.min(boxX, innerX + innerW - boxW));
        boxY = Math.max(innerY, Math.min(boxY, innerY + innerH - boxH));

        Gui.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x99333333);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(COIN_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(boxX + hMargin, boxY + vMargin, 0, 0, coinSize, coinSize, coinSize, coinSize);
        GlStateManager.disableBlend();
        fontRenderer.drawString(sampleText, boxX + hMargin + coinSize + spaceBetween, boxY + vMargin - fontRenderer.FONT_HEIGHT / 4, 0xFFD700);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}

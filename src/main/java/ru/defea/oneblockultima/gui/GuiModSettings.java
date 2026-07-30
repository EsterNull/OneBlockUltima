package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.layout.*;

import java.io.IOException;

public class GuiModSettings extends GuiScreen
{
    private static final int BUTTON_SAVE = 0;
    private static final int BUTTON_BACK = 1;
    private static final int BUTTON_H_OFFSET_DEC = 21;
    private static final int BUTTON_H_OFFSET_INC = 22;
    private static final int BUTTON_V_OFFSET_DEC = 23;
    private static final int BUTTON_V_OFFSET_INC = 24;

    private static final ResourceLocation COIN_TEXTURE = new ResourceLocation(OneBlockUltima.MODID, "textures/gui/coin.png");

    private final GuiScreen parent;
    private ModSettings.BalancePosition currentPos;
    private int hOffset;
    private int vOffset;
    private ViewFactory factory;
    private TextFieldElement hOffsetField;
    private TextFieldElement vOffsetField;

    private LabelElement positionLabel;

    public GuiModSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        ModSettings settings = ModSettings.get();
        currentPos = settings.getBalancePosition();
        hOffset = settings.getHOffset();
        vOffset = settings.getVOffset();

        int contentWidth = width - 20;
        int fieldWidth = Math.max(40, contentWidth * 5 / 100);
        String hLabel = I18n.format("gui.oneblockultima.mod_settings.h_offset");
        String vLabel = I18n.format("gui.oneblockultima.mod_settings.v_offset");

        int previewWidth = Math.min(contentWidth, 300);
        int previewHeight = Math.max(60, (height - 40) * 15 / 100);
        CustomDrawCallbackElement previewElement = new CustomDrawCallbackElement(
            (x, y, w, h, fr, mx, my, pt) -> drawPreviewAt(x, y, w, h, fr),
            previewWidth, previewHeight
        );
        previewElement.align(Alignment.CENTER);

        int gridCellSize = Math.max(16, contentWidth * 4 / 100);
        GridElement gridElement = new GridElement(3, 3)
            .cellSize(gridCellSize)
            .cellGap(4)
            .select(-1, -1)
            .renderer((x, y, cw, ch, row, col, hovered, selected, fr, mx, my, pt) -> {
                ModSettings.BalancePosition[][] g = {
                    {ModSettings.BalancePosition.TOP_LEFT, ModSettings.BalancePosition.TOP, ModSettings.BalancePosition.TOP_RIGHT},
                    {ModSettings.BalancePosition.LEFT, null, ModSettings.BalancePosition.RIGHT},
                    {ModSettings.BalancePosition.BOTTOM_LEFT, ModSettings.BalancePosition.BOTTOM, ModSettings.BalancePosition.BOTTOM_RIGHT}
                };
                if (row < 0 || row >= 3 || col < 0 || col >= 3) return;
                ModSettings.BalancePosition pos = g[row][col];
                if (pos == null) return;

                boolean isSelected = pos == currentPos;
                int bgColor;
                if (isSelected) bgColor = 0xFF2A6B35;
                else if (hovered) bgColor = 0xFF4A4A5A;
                else bgColor = 0xFF3A3A4A;

                Gui.drawRect(x, y, x + cw, y + ch, bgColor);
                Gui.drawRect(x, y, x + cw, y + 1, 0xFF666666);
                Gui.drawRect(x, y + ch - 1, x + cw, y + ch, 0xFF666666);
                Gui.drawRect(x, y, x + 1, y + ch, 0xFF666666);
                Gui.drawRect(x + cw - 1, y, x + cw, y + ch, 0xFF666666);

                String label = getPositionLabel(pos);
                int textColor = isSelected ? 0x55FF55 : 0xFFFFFF;
                int tw = fr.getStringWidth(label);
                fr.drawStringWithShadow(label, x + (cw - tw) / 2.0F, y + (ch - 8) / 2.0F, textColor);
            })
            .clickHandler((row, col, mx, my, mb) -> {
                ModSettings.BalancePosition[][] g = {
                    {ModSettings.BalancePosition.TOP_LEFT, ModSettings.BalancePosition.TOP, ModSettings.BalancePosition.TOP_RIGHT},
                    {ModSettings.BalancePosition.LEFT, null, ModSettings.BalancePosition.RIGHT},
                    {ModSettings.BalancePosition.BOTTOM_LEFT, ModSettings.BalancePosition.BOTTOM, ModSettings.BalancePosition.BOTTOM_RIGHT}
                };
                if (row >= 0 && row < 3 && col >= 0 && col < 3 && g[row][col] != null) {
                    currentPos = g[row][col];
                    positionLabel.text(I18n.format("gui.oneblockultima.mod_settings.pos." + currentPos.name().toLowerCase()));
                    return true;
                }
                return false;
            });

        factory = new ViewFactory(width, height)
            .margin(8).padding(2)
            .gap(8)
            .align(Alignment.CENTER)
            .panel(0xCC22272E, 0xFF3A3F44);

        factory.title("gui.oneblockultima.mod_settings.title");
        factory.add(previewElement);
        positionLabel = new LabelElement(I18n.format("gui.oneblockultima.mod_settings.pos." + currentPos.name().toLowerCase())).centered(true).color(0x55FF55);
        factory.add(positionLabel);

        hOffsetField = new TextFieldElement(fieldWidth)
            .text(String.valueOf(hOffset))
            .enabled(false)
            .textColor(0xE0E0E0);

        vOffsetField = new TextFieldElement(fieldWidth)
            .text(String.valueOf(vOffset))
            .enabled(false)
            .textColor(0xE0E0E0);

        int hLabelW = fontRenderer.getStringWidth(hLabel);
        int vLabelW = fontRenderer.getStringWidth(vLabel);
        int maxLabelW = Math.max(hLabelW, vLabelW);

        RowElement hControls = new RowElement(Alignment.LEFT).gap(5);
        hControls.label(hLabel, 0xC0C0C0);
        hControls.spacer(maxLabelW - hLabelW);
        hControls.button(BUTTON_H_OFFSET_DEC, "-");
        hControls.add(hOffsetField);
        hControls.button(BUTTON_H_OFFSET_INC, "+");

        RowElement vControls = new RowElement(Alignment.LEFT).gap(5);
        vControls.label(vLabel, 0xC0C0C0);
        vControls.spacer(maxLabelW - vLabelW);
        vControls.button(BUTTON_V_OFFSET_DEC, "-");
        vControls.add(vOffsetField);
        vControls.button(BUTTON_V_OFFSET_INC, "+");

        ColumnElement offsetsColumn = new ColumnElement().gap(4).align(Alignment.LEFT);
        offsetsColumn.add(hControls);
        offsetsColumn.add(vControls);

        ColumnElement gridColumn = new ColumnElement().align(Alignment.CENTER);
        gridColumn.add(gridElement);

        RowElement controlsRow = new RowElement(Alignment.CENTER).gap(15);
        controlsRow.widthPercent(80);
        controlsRow.add(gridColumn);
        controlsRow.add(offsetsColumn);
        factory.add(controlsRow);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(8);
        btnRow.widthPercent(60);
        btnRow.button(BUTTON_SAVE, I18n.format("gui.oneblockultima.save"));
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));

        factory.build(buttonList, fontRenderer);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_SAVE)
        {
            ModSettings.get().setBalancePosition(currentPos);
            ModSettings.get().setHOffset(hOffset);
            ModSettings.get().setVOffset(vOffset);
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
            if (hOffsetField != null) hOffsetField.setText(String.valueOf(hOffset));
        }
        else if (button.id == BUTTON_H_OFFSET_INC)
        {
            hOffset = Math.min(50, hOffset + 1);
            if (hOffsetField != null) hOffsetField.setText(String.valueOf(hOffset));
        }
        else if (button.id == BUTTON_V_OFFSET_DEC)
        {
            vOffset = Math.max(0, vOffset - 1);
            if (vOffsetField != null) vOffsetField.setText(String.valueOf(vOffset));
        }
        else if (button.id == BUTTON_V_OFFSET_INC)
        {
            vOffset = Math.min(50, vOffset + 1);
            if (vOffsetField != null) vOffsetField.setText(String.valueOf(vOffset));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (factory != null) factory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        if (factory != null) factory.draw(fontRenderer, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPreviewAt(int previewX, int previewY, int previewWidth, int previewHeight, net.minecraft.client.gui.FontRenderer fr)
    {
        Gui.drawRect(previewX + 1, previewY + 1, previewX + previewWidth - 1, previewY + previewHeight - 1, 0xAA111111);
        drawHorizontalLine(previewX, previewX + previewWidth, previewY, 0xFF555555);
        drawHorizontalLine(previewX, previewX + previewWidth, previewY + previewHeight, 0xFF555555);
        drawVerticalLine(previewX, previewY, previewY + previewHeight, 0xFF555555);
        drawVerticalLine(previewX + previewWidth, previewY, previewY + previewHeight, 0xFF555555);

        int chX = previewX + previewWidth / 2;
        int chY = previewY + previewHeight / 2;
        int chLen = 5;
        Gui.drawRect(chX - chLen, chY, chX + chLen + 1, chY + 1, 0xFFCCCCCC);
        Gui.drawRect(chX, chY - chLen, chX + 1, chY + chLen + 1, 0xFFCCCCCC);

        int coinSize = 6;
        int spaceBetween = 2;
        int hMargin = 5;
        int vMargin = 3;
        String sampleText = "12345";
        int textWidth = fr.getStringWidth(sampleText);
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
        fr.drawString(sampleText, boxX + hMargin + coinSize + spaceBetween, boxY + vMargin - fr.FONT_HEIGHT / 4, 0xFFD700);
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

    @Override
    public boolean doesGuiPauseGame()
    {
        return true;
    }
}

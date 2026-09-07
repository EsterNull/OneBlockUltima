package ru.defea.oneblockultima.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.layout.*;

import java.io.IOException;

import static ru.defea.oneblockultima.Constants.*;

public class GuiUiSettings extends ModScreen
{
    private static final int BUTTON_SAVE = 0;
    private static final int BUTTON_BACK = 1;
    private static final int BUTTON_SHOW_BALANCE = 25;

    private final Screen parent;
    private ModSettings.BalancePosition currentPos;
    private ModSettings settings;
    private int hOffset;
    private int vOffset;
    private boolean isShowBalance;
    private ViewFactory factory;
    private ButtonToggleElement showBalanceToggle;

    private LabelElement positionLabel;
    private StepperElement hStepper;
    private StepperElement vStepper;
    private int[] sessionHOffset;
    private int[] sessionVOffset;
    private GuiGraphics gfx;

    public GuiUiSettings(Screen parent)
    {
        super(Component.literal(""));
        this.parent = parent;
    }

    @Override
    public void init()
    {
        
        settings = ModSettings.get();
        currentPos = settings.getBalancePosition();
        ModSettings.BalancePosition[] positions = ModSettings.BalancePosition.values();
        sessionHOffset = new int[positions.length];
        sessionVOffset = new int[positions.length];
        for (int i = 0; i < positions.length; i++)
        {
            sessionHOffset[i] = settings.getHOffset(positions[i]);
            sessionVOffset[i] = settings.getVOffset(positions[i]);
        }
        hOffset = sessionHOffset[currentPos.ordinal()];
        vOffset = sessionVOffset[currentPos.ordinal()];
        isShowBalance = settings.isShowBalance();

        buildView();
    }

    private void buildView() {
        int contentWidth = width - 20;
        int fieldWidth = Math.max(40, contentWidth / 20);
        String hLabel = I18n.get("gui.oneblockultima.ui_settings.h_offset");
        String vLabel = I18n.get("gui.oneblockultima.ui_settings.v_offset");
        String showBalanceLabel = I18n.get("gui.oneblockultima.ui_settings.show_balance");

        int previewWidth = contentWidth * 4 / 5;
        int previewHeight = Math.max(60, height * 3 / 20 - 6);
        CustomDrawCallbackElement previewElement = new CustomDrawCallbackElement(
                (g, x, y, w, h, fr, mx, my, pt) -> drawPreviewAt(g, x, y, w, h, fr),
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
                    if (isSelected) bgColor = hovered ? SUCCESS_HOVERED_COLOR : DARK_GREEN;
                    else if (hovered) bgColor = GRAY_COLOR_6;
                    else bgColor = DARK_GRAY_COLOR_1;

                    gfx.fill(x, y, x + cw, y + ch, bgColor);
                    gfx.fill(x, y, x + cw, y + 1, GRAY_COLOR_2);
                    gfx.fill(x, y + ch - 1, x + cw, y + ch, GRAY_COLOR_2);
                    gfx.fill(x, y, x + 1, y + ch, GRAY_COLOR_2);
                    gfx.fill(x + cw - 1, y, x + cw, y + ch, GRAY_COLOR_2);

                    String label = getPositionLabel(pos);
                    int textColor = isSelected ? SUCCESS_COLOR : WHITE_COLOR_1;
                    int tw = fr.width(label);
                    gfx.drawString(fr, label, (int)(x + (cw - tw) / 2.0F), (int)(y + (ch - 8) / 2.0F), textColor, true);
                })
                .clickHandler((row, col, mx, my, mb) -> {
                    ModSettings.BalancePosition[][] g = {
                            {ModSettings.BalancePosition.TOP_LEFT, ModSettings.BalancePosition.TOP, ModSettings.BalancePosition.TOP_RIGHT},
                            {ModSettings.BalancePosition.LEFT, null, ModSettings.BalancePosition.RIGHT},
                            {ModSettings.BalancePosition.BOTTOM_LEFT, ModSettings.BalancePosition.BOTTOM, ModSettings.BalancePosition.BOTTOM_RIGHT}
                    };
                    if (row >= 0 && row < 3 && col >= 0 && col < 3 && g[row][col] != null) {
                        sessionHOffset[currentPos.ordinal()] = hOffset;
                        sessionVOffset[currentPos.ordinal()] = vOffset;
                        currentPos = g[row][col];
                        positionLabel.text(I18n.get("gui.oneblockultima.ui_settings.pos." + currentPos.name().toLowerCase()));
                        hStepper.setMin(hOffsetMin());
                        vStepper.setMin(vOffsetMin());
                        hOffset = sessionHOffset[currentPos.ordinal()];
                        vOffset = sessionVOffset[currentPos.ordinal()];
                        hStepper.setValue(hOffset);
                        vStepper.setValue(vOffset);
                        hOffset = hStepper.getValue();
                        vOffset = vStepper.getValue();
                        return true;
                    }
                    return false;
                });

        factory = new ViewFactory(width, height)
                .margin(8).padding(2)
                .gap(8)
                .align(Alignment.CENTER);

        factory.title("gui.oneblockultima.ui_settings.title");
        factory.add(previewElement);
        positionLabel = new LabelElement(I18n.get("gui.oneblockultima.ui_settings.pos." + currentPos.name().toLowerCase())).centered().color(SUCCESS_COLOR);
        factory.add(positionLabel);

        RowElement toggleControls = new RowElement(Alignment.LEFT).gap(5).widthPercent(70);
        showBalanceToggle = toggleControls.buttonToggle(BUTTON_SHOW_BALANCE, isShowBalance).label(showBalanceLabel)
                .onPress(() -> { isShowBalance = !isShowBalance; showBalanceToggle.toggle(); });
        factory.add(toggleControls);

        StepperElement hStepper = new StepperElement()
                .value(hOffset)
                .min(hOffsetMin())
                .max(50)
                .step(1)
                .fieldWidth(fieldWidth)
                .onChange(v -> hOffset = v);

        StepperElement vStepper = new StepperElement()
                .value(vOffset)
                .min(vOffsetMin())
                .max(50)
                .step(1)
                .fieldWidth(fieldWidth)
                .onChange(v -> vOffset = v);

        this.hStepper = hStepper;
        this.vStepper = vStepper;

        hOffset = hStepper.getValue();
        vOffset = vStepper.getValue();

        int hLabelW = Minecraft.getInstance().font.width(hLabel);
        int vLabelW = Minecraft.getInstance().font.width(vLabel);
        int maxLabelW = Math.max(hLabelW, vLabelW);

        RowElement hControls = new RowElement(Alignment.LEFT).gap(5);
        hControls.label(hLabel);
        hControls.spacer(maxLabelW - hLabelW);
        hControls.add(hStepper);

        RowElement vControls = new RowElement(Alignment.LEFT).gap(5);
        vControls.label(vLabel);
        vControls.spacer(maxLabelW - vLabelW);
        vControls.add(vStepper);

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
        btnRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.cancel")).onPress(() -> Minecraft.getInstance().setScreen(parent));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.get("gui.oneblockultima.save")).onPress(() -> {
            sessionHOffset[currentPos.ordinal()] = hOffset;
            sessionVOffset[currentPos.ordinal()] = vOffset;
            settings.setBalancePosition(currentPos);
            settings.setAllPositionOffsets(sessionHOffset, sessionVOffset);
            settings.setShowBalance(isShowBalance);
            Minecraft.getInstance().setScreen(parent);
        }));

        factory.build(this, Minecraft.getInstance().font);
        for (net.minecraft.client.gui.components.AbstractWidget w : factory.getWidgets()) {
            this.addRenderableWidget(w);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        if (factory != null && factory.mouseClicked((int) mouseX, (int) mouseY, mouseButton)) return true;
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        this.gfx = g;
        drawModBackground(g);
        if (factory != null) factory.draw(g, Minecraft.getInstance().font, mouseX, mouseY, partialTicks);
        for (var r : this.renderables) r.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick()
    {
        if (factory != null) factory.updateScreen();
        super.tick();
    }

    private void drawPreviewAt(GuiGraphics g, int previewX, int previewY, int previewWidth, int previewHeight, net.minecraft.client.gui.Font fr)
    {
        g.fill(previewX + 1, previewY + 1, previewX + previewWidth - 1, previewY + previewHeight - 1, TRANSPARENT_DARK_GRAY_COLOR_1);
        g.hLine(previewX, previewX + previewWidth, previewY, GRAY_COLOR_3);
        g.hLine(previewX, previewX + previewWidth, previewY + previewHeight, GRAY_COLOR_3);
        g.vLine(previewX, previewY, previewY + previewHeight, GRAY_COLOR_3);
        g.vLine(previewX + previewWidth, previewY, previewY + previewHeight, GRAY_COLOR_3);

        int chX = previewX + previewWidth / 2;
        int chY = previewY + previewHeight / 2;
        int chLen = 5;
        g.fill(chX - chLen, chY, chX + chLen + 1, chY + 1, LIGHT_GRAY_COLOR_1);
        g.fill(chX, chY - chLen, chX + 1, chY + chLen + 1, LIGHT_GRAY_COLOR_1);

        if (!isShowBalance) return;

        int coinSize = 6;
        int spaceBetween = 2;
        int hMargin = 5;
        int vMargin = 3;
        String sampleText = "12345.25";
        int textWidth = fr.width(sampleText);
        int boxW = coinSize + textWidth + spaceBetween + hMargin * 2;
        int boxH = coinSize + vMargin * 2;

        int innerX = previewX + 1;
        int innerY = previewY + 1;
        int innerW = previewWidth - 2;
        int innerH = previewHeight - 2;

        g.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        int areaX = innerX + 2;
        int areaY = innerY + 2;
        int areaW = innerW - 4;
        int areaH = innerH - 4;

        int[] placed = ModSettings.computeBalanceBox(currentPos, hOffset, vOffset, boxW, boxH, areaW, areaH);
        int boxX = areaX + placed[0];
        int boxY = areaY + placed[1];

        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, TRANSPARENT_DARK_GRAY_COLOR_2);
        g.fill(boxX, boxY, boxX + boxW, boxY + 1, GRAY_COLOR_7);
        g.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, GRAY_COLOR_7);
        g.fill(boxX, boxY, boxX + 1, boxY + boxH, GRAY_COLOR_7);
        g.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, GRAY_COLOR_7);
        TextureElement coinIcon = new TextureElement(COIN_TEXTURE, coinSize, coinSize);
        coinIcon.setComputedPosition(boxX + hMargin, boxY + vMargin);
        coinIcon.setComputedSize(coinSize, coinSize);
        coinIcon.draw(g, fr, 0, 0, 0);
        g.drawString(fr, sampleText, boxX + hMargin + coinSize + spaceBetween, boxY + vMargin, GOLD_COLOR, true);

        g.disableScissor();
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

    private boolean isVerticalCentered(ModSettings.BalancePosition pos)
    {
        return pos == ModSettings.BalancePosition.LEFT || pos == ModSettings.BalancePosition.RIGHT;
    }

    private boolean isHorizontalCentered(ModSettings.BalancePosition pos)
    {
        return pos == ModSettings.BalancePosition.TOP || pos == ModSettings.BalancePosition.BOTTOM;
    }

    private int hOffsetMin()
    {
        return isHorizontalCentered(currentPos) ? -50 : 0;
    }

    private int vOffsetMin()
    {
        return isVerticalCentered(currentPos) ? -50 : 0;
    }
}

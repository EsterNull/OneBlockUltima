package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.BlockElement;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.ColumnElement;
import ru.defea.oneblockultima.gui.layout.CustomDrawCallbackElement;
import ru.defea.oneblockultima.gui.layout.EntityRendererElement;
import ru.defea.oneblockultima.gui.layout.FluidElement;
import ru.defea.oneblockultima.gui.layout.RowElement;
import ru.defea.oneblockultima.gui.layout.ScrollbarElement;
import ru.defea.oneblockultima.gui.layout.SpacerElement;
import ru.defea.oneblockultima.gui.layout.TabBarElement;
import ru.defea.oneblockultima.gui.layout.TextureElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.gui.layout.ViewSwitcherElement;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.util.BlockUtil;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.*;

import static ru.defea.oneblockultima.Constants.*;
import static ru.defea.oneblockultima.util.BlockUtil.isFullBlock;

public class GuiOneBlock extends GuiContainer
{
    private static final int BUTTON_PREV_SET = 0;
    private static final int BUTTON_NEXT_SET = 1;
    private static final int BUTTON_SELECT_SET = 2;
    private static final int BUTTON_UPGRADE_SET = 3;
    private static final int BUTTON_TAB_SETS = 4;
    private static final int BUTTON_TAB_SETTINGS = 5;
    private static final int BUTTON_OPEN_CONFIG_EDITOR = 6;
    private static final int BUTTON_TOGGLE_FLUIDS = 7;
    private static final int BUTTON_TOGGLE_MOBS = 8;
    private static final int BUTTON_TOGGLE_CHESTS = 9;
    private static final int BUTTON_TOGGLE_SAPLINGS = 10;
    private static final int BUTTON_OPEN_PRICES = 12;
    private static final int BUTTON_OPEN_UI_SETTINGS = 13;
    private static final int BUTTON_OPEN_MISC_SETTINGS = 14;
    private static final int BUTTON_TAB_DONATE = 999;
    private static final int BUTTON_DONATE_BASE = 1000;
    private static final int VIEW_SETS = 0;
    private static final int VIEW_SETTINGS = 1;
    private static final int VIEW_DONATE = 2;

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int FACTORY_GAP = 8;
    private static final int TAB_ROW_Y = 27;
    private static final int TAB_HEIGHT = 20;
    private static final int CONTENT_TOP_PADDING = 2;
    private static final int MIN_PANEL_HEIGHT = 56;

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int INNER_PADDING = 6;
    private static final int SECTION_GAP = 8;
    private static final int BACKGROUND_TEXTURE_SIZE = 32;

    private static final int DONATE_BUTTON_GAP = 12;

    private static final int SB_BLOCK_LEFT = 0;
    private static final int SB_BLOCK_RIGHT = 1;
    private static final int SB_MOB_LEFT = 2;
    private static final int SB_MOB_RIGHT = 3;

    private final ContainerOneBlock container;
    private final List<BlockSetConfig.BlockSetDefinition> visibleSets = new ArrayList<>();
    private int selectedSetIndex = 0;
    private ButtonElement<?> selectButton;
    private ButtonElement<?> upgradeButton;
    private ButtonElement<?> toggleFluidButton;
    private ButtonElement<?> toggleMobsButton;
    private ButtonElement<?> toggleChestsButton;
    private ButtonElement<?> toggleSaplingsButton;
    private ButtonElement<?>[] donateButtons;
    private Boolean pendingDisableFluid = null;
    private Boolean pendingDisableMob = null;
    private Boolean pendingDisableChest = null;
    private Boolean pendingDisableSapling = null;
    private int activeView = VIEW_SETS;
    private boolean showModSettingsButtons = true;
    private int blockScroll = 0;
    private int mobScroll = 0;
    private int blockScrollNext = 0;
    private int mobScrollNext = 0;
    private int conditionsScroll = 0;
    private int conditionsStartX = 0;
    private int conditionsColumnWidth = 0;
    private ScrollbarElement conditionsScrollbar;

    private final ScrollbarElement[] scrollbars = new ScrollbarElement[4];
    private final boolean[] scrollbarActive = new boolean[4];
    private boolean donateJustCopied = false;

    private BlockSetConfig.BlockEntryDefinition hoveredEntryLeft = null;
    private ItemStack hoveredStackLeft = ItemStack.EMPTY;
    private BlockSetConfig.MobEntryDefinition hoveredMobEntryLeft = null;
    private String hoveredMobNameLeft = null;

    private BlockSetConfig.BlockEntryDefinition hoveredEntryRight = null;
    private ItemStack hoveredStackRight = ItemStack.EMPTY;
    private BlockSetConfig.MobEntryDefinition hoveredMobEntryRight = null;
    private String hoveredMobNameRight = null;

    private int cellSize = 18;
    private int cellPadding = 1;
    private int blockCols = 4;
    private int mobCols = 2;
    private String clientActiveSetId = null;

    private final List<BlockSetConfig.BlockEntryDefinition> backgroundBlocks = new ArrayList<>();
    private final Map<BlockSetConfig.BlockEntryDefinition, TextureAtlasSprite> backgroundSpriteCache = new HashMap<>();
    private final Map<BlockSetConfig.MobEntryDefinition, Entity> mobEntityCache = new HashMap<>();

    private ViewFactory factory;
    private ViewSwitcherElement switcher;
    private TabBarElement tabs;
    private CustomDrawCallbackElement headerElement;
    private CustomDrawCallbackElement infoElement;
    private CustomDrawCallbackElement panelsElement;

    public GuiOneBlock(EntityPlayer player, World world, BlockPos generatorPos)
    {
        super(new ContainerOneBlock(player, world, generatorPos));
        this.container = (ContainerOneBlock) this.inventorySlots;
        this.xSize = 360;
        this.ySize = 280;
    }

    private int getRowInterval()
    {
        return fontRenderer.FONT_HEIGHT + 4;
    }

    private int getContentAreaWidth()
    {
        int horizontalMargin = Math.max(12, Math.min(24, xSize / 24));
        return Math.max(120, xSize - horizontalMargin * 2);
    }

    private int computeTabBarWidth()
    {
        return xSize;
    }

    private int computeInfoHeight()
    {
        if (visibleSets.isEmpty())
        {
            return fontRenderer.FONT_HEIGHT * 2 + 16;
        }
        BlockSetConfig.BlockSetDefinition set = getBlockSetDefinition();
        if (set == null)
        {
            return fontRenderer.FONT_HEIGHT * 2 + 16;
        }

        TileEntityOneBlockGenerator generator = container.getGenerator();
        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);

        String setTitle = getLocalizedSetName(set) + " " + I18n.format("gui.oneblockultima.lv") + currentLevel;
        if (currentLevel <= 0)
        {
            setTitle += " (" + I18n.format("gui.oneblockultima.locked") + ")";
        }
        else if (set.id.equals(clientActiveSetId))
        {
            setTitle += " (" + I18n.format("gui.oneblockultima.selected") + ")";
        }

        int infoInternalWidth = Math.max(80, getContentAreaWidth() - 16);
        int lines = fontRenderer.listFormattedStringToWidth(setTitle, infoInternalWidth).size();

        String statusText;
        if (currentLevel <= 0)
        {
            statusText = I18n.format("gui.oneblockultima.unlock_cost") + ": " + set.unlockCost;
        }
        else
        {
            BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
            statusText = nextLevel != null
                    ? I18n.format("gui.oneblockultima.upgrade_cost") + ": " + nextLevel.upgradeCost
                    : I18n.format("gui.oneblockultima.max_level");
        }
        lines += fontRenderer.listFormattedStringToWidth(statusText, infoInternalWidth).size();

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(container.getPlayer());
        String brokenText = I18n.format("gui.oneblockultima.blocks_broken") + ": " +
                (data != null ? data.getBrokenBlocksCount(set.id) : 0);
        lines += fontRenderer.listFormattedStringToWidth(brokenText, infoInternalWidth).size();

        if (data != null && currentLevel <= 0 && set.unlockConditions != null &&
                !set.unlockConditions.conditions.isEmpty())
        {
            lines += 1 + set.unlockConditions.conditions.size();
        }

        return capInfoHeight(lines * fontRenderer.FONT_HEIGHT + Math.max(0, lines - 1) * 4 + 8);
    }

    private int capInfoHeight(int desiredHeight)
    {
        int switcherHeight = ySize - TAB_ROW_Y - TAB_HEIGHT;
        int rowArea = BUTTON_HEIGHT * 2 + FACTORY_GAP * 3;
        int maxAllowed = switcherHeight - CONTENT_TOP_PADDING - FACTORY_GAP - rowArea - MIN_PANEL_HEIGHT;
        return Math.min(desiredHeight, Math.max(8, maxAllowed));
    }

    @Override
    public void initGui()
    {
        this.xSize = this.width - 40;
        this.ySize = this.height - 40;
        super.initGui();
        buttonList.clear();

        visibleSets.clear();
        for (BlockSetConfig.BlockSetDefinition set : BlockSetConfig.get().getSets())
        {
            if (set != null && set.isAvailable())
            {
                visibleSets.add(set);
            }
        }

        refreshActiveSetFromGenerator();

        String setIdToFind = clientActiveSetId;
        selectedSetIndex = 0;
        if (setIdToFind != null)
        {
            for (int i = 0; i < visibleSets.size(); i++)
            {
                if (visibleSets.get(i).id.equals(setIdToFind))
                {
                    selectedSetIndex = i;
                    break;
                }
            }
        }

        showModSettingsButtons = mc.isIntegratedServerRunning();

        buildView();
        initBackgroundBlocks();
    }

    private void rebuildView()
    {
        buttonList.clear();
        buildView();
        initBackgroundBlocks();
    }

    private void buildView()
    {
        if (factory == null || factory.getScreenWidth() != xSize || factory.getScreenHeight() != ySize)
        {
            factory = new ViewFactory(xSize, ySize)
                    .margin(0).padding(0)
                    .gap(0)
                    .align(Alignment.CENTER)
                    .panel(0, 0);
            headerElement = new CustomDrawCallbackElement(this::drawHeader, 0, TAB_ROW_Y).widthPercent(100);
            factory.add(headerElement);
            tabs = new TabBarElement().width(computeTabBarWidth());
            factory.add(tabs);
            switcher = new ViewSwitcherElement().width(getContentAreaWidth()).flexible(true);
            factory.add(switcher);
        }
        else
        {
            tabs = new TabBarElement().width(computeTabBarWidth());
            factory.getElements().set(1, tabs);
        }

        tabs.tab(BUTTON_TAB_SETS, I18n.format("gui.oneblockultima.tabs.sets"));
        tabs.tab(BUTTON_TAB_SETTINGS, I18n.format("gui.oneblockultima.tabs.settings"));
        tabs.tab(BUTTON_TAB_DONATE, I18n.format("gui.oneblockultima.tabs.donate"));

        ColumnElement view = new ColumnElement().gap(FACTORY_GAP);
        view.add(new SpacerElement(CONTENT_TOP_PADDING));
        if (activeView == VIEW_SETS)
        {
            buildSetsView(view);
        }
        else if (activeView == VIEW_SETTINGS)
        {
            buildSettingsView(view);
        }
        else
        {
            buildDonateView(view);
        }

        switcher.replaceView(activeView, view);
        switcher.setView(activeView);
        updateViewButtons();
        factory.build(buttonList, fontRenderer, guiLeft, guiTop, xSize, ySize);
    }

    private void buildSetsView(ColumnElement view)
    {
        CustomDrawCallbackElement infoElement = new CustomDrawCallbackElement(this::drawInfo, 0, computeInfoHeight()).widthPercent(100);
        this.infoElement = infoElement;
        view.add(infoElement);

        //noinspection SuspiciousNameCombination
        int buttonSetChangerWidth = BUTTON_HEIGHT;
        RowElement row1 = view.row(Alignment.LEFT);
        row1.button(BUTTON_PREV_SET, "<").width(buttonSetChangerWidth).height(BUTTON_HEIGHT);
        row1.add(new SpacerElement(6, 0));
        row1.button(BUTTON_NEXT_SET, ">").width(buttonSetChangerWidth).height(BUTTON_HEIGHT);

        RowElement row2 = view.row(Alignment.SPACE_BETWEEN).stretchToContent().gap(BUTTON_GAP);
        int selectWidth = (getContentAreaWidth() - BUTTON_GAP) / 2;
        selectButton = row2.button(BUTTON_SELECT_SET, I18n.format("gui.oneblockultima.select")).width(selectWidth).height(BUTTON_HEIGHT);
        upgradeButton = row2.button(BUTTON_UPGRADE_SET, I18n.format("gui.oneblockultima.upgrade")).width(selectWidth).height(BUTTON_HEIGHT);

        panelsElement = new CustomDrawCallbackElement(this::drawPanels, 0, 0).flexible(true);
        view.add(panelsElement);
    }

    private void buildSettingsView(ColumnElement view)
    {
        int contentWidth = getContentAreaWidth();
        int buttonWidth = (contentWidth - BUTTON_GAP) / 2;

        RowElement row1 = view.row(Alignment.SPACE_BETWEEN).stretchToContent();
        toggleFluidButton = row1.button(BUTTON_TOGGLE_FLUIDS, "").width(buttonWidth).height(BUTTON_HEIGHT);
        toggleMobsButton = row1.button(BUTTON_TOGGLE_MOBS, "").width(buttonWidth).height(BUTTON_HEIGHT);

        RowElement row2 = view.row(Alignment.SPACE_BETWEEN).stretchToContent();
        toggleChestsButton = row2.button(BUTTON_TOGGLE_CHESTS, "").width(buttonWidth).height(BUTTON_HEIGHT);
        toggleSaplingsButton = row2.button(BUTTON_TOGGLE_SAPLINGS, "").width(buttonWidth).height(BUTTON_HEIGHT);

        if (showModSettingsButtons)
        {
            RowElement row3 = view.row(Alignment.SPACE_BETWEEN).stretchToContent();
            row3.button(BUTTON_OPEN_PRICES, I18n.format("gui.oneblockultima.settings.open_prices")).width(buttonWidth).height(BUTTON_HEIGHT);
            row3.button(BUTTON_OPEN_CONFIG_EDITOR, I18n.format("gui.oneblockultima.config.sets_title")).width(buttonWidth).height(BUTTON_HEIGHT);

            RowElement row4 = view.row(Alignment.SPACE_BETWEEN).stretchToContent();
            row4.button(BUTTON_OPEN_UI_SETTINGS, I18n.format("gui.oneblockultima.ui_settings.title")).width(buttonWidth).height(BUTTON_HEIGHT);
            row4.button(BUTTON_OPEN_MISC_SETTINGS, I18n.format("gui.oneblockultima.misc.title")).width(buttonWidth).height(BUTTON_HEIGHT);
        }
    }

    private void buildDonateView(ColumnElement view)
    {
        CustomDrawCallbackElement donateTextElement = new CustomDrawCallbackElement(this::drawDonateText, 0, getRowInterval() * 4 - 4).widthPercent(100);
        view.add(donateTextElement);

        int contentWidth = getContentAreaWidth();
        RowElement contentRow = view.row(Alignment.LEFT);
        ColumnElement qrCol = new ColumnElement().align(Alignment.CENTER);
        qrCol.add(new TextureElement(SBP_TEXTURE, 64, 64));
        qrCol.label("SBP", WHITE_COLOR_1);
        contentRow.add(qrCol);

        ColumnElement buttonsCol = new ColumnElement().align(Alignment.LEFT).gap(DONATE_BUTTON_GAP);
        int donateBtnWidth = Math.max(120, contentWidth - 72 - 4);
        donateButtons = new ButtonElement<?>[DonateMethod.METHODS.length];
        for (int i = 0; i < DonateMethod.METHODS.length; i++)
        {
            donateButtons[i] = buttonsCol.button(BUTTON_DONATE_BASE + i, DonateMethod.METHODS[i].text)
                    .width(donateBtnWidth).height(BUTTON_HEIGHT);
        }
        contentRow.add(buttonsCol);

        view.add(new SpacerElement(0).flexible(true));
    }

    private void drawHeader(int x, int y, int width, int height, FontRenderer fr, int mouseX, int mouseY, float partialTicks)
    {
        drawRect(x, y, x + width, y + height, DARK_GRAY_COLOR_3);

        String title = I18n.format("tile.one_block_generator.name");
        drawCenteredString(fr, title, x + width / 2, y + fr.FONT_HEIGHT, WHITE_COLOR_1);

        IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(container.getPlayer());
        int brokenTotal = playerData == null ? 0 : playerData.getBrokenBlocksCount();
        String brokenLabel = I18n.format("gui.oneblockultima.blocks_broken_total") + ":";
        int brokenX = x + 10;
        int brokenY = y + fr.FONT_HEIGHT;
        fr.drawString(brokenLabel, brokenX, brokenY, LIGHT_GRAY_COLOR_2);
        fr.drawString(String.valueOf(brokenTotal), brokenX + fr.getStringWidth(brokenLabel) + 2, brokenY, WHITE_COLOR_1);

        double currency = ru.defea.oneblockultima.event.ModEventsClient.getDisplayedCurrency(container.getPlayer());
        String balanceValue = ru.defea.oneblockultima.event.ModEventsClient.formatCurrency(currency);
        int iconSize = 12;
        int padding = 4;
        int balanceWidth = fr.getStringWidth(balanceValue);
        int boxWidth = iconSize + 2 + balanceWidth + padding * 2;
        int boxX = x + width - 10 - boxWidth;
        int boxY = y + 4;
        int iconX = boxX + padding;
        int iconY = boxY + padding;
        int numberX = iconX + iconSize + 2;
        int numberY = iconY + 2;

        TextureElement coinIcon = new TextureElement(COIN_TEXTURE, iconSize, iconSize);
        coinIcon.setComputedPosition(iconX, iconY);
        coinIcon.setComputedSize(iconSize, iconSize);
        coinIcon.draw(fr, mouseX, mouseY, partialTicks);
        fr.drawString(balanceValue, numberX, numberY, GOLD_COLOR);
    }

    private void drawInfo(int x, int y, int width, int height, FontRenderer fr, int mouseX, int mouseY, float partialTicks)
    {
        if (visibleSets.isEmpty())
        {
            return;
        }
        if (height <= 0)
        {
            return;
        }
        BlockSetConfig.BlockSetDefinition set = getBlockSetDefinition();
        if (set == null)
        {
            return;
        }

        TileEntityOneBlockGenerator generator = container.getGenerator();
        String activeSetId = generator == null ? null : generator.getSelectedSetId();
        String activeSetName = "-";
        if (activeSetId != null)
        {
            BlockSetConfig.BlockSetDefinition activeSetDef = BlockSetConfig.get().getSet(activeSetId);
            activeSetName = activeSetDef == null ? activeSetId : getLocalizedSetName(activeSetDef);
        }
        String activeSetString = I18n.format("gui.oneblockultima.active_set");
        int rightTextX = x + width - fr.getStringWidth(activeSetString);
        fr.drawString(activeSetString + ":", rightTextX, y, LIGHT_BLUE_GRAY_COLOR);
        fr.drawString(activeSetName, rightTextX, y + fr.FONT_HEIGHT + 2, WHITE_COLOR_1);

        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
        boolean isActiveSet = set.id.equals(clientActiveSetId);
        int setColor = currentLevel <= 0 ? REDDISH_COLOR : isActiveSet ? GREENISH_COLOR : WHITE_COLOR_1;
        String setTitle = getLocalizedSetName(set) + " " + I18n.format("gui.oneblockultima.lv") + currentLevel;
        if (isActiveSet)
        {
            setTitle += " (" + I18n.format("gui.oneblockultima.selected") + ")";
        }
        else if (currentLevel <= 0)
        {
            setTitle += " (" + I18n.format("gui.oneblockultima.locked") + ")";
        }
        fr.drawString(setTitle, x, y, setColor);

        int statusY = y + getRowInterval();
        if (currentLevel <= 0)
        {
            fr.drawString(I18n.format("gui.oneblockultima.unlock_cost") + ": " + set.unlockCost, x + 4, statusY, LIGHT_GRAY_COLOR_2);
        }
        else
        {
            BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
            if (nextLevel != null)
            {
                fr.drawString(I18n.format("gui.oneblockultima.upgrade_cost") + ": " + nextLevel.upgradeCost, x + 4, statusY, LIGHT_GRAY_COLOR_2);
            }
            else
            {
                fr.drawString(I18n.format("gui.oneblockultima.max_level"), x + 4, statusY, LIGHT_GRAY_COLOR_2);
            }
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(container.getPlayer());
        if (data != null)
        {
            fr.drawString(I18n.format("gui.oneblockultima.blocks_broken") + ": " + data.getBrokenBlocksCount(set.id),
                    x + 4, y + getRowInterval() * 2, LIGHT_GRAY_COLOR_2);
        }

        if (data != null && currentLevel <= 0 && set.unlockConditions != null &&
                !set.unlockConditions.conditions.isEmpty())
        {
            drawUnlockConditions(set, x, y, width, height + BUTTON_HEIGHT, generator);
        }

        if (selectButton != null)
        {
            selectButton.text(currentLevel <= 0 ? I18n.format("gui.oneblockultima.locked") :
                    (isActiveSet ? I18n.format("gui.oneblockultima.selected") : I18n.format("gui.oneblockultima.select")));
            selectButton.enabled(currentLevel > 0 && !isActiveSet);
        }
        if (upgradeButton != null)
        {
            if (currentLevel <= 0)
            {
                upgradeButton.text(I18n.format("gui.oneblockultima.unlock"));
                upgradeButton.enabled(true);
            }
            else
            {
                BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
                if (nextLevel != null)
                {
                    upgradeButton.text(I18n.format("gui.oneblockultima.upgrade"));
                    upgradeButton.enabled(true);
                }
                else
                {
                    upgradeButton.text(I18n.format("gui.oneblockultima.max"));
                    upgradeButton.enabled(false);
                }
            }
        }
    }

    private void drawPanels(int x, int y, int width, int height, FontRenderer fr, int mouseX, int mouseY, float partialTicks)
    {
        if (visibleSets.isEmpty())
        {
            return;
        }
        if (height <= 0)
        {
            return;
        }
        BlockSetConfig.BlockSetDefinition set = getBlockSetDefinition();
        if (set == null)
        {
            return;
        }

        TileEntityOneBlockGenerator generator = container.getGenerator();
        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
        boolean canShowCurrent = currentLevel > 0;

        int panelGap = Math.max(4, width / 40);
        int panelWidth = (width - panelGap) / 2;
        calculateColumns(panelWidth);

        int rightPanelX = x + panelWidth + panelGap;

        if (canShowCurrent)
        {
            BlockSetConfig.SetLevelDefinition currentDef = set.getLevel(currentLevel);
            renderLevelPanel(currentDef, x, y, true, mouseX, mouseY);
        }
        int rightStartX = canShowCurrent ? rightPanelX : x;
        BlockSetConfig.SetLevelDefinition nextDef = set.getLevel(currentLevel <= 0 ? 1 : currentLevel + 1);
        renderLevelPanel(nextDef, rightStartX, y, false, mouseX, mouseY);

        if (canShowCurrent && nextDef != null)
        {
            int separatorX = x + panelWidth + panelGap / 2;
            int separatorTop = y + 5;
            int separatorBottom = y + height - 5;
            drawRect(separatorX, separatorTop, separatorX + 1, separatorBottom, GRAY_COLOR_8);
        }
    }

    private void drawDonateText(int x, int y, int width, int height, FontRenderer fr, int mouseX, int mouseY, float partialTicks)
    {
        int centerX = x + width / 2;
        int yy = y;

        String thankYou = I18n.format("gui.oneblockultima.donate.thank_you");
        drawCenteredString(fr, thankYou, centerX, yy, SUCCESS_COLOR);
        yy += getRowInterval();

        String line1 = I18n.format("gui.oneblockultima.donate.line1");
        drawCenteredString(fr, line1, centerX, yy, LIGHT_GRAY_COLOR_1);
        yy += getRowInterval();

        String line2 = I18n.format("gui.oneblockultima.donate.line2");
        drawCenteredString(fr, line2, centerX, yy, LIGHT_GRAY_COLOR_1);
        yy += getRowInterval();

        String line3 = I18n.format("gui.oneblockultima.donate.line3");
        drawCenteredString(fr, line3, centerX, yy, LIGHT_GRAY_COLOR_1);
    }

    private void initBackgroundBlocks()
    {
        backgroundBlocks.clear();
        backgroundSpriteCache.clear();
        mobEntityCache.clear();

        BlockSetConfig.BlockSetDefinition currentSet = getBlockSetDefinition();

        if (currentSet != null)
        {
            currentSet.ensureComputedLevels();
            Map<Integer, BlockSetConfig.SetLevelDefinition> levels = currentSet.computedLevels;
            if (levels != null)
            {
                for (BlockSetConfig.SetLevelDefinition level : levels.values())
                {
                    if (level.blocks == null) continue;
                    for (BlockSetConfig.BlockEntryDefinition block : level.blocks)
                    {
                        if (block == null) continue;

                        net.minecraft.block.Block mcBlock = block.resolveBlock();
                        if (mcBlock == null) continue;

                        if (!isFullBlock(mcBlock, block.meta)) continue;

                        backgroundBlocks.add(block);
                    }
                }
            }
        }

        if (backgroundBlocks.isEmpty())
        {
            OneBlockUltima.getLogger().warn("No blocks found for background, adding defaults");
            addDefaultBackgroundBlocks();
        }
    }

    private void addDefaultBackgroundBlocks()
    {
        addBlockIfFull("minecraft:stone", 0);
        addBlockIfFull("minecraft:dirt", 0);
        addBlockIfFull("minecraft:cobblestone", 0);
        addBlockIfFull("minecraft:planks", 0);
        addBlockIfFull("minecraft:sand", 0);
        addBlockIfFull("minecraft:gravel", 0);
        addBlockIfFull("minecraft:netherrack", 0);
        addBlockIfFull("minecraft:end_stone", 0);
        addBlockIfFull("minecraft:bricks", 0);
        addBlockIfFull("minecraft:stonebrick", 0);
        addBlockIfFull("minecraft:quartz_block", 0);
    }

    @SuppressWarnings("SameParameterValue")
    private void addBlockIfFull(String registry, int meta)
    {
        try
        {
            net.minecraft.block.Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registry));
            if (block != null && isFullBlock(block, meta))
            {
                backgroundBlocks.add(createBlockEntry(registry, meta));
            }
        }
        catch (Exception ignored) {}
    }

    private BlockSetConfig.BlockEntryDefinition createBlockEntry(String registry, int meta)
    {
        BlockSetConfig.BlockEntryDefinition entry = new BlockSetConfig.BlockEntryDefinition();
        entry.registry = registry;
        entry.meta = meta;
        entry.chance = 100;
        return entry;
    }

    private void renderProceduralBackground(int startX, int startY, int width, int height)
    {
        if (backgroundBlocks.isEmpty() || width <= 0 || height <= 0)
        {
            return;
        }

        int totalBlocks = backgroundBlocks.size();
        int texSize = BACKGROUND_TEXTURE_SIZE;

        int cols = (width + texSize - 1) / texSize;
        int rows = (height + texSize - 1) / texSize;

        int extraCols = 2;
        int extraRows = 2;

        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        try
        {
            for (int row = -extraRows; row <= rows + extraRows; row++)
            {
                for (int col = -extraCols; col <= cols + extraCols; col++)
                {
                    int blockIndex = ((row + col) * 7 + col * 3) % totalBlocks;
                    if (blockIndex < 0) blockIndex += totalBlocks;

                    BlockSetConfig.BlockEntryDefinition entry = backgroundBlocks.get(blockIndex);
                    if (entry == null) continue;

                    TextureAtlasSprite sprite = resolveBackgroundSprite(entry);
                    if (sprite == null) continue;

                    int x = startX + col * texSize;
                    int y = startY + row * texSize;

                    int drawX = Math.max(startX, x);
                    int drawY = Math.max(startY, y);
                    int drawX2 = Math.min(startX + width, x + texSize);
                    int drawY2 = Math.min(startY + height, y + texSize);

                    if (drawX >= drawX2 || drawY >= drawY2) continue;

                    float u1 = (drawX - x) / (float)texSize;
                    float v1 = (drawY - y) / (float)texSize;
                    float u2 = (drawX2 - x) / (float)texSize;
                    float v2 = (drawY2 - y) / (float)texSize;

                    float minU = sprite.getMinU();
                    float maxU = sprite.getMaxU();
                    float minV = sprite.getMinV();
                    float maxV = sprite.getMaxV();

                    float uMin = minU + (maxU - minU) * u1;
                    float uMax = minU + (maxU - minU) * u2;
                    float vMin = minV + (maxV - minV) * v1;
                    float vMax = minV + (maxV - minV) * v2;

                    int quadWidth = drawX2 - drawX;
                    int quadHeight = drawY2 - drawY;

                    buf.pos(drawX, drawY + quadHeight, 0.0D).tex(uMin, vMax).endVertex();
                    buf.pos(drawX + quadWidth, drawY + quadHeight, 0.0D).tex(uMax, vMax).endVertex();
                    buf.pos(drawX + quadWidth, drawY, 0.0D).tex(uMax, vMin).endVertex();
                    buf.pos(drawX, drawY, 0.0D).tex(uMin, vMin).endVertex();
                }
            }

            tess.draw();
        }
        catch (Exception ignored) {}

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private TextureAtlasSprite resolveBackgroundSprite(BlockSetConfig.BlockEntryDefinition entry)
    {
        TextureAtlasSprite cached = backgroundSpriteCache.get(entry);
        if (cached != null)
        {
            return cached;
        }

        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            net.minecraft.block.Block block = entry.resolveBlock();
            if (block == null) return null;

            net.minecraft.block.state.IBlockState state = null;
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

            BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();
            TextureAtlasSprite sprite = null;

            try
            {
                sprite = blockRenderer.getBlockModelShapes().getTexture(state);
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

            backgroundSpriteCache.put(entry, sprite);
            return sprite;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private Entity resolveMobEntity(BlockSetConfig.MobEntryDefinition entry)
    {
        if (entry == null || entry.registry == null || entry.registry.isEmpty())
        {
            return null;
        }

        Entity cached = mobEntityCache.get(entry);
        if (cached != null)
        {
            return cached;
        }

        try
        {
            World mcWorld = Minecraft.getMinecraft().world;
            Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(entry.registry), mcWorld);
            if (entity != null)
            {
                if (entity.world == null)
                {
                    entity.world = mcWorld;
                }
                mobEntityCache.put(entry, entity);
            }
            return entity;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private void calculateColumns(int panelWidth)
    {
        int availableWidth = panelWidth - INNER_PADDING * 2;
        int halfWidth = (availableWidth - SECTION_GAP) / 2;
        int blockAreaWidth = halfWidth - 8;
        int mobAreaWidth = halfWidth - 8;

        blockCols = Math.max(2, blockAreaWidth / 20);
        mobCols = blockCols;

        cellPadding = Math.max(1, Math.min(2, blockAreaWidth / 80));
        cellSize = Math.max(14, Math.min(20, (mobAreaWidth - (mobCols - 1) * cellPadding) / mobCols));
    }

    private int getPanelGap()
    {
        int w = panelsElement != null ? panelsElement.getComputedWidth() : 0;
        return Math.max(4, w / 40);
    }

    private int getPanelWidth()
    {
        int w = panelsElement != null ? panelsElement.getComputedWidth() : 0;
        return (w - getPanelGap()) / 2;
    }

    private int getPanelHeight()
    {
        return panelsElement != null ? panelsElement.getComputedHeight() : 0;
    }

    private int getBlocksAreaWidth() {
        return blockCols * (cellSize + cellPadding) - cellPadding;
    }

    private int getMobsAreaWidth() {
        return mobCols * (cellSize + cellPadding) - cellPadding;
    }

    private int getMobsStartX(int panelX) {
        return panelX + INNER_PADDING + getBlocksAreaWidth() + SECTION_GAP + 8;
    }

    private int getGridStartY(int panelY) {
        return panelY + getRowInterval() * 2;
    }

    private int getAreaHeight() {
        return Math.max(0, getPanelHeight() - getRowInterval() * 2);
    }

    private void renderLevelPanel(BlockSetConfig.SetLevelDefinition levelDefinition, int panelX, int panelY, boolean isLeft, int mouseX, int mouseY)
    {
        if (levelDefinition == null) return;

        int localBlockScroll = isLeft ? blockScroll : blockScrollNext;
        int localMobScroll = isLeft ? mobScroll : mobScrollNext;

        int blocksAreaWidth = getBlocksAreaWidth();
        int mobsAreaWidth = getMobsAreaWidth();
        int mobsStartX = getMobsStartX(panelX);
        int gridStartY = getGridStartY(panelY);
        int areaHeight = getAreaHeight();

        fontRenderer.drawString(I18n.format(isLeft ? "gui.oneblockultima.current_level" : "gui.oneblockultima.next_level") + ": " + levelDefinition.level,
                panelX + INNER_PADDING, panelY + 4, LIGHT_BLUE_GRAY_COLOR);
        fontRenderer.drawString(I18n.format("gui.oneblockultima.possible_blocks") + ": ",
                panelX + INNER_PADDING, panelY + getRowInterval(), LIGHT_BLUE_GRAY_COLOR);

        if (levelDefinition.blocks != null && !levelDefinition.blocks.isEmpty())
        {
            int total = levelDefinition.blocks.size();
            int rows = (total + blockCols - 1) / blockCols;

            int visibleRows = Math.min(rows, Math.max(1, areaHeight / (cellSize + cellPadding)));
            int maxScroll = Math.max(0, rows - visibleRows);
            if (localBlockScroll > maxScroll) localBlockScroll = maxScroll;
            if (isLeft)
            {
                blockScroll = localBlockScroll;
            }
            else
            {
                blockScrollNext = localBlockScroll;
            }

            BlockSetConfig.BlockEntryDefinition hoveredEntry = null;

            for (int row = 0; row < visibleRows; row++)
            {
                for (int col = 0; col < blockCols; col++)
                {
                    int realRow = row + localBlockScroll;
                    int index = realRow * blockCols + col;
                    if (index >= total) break;

                    BlockSetConfig.BlockEntryDefinition entry = levelDefinition.blocks.get(index);
                    if (entry == null) continue;

                    int cellX = panelX + INNER_PADDING + col * (cellSize + cellPadding);
                    int cellY = gridStartY + row * (cellSize + cellPadding);

                    if (cellY + cellSize < gridStartY || cellY > gridStartY + areaHeight) {
                        continue;
                    }

                    boolean isHovered = false;
                    if (mouseX >= cellX && mouseX < cellX + cellSize &&
                            mouseY >= cellY && mouseY < cellY + cellSize)
                    {
                        isHovered = true;
                        hoveredEntry = entry;
                    }

                    ItemStack stack = entry.getPickBlock();
                    if (stack.isEmpty())
                    {
                        net.minecraft.block.Block blockForIcon = entry.resolveBlock();
                        Item itemForIcon = null;
                        if (blockForIcon != null)
                        {
                            itemForIcon = Item.getItemFromBlock(blockForIcon);
                        }
                        if (itemForIcon == null || itemForIcon == Items.AIR)
                        {
                            try { itemForIcon = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.registry)); } catch (Exception ignored) { }
                        }
                        if (itemForIcon != null && itemForIcon != Items.AIR)
                        {
                            try {
                                stack = new ItemStack(itemForIcon, 1, entry.meta);
                                if (entry.nbtTags != null && !entry.nbtTags.hasNoTags()) {
                                    stack.setTagCompound(entry.nbtTags.copy());
                                }
                            } catch (Exception ignored) {
                                stack = new ItemStack(itemForIcon);
                            }
                        }
                    }

                    net.minecraft.block.Block blockForIcon = entry.resolveBlock();

                    int bgColor = isHovered ? DARK_BLUE_GRAY_COLOR_1 : DARK_GRAY_COLOR_2;
                    int borderColor = isHovered ? WHITE_COLOR_1 : DARK_GRAY_COLOR_1;

                    drawRect(cellX, cellY, cellX + cellSize, cellY + cellSize, bgColor);
                    drawRect(cellX, cellY, cellX + cellSize, cellY + 1, borderColor);
                    drawRect(cellX, cellY + cellSize - 1, cellX + cellSize, cellY + cellSize, borderColor);
                    drawRect(cellX, cellY, cellX + 1, cellY + cellSize, borderColor);
                    drawRect(cellX + cellSize - 1, cellY, cellX + cellSize, cellY + cellSize, borderColor);

                    if (!stack.isEmpty())
                    {
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableDepth();

                        int iconX = cellX + (cellSize - 16) / 2;
                        int iconY = cellY + (cellSize - 16) / 2;

                        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                        renderItem.renderItemAndEffectIntoGUI(stack, iconX, iconY);

                        GlStateManager.disableDepth();
                        RenderHelper.disableStandardItemLighting();
                    }
                    else
                    {
                        if (blockForIcon != null)
                        {
                            net.minecraft.block.state.IBlockState state = null;
                            try { state = blockForIcon.getStateFromMeta(entry.meta); } catch (Exception ex) { try { state = blockForIcon.getDefaultState(); } catch (Exception ignored) { } }
                            int iconX = cellX + (cellSize - 12) / 2;
                            int iconY = cellY + (cellSize - 12) / 2;

                            Fluid fluid = null;
                            if (blockForIcon instanceof IFluidBlock)
                            {
                                try { fluid = ((IFluidBlock) blockForIcon).getFluid(); } catch (Exception ignored) { }
                            }
                            if (fluid == null)
                            {
                                try { fluid = FluidRegistry.lookupFluidForBlock(blockForIcon); } catch (Exception ignored) { }
                            }

                            if (fluid != null)
                            {
                                FluidElement fluidIcon = new FluidElement(fluid).size(12);
                                fluidIcon.setComputedPosition(iconX, iconY);
                                fluidIcon.setComputedSize(12, 12);
                                fluidIcon.draw(fontRenderer, mouseX, mouseY, 0);
                            }
                            else if (state != null)
                            {
                                BlockElement blockIcon = new BlockElement(state).size(16);
                                blockIcon.setComputedPosition(iconX, iconY);
                                blockIcon.setComputedSize(16, 16);
                                blockIcon.draw(fontRenderer, mouseX, mouseY, 0);
                            }
                        }
                    }

                    int chance = entry.getChance();
                    String percent = chance + "%";
                    int percentWidth = fontRenderer.getStringWidth(percent);
                    int percentX = cellX + (cellSize - percentWidth) / 2;
                    int percentY = cellY + cellSize - fontRenderer.FONT_HEIGHT - 1;
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    int color = chance < 10 ? RED_COLOR : chance < 20 ? ORANGE_COLOR : GREEN_COLOR;
                    fontRenderer.drawStringWithShadow(percent, percentX, percentY, color);

                    if (isHovered && hoveredEntry != null)
                    {
                        if (isLeft)
                        {
                            hoveredEntryLeft = hoveredEntry;
                            hoveredStackLeft = stack;
                        }
                        else
                        {
                            hoveredEntryRight = hoveredEntry;
                            hoveredStackRight = stack;
                        }
                    }
                }
            }

            int maxScrollBlocks = Math.max(0, rows - visibleRows);
            int blockScrollbarSlot = SB_BLOCK_LEFT + (isLeft ? 0 : 1);
            if (maxScrollBlocks > 0)
            {
                int blockScrollbarX = panelX + INNER_PADDING + blocksAreaWidth + 2;
                scrollbars[blockScrollbarSlot] = createScrollBar(blockScrollbarX, gridStartY, areaHeight, localBlockScroll, maxScrollBlocks);
                scrollbarActive[blockScrollbarSlot] = true;
            }
            else
            {
                scrollbarActive[blockScrollbarSlot] = false;
            }

            if (levelDefinition.mobs != null && !levelDefinition.mobs.isEmpty())
            {
                fontRenderer.drawString(I18n.format("gui.oneblockultima.mobs") + ":",
                        mobsStartX, panelY + getRowInterval(), LIGHT_BLUE_GRAY_COLOR);

                int mobTotal = levelDefinition.mobs.size();
                int mobRows = (mobTotal + mobCols - 1) / mobCols;
                int mobVisibleRows = Math.min(mobRows, Math.max(1, areaHeight / (cellSize + cellPadding)));
                int mobMaxScroll = Math.max(0, mobRows - mobVisibleRows);
                if (localMobScroll > mobMaxScroll) localMobScroll = mobMaxScroll;
                if (isLeft)
                {
                    mobScroll = localMobScroll;
                }
                else
                {
                    mobScrollNext = localMobScroll;
                }

                for (int row = 0; row < mobVisibleRows; row++)
                {
                    for (int col = 0; col < mobCols; col++)
                    {
                        int realMobIndex = (row + localMobScroll) * mobCols + col;
                        if (realMobIndex >= mobTotal) break;

                        BlockSetConfig.MobEntryDefinition mobEntry = levelDefinition.mobs.get(realMobIndex);
                        if (mobEntry == null) continue;

                        int cellX = mobsStartX + col * (cellSize + cellPadding);
                        int cellY = gridStartY + row * (cellSize + cellPadding);

                        if (cellY + cellSize < gridStartY || cellY > gridStartY + areaHeight) continue;

                        boolean isMobHovered = (mouseX >= cellX && mouseX < cellX + cellSize &&
                                mouseY >= cellY && mouseY < cellY + cellSize);

                        int mobBgColor = isMobHovered ? DARK_BLUE_GRAY_COLOR_1 : DARK_GRAY_COLOR_2;
                        int mobBorderColor = isMobHovered ? WHITE_COLOR_1 : DARK_GRAY_COLOR_1;

                        drawRect(cellX, cellY, cellX + cellSize, cellY + cellSize, mobBgColor);
                        drawRect(cellX, cellY, cellX + cellSize, cellY + 1, mobBorderColor);
                        drawRect(cellX, cellY + cellSize - 1, cellX + cellSize, cellY + cellSize, mobBorderColor);
                        drawRect(cellX, cellY, cellX + 1, cellY + cellSize, mobBorderColor);
                        drawRect(cellX + cellSize - 1, cellY, cellX + cellSize, cellY + cellSize, mobBorderColor);

                        Entity entity = resolveMobEntity(mobEntry);

                        if (entity instanceof EntityLivingBase)
                        {
                            int iconScale = cellSize - 2 * cellPadding;
                            EntityRendererElement mobIcon = new EntityRendererElement(entity).scale(iconScale);
                            mobIcon.setComputedPosition(cellX + cellPadding, cellY - cellSize / 2 + 3 * cellPadding);
                            mobIcon.setComputedSize(iconScale, iconScale);
                            mobIcon.draw(fontRenderer, mouseX, mouseY, 0);
                        }

                        if (isMobHovered)
                        {
                            if (isLeft)
                            {
                                hoveredMobEntryLeft = mobEntry;
                                hoveredMobNameLeft = null;
                                if (entity != null)
                                {
                                    try { hoveredMobNameLeft = entity.getDisplayName().getUnformattedText(); } catch (Exception ignored) { }
                                }
                            }
                            else
                            {
                                hoveredMobEntryRight = mobEntry;
                                hoveredMobNameRight = null;
                                if (entity != null)
                                {
                                    try { hoveredMobNameRight = entity.getDisplayName().getUnformattedText(); } catch (Exception ignored) { }
                                }
                            }
                        }

                        int chance = mobEntry.getChance();
                        String percent = chance + "%";
                        int pw = fontRenderer.getStringWidth(percent);
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        int color = chance < 10 ? RED_COLOR : chance < 20 ? ORANGE_COLOR : GREEN_COLOR;
                        fontRenderer.drawStringWithShadow(percent, cellX + (float) (cellSize - pw) / 2, cellY + cellSize - fontRenderer.FONT_HEIGHT - 2, color);
                    }
                }

                int mobScrollbarSlot = SB_MOB_LEFT + (isLeft ? 0 : 1);
                if (mobMaxScroll > 0)
                {
                    int mobScrollbarX = mobsStartX + mobsAreaWidth + 2;
                    scrollbars[mobScrollbarSlot] = createScrollBar(mobScrollbarX, gridStartY, areaHeight, localMobScroll, mobMaxScroll);
                    scrollbarActive[mobScrollbarSlot] = true;
                }
                else
                {
                    scrollbarActive[mobScrollbarSlot] = false;
                }
            }
        }
    }

    private void updateViewButtons()
    {
        if (tabs != null)
        {
            tabs.activeTab(activeView == VIEW_SETS ? BUTTON_TAB_SETS : BUTTON_TAB_DONATE);
        }

        TileEntityOneBlockGenerator generator = container.getGenerator();
        if (toggleFluidButton != null)
        {
            boolean disabled = generator != null && generator.isDisableFluidGeneration();
            if (pendingDisableFluid != null)
            {
                disabled = pendingDisableFluid;
            }
            toggleFluidButton.text(I18n.format("gui.oneblockultima.settings.fluid") + ": " + (disabled ? I18n.format("gui.oneblockultima.settings.disabled") : I18n.format("gui.oneblockultima.settings.enabled")));
        }
        if (toggleMobsButton != null)
        {
            boolean disabled = generator != null && generator.isDisableMobGeneration();
            if (pendingDisableMob != null)
            {
                disabled = pendingDisableMob;
            }
            toggleMobsButton.text(I18n.format("gui.oneblockultima.settings.mobs") + ": " + (disabled ? I18n.format("gui.oneblockultima.settings.disabled") : I18n.format("gui.oneblockultima.settings.enabled")));
        }
        if (toggleChestsButton != null)
        {
            boolean disabled = generator != null && generator.isDisableChestGeneration();
            if (pendingDisableChest != null)
            {
                disabled = pendingDisableChest;
            }
            toggleChestsButton.text(I18n.format("gui.oneblockultima.settings.chests") + ": " + (disabled ? I18n.format("gui.oneblockultima.settings.disabled") : I18n.format("gui.oneblockultima.settings.enabled")));
        }
        if (toggleSaplingsButton != null)
        {
            boolean disabled = generator != null && generator.isDisableSaplingGeneration();
            if (pendingDisableSapling != null)
            {
                disabled = pendingDisableSapling;
            }
            toggleSaplingsButton.text(I18n.format("gui.oneblockultima.settings.saplings") + ": " + (disabled ? I18n.format("gui.oneblockultima.settings.disabled") : I18n.format("gui.oneblockultima.settings.enabled")));
        }
    }

    private void refreshActiveSetFromGenerator()
    {
        TileEntityOneBlockGenerator generator = container.getGenerator();
        if (generator == null)
        {
            return;
        }

        String activeSetId = generator.getSelectedSetId();
        if (activeSetId == null)
        {
            return;
        }

        if (!activeSetId.equals(clientActiveSetId))
        {
            clientActiveSetId = activeSetId;
            initBackgroundBlocks();
        }
    }

    private String getLocalizedSetName(BlockSetConfig.BlockSetDefinition set)
    {
        return ContainerSetsConfig.getLocalizedSetName(set);
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button) {
        if (visibleSets.isEmpty())
        {
            return;
        }

        if (button.id == BUTTON_TOGGLE_FLUIDS)
        {
            boolean currentDisabled = container.getGenerator() != null && container.getGenerator().isDisableFluidGeneration();
            pendingDisableFluid = !currentDisabled;
            container.toggleFluidGeneration();
            updateViewButtons();
        }
        else if (button.id == BUTTON_TOGGLE_MOBS)
        {
            boolean currentDisabled = container.getGenerator() != null && container.getGenerator().isDisableMobGeneration();
            pendingDisableMob = !currentDisabled;
            container.toggleMobGeneration();
            updateViewButtons();
        }
        else if (button.id == BUTTON_TOGGLE_CHESTS)
        {
            boolean currentDisabled = container.getGenerator() != null && container.getGenerator().isDisableChestGeneration();
            pendingDisableChest = !currentDisabled;
            container.toggleChestGeneration();
            updateViewButtons();
        }
        else if (button.id == BUTTON_TOGGLE_SAPLINGS)
        {
            boolean currentDisabled = container.getGenerator() != null && container.getGenerator().isDisableSaplingGeneration();
            pendingDisableSapling = !currentDisabled;
            container.toggleSaplingGeneration();
            updateViewButtons();
        }
        else if (button.id == BUTTON_OPEN_CONFIG_EDITOR)
        {
            mc.displayGuiScreen(new GuiSetsConfig(this));
        }
        else if (button.id == BUTTON_OPEN_PRICES)
        {
            mc.displayGuiScreen(new GuiBlockPrices(this));
        }
        else if (button.id == BUTTON_OPEN_UI_SETTINGS)
        {
            mc.displayGuiScreen(new GuiUiSettings(this));
        }
        else if (button.id == BUTTON_OPEN_MISC_SETTINGS)
        {
            mc.displayGuiScreen(new GuiMiscSettings(this));
        }
        else if (button.id >= BUTTON_DONATE_BASE && button.id < BUTTON_DONATE_BASE + DonateMethod.METHODS.length)
        {
            handleDonateClick(DonateMethod.METHODS[button.id - BUTTON_DONATE_BASE]);
        }
        else if (button.id == BUTTON_PREV_SET)
        {
            selectedSetIndex = (selectedSetIndex - 1 + visibleSets.size()) % visibleSets.size();
            conditionsScroll = 0;
            initBackgroundBlocks();
        }
        else if (button.id == BUTTON_NEXT_SET)
        {
            selectedSetIndex = (selectedSetIndex + 1) % visibleSets.size();
            conditionsScroll = 0;
            initBackgroundBlocks();
        }
        else if (button.id == BUTTON_SELECT_SET)
        {
            BlockSetConfig.BlockSetDefinition selectedSet = getBlockSetDefinition();
            if (selectedSet != null)
            {
                container.selectSet(selectedSet.id);
                initBackgroundBlocks();
            }
        }
        else if (button.id == BUTTON_UPGRADE_SET)
        {
            BlockSetConfig.BlockSetDefinition selectedSet = getBlockSetDefinition();
            if (selectedSet != null)
            {
                container.upgradeSet(selectedSet.id);
                initBackgroundBlocks();
            }
        }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        refreshActiveSetFromGenerator();
        TileEntityOneBlockGenerator generator = container.getGenerator();
        if (generator != null)
        {
            if (pendingDisableFluid != null && generator.isDisableFluidGeneration() == pendingDisableFluid)
            {
                pendingDisableFluid = null;
            }
            if (pendingDisableMob != null && generator.isDisableMobGeneration() == pendingDisableMob)
            {
                pendingDisableMob = null;
            }
            if (pendingDisableChest != null && generator.isDisableChestGeneration() == pendingDisableChest)
            {
                pendingDisableChest = null;
            }
            if (pendingDisableSapling != null && generator.isDisableSaplingGeneration() == pendingDisableSapling)
            {
                pendingDisableSapling = null;
            }
        }
        if (factory != null)
        {
            factory.updateScreen();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        updateViewButtons();
        drawDonateHoverHint(mouseX, mouseY);

        if (activeView != VIEW_SETS)
        {
            return;
        }

        if (hoveredEntryLeft == null && hoveredStackLeft.isEmpty() && hoveredMobEntryLeft == null &&
                hoveredEntryRight == null && hoveredStackRight.isEmpty() && hoveredMobEntryRight == null) {
            return;
        }

        if (!hoveredStackLeft.isEmpty())
        {
            List<String> tooltip = hoveredStackLeft.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + (hoveredEntryLeft != null ? hoveredEntryLeft.getChance() : 0) + "%");
            drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
        else if (hoveredEntryLeft != null && hoveredEntryLeft.isFluid())
        {
            List<String> tooltip = BlockUtil.getTooltip(hoveredEntryLeft, mc.gameSettings.advancedItemTooltips);
            tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + hoveredEntryLeft.getChance() + "%");
            drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
        else if (hoveredMobEntryLeft != null)
        {
            List<String> tooltip = new ArrayList<>();
            String mobName = hoveredMobNameLeft;
            if (mobName == null || mobName.isEmpty())
            {
                try
                {
                    String entityName = EntityList.getTranslationName(new ResourceLocation(hoveredMobEntryLeft.registry));
                    if (entityName != null && !entityName.isEmpty())
                    {
                        String translationKey = "entity." + entityName + ".name";
                        mobName = I18n.format(translationKey);
                        if (mobName.equals(translationKey)) mobName = null;
                    }
                }
                catch (Exception ignored) { }
            }
            if (mobName == null || mobName.isEmpty())
            {
                mobName = hoveredMobEntryLeft.registry;
            }
            tooltip.add(mobName);
            tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + hoveredMobEntryLeft.getChance() + "%");
            if (hoveredMobEntryLeft.count > 1)
            {
                tooltip.add("x" + hoveredMobEntryLeft.count);
            }
            drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
        else if (!hoveredStackRight.isEmpty())
        {
            List<String> tooltip = hoveredStackRight.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + (hoveredEntryRight != null ? hoveredEntryRight.getChance() : 0) + "%");
            drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
        else if (hoveredEntryRight != null && hoveredEntryRight.isFluid())
        {
            List<String> tooltip = BlockUtil.getTooltip(hoveredEntryRight, mc.gameSettings.advancedItemTooltips);
            tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + hoveredEntryRight.getChance() + "%");
            drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
        else if (hoveredMobEntryRight != null)
        {
            List<String> tooltip = new ArrayList<>();
            String mobName = hoveredMobNameRight;
            if (mobName == null || mobName.isEmpty())
            {
                try
                {
                    String entityName = EntityList.getTranslationName(new ResourceLocation(hoveredMobEntryRight.registry));
                    if (entityName != null && !entityName.isEmpty())
                    {
                        String translationKey = "entity." + entityName + ".name";
                        mobName = I18n.format(translationKey);
                        if (mobName.equals(translationKey)) mobName = null;
                    }
                }
                catch (Exception ignored) { }
            }
            if (mobName == null || mobName.isEmpty())
            {
                mobName = hoveredMobEntryRight.registry;
            }
            tooltip.add(mobName);
            tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + hoveredMobEntryRight.getChance() + "%");
            if (hoveredMobEntryRight.count > 1)
            {
                tooltip.add("x" + hoveredMobEntryRight.count);
            }
            drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
    }

    private void drawUnlockConditions(BlockSetConfig.BlockSetDefinition set, int x, int y, int width, int height, TileEntityOneBlockGenerator generator)
    {
        conditionsScrollbar = null;
        if (set == null || set.unlockConditions == null || set.unlockConditions.conditions == null ||
                set.unlockConditions.conditions.isEmpty())
        {
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(container.getPlayer());
        if (data == null) return;

        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
        if (currentLevel > 0) return;

        int columnWidth = 0;
        for (BlockSetConfig.UnlockConditionDefinition condition : set.unlockConditions.conditions)
        {
            String conditionText = formatUnlockCondition(condition, data, generator);
            String fullText = " - " + conditionText + " \u2713";

            int textWidth = fontRenderer.getStringWidth(fullText);
            columnWidth = Math.max(textWidth, columnWidth);
        }

        String title = I18n.format("gui.oneblockultima.unlock_conditions") + ": " + I18n.format("gui.oneblockultima.config." + set.unlockConditions.mode);
        int titleWidth = fontRenderer.getStringWidth(title);
        columnWidth = Math.max(titleWidth, columnWidth);
        int startX = x + (width - columnWidth) / 2;
        conditionsStartX = startX;
        conditionsColumnWidth = columnWidth;
        int titleX = startX + titleWidth / 2;
        fontRenderer.drawString(title, titleX, y, LIGHT_BLUE_GRAY_COLOR);

        int lineSpacing = fontRenderer.FONT_HEIGHT + 1;
        int areaTop = y + fontRenderer.FONT_HEIGHT + 2;
        int areaBottom = y + height;
        int areaHeight = areaBottom - areaTop;
        if (areaHeight < lineSpacing) return;

        List<BlockSetConfig.UnlockConditionDefinition> conditions = set.unlockConditions.conditions;
        int visibleRows = areaHeight / lineSpacing;
        int maxScroll = Math.max(0, conditions.size() - visibleRows);
        if (conditionsScroll > maxScroll) conditionsScroll = maxScroll;
        if (conditionsScroll < 0) conditionsScroll = 0;

        int firstRow = conditionsScroll;
        int lastRow = Math.min(conditions.size(), firstRow + visibleRows);
        int textY = areaTop;
        for (int i = firstRow; i < lastRow; i++)
        {
            BlockSetConfig.UnlockConditionDefinition condition = conditions.get(i);
            if (condition == null)
            {
                textY += lineSpacing;
                continue;
            }

            String conditionText = formatUnlockCondition(condition, data, generator);
            boolean satisfied = condition.isSatisfied(data, generator);
            int color = satisfied ? GREENISH_COLOR : REDDISH_COLOR;
            String status = satisfied ? " \u2713" : " \u2717";
            String fullText = " - " + conditionText + status;

            fontRenderer.drawString(fullText, startX, textY, color);
            textY += lineSpacing;
        }

        if (conditions.size() > visibleRows)
        {
            ScrollbarElement sb = new ScrollbarElement()
                    .totalItems(conditions.size())
                    .visibleItems(visibleRows)
                    .scrollOffset(conditionsScroll)
                    .trackWidth(SCROLLBAR_WIDTH);
            sb.setComputedPosition(startX + columnWidth, areaTop);
            sb.setComputedSize(SCROLLBAR_WIDTH, areaHeight);
            sb.draw(fontRenderer, 0, 0, 0);
            conditionsScrollbar = sb;
        }
    }

    private String formatUnlockCondition(BlockSetConfig.UnlockConditionDefinition condition, IOneBlockPlayerData data, TileEntityOneBlockGenerator generator)
    {
        if (condition == null) return "";

        switch (condition.type == null ? "" : condition.type.toLowerCase(Locale.ROOT))
        {
            case "set_level":
                String setId = condition.setId != null ? condition.setId : "?";
                BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(setId);
                String setName = set != null ? getLocalizedSetName(set) : setId;
                int levelValue = generator == null ? data.getSetLevel(setId) : generator.getSetLevel(setId);
                return I18n.format("gui.oneblockultima.condition.set_level", setName, levelValue, condition.level);
            case "broken_blocks":
                String targetSetId = condition.setId != null ? condition.setId : "?";
                BlockSetConfig.BlockSetDefinition targetSet = BlockSetConfig.get().getSet(targetSetId);
                String targetSetName = targetSet != null ? getLocalizedSetName(targetSet) : targetSetId;
                return I18n.format("gui.oneblockultima.condition.broken_blocks", String.format("%d/%d", data.getBrokenBlocksCount(targetSetId), condition.count), targetSetName);
            case "broken_blocks_total":
                return I18n.format("gui.oneblockultima.condition.broken_blocks_total", String.format("%s/%s", data.getBrokenBlocksCount(), condition.count));
            default:
                return I18n.format("gui.oneblockultima.condition.unknown", condition.type);
        }
    }

    private BlockSetConfig.BlockSetDefinition getBlockSetDefinition() {
        if (selectedSetIndex >= 0 && selectedSetIndex < visibleSets.size())
        {
            return visibleSets.get(selectedSetIndex);
        }
        return null;
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0 || activeView != VIEW_SETS || panelsElement == null || visibleSets.isEmpty())
        {
            return;
        }
        int delta = d > 0 ? -1 : 1;
        int mouseX = Mouse.getX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getY() * height / mc.displayHeight - 1;

        BlockSetConfig.BlockSetDefinition set = visibleSets.get(selectedSetIndex);
        TileEntityOneBlockGenerator generator = container.getGenerator();
        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
        boolean canShowCurrent = currentLevel > 0;

        boolean showConditions = currentLevel <= 0 && set.unlockConditions != null &&
                set.unlockConditions.conditions != null && !set.unlockConditions.conditions.isEmpty();
        if (showConditions && infoElement != null && conditionsColumnWidth > 0)
        {
            int infoY = infoElement.getComputedY();
            if (mouseX >= conditionsStartX && mouseX <= conditionsStartX + conditionsColumnWidth &&
                    mouseY >= infoY && mouseY <= infoY + infoElement.getComputedHeight())
            {
                conditionsScroll += delta;
                return;
            }
        }

        int panelGap = getPanelGap();
        int panelWidth = getPanelWidth();
        calculateColumns(panelWidth);

        int leftPanelX = panelsElement.getComputedX();
        int rightPanelX = leftPanelX + panelWidth + panelGap;
        int panelY = panelsElement.getComputedY();

        int blocksAreaWidth = getBlocksAreaWidth();
        int mobsAreaWidth = getMobsAreaWidth();
        int gridStartY = getGridStartY(panelY);
        int areaHeight = getAreaHeight();

        if (canShowCurrent)
        {
            if (mouseX >= leftPanelX + INNER_PADDING &&
                    mouseX <= leftPanelX + INNER_PADDING + blocksAreaWidth &&
                    mouseY >= gridStartY && mouseY <= gridStartY + areaHeight)
            {
                int maxScroll = getMaxBlockScroll(set, currentLevel);
                blockScroll = Math.max(0, Math.min(blockScroll + delta, maxScroll));
                return;
            }

            int mobsStartX = getMobsStartX(leftPanelX);
            if (mouseX >= mobsStartX &&
                    mouseX <= mobsStartX + mobsAreaWidth &&
                    mouseY >= gridStartY && mouseY <= gridStartY + areaHeight)
            {
                int maxScroll = getMaxMobScroll(set, currentLevel);
                mobScroll = Math.max(0, Math.min(mobScroll + delta, maxScroll));
                return;
            }
        }

        int rightStartX = canShowCurrent ? rightPanelX : leftPanelX;
        int nextLevel = currentLevel <= 0 ? 1 : currentLevel + 1;

        if (mouseX >= rightStartX + INNER_PADDING &&
                mouseX <= rightStartX + INNER_PADDING + blocksAreaWidth &&
                mouseY >= gridStartY && mouseY <= gridStartY + areaHeight)
        {
            int maxScroll = getMaxBlockScroll(set, nextLevel);
            blockScrollNext = Math.max(0, Math.min(blockScrollNext + delta, maxScroll));
            return;
        }

        int rightMobsStartX = getMobsStartX(rightStartX);
        if (mouseX >= rightMobsStartX &&
                mouseX <= rightMobsStartX + mobsAreaWidth &&
                mouseY >= gridStartY && mouseY <= gridStartY + areaHeight)
        {
            int maxScroll = getMaxMobScroll(set, nextLevel);
            mobScrollNext = Math.max(0, Math.min(mobScrollNext + delta, maxScroll));
        }
    }

    private int getMaxBlockScroll(BlockSetConfig.BlockSetDefinition set, int level)
    {
        if (set == null) return 0;
        BlockSetConfig.SetLevelDefinition levelDef = set.getLevel(level);
        if (levelDef == null || levelDef.blocks == null || levelDef.blocks.isEmpty()) return 0;

        calculateColumns(getPanelWidth());
        int areaHeight = getAreaHeight();

        int total = levelDef.blocks.size();
        int rows = (total + blockCols - 1) / blockCols;
        int visibleRows = Math.min(rows, Math.max(1, areaHeight / (cellSize + cellPadding)));

        return Math.max(0, rows - visibleRows);
    }

    private int getMaxMobScroll(BlockSetConfig.BlockSetDefinition set, int level)
    {
        if (set == null) return 0;
        BlockSetConfig.SetLevelDefinition levelDef = set.getLevel(level);
        if (levelDef == null || levelDef.mobs == null || levelDef.mobs.isEmpty()) return 0;

        calculateColumns(getPanelWidth());
        int areaHeight = getAreaHeight();

        int total = levelDef.mobs.size();
        int rows = (total + mobCols - 1) / mobCols;
        int visibleRows = Math.min(rows, Math.max(1, areaHeight / (cellSize + cellPadding)));

        return Math.max(0, rows - visibleRows);
    }

    private void handleDonateClick(DonateMethod method)
    {
        if (method.type == DonateMethod.Type.LINK)
        {
            try
            {
                Desktop.getDesktop().browse(new URI(method.value));
            }
            catch (Exception ignored)
            {
            }
        }
        else
        {
            GuiOneBlock.setClipboardString(method.value);
            donateJustCopied = true;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        clearHovered();

        if (factory == null)
        {
            return;
        }

        int headerBottom = headerElement != null ? headerElement.getComputedY() + headerElement.getComputedHeight() : guiTop;
        int contentTop = switcher != null ? switcher.getComputedY() : headerBottom;

        drawRect(guiLeft, guiTop, guiLeft + xSize, headerBottom, DARK_GRAY_COLOR_3);
        if (contentTop > headerBottom)
        {
            drawRect(guiLeft, headerBottom, guiLeft + xSize, contentTop, DARK_BLUE_GRAY_COLOR_2);
        }

        int contentBottom = guiTop + ySize;
        int contentHeight = contentBottom - contentTop;
        if (contentHeight > 0)
        {
            renderProceduralBackground(guiLeft, contentTop, xSize, contentHeight);
            drawRect(guiLeft, contentTop, guiLeft + xSize, contentBottom, TRANSPARENT_DARK_GRAY_COLOR_1);
        }

        factory.draw(fontRenderer, mouseX, mouseY, partialTicks, guiLeft, guiTop, xSize, ySize);
    }

    private void drawDonateHoverHint(int mouseX, int mouseY)
    {
        if (activeView != VIEW_DONATE || donateButtons == null)
        {
            return;
        }
        boolean hoveringTextButton = false;
        for (int i = 0; i < donateButtons.length && i < DonateMethod.METHODS.length; i++)
        {
            ButtonElement<?> button = donateButtons[i];
            if (button == null) continue;
            if (DonateMethod.METHODS[i].type != DonateMethod.Type.TEXT) continue;
            int x = button.getComputedX();
            int y = button.getComputedY();
            int w = button.getComputedWidth();
            int h = button.getComputedHeight();
            if (w <= 0 || h <= 0) continue;
            if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) continue;

            hoveringTextButton = true;

            String hint = donateJustCopied
                    ? I18n.format("gui.oneblockultima.donate.copied_short")
                    : I18n.format("gui.oneblockultima.donate.copy_hint");

            List<String> tooltip = new ArrayList<>();
            tooltip.add(hint);
            drawHoveringText(tooltip, x + w / 2 - guiLeft, y - guiTop - 4, fontRenderer);
            break;
        }
        if (!hoveringTextButton)
        {
            donateJustCopied = false;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (factory != null)
        {
            factory.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (activeView == VIEW_SETS)
        {
            handleScrollbarClick(mouseX, mouseY, mouseButton);
            handleConditionsScrollbarClick(mouseX, mouseY, mouseButton);
        }
        if (tabs != null)
        {
            int clickedId = tabs.getActiveTabId();
            if (clickedId == BUTTON_TAB_SETS && activeView != VIEW_SETS)
            {
                changeView(VIEW_SETS);
            }
            else if (clickedId == BUTTON_TAB_SETTINGS && activeView != VIEW_SETTINGS)
            {
                changeView(VIEW_SETTINGS);
            }
            else if (clickedId == BUTTON_TAB_DONATE && activeView != VIEW_DONATE)
            {
                changeView(VIEW_DONATE);
            }
        }
    }

    private void handleScrollbarClick(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton != 0)
        {
            return;
        }
        for (int slot = 0; slot < scrollbars.length; slot++)
        {
            if (!scrollbarActive[slot] || scrollbars[slot] == null)
            {
                continue;
            }
            ScrollbarElement sb = scrollbars[slot];
            if (mouseX < sb.getComputedX() || mouseX > sb.getComputedX() + sb.getComputedWidth() ||
                    mouseY < sb.getComputedY() || mouseY > sb.getComputedY() + sb.getComputedHeight())
            {
                continue;
            }
            if (sb.mouseClicked(mouseX, mouseY, mouseButton))
            {
                int target = sb.getScrollOffset();
                switch (slot)
                {
                    case SB_BLOCK_LEFT:
                        blockScroll = target;
                        break;
                    case SB_BLOCK_RIGHT:
                        blockScrollNext = target;
                        break;
                    case SB_MOB_LEFT:
                        mobScroll = target;
                        break;
                    case SB_MOB_RIGHT:
                        mobScrollNext = target;
                        break;
                }
            }
            return;
        }
    }

    private void handleConditionsScrollbarClick(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton != 0 || conditionsScrollbar == null) return;
        if (conditionsScrollbar.mouseClicked(mouseX, mouseY, mouseButton))
        {
            conditionsScroll = conditionsScrollbar.getScrollOffset();
        }
    }

    private void changeView(int view)
    {
        if (activeView != view)
        {
            activeView = view;
            rebuildView();
        }
    }

    private void clearHovered()
    {
        hoveredEntryLeft = null;
        hoveredStackLeft = ItemStack.EMPTY;
        hoveredMobEntryLeft = null;
        hoveredMobNameLeft = null;
        hoveredEntryRight = null;
        hoveredStackRight = ItemStack.EMPTY;
        hoveredMobEntryRight = null;
        hoveredMobNameRight = null;
    }

    private ScrollbarElement createScrollBar(int scrollX, int scrollY, int scrollHeight, int currentScroll, int maxScroll)
    {
        if (maxScroll <= 0 || scrollHeight <= 0) return null;

        int visible = Math.max(1, scrollHeight / (cellSize + cellPadding));
        ScrollbarElement sb = new ScrollbarElement()
                .totalItems(maxScroll + visible)
                .visibleItems(visible)
                .scrollOffset(currentScroll)
                .trackWidth(SCROLLBAR_WIDTH);
        sb.setComputedPosition(scrollX, scrollY);
        sb.setComputedSize(SCROLLBAR_WIDTH, scrollHeight);
        sb.draw(fontRenderer, 0, 0, 0);
        return sb;
    }
}

package ru.defea.oneblockultima.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockCompressedBase;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.util.ModelUtil;

import static ru.defea.oneblockultima.Constants.*;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.defea.oneblockultima.util.BlockUtil.isFullBlock;

public class GuiOneBlock extends AbstractContainerScreen<ContainerOneBlock>
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

    private static final int VIEW_SETS = 0;
    private static final int VIEW_SETTINGS = 1;
    private static final int VIEW_DONATE = 2;

    private static final int TAB_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int SECTION_GAP = 8;
    private static final int INNER_PADDING = 6;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int MIN_PANEL_HEIGHT = 56;
    private static final int BACKGROUND_TEXTURE_SIZE = 32;
    private static final int CLOSE_BUTTON_SIZE = 14;
    private static final int CLOSE_BUTTON_OFFSET = 6;
    private static final int HEADER_H = 27;
    private static final int HEADER_OFFSET = 0;
    private static final int TAB_ROW_Y = 27;
    private static final int TABS_Y = TAB_ROW_Y;
    private static final int CONTENT_TOP_PADDING = 2;
    private static final int INFO_OFFSET_Y = TABS_Y + TAB_HEIGHT + CONTENT_TOP_PADDING + 6;
    private static final int ROW_H = 20;

    private final ContainerOneBlock container;
    private boolean lastDisableFluid = false;
    private boolean lastDisableMob = false;
    private boolean lastDisableChest = false;
    private boolean lastDisableSapling = false;
    private final List<BlockSetConfig.BlockSetDefinition> visibleSets = new ArrayList<>();
    private int selectedSetIndex = 0;
    private String activeSetSyncId = null;
    private int activeView = VIEW_SETS;
    private int blockScroll = 0;
    private int mobScroll = 0;
    private int blockScrollNext = 0;
    private int mobScrollNext = 0;
    private int conditionsScroll = 0;

    private final Map<EntityType<?>, Entity> mobEntityCache = new HashMap<>();
    private Level mobEntityCacheLevel = null;
    private static long lastModelFitLog = 0L;

    private final List<BlockSetConfig.BlockEntryDefinition> backgroundBlocks = new ArrayList<>();
    private final List<BlockSetConfig.BlockEntryDefinition> backgroundFullBlocks = new ArrayList<>();
    private final Map<BlockSetConfig.BlockEntryDefinition, TextureAtlasSprite> backgroundSpriteCache = new HashMap<>();
    private final Map<BlockSetConfig.BlockEntryDefinition, Integer> backgroundTintCache = new HashMap<>();
    private String backgroundSetId = null;

    private int headerX, headerY, headerW, headerH;
    private int infoX, infoY, infoW, infoH;
    private int lpX, lpY, lpW, lpH;
    private int rpX, rpY, rpW, rpH;
    private int condX, condY, condW, condH;
    private int closeButtonX, closeButtonY;
    private boolean closeButtonHovered = false;
    private final int[] tabX = new int[3];
    private final int[] tabView = { VIEW_SETS, VIEW_SETTINGS, VIEW_DONATE };
    private int tabY = 0;
    private int tabW = 0;

    private PanelGeometry leftGeom;
    private PanelGeometry rightGeom;

    private int draggingScrollbar = -1;
    private int dragStartMouseY = 0;
    private int dragStartScroll = 0;
    private int condScrollTopY = 0;
    private int condScrollBotY = 0;
    private int condMaxScroll = 0;

    private final List<DonateMethod> donateMethods = new ArrayList<>();
    private boolean donateJustCopied = false;
    private int donateStartY = 0;
    private int donateRowH = 0;
    private int donateBtnX = 0;
    private int donateBtnW = 0;
    private int condVisibleRows = 0;
    private Button selectButton;
    private Button upgradeButton;

    private static final class DonateMethod
    {
        enum Type { TEXT, LINK }

        final Type type;
        final String text;
        final String value;

        DonateMethod(Type type, String text, String value)
        {
            this.type = type;
            this.text = text;
            this.value = value;
        }
    }

    private static final class PanelGeometry
    {
        final int panelX, panelY, panelW, panelH;
        final int cellSize, cellPadding, blockCols, mobCols;
        final int gridStartY, areaH, blocksW, mobsW, mobsStartX;
        final int blockScrollbarX, mobScrollbarX;
        final int blockTotalRows, mobTotalRows;
        final int blockMaxScroll, mobMaxScroll;
        final int blockVisibleRows, mobVisibleRows;
        final boolean showCase;
        final int caseDropPercentDisplay;
        final double caseScaleFactor;

        PanelGeometry(int panelX, int panelY, int panelW, int panelH,
                      int cellSize, int cellPadding, int blockCols, int mobCols,
                      int gridStartY, int areaH, int blocksW, int mobsW, int mobsStartX,
                      int blockScrollbarX, int mobScrollbarX,
                      int blockTotalRows, int mobTotalRows,
                      int blockMaxScroll, int mobMaxScroll,
                      int blockVisibleRows, int mobVisibleRows,
                      boolean showCase, int caseDropPercentDisplay, double caseScaleFactor)
        {
            this.panelX = panelX; this.panelY = panelY; this.panelW = panelW; this.panelH = panelH;
            this.cellSize = cellSize; this.cellPadding = cellPadding;
            this.blockCols = blockCols; this.mobCols = mobCols;
            this.gridStartY = gridStartY; this.areaH = areaH;
            this.blocksW = blocksW; this.mobsW = mobsW; this.mobsStartX = mobsStartX;
            this.blockScrollbarX = blockScrollbarX; this.mobScrollbarX = mobScrollbarX;
            this.blockTotalRows = blockTotalRows; this.mobTotalRows = mobTotalRows;
            this.blockMaxScroll = blockMaxScroll; this.mobMaxScroll = mobMaxScroll;
            this.blockVisibleRows = blockVisibleRows; this.mobVisibleRows = mobVisibleRows;
            this.showCase = showCase;
            this.caseDropPercentDisplay = caseDropPercentDisplay;
            this.caseScaleFactor = caseScaleFactor;
        }

        int gridStartX()
        {
            return panelX + GuiOneBlock.INNER_PADDING;
        }

        int gridEndX()
        {
            return mobsStartX + mobsW;
        }
    }

    public GuiOneBlock(ContainerOneBlock menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        this.container = menu;
        this.imageWidth = 348;
        this.imageHeight = 248;
        this.minecraft = Minecraft.getInstance();
    }

    private Button addButton(int id, int x, int y, int w, int h, String label)
    {
        OBUButton b = new OBUButton(x, y, w, h, Component.literal(label), btn -> actionPerformed(id), false);
        this.addRenderableWidget(b);
        return b;
    }

    private Button addButtonPrimary(int id, int x, int y, int w, int h, String label)
    {
        OBUButton b = new OBUButton(x, y, w, h, Component.literal(label), btn -> actionPerformed(id), true);
        this.addRenderableWidget(b);
        return b;
    }

    private static class OBUButton extends Button
    {
        private final boolean primary;

        OBUButton(int x, int y, int w, int h, Component message, OnPress onPress, boolean primary)
        {
            super(x, y, w, h, message, onPress, Button.DEFAULT_NARRATION);
            this.primary = primary;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt)
        {
            boolean hovered = this.isHovered() && this.active;
            int bg;
            int border;
            if (!this.active)
            {
                bg = DARK_GRAY_COLOR_1;
                border = DARK_GRAY_COLOR_3;
            }
            else if (primary)
            {
                bg = DARK_GREEN;
                border = hovered ? GREENISH_COLOR : GRAY_COLOR_2;
            }
            else if (hovered)
            {
                bg = DARK_BLUE_GRAY_COLOR_1;
                border = LIGHT_BLUE_GRAY_COLOR;
            }
            else
            {
                bg = DARK_GRAY_COLOR_2;
                border = GRAY_COLOR_7;
            }
            int x0 = getX();
            int y0 = getY();
            int w0 = getWidth();
            int h0 = getHeight();
            g.fill(x0, y0, x0 + w0, y0 + h0, bg);
            g.fill(x0, y0, x0 + w0, y0 + 1, border);
            g.fill(x0, y0 + h0 - 1, x0 + w0, y0 + h0, border);
            g.fill(x0, y0, x0 + 1, y0 + h0, border);
            g.fill(x0 + w0 - 1, y0, x0 + w0, y0 + h0, border);
            int textColor = !this.active ? GRAY_COLOR_7 : primary ? WHITE_COLOR_1 : LIGHT_GRAY_COLOR_2;
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x0 + w0 / 2, y0 + (h0 - 8) / 2, textColor);
        }
    }

    @Override
    public void init()
    {
        this.imageWidth = Math.max(320, this.width - 40);
        this.imageHeight = Math.max(240, this.height - 40);
        super.init();
        this.donateMethods.clear();
        this.donateJustCopied = false;
        this.donateMethods.add(new DonateMethod(DonateMethod.Type.TEXT, "Bitcoin", "bc1qdra5454kw9wncg8s6dtngswxs2musaqpnqdr4k"));
        this.donateMethods.add(new DonateMethod(DonateMethod.Type.TEXT, "Ethereum", "0x5f0864a5687b845200cC1fCb987E1F671E0feecd"));
        this.donateMethods.add(new DonateMethod(DonateMethod.Type.LINK, "Steam Trade", "https://steamcommunity.com/tradeoffer/new/?partner=1094904831&token=FGQy9z8F"));
        populateVisibleSets();
        refreshActiveSetFromGenerator();
        activeSetSyncId = getGeneratorActiveSetId();
        initBackgroundBlocks();
        cacheGenerationFlags();
        rebuildView();
    }

    private void cacheGenerationFlags()
    {
        ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator gen = container.getGenerator();
        if (gen != null)
        {
            lastDisableFluid = gen.isDisableFluidGeneration();
            lastDisableMob = gen.isDisableMobGeneration();
            lastDisableChest = gen.isDisableChestGeneration();
            lastDisableSapling = gen.isDisableSaplingGeneration();
        }
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator gen = container.getGenerator();
        if (gen == null)
        {
            return;
        }
        boolean dFluid = gen.isDisableFluidGeneration();
        boolean dMob = gen.isDisableMobGeneration();
        boolean dChest = gen.isDisableChestGeneration();
        boolean dSapling = gen.isDisableSaplingGeneration();
        if (dFluid != lastDisableFluid || dMob != lastDisableMob || dChest != lastDisableChest || dSapling != lastDisableSapling)
        {
            lastDisableFluid = dFluid;
            lastDisableMob = dMob;
            lastDisableChest = dChest;
            lastDisableSapling = dSapling;
            rebuildView();
        }
    }

    private void refreshBackgroundForCurrentSet()
    {
        BlockSetConfig.BlockSetDefinition set = getCurrentSet();
        String id = set != null ? set.id : null;
        if (!java.util.Objects.equals(id, backgroundSetId))
        {
            initBackgroundBlocks();
        }
    }

    private void initBackgroundBlocks()
    {
        backgroundBlocks.clear();
        backgroundFullBlocks.clear();
        backgroundSpriteCache.clear();
        backgroundTintCache.clear();

        BlockSetConfig.BlockSetDefinition set = getCurrentSet();
        if (set != null)
        {
            set.ensureComputedLevels();
            Map<Integer, BlockSetConfig.SetLevelDefinition> levels = set.computedLevels;
            if (levels != null)
            {
                for (BlockSetConfig.SetLevelDefinition level : levels.values())
                {
                    if (level.blocks == null) continue;
                    for (BlockSetConfig.BlockEntryDefinition entry : level.blocks)
                    {
                        if (entry == null) continue;
                        Block mcBlock = entry.resolveBlock();
                        if (mcBlock == null || mcBlock == Blocks.AIR) continue;
                        if (!mcBlock.defaultBlockState().getFluidState().isEmpty()) continue;
                        backgroundBlocks.add(entry);
                        if (isFullBlock(mcBlock, entry.meta))
                        {
                            backgroundFullBlocks.add(entry);
                        }
                    }
                }
            }
        }
        backgroundSetId = set != null ? set.id : null;

        if (backgroundBlocks.isEmpty())
        {
            OneBlockUltima.getLogger().warn("No blocks found for background, adding defaults");
            addDefaultBackgroundBlocks();
        }
        if (backgroundFullBlocks.isEmpty())
        {
            backgroundFullBlocks.add(createBackgroundEntry("minecraft:dirt"));
        }
    }

    private BlockSetConfig.BlockEntryDefinition createBackgroundEntry(String registry)
    {
        BlockSetConfig.BlockEntryDefinition entry = new BlockSetConfig.BlockEntryDefinition();
        entry.registry = registry;
        entry.meta = 0;
        entry.chance = 100;
        return entry;
    }

    private void addDefaultBackgroundBlocks()
    {
        addBlockEntryIfFull("minecraft:stone");
        addBlockEntryIfFull("minecraft:dirt");
        addBlockEntryIfFull("minecraft:cobblestone");
        addBlockEntryIfFull("minecraft:oak_planks");
        addBlockEntryIfFull("minecraft:sand");
        addBlockEntryIfFull("minecraft:gravel");
        addBlockEntryIfFull("minecraft:netherrack");
        addBlockEntryIfFull("minecraft:end_stone");
        addBlockEntryIfFull("minecraft:bricks");
        addBlockEntryIfFull("minecraft:stone_bricks");
        addBlockEntryIfFull("minecraft:quartz_block");
    }

    private void addBlockEntryIfFull(String registry)
    {
        try
        {
            BlockSetConfig.BlockEntryDefinition entry = createBackgroundEntry(registry);
            Block block = entry.resolveBlock();
            if (block != null && block != Blocks.AIR && isFullBlock(block, 0))
            {
                backgroundBlocks.add(entry);
                backgroundFullBlocks.add(entry);
            }
        }
        catch (Exception ignored) {}
    }

    private void rebuildView()
    {
        this.renderables.clear();
        this.children().clear();
        refreshBackgroundForCurrentSet();
        if (activeView == VIEW_SETS) buildSetsView();
        else if (activeView == VIEW_SETTINGS) buildSettingsView();
        else if (activeView == VIEW_DONATE) buildDonateView();
    }

    private void changeView(int view)
    {
        if (activeView == view) return;
        blockScroll = 0;
        mobScroll = 0;
        conditionsScroll = 0;
        activeView = view;
        rebuildView();
    }

    private int getContentWidth()
    {
        return imageWidth - 16;
    }

    private void buildSetsView()
    {
        syncActiveSetFromGenerator();
        BlockSetConfig.BlockSetDefinition set = getCurrentSet();
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;

        int row1Y = y + INFO_OFFSET_Y + computeInfoHeight();
        addButton(BUTTON_PREV_SET, x + getHorizontalMargin(), row1Y, 20, BUTTON_HEIGHT, "<");
        addButton(BUTTON_NEXT_SET, x + getHorizontalMargin() + 20 + 6, row1Y, 20, BUTTON_HEIGHT, ">");

        int row2Y = row1Y + BUTTON_HEIGHT + BUTTON_GAP;
        int contentW = getContentAreaWidth();
        int halfW = (contentW - BUTTON_GAP) / 2;
        selectButton = null;
        upgradeButton = null;
        selectButton = addButton(BUTTON_SELECT_SET, x + getHorizontalMargin(), row2Y, halfW, BUTTON_HEIGHT, I18n.get("gui.oneblockultima.select"));
        if (set != null)
        {
            upgradeButton = addButtonPrimary(BUTTON_UPGRADE_SET, x + getHorizontalMargin() + halfW + BUTTON_GAP, row2Y, halfW, BUTTON_HEIGHT, I18n.get("gui.oneblockultima.upgrade"));
        }
    }

    private void buildSettingsView()
    {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int buttonWidth = (getContentAreaWidth() - BUTTON_GAP) / 2;

        int row1Y = y + TABS_Y + TAB_HEIGHT + CONTENT_TOP_PADDING;
        addButton(BUTTON_TOGGLE_FLUIDS, x + getHorizontalMargin(), row1Y, buttonWidth, BUTTON_HEIGHT, getFluidToggleLabel());
        addButton(BUTTON_TOGGLE_MOBS, x + getHorizontalMargin() + buttonWidth + BUTTON_GAP, row1Y, buttonWidth, BUTTON_HEIGHT, getMobToggleLabel());

        int row2Y = row1Y + BUTTON_HEIGHT + BUTTON_GAP;
        addButton(BUTTON_TOGGLE_CHESTS, x + getHorizontalMargin(), row2Y, buttonWidth, BUTTON_HEIGHT, getChestToggleLabel());
        addButton(BUTTON_TOGGLE_SAPLINGS, x + getHorizontalMargin() + buttonWidth + BUTTON_GAP, row2Y, buttonWidth, BUTTON_HEIGHT, getSaplingToggleLabel());

        int row3Y = row2Y + BUTTON_HEIGHT + BUTTON_GAP;
        addButton(BUTTON_OPEN_PRICES, x + getHorizontalMargin(), row3Y, buttonWidth, BUTTON_HEIGHT, I18n.get("gui.oneblockultima.settings.open_prices"));
        addButton(BUTTON_OPEN_CONFIG_EDITOR, x + getHorizontalMargin() + buttonWidth + BUTTON_GAP, row3Y, buttonWidth, BUTTON_HEIGHT, I18n.get("gui.oneblockultima.config.sets_title"));

        int row4Y = row3Y + BUTTON_HEIGHT + BUTTON_GAP;
        addButton(BUTTON_OPEN_UI_SETTINGS, x + getHorizontalMargin(), row4Y, buttonWidth, BUTTON_HEIGHT, I18n.get("gui.oneblockultima.ui_settings.title"));
        addButton(BUTTON_OPEN_MISC_SETTINGS, x + getHorizontalMargin() + buttonWidth + BUTTON_GAP, row4Y, buttonWidth, BUTTON_HEIGHT, I18n.get("gui.oneblockultima.misc.title"));
    }

    private void buildDonateView()
    {
    }

    private String getFluidToggleLabel()
    {
        ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator gen = container.getGenerator();
        boolean on = gen != null && !gen.isDisableFluidGeneration();
        return I18n.get("gui.oneblockultima.settings.fluid") + ": " + (on ? I18n.get("gui.on") : I18n.get("gui.off"));
    }

    private String getMobToggleLabel()
    {
        ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator gen = container.getGenerator();
        boolean on = gen != null && !gen.isDisableMobGeneration();
        return I18n.get("gui.oneblockultima.settings.mobs") + ": " + (on ? I18n.get("gui.on") : I18n.get("gui.off"));
    }

    private String getChestToggleLabel()
    {
        ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator gen = container.getGenerator();
        boolean on = gen != null && !gen.isDisableChestGeneration();
        return I18n.get("gui.oneblockultima.settings.chests") + ": " + (on ? I18n.get("gui.on") : I18n.get("gui.off"));
    }

    private String getSaplingToggleLabel()
    {
        ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator gen = container.getGenerator();
        boolean on = gen != null && !gen.isDisableSaplingGeneration();
        return I18n.get("gui.oneblockultima.settings.saplings") + ": " + (on ? I18n.get("gui.on") : I18n.get("gui.off"));
    }

    public BlockSetConfig.BlockSetDefinition getCurrentSet()
    {
        if (visibleSets == null || visibleSets.isEmpty()) return null;
        if (selectedSetIndex < 0 || selectedSetIndex >= visibleSets.size()) selectedSetIndex = 0;
        return visibleSets.get(selectedSetIndex);
    }

    private void populateVisibleSets()
    {
        visibleSets.clear();
        List<BlockSetConfig.BlockSetDefinition> sets = BlockSetConfig.get().getSets();
        if (sets != null)
        {
            for (BlockSetConfig.BlockSetDefinition set : sets)
            {
                if (set != null && set.isAvailable())
                {
                    visibleSets.add(set);
                }
            }
        }
    }

    private int getCurrentLevel(BlockSetConfig.BlockSetDefinition set)
    {
        TileEntityOneBlockGenerator gen = container.getGenerator();
        int lvl = gen == null ? 0 : gen.getSetLevel(set.id);
        return Mth.clamp(lvl, 0, set.getMaxLevel());
    }

    private String getGeneratorActiveSetId()
    {
        TileEntityOneBlockGenerator gen = container.getGenerator();
        return gen == null ? null : gen.getSelectedSetId();
    }

    private void refreshActiveSetFromGenerator()
    {
        String active = getGeneratorActiveSetId();
        if (active == null) return;
        int idx = 0;
        for (BlockSetConfig.BlockSetDefinition s : visibleSets)
        {
            if (s.id != null && s.id.equals(active))
            {
                selectedSetIndex = idx;
                break;
            }
            idx++;
        }
    }

    private void syncActiveSetFromGenerator()
    {
        String active = getGeneratorActiveSetId();
        if (active == null || active.equals(activeSetSyncId)) return;
        activeSetSyncId = active;
        int idx = 0;
        for (BlockSetConfig.BlockSetDefinition s : visibleSets)
        {
            if (s.id != null && s.id.equals(active))
            {
                if (idx != selectedSetIndex)
                {
                    selectedSetIndex = idx;
                    blockScroll = 0;
                    mobScroll = 0;
                }
                break;
            }
            idx++;
        }
    }

    private String getLocalizedSetName(BlockSetConfig.BlockSetDefinition set)
    {
        if (set == null) return "";
        return ru.defea.oneblockultima.gui.containers.ContainerSetsConfig.getLocalizedSetName(set);
    }

    private void actionPerformed(int id)
    {
        switch (id)
        {
            case BUTTON_PREV_SET:
                if (selectedSetIndex > 0) selectedSetIndex--;
                else selectedSetIndex = Math.max(0, visibleSets.size() - 1);
                blockScroll = 0; mobScroll = 0;
                rebuildView();
                break;
            case BUTTON_NEXT_SET:
                if (selectedSetIndex < visibleSets.size() - 1) selectedSetIndex++;
                else selectedSetIndex = 0;
                blockScroll = 0; mobScroll = 0;
                rebuildView();
                break;
            case BUTTON_SELECT_SET:
            {
                BlockSetConfig.BlockSetDefinition set = getCurrentSet();
                if (set != null)
                {
                    container.selectSet(set.id);
                    rebuildView();
                }
                break;
            }
            case BUTTON_UPGRADE_SET:
            {
                BlockSetConfig.BlockSetDefinition set = getCurrentSet();
                if (set != null)
                {
                    container.upgradeSet(set.id);
                    rebuildView();
                }
                break;
            }
            case BUTTON_OPEN_CONFIG_EDITOR:
                minecraft.setScreen(new GuiSetsConfig(this));
                break;
            case BUTTON_TOGGLE_FLUIDS:
                container.toggleFluidGeneration(); rebuildView();
                break;
            case BUTTON_TOGGLE_MOBS:
                container.toggleMobGeneration(); rebuildView();
                break;
            case BUTTON_TOGGLE_CHESTS:
                container.toggleChestGeneration(); rebuildView();
                break;
            case BUTTON_TOGGLE_SAPLINGS:
                container.toggleSaplingGeneration(); rebuildView();
                break;
            case BUTTON_OPEN_PRICES:
                minecraft.setScreen(new GuiBlockPrices(this));
                break;
            case BUTTON_OPEN_UI_SETTINGS:
                minecraft.setScreen(new GuiUiSettings(this));
                break;
            case BUTTON_OPEN_MISC_SETTINGS:
                minecraft.setScreen(new GuiMiscSettings(this));
                break;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY)
    {
    }

    @Override
    public void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY)
    {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        int headerBottom = y + HEADER_OFFSET + HEADER_H;
        int contentTop = y + TABS_Y + TAB_HEIGHT;
        g.fill(x, y, x + w, headerBottom, DARK_GRAY_COLOR_3);
        g.fill(x, headerBottom, x + w, contentTop, DARK_BLUE_GRAY_COLOR_2);
        if (y + h > contentTop + 1)
        {
            drawProceduralBackground(g, x, contentTop, w, y + h - contentTop);
            g.fill(x, contentTop, x + w, y + h, TRANSPARENT_DARK_GRAY_COLOR_1);
        }

        layout(x, y, w, h);
        closeButtonX = x + CLOSE_BUTTON_OFFSET;
        closeButtonY = y + CLOSE_BUTTON_OFFSET;
        closeButtonHovered = mouseX >= closeButtonX && mouseX <= closeButtonX + CLOSE_BUTTON_SIZE &&
                mouseY >= closeButtonY && mouseY <= closeButtonY + CLOSE_BUTTON_SIZE;

        drawHeader(g, headerX, headerY, headerW, headerH, mouseX, mouseY, partialTicks);
        drawTabs(g, x, mouseX, mouseY);

        if (activeView == VIEW_SETS)
        {
            syncActiveSetFromGenerator();
            infoH = computeInfoHeight();
            drawInfo(g, infoX, infoY, infoW, infoH, mouseX, mouseY, partialTicks);
            drawPanels(g, x, y, w, h, mouseX, mouseY, partialTicks);
        }
        else if (activeView == VIEW_DONATE)
        {
            drawDonate(g, x, y, w, h, mouseX, mouseY);
        }
    }

    private int getHorizontalMargin()
    {
        return Math.max(12, Math.min(24, this.imageWidth / 24));
    }

    private int getContentAreaWidth()
    {
        return Math.max(120, this.imageWidth - getHorizontalMargin() * 2);
    }

    private void layout(int x, int y, int w, int h)
    {
        int hm = getHorizontalMargin();
        int contentW = getContentAreaWidth();
        headerX = x; headerY = y + HEADER_OFFSET; headerW = w; headerH = HEADER_H;
        infoX = x + hm; infoY = y + INFO_OFFSET_Y; infoW = contentW; infoH = computeInfoHeight();
        int rowsY = infoY + infoH;
        int panelsTop = rowsY + BUTTON_HEIGHT * 2 + SECTION_GAP * 3;
        int halfW = (contentW - SECTION_GAP) / 2;
        lpX = x + hm; lpY = panelsTop; lpW = halfW; lpH = (y + h - 8) - panelsTop;
        rpX = x + hm + halfW + SECTION_GAP; rpY = panelsTop; rpW = halfW; rpH = lpH;
    }

    private int computeInfoHeight()
    {
        BlockSetConfig.BlockSetDefinition set = getCurrentSet();
        int lines = 3;
        int condRows = 0;
        if (set != null)
        {
            int currentLevel = getCurrentLevel(set);
            if (currentLevel <= 0 && set.unlockConditions != null && set.unlockConditions.conditions != null)
            {
                condRows = set.unlockConditions.conditions.size();
            }
        }
        int desired = lines * font.lineHeight + Math.max(0, lines - 1) * 4 + 8
                + (condRows > 0 ? font.lineHeight + 3 + condRows * (font.lineHeight + 2) + 6 : 0);
        int maxInfo = (topPos + imageHeight - 8) - MIN_PANEL_HEIGHT - (BUTTON_HEIGHT * 2 + SECTION_GAP * 3) - (topPos + INFO_OFFSET_Y);
        return Math.max(8, Math.min(desired, maxInfo));
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTicks)
    {
        String title = I18n.get("tile.one_block_generator.name");
        g.drawCenteredString(font, Component.literal(title), x + w / 2, y + font.lineHeight, WHITE_COLOR_1);

        IOneBlockPlayerData playerData = this.minecraft.player != null
                ? this.minecraft.player.getCapability(OneBlockPlayerDataProvider.ONE_BLOCK_PLAYER_DATA).orElse(null) : null;
        int brokenTotal = playerData == null ? 0 : playerData.getBrokenBlocksCount();
        String brokenLabel = I18n.get("gui.oneblockultima.blocks_broken_total") + ":";
        int brokenX = closeButtonX + CLOSE_BUTTON_SIZE + 6;
        int brokenY = y + font.lineHeight;
        g.drawString(font, Component.literal(brokenLabel), brokenX, brokenY, LIGHT_GRAY_COLOR_2, false);
        g.drawString(font, Component.literal(String.valueOf(brokenTotal)), brokenX + font.width(Component.literal(brokenLabel)) + 2, brokenY, WHITE_COLOR_1, false);

        double currency = playerData == null ? 0 : playerData.getCurrency();
        String balanceValue = formatCurrency(currency);
        int iconSize = 12;
        int padding = 4;
        int balanceWidth = font.width(Component.literal(balanceValue));
        int boxWidth = iconSize + 2 + balanceWidth + padding * 2;
        int boxX = x + w - 10 - boxWidth;
        int boxY = y + 4;
        int iconX = boxX + padding;
        int iconY = boxY + padding;
        int numberX = iconX + iconSize + 2;
        int numberY = iconY + 2;
        RenderSystem.setShaderTexture(0, ru.defea.oneblockultima.Constants.COIN_TEXTURE);
        g.blit(ru.defea.oneblockultima.Constants.COIN_TEXTURE, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        g.drawString(font, Component.literal(balanceValue), numberX, numberY, GOLD_COLOR, true);

        int closeBg = closeButtonHovered ? REDDISH_COLOR : DARK_GRAY_COLOR_2;
        int closeBorder = closeButtonHovered ? WHITE_COLOR_1 : GRAY_COLOR_7;
        g.fill(closeButtonX, closeButtonY, closeButtonX + CLOSE_BUTTON_SIZE, closeButtonY + CLOSE_BUTTON_SIZE, closeBg);
        g.fill(closeButtonX, closeButtonY, closeButtonX + CLOSE_BUTTON_SIZE, closeButtonY + 1, closeBorder);
        g.fill(closeButtonX, closeButtonY + CLOSE_BUTTON_SIZE - 1, closeButtonX + CLOSE_BUTTON_SIZE, closeButtonY + CLOSE_BUTTON_SIZE, closeBorder);
        g.fill(closeButtonX, closeButtonY, closeButtonX + 1, closeButtonY + CLOSE_BUTTON_SIZE, closeBorder);
        g.fill(closeButtonX + CLOSE_BUTTON_SIZE - 1, closeButtonY, closeButtonX + CLOSE_BUTTON_SIZE, closeButtonY + CLOSE_BUTTON_SIZE, closeBorder);
        g.drawCenteredString(font, Component.literal("\u2715"), closeButtonX + CLOSE_BUTTON_SIZE / 2, closeButtonY + 3, WHITE_COLOR_1);
    }

    private void drawTabs(GuiGraphics g, int x, int mouseX, int mouseY)
    {
        int tw = imageWidth / 3;
        int ty = topPos + TABS_Y;
        tabY = ty;
        tabW = tw;
        String[] labels = new String[]{
                I18n.get("gui.oneblockultima.tabs.sets"),
                I18n.get("gui.oneblockultima.tabs.settings"),
                I18n.get("gui.oneblockultima.tabs.donate")};
        for (int i = 0; i < 3; i++)
        {
            int cx = x + i * tw;
            tabX[i] = cx;
            boolean active = activeView == tabView[i];
            boolean hovered = mouseX >= cx && mouseX < cx + tw && mouseY >= ty && mouseY < ty + TAB_HEIGHT;
            int bg = active ? DARK_BLUE_GRAY_COLOR_1 : hovered ? DARK_GRAY_COLOR_2 : PANEL_COLOR;
            g.fill(cx, ty, cx + tw, ty + TAB_HEIGHT, bg);
            String label = labels[i];
            g.drawString(font, Component.literal(label),
                    cx + (tw - font.width(Component.literal(label))) / 2,
                    ty + (TAB_HEIGHT - 8) / 2, active ? WHITE_COLOR_1 : LIGHT_BLUE_GRAY_COLOR, true);
        }
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

    private void drawInfo(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTicks)
    {
        BlockSetConfig.BlockSetDefinition set = getCurrentSet();
        if (set == null) return;
        int currentLevel = getCurrentLevel(set);

        TileEntityOneBlockGenerator gen = container.getGenerator();
        String activeSetId = gen != null ? gen.getSelectedSetId() : null;
        String activeSetName = "-";
        if (activeSetId != null)
        {
            BlockSetConfig.BlockSetDefinition activeSetDef = BlockSetConfig.get().getSet(activeSetId);
            activeSetName = activeSetDef == null ? activeSetId : getLocalizedSetName(activeSetDef);
        }
        String activeSetString = I18n.get("gui.oneblockultima.active_set");
        int rightTextX = x + w - font.width(Component.literal(activeSetString));
        g.drawString(font, Component.literal(activeSetString + ":"), rightTextX, y, 0xFFA0B0C0, false);
        g.drawString(font, Component.literal(activeSetName), rightTextX, y + font.lineHeight + 2, 0xFFFFFFFF, false);

        boolean isActiveSet = set.id != null && set.id.equals(activeSetId);
        int setColor = currentLevel <= 0 ? 0xFFFF7D7D : isActiveSet ? 0xFF7CEC9F : 0xFFFFFFFF;
        String setTitle = getLocalizedSetName(set) + " " + I18n.get("gui.oneblockultima.lv") + currentLevel;
        if (isActiveSet)
        {
            setTitle += " (" + I18n.get("gui.oneblockultima.selected") + ")";
        }
        else if (currentLevel <= 0)
        {
            setTitle += " (" + I18n.get("gui.oneblockultima.locked") + ")";
        }
        g.drawString(font, Component.literal(setTitle), x, y, setColor, false);

        int ty = y + rowInterval();
        if (currentLevel <= 0)
        {
            g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.unlock_cost") + ": " + set.unlockCost), x + 4, ty, 0xFFC0C0C0, false);
        }
        else
        {
            BlockSetConfig.SetLevelDefinition next = set.getLevel(currentLevel + 1);
            if (next != null)
            {
                g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.upgrade_cost") + ": " + next.upgradeCost), x + 4, ty, 0xFFC0C0C0, false);
            }
            else
            {
                g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.max_level")), x + 4, ty, 0xFFC0C0C0, false);
            }
        }

        IOneBlockPlayerData data = this.minecraft.player != null
                ? this.minecraft.player.getCapability(OneBlockPlayerDataProvider.ONE_BLOCK_PLAYER_DATA).orElse(null) : null;
        int broken = data == null ? 0 : data.getBrokenBlocksCount(set.id);
        g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.blocks_broken") + ": " + broken), x + 4, y + rowInterval() * 2, 0xFFC0C0C0, false);

        if (currentLevel <= 0 && set.unlockConditions != null && set.unlockConditions.conditions != null &&
                !set.unlockConditions.conditions.isEmpty())
        {
            List<BlockSetConfig.UnlockConditionDefinition> conds = set.unlockConditions.conditions;
            String title = I18n.get("gui.oneblockultima.unlock_conditions") + ": "
                    + I18n.get("gui.oneblockultima.config." + set.unlockConditions.mode);
            int titleWidth = font.width(Component.literal(title));
            int columnWidth = titleWidth;
            for (BlockSetConfig.UnlockConditionDefinition c : conds)
            {
                columnWidth = Math.max(columnWidth, font.width(Component.literal(" - " + formatUnlockCondition(c, data) + " \u2713")));
            }
            int startX = x + (w - columnWidth) / 2;
            g.drawString(font, Component.literal(title), startX + titleWidth / 2, y, 0xFFA0B0C0, false);

            int rowH = font.lineHeight + 1;
            int condTop = y + font.lineHeight + 2;
            int condBot = y + h;
            int visible = Math.max(0, (condBot - condTop) / rowH);
            condMaxScroll = Math.max(0, conds.size() - visible);
            condVisibleRows = conds.size() - condMaxScroll;
            conditionsScroll = clamp(conditionsScroll, 0, condMaxScroll);
            condScrollTopY = condTop;
            condScrollBotY = condBot;
            int start = conditionsScroll;
            for (int i = start, cy = condTop; i < conds.size() && cy < condBot - rowH; i++)
            {
                BlockSetConfig.UnlockConditionDefinition c = conds.get(i);
                boolean sat = data != null && c.isSatisfied(data, container.getGenerator());
                g.drawString(font, Component.literal(" - " + formatUnlockCondition(c, data) + " \u2713"), startX, cy, sat ? 0x55FF55 : 0xFFFF7D7D, false);
                cy += rowH;
            }
            if (condMaxScroll > 0)
            {
                drawScrollbar(g, x + w - SCROLLBAR_WIDTH, condTop, condBot - condTop, start, condMaxScroll, condVisibleRows, mouseX, mouseY);
            }
        }

        if (selectButton != null)
        {
            selectButton.setMessage(Component.literal(currentLevel <= 0 ? I18n.get("gui.oneblockultima.locked") :
                    (isActiveSet ? I18n.get("gui.oneblockultima.selected") : I18n.get("gui.oneblockultima.select"))));
            selectButton.active = currentLevel > 0 && !isActiveSet;
        }
        if (upgradeButton != null)
        {
            if (currentLevel <= 0)
            {
                upgradeButton.setMessage(Component.literal(I18n.get("gui.oneblockultima.unlock")));
                upgradeButton.active = true;
            }
            else if (set.getLevel(currentLevel + 1) != null)
            {
                upgradeButton.setMessage(Component.literal(I18n.get("gui.oneblockultima.upgrade")));
                upgradeButton.active = true;
            }
            else
            {
                upgradeButton.setMessage(Component.literal(I18n.get("gui.oneblockultima.max")));
                upgradeButton.active = false;
            }
        }
    }

    private String getSetNameById(String id)
    {
        if (id == null) return "?";
        List<BlockSetConfig.BlockSetDefinition> sets = BlockSetConfig.get().getSets();
        if (sets != null)
        {
            for (BlockSetConfig.BlockSetDefinition s : sets)
            {
                if (id.equals(s.id)) return getLocalizedSetName(s);
            }
        }
        return id;
    }

    private String formatUnlockCondition(BlockSetConfig.UnlockConditionDefinition c, IOneBlockPlayerData data)
    {
        String type = c.type == null ? "" : c.type.toLowerCase(Locale.ROOT);
        if ("set_level".equals(type))
        {
            TileEntityOneBlockGenerator gen = container.getGenerator();
            int cur = gen != null ? gen.getSetLevel(c.setId) : (data != null ? data.getSetLevel(c.setId) : 0);
            return I18n.get("gui.oneblockultima.condition.set_level", getSetNameById(c.setId), cur, c.level);
        }
        else if ("broken_blocks".equals(type))
        {
            return I18n.get("gui.oneblockultima.condition.broken_blocks", c.count, getSetNameById(c.setId));
        }
        else if ("broken_blocks_total".equals(type))
        {
            return I18n.get("gui.oneblockultima.condition.broken_blocks_total", c.count);
        }
        return I18n.get("gui.oneblockultima.condition.unknown", c.type == null ? "?" : c.type);
    }

    private int rowInterval()
    {
        return font.lineHeight + 4;
    }

    private PanelGeometry computePanelGeometry(int px, int py, int pw, int ph, BlockSetConfig.SetLevelDefinition level, boolean showCase, int caseDropPercentDisplay, double caseScaleFactor)
    {
        int availableWidth = pw - INNER_PADDING * 2;
        int halfWidth = (availableWidth - SECTION_GAP) / 2;
        int blockAreaWidth = halfWidth - 8;
        int mobAreaWidth = halfWidth - 8;
        int blockCols = Math.max(2, blockAreaWidth / 20);
        int mobCols = blockCols;
        int cellPadding = Math.max(1, Math.min(2, blockAreaWidth / 80));
        int cellSize = Math.max(14, Math.min(20, (mobAreaWidth - (mobCols - 1) * cellPadding) / mobCols));
        int blocksW = blockCols * (cellSize + cellPadding) - cellPadding;
        int mobsW = mobCols * (cellSize + cellPadding) - cellPadding;
        int mobsStartX = px + INNER_PADDING + blocksW + SECTION_GAP + 8;
        int gridStartY = py + rowInterval() * 2;
        int areaH = Math.max(0, ph - rowInterval() * 2);
        int blockScrollbarX = px + INNER_PADDING + blocksW + 2;
        int mobScrollbarX = mobsStartX + mobsW + 2;

        int blockTotal = 0;
        int mobTotal = 0;
        if (level != null)
        {
            blockTotal = (level.blocks != null ? level.blocks.size() : 0) + (showCase ? 1 : 0);
            mobTotal = level.mobs != null ? level.mobs.size() : 0;
        }
        int blockRows = blockTotal == 0 ? 0 : (blockTotal + blockCols - 1) / blockCols;
        int mobRows = mobTotal == 0 ? 0 : (mobTotal + mobCols - 1) / mobCols;
        int cellStep = cellSize + cellPadding;
        int visibleRows = Math.max(1, areaH / cellStep);
        int blockMaxScroll = Math.max(0, blockRows - visibleRows);
        int mobMaxScroll = Math.max(0, mobRows - visibleRows);

        return new PanelGeometry(px, py, pw, ph,
                cellSize, cellPadding, blockCols, mobCols,
                gridStartY, areaH, blocksW, mobsW, mobsStartX,
                blockScrollbarX, mobScrollbarX,
                blockRows, mobRows, blockMaxScroll, mobMaxScroll,
                visibleRows, visibleRows,
                showCase, caseDropPercentDisplay, caseScaleFactor);
    }

    private void drawPanels(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTicks)
    {
        BlockSetConfig.BlockSetDefinition set = getCurrentSet();
        if (set == null) return;
        int currentLevel = getCurrentLevel(set);
        boolean canShowCurrent = currentLevel > 0;
        BlockSetConfig.SetLevelDefinition cur = set.getLevelClamped(currentLevel);

        boolean showCase = set.hasCaseEntries();
        double caseDropPercent = showCase ? ru.defea.oneblockultima.config.ModSettings.get().getCaseDropPercent() : 0.0D;
        if (caseDropPercent < 0.0D) caseDropPercent = 0.0D;
        if (caseDropPercent > 100.0D) caseDropPercent = 100.0D;
        showCase = showCase && caseDropPercent > 0.0D;
        int caseDropPercentDisplay = (int) Math.round(caseDropPercent);
        double caseScaleFactor = (100.0D - caseDropPercent) / 100.0D;

        int areaW = w - 16;
        int gap = Math.max(4, areaW / 40);
        int pW = (areaW - gap) / 2;
        int pX1 = x + 8;
        int pX2 = x + 8 + pW + gap;
        int pY = lpY;
        int pH = lpH;
        lpX = pX1;
        lpW = pW;
        rpX = pX2;
        rpW = pW;
        leftGeom = null;
        rightGeom = null;

        if (canShowCurrent)
        {
            leftGeom = computePanelGeometry(pX1, pY, pW, pH, cur, showCase, caseDropPercentDisplay, caseScaleFactor);
            blockScroll = clamp(blockScroll, 0, leftGeom.blockMaxScroll);
            mobScroll = clamp(mobScroll, 0, leftGeom.mobMaxScroll);
            renderLevelPanel(g, cur, leftGeom, true, blockScroll, mobScroll, mouseX, mouseY, partialTicks);
        }

        BlockSetConfig.SetLevelDefinition nxt = set.getLevel(currentLevel <= 0 ? 1 : currentLevel + 1);
        if (nxt != null)
        {
            int nx = canShowCurrent ? pX2 : pX1;
            int nw = canShowCurrent ? pW : areaW;
            if (!canShowCurrent)
            {
                rpX = nx;
                rpW = nw;
            }
            rightGeom = computePanelGeometry(nx, pY, nw, pH, nxt, showCase, caseDropPercentDisplay, caseScaleFactor);
            blockScrollNext = clamp(blockScrollNext, 0, rightGeom.blockMaxScroll);
            mobScrollNext = clamp(mobScrollNext, 0, rightGeom.mobMaxScroll);
            renderLevelPanel(g, nxt, rightGeom, false, blockScrollNext, mobScrollNext, mouseX, mouseY, partialTicks);
        }

        if (canShowCurrent && nxt != null)
        {
            int separatorX = pX1 + pW + gap / 2;
            g.fill(separatorX, pY + 5, separatorX + 1, pY + pH - 5, GRAY_COLOR_8);
        }
    }

    private void renderLevelPanel(GuiGraphics g, BlockSetConfig.SetLevelDefinition level, PanelGeometry geo, boolean isLeft, int blockScroll, int mobScroll, int mouseX, int mouseY, float partialTicks)
    {
        if (level == null) return;

        int headerColor = LIGHT_BLUE_GRAY_COLOR;
        g.drawString(font, Component.literal(I18n.get(isLeft ? "gui.oneblockultima.current_level" : "gui.oneblockultima.next_level") + ": " + level.level),
                geo.panelX + INNER_PADDING, geo.panelY + 4, headerColor, true);

        List<BlockSetConfig.BlockEntryDefinition> blocks = level.blocks != null ? level.blocks : new ArrayList<>();
        List<BlockSetConfig.MobEntryDefinition> mobs = level.mobs != null ? level.mobs : new ArrayList<>();

        if (geo.showCase || !blocks.isEmpty())
        {
            g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.possible_blocks") + ":"),
                    geo.panelX + INNER_PADDING, geo.panelY + rowInterval(), headerColor, false);
            renderBlockCells(g, geo, blocks, blockScroll, mouseX, mouseY);
            drawScrollbar(g, geo.blockScrollbarX, geo.gridStartY, geo.areaH, blockScroll, geo.blockMaxScroll, geo.blockVisibleRows, mouseX, mouseY);
        }

        if (!mobs.isEmpty())
        {
            g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.mobs") + ":"),
                    geo.mobsStartX, geo.panelY + rowInterval(), headerColor, false);
            renderMobCells(g, geo, mobs, mobScroll, mouseX, mouseY);
            drawScrollbar(g, geo.mobScrollbarX, geo.gridStartY, geo.areaH, mobScroll, geo.mobMaxScroll, geo.mobVisibleRows, mouseX, mouseY);
        }
    }

    private void renderBlockCells(GuiGraphics g, PanelGeometry geo, List<BlockSetConfig.BlockEntryDefinition> blocks, int scroll, int mouseX, int mouseY)
    {
        int total = blocks.size() + (geo.showCase ? 1 : 0);
        int cellStep = geo.cellSize + geo.cellPadding;
        int visibleRows = Math.max(1, geo.areaH / cellStep);
        int end = Math.min(geo.blockTotalRows, scroll + visibleRows);
        for (int row = scroll; row < end; row++)
        {
            for (int col = 0; col < geo.blockCols; col++)
            {
                int index = row * geo.blockCols + col;
                if (index >= total) break;
                int cellX = geo.panelX + INNER_PADDING + col * cellStep;
                int cellY = geo.gridStartY + (row - scroll) * cellStep;
                if (cellY + geo.cellSize < geo.gridStartY || cellY > geo.gridStartY + geo.areaH) continue;
                boolean hovered = mouseX >= cellX && mouseX < cellX + geo.cellSize && mouseY >= cellY && mouseY < cellY + geo.cellSize;

                if (geo.showCase && index == blocks.size())
                {
                    renderCaseCell(g, geo, cellX, cellY, hovered);
                    continue;
                }
                if (index >= blocks.size()) break;
                renderBlockCell(g, geo, blocks.get(index), cellX, cellY, hovered);
            }
        }
    }

    private void renderBlockCell(GuiGraphics g, PanelGeometry geo, BlockSetConfig.BlockEntryDefinition be, int cellX, int cellY, boolean hovered)
    {
        drawCellBg(g, cellX, cellY, geo.cellSize, hovered);
        int ix = cellX + (geo.cellSize - 16) / 2;
        int iy = cellY + (geo.cellSize - 16) / 2;
        Block block = be.resolveBlock();
        if (block instanceof LiquidBlock)
        {
            int fs = geo.cellSize - 8;
            drawFluidIcon(g, block, cellX + (geo.cellSize - fs) / 2, cellY + (geo.cellSize - fs) / 2, fs);
        }
        else if (block instanceof ChestBlock)
        {
            g.renderFakeItem(new ItemStack(Items.CHEST), ix, iy);
        }
        else if (block != null)
        {
            Item item = block.asItem();
            if (block instanceof ru.defea.oneblockultima.block.BlockCompressedBase)
            {
                Item levelItem = ru.defea.oneblockultima.block.ModBlocks.getCompressedItem(block, be.meta);
                if (levelItem != null && levelItem != Items.AIR) item = levelItem;
            }
            if (item != null && item != Items.AIR) g.renderFakeItem(new ItemStack(item), ix, iy);
            else if (block != null)
            {
                try
                {
                    ModelUtil.renderBlockModelToGUI(g, block.defaultBlockState(), cellX, cellY, geo.cellSize);
                }
                catch (Exception ignored) {}
            }
        }
        int displayChance = geo.showCase ? (int) Math.round(be.chance * geo.caseScaleFactor) : be.chance;
        drawPercent(g, geo, cellX, cellY, displayChance);
        if (hovered)
        {
            Component tip = block != null ? block.getName() : Component.literal(be.registry);
            setTooltipForNextRenderPass(tip.copy().append(Component.literal("  " + displayChance + "%")));
        }
    }

    private void renderMobCells(GuiGraphics g, PanelGeometry geo, List<BlockSetConfig.MobEntryDefinition> mobs, int scroll, int mouseX, int mouseY)
    {
        int cellStep = geo.cellSize + geo.cellPadding;
        int visibleRows = Math.max(1, geo.areaH / cellStep);
        int end = Math.min(geo.mobTotalRows, scroll + visibleRows);
        for (int row = scroll; row < end; row++)
        {
            for (int col = 0; col < geo.mobCols; col++)
            {
                int index = row * geo.mobCols + col;
                if (index >= mobs.size()) break;
                int cellX = geo.mobsStartX + col * cellStep;
                int cellY = geo.gridStartY + (row - scroll) * cellStep;
                if (cellY + geo.cellSize < geo.gridStartY || cellY > geo.gridStartY + geo.areaH) continue;
                boolean hovered = mouseX >= cellX && mouseX < cellX + geo.cellSize && mouseY >= cellY && mouseY < cellY + geo.cellSize;
                BlockSetConfig.MobEntryDefinition me = mobs.get(index);
                drawCellBg(g, cellX, cellY, geo.cellSize, hovered);
                EntityType<?> et = resolveMob(me.registry);
                boolean renderedModel = false;
                if (et != null)
                {
                    Entity entity = resolveMobEntity(et);
                    if (entity instanceof LivingEntity living)
                    {
                        renderedModel = renderMobOntoCell(g, living, cellX, cellY, geo.cellSize, geo.cellPadding);
                    }
                }
                if (!renderedModel)
                {
                    int ix = cellX + (geo.cellSize - 16) / 2;
                    int iy = cellY + (geo.cellSize - 16) / 2;
                    if (et != null)
                    {
                        Item egg = SpawnEggItem.byId(et);
                        if (egg != null) g.renderFakeItem(new ItemStack(egg), ix, iy);
                    }
                }
                int displayChance = geo.showCase ? (int) Math.round(me.chance * geo.caseScaleFactor) : me.chance;
                drawPercent(g, geo, cellX, cellY, displayChance);
                if (hovered && et != null)
                {
                    setTooltipForNextRenderPass(Component.literal(I18n.get(et.getDescriptionId()) + "  " + displayChance + "%"));
                }
            }
        }
    }

    private void renderCaseCell(GuiGraphics g, PanelGeometry geo, int cellX, int cellY, boolean hovered)
    {
        drawCellBg(g, cellX, cellY, geo.cellSize, hovered);
        if (ru.defea.oneblockultima.block.ModBlocks.CASE_BLOCK != null)
        {
            ModelUtil.renderBlockModelToGUI(g, ru.defea.oneblockultima.block.ModBlocks.CASE_BLOCK.defaultBlockState(), cellX, cellY, geo.cellSize);
        }
        drawPercent(g, geo, cellX, cellY, geo.caseDropPercentDisplay);
        if (hovered)
        {
            setTooltipForNextRenderPass(Component.literal(I18n.get("gui.oneblockultima.misc.case_drop_percent") + "  " + geo.caseDropPercentDisplay + "%"));
        }
    }

    private void drawPercent(GuiGraphics g, PanelGeometry geo, int cellX, int cellY, int chance)
    {
        String pct = chance + "%";
        int cw = font.width(Component.literal(pct));
        int ccolor = chance < 10 ? RED_COLOR : chance < 20 ? ORANGE_COLOR : GREEN_COLOR;
        RenderSystem.disableDepthTest();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        g.drawString(font, Component.literal(pct), cellX + (geo.cellSize - cw) / 2, cellY + geo.cellSize - font.lineHeight - 1, ccolor, true);
        g.pose().popPose();
        RenderSystem.enableDepthTest();
    }

    private void drawCellBg(GuiGraphics g, int cellX, int cellY, int size, boolean hovered)
    {
        int bg = hovered ? DARK_BLUE_GRAY_COLOR_1 : DARK_GRAY_COLOR_2;
        int border = hovered ? WHITE_COLOR_1 : DARK_GRAY_COLOR_1;
        g.fill(cellX, cellY, cellX + size, cellY + size, bg);
        g.fill(cellX, cellY, cellX + size, cellY + 1, border);
        g.fill(cellX, cellY + size - 1, cellX + size, cellY + size, border);
        g.fill(cellX, cellY, cellX + 1, cellY + size, border);
        g.fill(cellX + size - 1, cellY, cellX + size, cellY + size, border);
    }

    private void drawFluidIcon(GuiGraphics g, Block block, int x, int y, int size)
    {
        net.minecraft.world.level.material.Fluid fluid = block instanceof LiquidBlock lb ? lb.getFluid() : null;
        TextureAtlasSprite sprite = null;
        int tint = 0xFFFFFFFF;
        if (fluid != null)
        {
            try
            {
                var fluidExt = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
                ResourceLocation tex = fluidExt.getStillTexture();
                if (tex == null) tex = fluidExt.getFlowingTexture();
                if (tex != null) sprite = this.minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(tex);
                int c = fluidExt.getTintColor();
                if (c != 0) tint = c | 0xFF000000;
            }
            catch (Exception ignored) { }
        }
        if (sprite == null || sprite.getU0() == sprite.getU1() || sprite.getV0() == sprite.getV1())
        {
            drawFlatFluidFallback(g, block, x, y, size);
            return;
        }

        try
        {
            float r = ((tint >> 16) & 0xFF) / 255.0F;
            float gr = ((tint >> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f pose = g.pose().last().pose();
            float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
            buf.addVertex(pose, x, y + size, 0.0F).setUv(u0, v1).setColor(r, gr, b, 1.0F);
            buf.addVertex(pose, x + size, y + size, 0.0F).setUv(u1, v1).setColor(r, gr, b, 1.0F);
            buf.addVertex(pose, x + size, y, 0.0F).setUv(u1, v0).setColor(r, gr, b, 1.0F);
            buf.addVertex(pose, x, y, 0.0F).setUv(u0, v0).setColor(r, gr, b, 1.0F);
            BufferUploader.drawWithShader(buf.build());
            g.fill(x, y, x + size, y + 1, 0x44000000);
            g.fill(x, y + size - (size / 4), x + size, y + size, 0x33000000);
        }
        catch (Exception ignored)
        {
            drawFlatFluidFallback(g, block, x, y, size);
        }
    }

    private void drawFlatFluidFallback(GuiGraphics g, Block block, int x, int y, int size)
    {
        int color = block.defaultMapColor().col;
        int r = (color >> 16) & 0xFF;
        int gg = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        g.fill(x, y, x + size, y + size, 0xFF000000 | (r << 16) | (gg << 8) | b);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x66000000 | (r << 16) | (gg << 8) | b);
    }

    private Block resolveBlock(String registry)
    {
        if (registry == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(BlockUtil.normalizeLegacyId(registry));
        if (rl == null) return null;
        Block b = BuiltInRegistries.BLOCK.get(rl);
        return b == null || b == Blocks.AIR ? null : b;
    }

    private EntityType<?> resolveMob(String registry)
    {
        if (registry == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(BlockUtil.normalizeLegacyId(registry));
        if (rl == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.get(rl);
    }

    private Entity resolveMobEntity(EntityType<?> et)
    {
        if (mobEntityCacheLevel != this.minecraft.level)
        {
            mobEntityCacheLevel = this.minecraft.level;
            mobEntityCache.clear();
        }
        Entity cached = mobEntityCache.get(et);
        if (cached != null) return cached;
        try
        {
            Entity created = et.create(this.minecraft.level);
            if (created == null) return null;
            if (created instanceof net.minecraft.world.entity.monster.Slime slime)
            {
                slime.setSize(3, false);
            }
            mobEntityCache.put(et, created);
            return created;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private boolean renderMobOntoCell(GuiGraphics g, LivingEntity living, int cellX, int cellY, int cellSize, int cellPadding)
    {
        try
        {
            int iconScale = Math.max(4, cellSize - 2 * cellPadding);
            float[] fit = ModelUtil.computeScreenEntityFit(iconScale, iconScale, living);
            float finalScale = fit[0];
            int ox = cellX + cellSize / 2 - Math.round(fit[1] * finalScale);
            int oy = cellY + cellSize / 2 + Math.round(fit[2] * finalScale);
            long now = System.currentTimeMillis();
            if (now - lastModelFitLog > 1000L)
            {
                lastModelFitLog = now;
                float[] u = ModelUtil.getModelUnits(living);
                ru.defea.oneblockultima.OneBlockUltima.logDebug("[ModelFit] {} hitbox={}x{} units={}x{} offX={} offY={} iconScale={} finalScale={} ox={} oy={}",
                        living.getClass().getSimpleName(),
                        living.getBbWidth(), living.getBbHeight(),
                        u[0], u[1], u[2], u[3], iconScale, finalScale, ox, oy);
            }
            ModelUtil.drawEntityOnScreenScaled(g, ox, oy, living, finalScale);
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h, int value, int max, int visible, int mouseX, int mouseY)
    {
        if (max <= 0 || h <= 0) return;
        g.fill(x, y, x + SCROLLBAR_WIDTH, y + h, DARK_GRAY_COLOR_2);
        int total = max + Math.max(1, visible);
        int thumbH = Math.max(10, Math.min(h - 2, (int) ((long) h * Math.max(1, visible) / Math.max(1, total))));
        if (thumbH > h) thumbH = h;
        int trackLen = h - thumbH;
        int thumbY = y + (int) ((long) trackLen * value / max);
        boolean hovered = mouseX >= x && mouseX < x + SCROLLBAR_WIDTH && mouseY >= thumbY && mouseY < thumbY + thumbH;
        g.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, hovered ? GRAY_COLOR_5 : GRAY_COLOR_1);
    }

    private void drawDonate(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY)
    {
        int cx = x + w / 2;
        int ty = y + TABS_Y + TAB_HEIGHT + 8;

        g.drawCenteredString(font, Component.literal(I18n.get("gui.oneblockultima.donate.thank_you")), cx, ty, SUCCESS_COLOR);
        ty += font.lineHeight + 3;
        g.drawCenteredString(font, Component.literal(I18n.get("gui.oneblockultima.donate.line1")), cx, ty, LIGHT_GRAY_COLOR_1);
        ty += font.lineHeight + 2;
        g.drawCenteredString(font, Component.literal(I18n.get("gui.oneblockultima.donate.line2")), cx, ty, LIGHT_GRAY_COLOR_1);
        ty += font.lineHeight + 2;
        g.drawCenteredString(font, Component.literal(I18n.get("gui.oneblockultima.donate.line3")), cx, ty, LIGHT_GRAY_COLOR_1);
        ty += font.lineHeight + 10;

        int qrSize = 69;
        int qrX = x + 14;
        int qrY = ty;
        g.fill(qrX - 2, qrY - 2, qrX + qrSize + 2, qrY + qrSize + 2, DARK_GRAY_COLOR_1);
        g.fill(qrX, qrY, qrX + qrSize, qrY + qrSize, PANEL_COLOR);
        RenderSystem.setShaderTexture(0, ru.defea.oneblockultima.Constants.SBP_TEXTURE);
        g.blit(ru.defea.oneblockultima.Constants.SBP_TEXTURE, qrX, qrY, 0, 0, qrSize, qrSize, 69, 69);
        g.drawCenteredString(font, Component.literal("SBP"), qrX + qrSize / 2, qrY + qrSize + 3, GRAY_COLOR_5);

        int btnX = qrX + qrSize + 12;
        int btnW = (x + w - 14) - btnX;
        if (btnW < 120)
        {
            btnX = x + 14;
            btnW = w - 28;
        }
        donateBtnX = btnX;
        donateBtnW = btnW;
        donateStartY = qrY;
        donateRowH = BUTTON_HEIGHT + 4;
        for (int i = 0; i < donateMethods.size(); i++)
        {
            DonateMethod m = donateMethods.get(i);
            int by = qrY + i * donateRowH;
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= by && mouseY <= by + BUTTON_HEIGHT;
            int bg = hovered ? DARK_BLUE_GRAY_COLOR_1 : DARK_GRAY_COLOR_2;
            int border = hovered ? LIGHT_BLUE_GRAY_COLOR : GRAY_COLOR_7;
            g.fill(btnX, by, btnX + btnW, by + BUTTON_HEIGHT, bg);
            g.fill(btnX, by, btnX + btnW, by + 1, border);
            g.fill(btnX, by + BUTTON_HEIGHT - 1, btnX + btnW, by + BUTTON_HEIGHT, border);
            g.fill(btnX, by, btnX + 1, by + BUTTON_HEIGHT, border);
            g.fill(btnX + btnW - 1, by, btnX + btnW, by + BUTTON_HEIGHT, border);
            g.drawCenteredString(font, Component.literal(m.text), btnX + btnW / 2, by + (BUTTON_HEIGHT - 8) / 2,
                    hovered ? WHITE_COLOR_1 : LIGHT_GRAY_COLOR_2);
            if (hovered)
            {
                if (m.type == DonateMethod.Type.TEXT)
                {
                    setTooltipForNextRenderPass(Component.literal(donateJustCopied
                            ? I18n.get("gui.oneblockultima.donate.copied", m.value)
                            : I18n.get("gui.oneblockultima.donate.copy_hint")));
                }
                else
                {
                    setTooltipForNextRenderPass(Component.literal(m.value));
                }
            }
        }
        if (donateJustCopied)
        {
            int hintY = qrY + donateMethods.size() * donateRowH + 6;
            g.drawString(font, Component.literal(I18n.get("gui.oneblockultima.donate.copied_short")),
                    btnX + (btnW - font.width(Component.literal(I18n.get("gui.oneblockultima.donate.copied_short")))) / 2,
                    hintY, SUCCESS_COLOR, true);
        }
    }

    private void drawProceduralBackground(GuiGraphics g, int x, int y, int w, int h)
    {
        if (backgroundBlocks.isEmpty() || w <= 0 || h <= 0)
        {
            return;
        }

        int total = backgroundBlocks.size();
        int texSize = BACKGROUND_TEXTURE_SIZE;
        int cols = (w + texSize - 1) / texSize + 1;
        int rows = (h + texSize - 1) / texSize + 1;

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f pose = g.pose().last().pose();

        for (int row = 0; row < rows; row++)
        {
            for (int col = 0; col < cols; col++)
            {
                int index = ((row + col) * 7 + col * 3) % total;
                if (index < 0) index += total;
                BlockSetConfig.BlockEntryDefinition entry = backgroundBlocks.get(index);
                if (entry == null) continue;

                if (!backgroundFullBlocks.isEmpty())
                {
                    int fullIdx = (index * 3 + row + col) % backgroundFullBlocks.size();
                    if (fullIdx < 0) fullIdx += backgroundFullBlocks.size();
                    BlockSetConfig.BlockEntryDefinition backEntry = backgroundFullBlocks.get(fullIdx);
                    if (backEntry != null)
                    {
                        TextureAtlasSprite backSprite = resolveBackgroundSprite(backEntry);
                        if (backSprite == null)
                        {
                            backSprite = resolveFallbackBackgroundSprite();
                        }
                        if (backSprite != null)
                        {
                            drawBackgroundTile(buf, pose, backSprite, resolveBackgroundTint(backEntry), x, y, w, h, texSize, col, row);
                        }
                    }
                }

                TextureAtlasSprite sprite = resolveBackgroundSprite(entry);
                if (sprite == null)
                {
                    sprite = resolveFallbackBackgroundSprite();
                }
                if (sprite == null) continue;
                drawBackgroundTile(buf, pose, sprite, resolveBackgroundTint(entry), x, y, w, h, texSize, col, row);
            }
        }

        BufferUploader.drawWithShader(buf.build());
    }

    private void drawBackgroundTile(BufferBuilder buf, Matrix4f pose, TextureAtlasSprite sprite, int tint,
            int x, int y, int w, int h, int texSize, int col, int row)
    {
        int bx = x + col * texSize;
        int by = y + row * texSize;
        int drawX = Math.max(x, bx);
        int drawY = Math.max(y, by);
        int drawX2 = Math.min(x + w, bx + texSize);
        int drawY2 = Math.min(y + h, by + texSize);
        if (drawX >= drawX2 || drawY >= drawY2) return;

        float u1 = (drawX - bx) / (float) texSize;
        float v1 = (drawY - by) / (float) texSize;
        float u2 = (drawX2 - bx) / (float) texSize;
        float v2 = (drawY2 - by) / (float) texSize;

        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        float uMin = minU + (maxU - minU) * u1;
        float uMax = minU + (maxU - minU) * u2;
        float vMin = minV + (maxV - minV) * v1;
        float vMax = minV + (maxV - minV) * v2;

        int qw = drawX2 - drawX;
        int qh = drawY2 - drawY;

        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;

        buf.addVertex(pose, drawX, drawY + qh, 0.0F).setUv(uMin, vMax).setColor(r, g, b, 1.0F);
        buf.addVertex(pose, drawX + qw, drawY + qh, 0.0F).setUv(uMax, vMax).setColor(r, g, b, 1.0F);
        buf.addVertex(pose, drawX + qw, drawY, 0.0F).setUv(uMax, vMin).setColor(r, g, b, 1.0F);
        buf.addVertex(pose, drawX, drawY, 0.0F).setUv(uMin, vMin).setColor(r, g, b, 1.0F);
    }

    private int resolveBackgroundTint(BlockSetConfig.BlockEntryDefinition entry)
    {
        Integer cached = backgroundTintCache.get(entry);
        if (cached != null) return cached;
        try
        {
            int tint = ModelUtil.getBlockSpriteTint(resolveBackgroundState(entry), resolveBackgroundSprite(entry));
            backgroundTintCache.put(entry, tint);
            return tint;
        }
        catch (Exception ignored)
        {
            return 0xFFFFFFFF;
        }
    }

    private TextureAtlasSprite resolveBackgroundSprite(BlockSetConfig.BlockEntryDefinition entry)
    {
        if (entry == null) return null;
        TextureAtlasSprite cached = backgroundSpriteCache.get(entry);
        if (cached != null) return cached;
        try
        {
            Minecraft mc = Minecraft.getInstance();
            BlockState state = resolveBackgroundState(entry);
            if (state == null) return null;

            TextureAtlasSprite sprite = null;
            try
            {
                sprite = mc.getBlockRenderer().getBlockModel(state).getParticleIcon();
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
            backgroundSpriteCache.put(entry, sprite);
            return sprite;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private TextureAtlasSprite resolveFallbackBackgroundSprite()
    {
        if (!backgroundFullBlocks.isEmpty())
        {
            TextureAtlasSprite sprite = resolveBackgroundSprite(backgroundFullBlocks.get(0));
            if (sprite != null) return sprite;
        }
        if (!backgroundBlocks.isEmpty())
        {
            TextureAtlasSprite sprite = resolveBackgroundSprite(backgroundBlocks.get(0));
            if (sprite != null) return sprite;
        }
        try
        {
            Minecraft mc = Minecraft.getInstance();
            return mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/stone"));
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private BlockState resolveBackgroundState(BlockSetConfig.BlockEntryDefinition entry)
    {
        try
        {
            Block block = entry.resolveBlock();
            if (block == null) return null;
            BlockState state = block.defaultBlockState();
            if (block instanceof BlockCompressedBase base)
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

    private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private int[] getBarDescriptor(int bar)
    {
        switch (bar)
        {
            case 0:
                return leftGeom == null ? null :
                        new int[]{leftGeom.blockScrollbarX, leftGeom.gridStartY, leftGeom.areaH,
                                blockScroll, leftGeom.blockMaxScroll, leftGeom.blockVisibleRows};
            case 1:
                return leftGeom == null ? null :
                        new int[]{leftGeom.mobScrollbarX, leftGeom.gridStartY, leftGeom.areaH,
                                mobScroll, leftGeom.mobMaxScroll, leftGeom.mobVisibleRows};
            case 2:
                return rightGeom == null ? null :
                        new int[]{rightGeom.blockScrollbarX, rightGeom.gridStartY, rightGeom.areaH,
                                blockScrollNext, rightGeom.blockMaxScroll, rightGeom.blockVisibleRows};
            case 3:
                return rightGeom == null ? null :
                        new int[]{rightGeom.mobScrollbarX, rightGeom.gridStartY, rightGeom.areaH,
                                mobScrollNext, rightGeom.mobMaxScroll, rightGeom.mobVisibleRows};
            case 4:
                return condMaxScroll <= 0 ? null :
                        new int[]{infoX + infoW - SCROLLBAR_WIDTH, condScrollTopY, condScrollBotY - condScrollTopY,
                                conditionsScroll, condMaxScroll, condVisibleRows};
            default:
                return null;
        }
    }

    private int scrollbarThumbHeight(int[] bd)
    {
        int trackH = bd[2];
        if (trackH <= 1) return Math.max(0, trackH);
        int max = bd[4];
        int visible = Math.max(1, bd[5]);
        int total = max + visible;
        int th = Math.max(10, Math.min(trackH - 2, (int) ((long) trackH * visible / Math.max(1, total))));
        return Math.min(th, trackH);
    }

    private void setBarValue(int bar, int value)
    {
        switch (bar)
        {
            case 0: blockScroll = value; break;
            case 1: mobScroll = value; break;
            case 2: blockScrollNext = value; break;
            case 3: mobScrollNext = value; break;
            case 4: conditionsScroll = value; break;
            default: break;
        }
    }

    private void applyBarDrag(int bar, double mouseY)
    {
        int[] bd = getBarDescriptor(bar);
        if (bd == null || bd[4] <= 0) return;
        int thumbH = scrollbarThumbHeight(bd);
        int trackLen = bd[2] - thumbH;
        if (trackLen <= 0) return;
        double rel = mouseY - dragStartMouseY;
        int v = (int) Math.round(rel / trackLen * bd[4]);
        setBarValue(bar, clamp(v, 0, bd[4]));
    }

    private boolean attemptScrollbarClick(int bar, double mouseX, double mouseY)
    {
        int[] bd = getBarDescriptor(bar);
        if (bd == null || bd[4] <= 0) return false;
        if (mouseX < bd[0] || mouseX > bd[0] + SCROLLBAR_WIDTH) return false;
        if (mouseY < bd[1] || mouseY > bd[1] + bd[2]) return false;
        int thumbH = scrollbarThumbHeight(bd);
        int trackLen = bd[2] - thumbH;
        int thumbY = bd[1] + (trackLen <= 0 ? 0 : (int) ((long) trackLen * bd[3] / bd[4]));
        double rel = mouseY - bd[1];
        if (rel < thumbY - bd[1])
        {
            int nv = clamp(bd[3] - bd[5], 0, bd[4]);
            setBarValue(bar, nv);
            int nt = bd[1] + (trackLen <= 0 ? 0 : (int) ((long) trackLen * nv / bd[4]));
            draggingScrollbar = bar;
            dragStartMouseY = (int) (mouseY - (nt - bd[1]));
            return true;
        }
        else if (rel > thumbY - bd[1] + thumbH)
        {
            int nv = clamp(bd[3] + bd[5], 0, bd[4]);
            setBarValue(bar, nv);
            int nt = bd[1] + (trackLen <= 0 ? 0 : (int) ((long) trackLen * nv / bd[4]));
            draggingScrollbar = bar;
            dragStartMouseY = (int) (mouseY - (nt - bd[1]));
            return true;
        }
        else
        {
            draggingScrollbar = bar;
            dragStartMouseY = (int) (mouseY - (thumbY - bd[1]));
            return true;
        }
    }

    private boolean isMouseOverPanel(double mx, double my, boolean mob)
    {
        int px = mob ? rpX : lpX;
        int py = mob ? rpY : lpY;
        int pw = mob ? rpW : lpW;
        int ph = mob ? rpH : lpH;
        return mx >= px && mx <= px + pw && my >= py && my <= py + ph;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        int dir = scrollY > 0 ? -1 : 1;
        if (activeView == VIEW_SETS)
        {
            if (condMaxScroll > 0 && condScrollTopY > 0
                    && mouseY >= condScrollTopY && mouseY <= condScrollBotY)
            {
                conditionsScroll = clamp(conditionsScroll + dir, 0, condMaxScroll);
                return true;
            }
            if (leftGeom != null && mouseX >= leftGeom.gridStartX() && mouseX <= leftGeom.gridEndX()
                    && mouseY >= leftGeom.gridStartY && mouseY <= leftGeom.gridStartY + leftGeom.areaH)
            {
                if (mouseX < leftGeom.mobsStartX)
                {
                    blockScroll = clamp(blockScroll + dir, 0, leftGeom.blockMaxScroll);
                }
                else
                {
                    mobScroll = clamp(mobScroll + dir, 0, leftGeom.mobMaxScroll);
                }
                return true;
            }
            if (rightGeom != null && mouseX >= rightGeom.gridStartX() && mouseX <= rightGeom.gridEndX()
                    && mouseY >= rightGeom.gridStartY && mouseY <= rightGeom.gridStartY + rightGeom.areaH)
            {
                if (mouseX < rightGeom.mobsStartX)
                {
                    blockScrollNext = clamp(blockScrollNext + dir, 0, rightGeom.blockMaxScroll);
                }
                else
                {
                    mobScrollNext = clamp(mobScrollNext + dir, 0, rightGeom.mobMaxScroll);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isCloseButtonHovered(int mx, int my)
    {
        return mx >= closeButtonX && mx <= closeButtonX + CLOSE_BUTTON_SIZE &&
                my >= closeButtonY && my <= closeButtonY + CLOSE_BUTTON_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx < leftPos || mx >= leftPos + imageWidth || my < topPos || my >= topPos + imageHeight)
        {
            minecraft.setScreen(null);
            return true;
        }
        if (isCloseButtonHovered(mx, my))
        {
            minecraft.setScreen(null);
            return true;
        }
        if (my >= tabY && my < tabY + TAB_HEIGHT)
        {
            for (int i = 0; i < 3; i++)
            {
                if (mx >= tabX[i] && mx < tabX[i] + tabW)
                {
                    changeView(tabView[i]);
                    return true;
                }
            }
        }
        if (activeView == VIEW_SETS)
        {
            for (int bar = 0; bar < 5; bar++)
            {
                if (attemptScrollbarClick(bar, mouseX, mouseY)) return true;
            }
        }
        if (activeView == VIEW_DONATE)
        {
            for (int i = 0; i < donateMethods.size(); i++)
            {
                DonateMethod dm = donateMethods.get(i);
                int ry = donateStartY + i * donateRowH;
                if (mx >= donateBtnX && mx <= donateBtnX + donateBtnW && my >= ry && my <= ry + BUTTON_HEIGHT)
                {
                    handleDonateClick(dm);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleDonateClick(DonateMethod method)
    {
        try
        {
            if (method.type == DonateMethod.Type.LINK)
            {
                Util.getPlatform().openUri(URI.create(method.value));
            }
            else
            {
                this.minecraft.keyboardHandler.setClipboard(method.value);
                donateJustCopied = true;
            }
        }
        catch (Exception ignored)
        {
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        draggingScrollbar = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (draggingScrollbar >= 0)
        {
            applyBarDrag(draggingScrollbar, mouseY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);
    }
}

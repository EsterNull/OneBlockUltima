package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
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
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.TabBarElement;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.util.BlockUtil;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.*;

import static ru.defea.oneblockultima.util.BlockUtil.isFullBlock;
import static ru.defea.oneblockultima.util.ModelUtil.*;

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
    private static final int BUTTON_TAB_DONATE = 11;
    private static final int BUTTON_DONATE_BASE = 12;
    private static final int BUTTON_OPEN_PRICES = 30;
    private static final int VIEW_SETS = 0;
    private static final int VIEW_SETTINGS = 1;
    private static final int VIEW_DONATE = 2;

    private static final int GREEN_COLOR = 0x00EE00;
    private static final int ORANGE_COLOR = 0xFFAA00;
    private static final int RED_COLOR = 0xEE0000;

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int INNER_PADDING = 6;
    private static final int SECTION_GAP = 8;
    private static final int BACKGROUND_TEXTURE_SIZE = 32;

    private int rowInterval;

    private final ContainerOneBlock container;
    private final List<BlockSetConfig.BlockSetDefinition> visibleSets = new ArrayList<>();
    private int selectedSetIndex = 0;
    private ButtonElement prevButton;
    private ButtonElement nextButton;
    private ButtonElement selectButton;
    private ButtonElement upgradeButton;
    private ButtonElement openConfigEditorButton;
    private ButtonElement openPricesButton;
    private ButtonElement toggleFluidButton;
    private ButtonElement toggleMobsButton;
    private ButtonElement toggleChestsButton;
    private ButtonElement toggleSaplingsButton;
    private ButtonElement[] donateButtons;
    private Boolean pendingDisableFluid = null;
    private Boolean pendingDisableMob = null;
    private Boolean pendingDisableChest = null;
    private Boolean pendingDisableSapling = null;
    private String donateStatusMessage = null;
    private int donateStatusTicks = 0;
    private int activeView = VIEW_SETS;
    private boolean settingsTabVisible = true;
    private int blockScroll = 0;
    private int mobScroll = 0;
    private int blockScrollNext = 0;
    private int mobScrollNext = 0;

    private BlockSetConfig.BlockEntryDefinition hoveredEntryLeft = null;
    private ItemStack hoveredStackLeft = ItemStack.EMPTY;
    private BlockSetConfig.MobEntryDefinition hoveredMobEntryLeft = null;
    private String hoveredMobNameLeft = null;

    private BlockSetConfig.BlockEntryDefinition hoveredEntryRight = null;
    private ItemStack hoveredStackRight = ItemStack.EMPTY;
    private BlockSetConfig.MobEntryDefinition hoveredMobEntryRight = null;
    private String hoveredMobNameRight = null;

    private static final ResourceLocation COIN_TEXTURE = new ResourceLocation(OneBlockUltima.MODID, "textures/gui/coin.png");
    private static final ResourceLocation SBP_TEXTURE = new ResourceLocation(OneBlockUltima.MODID, "textures/gui/sbp.png");

    private int cellSize = 18;
    private int cellPadding = 1;
    private int blockCols = 4;
    private int mobCols = 2;
    private int panelGap = 8;
    private int panelWidth;
    private int panelHeight;
    private int contentLeft;
    private int contentWidth;
    private int leftPanelX;
    private int rightPanelX;
    private int tabRowY;
    private int textHeight;
    private int infoRowY;
    private int infoHeight;
    private String clientActiveSetId = null;

    private final List<BlockSetConfig.BlockEntryDefinition> backgroundBlocks = new ArrayList<>();

    private TabBarElement tabs;

    public GuiOneBlock(EntityPlayer player, World world, BlockPos generatorPos)
    {
        super(new ContainerOneBlock(player, world, generatorPos));
        this.container = (ContainerOneBlock) this.inventorySlots;
        this.xSize = 360;
        this.ySize = 280;
    }

    private void updateLayoutMetrics()
    {
        int horizontalMargin = Math.max(12, Math.min(24, xSize / 24));
        textHeight = fontRenderer.FONT_HEIGHT;

        tabRowY = textHeight * 3;
        int tabHeight = 24;
        infoRowY = tabRowY + tabHeight + textHeight;

        contentWidth = Math.max(120, xSize - horizontalMargin * 2);
        contentLeft = Math.max(horizontalMargin, (xSize - contentWidth) / 2);

        int infoInternalWidth = Math.max(80, contentWidth - 16);
        int infoLines = 0;
        if (activeView != VIEW_SETTINGS)
        {
            TileEntityOneBlockGenerator generator = container.getGenerator();
            String activeSetId = generator == null ? null : generator.getSelectedSetId();

            if (!visibleSets.isEmpty())
            {
                BlockSetConfig.BlockSetDefinition set = getBlockSetDefinition();
                if (set != null)
                {
                    int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
                    String setTitle = getLocalizedSetName(set) + " " + I18n.format("gui.oneblockultima.lv") + currentLevel;
                    if (currentLevel <= 0)
                    {
                        setTitle += " (" + I18n.format("gui.oneblockultima.locked") + ")";
                    }
                    else if (set.id.equals(activeSetId))
                    {
                        setTitle += " (" + I18n.format("gui.oneblockultima.selected") + ")";
                    }
                    infoLines += fontRenderer.listFormattedStringToWidth(setTitle, infoInternalWidth).size();

                    String statusText;
                    if (currentLevel <= 0)
                    {
                        statusText = I18n.format("gui.oneblockultima.unlock_cost") + ": " + set.unlockCost;
                    }
                    else
                    {
                        BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
                        if (nextLevel != null)
                        {
                            statusText = I18n.format("gui.oneblockultima.upgrade_cost") + ": " + nextLevel.upgradeCost;
                        }
                        else
                        {
                            statusText = I18n.format("gui.oneblockultima.max_level");
                        }
                    }
                    infoLines += fontRenderer.listFormattedStringToWidth(statusText, infoInternalWidth).size();
                }
                else
                {
                    infoLines += 1;
                }
            }
            else
            {
                infoLines += 1;
            }
        }

        int infoButtonRows = activeView == VIEW_SETTINGS ? 3 : 2;
        int infoTextHeight = infoLines * fontRenderer.FONT_HEIGHT + Math.max(0, infoLines - 1) * 4;
        int infoButtonArea = infoButtonRows * BUTTON_HEIGHT + (infoButtonRows - 1) * BUTTON_GAP;
        infoHeight = infoTextHeight + infoButtonArea + textHeight;

        int contentHeight = ySize - infoRowY - 20;

        panelGap = contentWidth / 40;
        panelWidth = (contentWidth - panelGap) / 2;
        panelHeight = contentHeight - 10;
        leftPanelX = contentLeft;
        rightPanelX = contentLeft + panelWidth + panelGap;

        calculateColumns();
    }

    private void calculateColumns() {
        int availableWidth = panelWidth - INNER_PADDING * 2;
        int halfWidth = (availableWidth - SECTION_GAP) / 2;
        int blockAreaWidth = halfWidth - 8;
        int mobAreaWidth = halfWidth - 8;

        blockCols = Math.max(2, blockAreaWidth / 20);

        mobCols = blockCols;

        cellPadding = Math.max(1, Math.min(2, blockAreaWidth / 80));
        cellSize = Math.max(14, Math.min(20, (mobAreaWidth - (mobCols - 1) * cellPadding) / mobCols));
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
        return panelY + rowInterval * 2;
    }

    private int getAreaHeight() {
        return panelHeight - rowInterval * 2 - BUTTON_HEIGHT - BUTTON_GAP * 2 - cellSize;
    }

    private void initBackgroundBlocks()
    {
        backgroundBlocks.clear();

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

        for (int row = -extraRows; row <= rows + extraRows; row++)
        {
            for (int col = -extraCols; col <= cols + extraCols; col++)
            {
                int blockIndex = ((row + col) * 7 + col * 3) % totalBlocks;
                if (blockIndex < 0) blockIndex += totalBlocks;

                BlockSetConfig.BlockEntryDefinition entry = backgroundBlocks.get(blockIndex);
                if (entry == null) continue;

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

                renderBlockAsBackgroundClipped(entry, drawX, drawY, drawX2 - drawX, drawY2 - drawY, u1, v1, u2, v2);
            }
        }
    }

    private void renderBlockAsBackgroundClipped(BlockSetConfig.BlockEntryDefinition entry, int x, int y, int width, int height, float u1, float v1, float u2, float v2)
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            net.minecraft.block.Block block = entry.resolveBlock();
            if (block == null) return;

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

            if (state == null) return;

            BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();
            TextureAtlasSprite sprite = null;

            try
            {
                sprite = blockRenderer.getBlockModelShapes().getTexture(state);
            }
            catch (Exception ignored) {}

            if (sprite == null)
            {
                try
                {
                    sprite = mc.getTextureMapBlocks().getAtlasSprite(Objects.requireNonNull(block.getRegistryName()).toString());
                }
                catch (Exception ignored) {}
            }

            if (sprite == null) return;

            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();

            float uMin = minU + (maxU - minU) * u1;
            float uMax = minU + (maxU - minU) * u2;
            float vMin = minV + (maxV - minV) * v1;
            float vMax = minV + (maxV - minV) * v2;

            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buf.pos(x, y + height, 0.0D).tex(uMin, vMax).endVertex();
            buf.pos(x + width, y + height, 0.0D).tex(uMax, vMax).endVertex();
            buf.pos(x + width, y, 0.0D).tex(uMax, vMin).endVertex();
            buf.pos(x, y, 0.0D).tex(uMin, vMin).endVertex();
            tess.draw();

            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        catch (Exception ignored) {}
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
        OneBlockUltima.getLogger().debug("[DEBUG] Area height: {}", areaHeight);

        fontRenderer.drawString(I18n.format(isLeft ? "gui.oneblockultima.current_level" : "gui.oneblockultima.next_level") + ": " + levelDefinition.level,
                panelX + INNER_PADDING, panelY + 4, 0xA0B0C0);
        fontRenderer.drawString(I18n.format("gui.oneblockultima.possible_blocks") + ": ",
                panelX + INNER_PADDING, panelY + rowInterval, 0xA0B0C0);

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

            int localMouseX = mouseX - guiLeft;
            int localMouseY = mouseY - guiTop;
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
                    if (localMouseX >= cellX && localMouseX < cellX + cellSize &&
                            localMouseY >= cellY && localMouseY < cellY + cellSize)
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
                                    if (entry.registry != null && entry.registry.toLowerCase().contains("forestry")) {
                                        stack.setTagCompound(entry.nbtTags.copy());
                                    }
                                }
                            } catch (Exception ignored) {
                                stack = new ItemStack(itemForIcon);
                            }
                        }
                    }

                    net.minecraft.block.Block blockForIcon = entry.resolveBlock();

                    int bgColor = isHovered ? 0xFF3F5060 : 0xFF2A2F34;
                    int borderColor = isHovered ? 0xFFFFFFFF : 0xFF3A3F44;

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
                                renderFluidSprite(fluid, iconX, iconY, 12, 12);
                            }
                            else if (state != null)
                            {
                                renderBlockModelToGUI(state, iconX + 8, iconY + 8, 16);
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
            if (maxScrollBlocks > 0)
            {
                int blockScrollbarX = panelX + INNER_PADDING + blocksAreaWidth + 2;
                renderScrollBar(blockScrollbarX, gridStartY, areaHeight, localBlockScroll, maxScrollBlocks);
            }

            if (levelDefinition.mobs != null && !levelDefinition.mobs.isEmpty())
            {
                fontRenderer.drawString(I18n.format("gui.oneblockultima.mobs") + ":",
                        mobsStartX, panelY + rowInterval, 0xA0B0C0);

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

                        boolean isMobHovered = (localMouseX >= cellX && localMouseX < cellX + cellSize &&
                                localMouseY >= cellY && localMouseY < cellY + cellSize);

                        int mobBgColor = isMobHovered ? 0xFF3F5060 : 0xFF2A2F34;
                        int mobBorderColor = isMobHovered ? 0xFFFFFFFF : 0xFF3A3F44;

                        drawRect(cellX, cellY, cellX + cellSize, cellY + cellSize, mobBgColor);
                        drawRect(cellX, cellY, cellX + cellSize, cellY + 1, mobBorderColor);
                        drawRect(cellX, cellY + cellSize - 1, cellX + cellSize, cellY + cellSize, mobBorderColor);
                        drawRect(cellX, cellY, cellX + 1, cellY + cellSize, mobBorderColor);
                        drawRect(cellX + cellSize - 1, cellY, cellX + cellSize, cellY + cellSize, mobBorderColor);

                        Entity entity = null;
                        try
                        {
                            World mcWorld = Minecraft.getMinecraft().world;
                            entity = EntityList.createEntityByIDFromName(new ResourceLocation(mobEntry.registry), mcWorld);
                            if (entity != null && entity.world == null) {
                                entity.world = mcWorld;
                            }
                            if (entity instanceof EntityLivingBase)
                            {
                                int centerX = cellX + cellSize / 2;
                                int centerY = cellY + cellSize / 2 + cellPadding;
                                drawEntityOnScreen(centerX, centerY, entity, cellSize - 2 * cellPadding);
                            }
                        }
                        catch (Exception ignored) { }

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
                        fontRenderer.drawStringWithShadow(percent, cellX + (cellSize - pw) / 2, cellY + cellSize - fontRenderer.FONT_HEIGHT - 2, color);
                    }
                }

                if (mobMaxScroll > 0)
                {
                    int mobScrollbarX = mobsStartX + mobsAreaWidth + 2;
                    renderMobScrollBar(mobScrollbarX, gridStartY, areaHeight, localMobScroll, mobMaxScroll);
                }
            }
        }
    }

    @Override
    public void initGui()
    {
        this.xSize = this.width - 40;
        this.ySize = this.height - 40;
        super.initGui();

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

        updateLayoutMetrics();
        buttonList.clear();
        rowInterval = fontRenderer.FONT_HEIGHT + 4;

        int tabGap = xSize / 36;
        int tabWidth = xSize / 7;
        settingsTabVisible = net.minecraft.client.Minecraft.getMinecraft().isIntegratedServerRunning();
        int visibleTabs = settingsTabVisible ? 3 : 2;
        int totalTabWidth = tabWidth * visibleTabs + tabGap * (visibleTabs - 1);
        int tabX = guiLeft + (xSize - totalTabWidth) / 2;
        int tabY = guiTop + tabRowY + textHeight / 2;
        int infoButtonsY = guiTop + infoRowY + rowInterval * 2;
        int leftButtonX = guiLeft + contentLeft;
        int selectWidth = contentWidth / 2 - BUTTON_GAP;
        int rightButtonX = guiLeft + contentLeft + contentWidth - selectWidth;
        int settingWidth = contentWidth;

        int settingsButtonWidth = settingWidth / 2 - BUTTON_GAP;
        int settingsButtonStartY = guiTop + infoRowY;

        tabs = new TabBarElement();
        tabs.setComputedPosition(tabX, tabY);
        tabs.setComputedSize(totalTabWidth, BUTTON_HEIGHT);
        tabs.tab(BUTTON_TAB_SETS, I18n.format("gui.oneblockultima.tabs.sets"));
        if (settingsTabVisible)
        {
            tabs.tab(BUTTON_TAB_SETTINGS, I18n.format("gui.oneblockultima.tabs.settings"));
        }
        tabs.tab(BUTTON_TAB_DONATE, I18n.format("gui.oneblockultima.tabs.donate"));
        tabs.activeTab(activeView == VIEW_SETS ? BUTTON_TAB_SETS :
                activeView == VIEW_SETTINGS ? BUTTON_TAB_SETTINGS : BUTTON_TAB_DONATE);

        prevButton = new ButtonElement(BUTTON_PREV_SET, "<");
        prevButton.setComputedPosition(leftButtonX, infoButtonsY);
        prevButton.setComputedSize(20, BUTTON_HEIGHT);
        prevButton.createWidgets(buttonList, fontRenderer, null);

        nextButton = new ButtonElement(BUTTON_NEXT_SET, ">");
        nextButton.setComputedPosition(leftButtonX + 26, infoButtonsY);
        nextButton.setComputedSize(20, BUTTON_HEIGHT);
        nextButton.createWidgets(buttonList, fontRenderer, null);

        selectButton = new ButtonElement(BUTTON_SELECT_SET, I18n.format("gui.oneblockultima.select"));
        selectButton.setComputedPosition(leftButtonX, infoButtonsY + BUTTON_HEIGHT + BUTTON_GAP);
        selectButton.setComputedSize(selectWidth, BUTTON_HEIGHT);
        selectButton.createWidgets(buttonList, fontRenderer, null);

        upgradeButton = new ButtonElement(BUTTON_UPGRADE_SET, I18n.format("gui.oneblockultima.upgrade"));
        upgradeButton.setComputedPosition(rightButtonX, infoButtonsY + BUTTON_HEIGHT + BUTTON_GAP);
        upgradeButton.setComputedSize(selectWidth, BUTTON_HEIGHT);
        upgradeButton.createWidgets(buttonList, fontRenderer, null);

        toggleFluidButton = new ButtonElement(BUTTON_TOGGLE_FLUIDS, "");
        toggleFluidButton.setComputedPosition(guiLeft + contentLeft, settingsButtonStartY);
        toggleFluidButton.setComputedSize(settingsButtonWidth, BUTTON_HEIGHT);
        toggleFluidButton.createWidgets(buttonList, fontRenderer, null);

        toggleMobsButton = new ButtonElement(BUTTON_TOGGLE_MOBS, "");
        toggleMobsButton.setComputedPosition(guiLeft + contentLeft + settingWidth / 2 + BUTTON_GAP, settingsButtonStartY);
        toggleMobsButton.setComputedSize(settingsButtonWidth, BUTTON_HEIGHT);
        toggleMobsButton.createWidgets(buttonList, fontRenderer, null);

        toggleChestsButton = new ButtonElement(BUTTON_TOGGLE_CHESTS, "");
        toggleChestsButton.setComputedPosition(guiLeft + contentLeft, settingsButtonStartY + BUTTON_HEIGHT + BUTTON_GAP);
        toggleChestsButton.setComputedSize(settingsButtonWidth, BUTTON_HEIGHT);
        toggleChestsButton.createWidgets(buttonList, fontRenderer, null);

        toggleSaplingsButton = new ButtonElement(BUTTON_TOGGLE_SAPLINGS, "");
        toggleSaplingsButton.setComputedPosition(guiLeft + contentLeft + settingWidth / 2 + BUTTON_GAP, settingsButtonStartY + BUTTON_HEIGHT + BUTTON_GAP);
        toggleSaplingsButton.setComputedSize(settingsButtonWidth, BUTTON_HEIGHT);
        toggleSaplingsButton.createWidgets(buttonList, fontRenderer, null);

        openConfigEditorButton = new ButtonElement(BUTTON_OPEN_CONFIG_EDITOR, I18n.format("gui.oneblockultima.settings.open_editor"));
        openConfigEditorButton.setComputedPosition(guiLeft + contentLeft + settingWidth / 2 + BUTTON_GAP, settingsButtonStartY + (BUTTON_HEIGHT + BUTTON_GAP) * 2);
        openConfigEditorButton.setComputedSize(settingsButtonWidth, BUTTON_HEIGHT);
        openConfigEditorButton.createWidgets(buttonList, fontRenderer, null);

        openPricesButton = new ButtonElement(BUTTON_OPEN_PRICES, I18n.format("gui.oneblockultima.settings.open_prices"));
        openPricesButton.setComputedPosition(guiLeft + contentLeft, settingsButtonStartY + (BUTTON_HEIGHT + BUTTON_GAP) * 2);
        openPricesButton.setComputedSize(settingsButtonWidth, BUTTON_HEIGHT);
        openPricesButton.createWidgets(buttonList, fontRenderer, null);

        int donateBtnX = guiLeft + contentLeft + 72;
        int donateBtnWidth = contentWidth - 72 - 4;
        int donateBtnY = guiTop + infoRowY + rowInterval * 5;
        donateButtons = new ButtonElement[DonateMethod.METHODS.length];
        for (int i = 0; i < DonateMethod.METHODS.length; i++)
        {
            donateButtons[i] = new ButtonElement(BUTTON_DONATE_BASE + i, DonateMethod.METHODS[i].text);
            donateButtons[i].setComputedPosition(donateBtnX, donateBtnY + (BUTTON_HEIGHT + BUTTON_GAP) * i);
            donateButtons[i].setComputedSize(donateBtnWidth, BUTTON_HEIGHT);
            donateButtons[i].createWidgets(buttonList, fontRenderer, null);
        }
        updateViewButtons();

        initBackgroundBlocks();
    }

    private void updateViewButtons()
    {
        if (!settingsTabVisible && activeView == VIEW_SETTINGS)
        {
            activeView = VIEW_SETS;
        }
        boolean setsView = activeView == VIEW_SETS;
        boolean settingsView = activeView == VIEW_SETTINGS;
        boolean donateView = activeView == VIEW_DONATE;
        if (tabs != null)
        {
            tabs.activeTab(setsView ? BUTTON_TAB_SETS : settingsView ? BUTTON_TAB_SETTINGS : BUTTON_TAB_DONATE);
        }
        if (prevButton != null) prevButton.visible(setsView);
        if (nextButton != null) nextButton.visible(setsView);
        if (selectButton != null) selectButton.visible(setsView);
        if (upgradeButton != null) upgradeButton.visible(setsView);
        if (openConfigEditorButton != null) openConfigEditorButton.visible(settingsView);
        if (openPricesButton != null) openPricesButton.visible(settingsView);
        if (toggleFluidButton != null) toggleFluidButton.visible(settingsView);
        if (toggleMobsButton != null) toggleMobsButton.visible(settingsView);
        if (toggleChestsButton != null) toggleChestsButton.visible(settingsView);
        if (toggleSaplingsButton != null) toggleSaplingsButton.visible(settingsView);
        if (donateButtons != null)
        {
            for (ButtonElement btn : donateButtons)
            {
                btn.visible(donateView);
            }
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
    protected void actionPerformed(GuiButton button) {
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
        else if (button.id >= BUTTON_DONATE_BASE && button.id < BUTTON_DONATE_BASE + DonateMethod.METHODS.length)
        {
            handleDonateClick(DonateMethod.METHODS[button.id - BUTTON_DONATE_BASE]);
        }
        else if (button.id == BUTTON_PREV_SET)
        {
            selectedSetIndex = (selectedSetIndex - 1 + visibleSets.size()) % visibleSets.size();
            initBackgroundBlocks();
        }
        else if (button.id == BUTTON_NEXT_SET)
        {
            selectedSetIndex = (selectedSetIndex + 1) % visibleSets.size();
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
        if (donateStatusTicks > 0)
        {
            donateStatusTicks--;
            if (donateStatusTicks <= 0)
            {
                donateStatusMessage = null;
            }
        }
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
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        hoveredEntryLeft = null;
        hoveredStackLeft = ItemStack.EMPTY;
        hoveredMobEntryLeft = null;
        hoveredMobNameLeft = null;
        hoveredEntryRight = null;
        hoveredStackRight = ItemStack.EMPTY;
        hoveredMobEntryRight = null;
        hoveredMobNameRight = null;

        updateLayoutMetrics();

        String title = I18n.format("tile.one_block_generator.name");
        drawCenteredString(fontRenderer, title, xSize / 2, textHeight, 0xFFFFFF);

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(container.getPlayer());
        double currency = ru.defea.oneblockultima.event.ModEventsClient.getDisplayedCurrency(container.getPlayer());
        String balanceValue = ru.defea.oneblockultima.event.ModEventsClient.formatCurrency(currency);
        int balanceWidth = fontRenderer.getStringWidth(balanceValue);
        int iconSize = 12;
        int padding = 4;
        int boxWidth = iconSize + 2 + balanceWidth + padding * 2;
        int boxX = xSize - 10 - boxWidth;
        int boxY = 4;
        int iconX = boxX + padding;
        int iconY = boxY + padding;
        int numberX = iconX + iconSize + 2;
        int numberY = iconY + 2;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(COIN_TEXTURE);
        drawModalRectWithCustomSizedTexture(iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        fontRenderer.drawString(balanceValue, numberX, numberY, 0xFFD700);

        TileEntityOneBlockGenerator generator = container.getGenerator();
        String activeSetId = generator == null ? null : generator.getSelectedSetId();
        String activeSetName = "-";
        if (activeSetId != null)
        {
            BlockSetConfig.BlockSetDefinition activeSetDef = BlockSetConfig.get().getSet(activeSetId);
            activeSetName = activeSetDef == null ? activeSetId : getLocalizedSetName(activeSetDef);
        }

        if (activeView == VIEW_SETS)
        {
            String activeSetString = I18n.format("gui.oneblockultima.active_set");
            int rightTextX = contentWidth - fontRenderer.getStringWidth(activeSetString);
            fontRenderer.drawString(activeSetString + ":", rightTextX, infoRowY, 0xA0B0C0);
            fontRenderer.drawString(activeSetName, rightTextX, infoRowY + fontRenderer.FONT_HEIGHT + 2, 0xFFFFFF);

            if (!visibleSets.isEmpty())
            {
                BlockSetConfig.BlockSetDefinition set = getBlockSetDefinition();
                if (set != null)
                {
                    if (data != null)
                    {
                        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
                        if (currentLevel <= 0 && set.unlockConditions != null &&
                                !set.unlockConditions.conditions.isEmpty())
                        {
                            int totalWidth = contentWidth;
                            int centerX = contentLeft + totalWidth / 2;

                            int maxConditionWidth = 0;
                            for (BlockSetConfig.UnlockConditionDefinition condition : set.unlockConditions.conditions)
                            {
                                if (condition == null) continue;
                                String text = " - " + formatUnlockCondition(condition, data, generator) + " РЎвЂљР В¬Р Р€";
                                int width = fontRenderer.getStringWidth(text);
                                if (width > maxConditionWidth) maxConditionWidth = width;
                            }

                            String unlockConditionsTitle = I18n.format("gui.oneblockultima.unlock_conditions");
                            int titleWidth = fontRenderer.getStringWidth(unlockConditionsTitle);
                            if (titleWidth > maxConditionWidth) maxConditionWidth = titleWidth;

                            maxConditionWidth += 20;

                            int conditionsX = centerX - maxConditionWidth / 2;
                            int conditionsY = infoRowY;
                            drawUnlockConditions(set, conditionsX, conditionsY, maxConditionWidth, generator);
                        }
                    }
                }
            }
        }

        if (activeView == VIEW_SETTINGS)
        {
            return;
        }

        if (activeView == VIEW_DONATE)
        {
            drawDonateView();
            return;
        }

        if (!visibleSets.isEmpty())
        {
            BlockSetConfig.BlockSetDefinition set = getBlockSetDefinition();

            if (set == null) return;

            int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
            boolean isActiveSet = (set.id.equals(clientActiveSetId));

            int setColor = currentLevel <= 0 ? 0xFF7D7D : isActiveSet ? 0x7CEC9F : 0xFFFFFF;
            String setTitle = getLocalizedSetName(set) + " " + I18n.format("gui.oneblockultima.lv") + currentLevel;
            if (isActiveSet)
            {
                setTitle += " (" + I18n.format("gui.oneblockultima.selected") + ")";
            }
            else if (currentLevel <= 0)
            {
                setTitle += " (" + I18n.format("gui.oneblockultima.locked") + ")";
            }
            int setTitleY = infoRowY;
            fontRenderer.drawString(setTitle, contentLeft, setTitleY, setColor);

            int statusY = setTitleY + rowInterval;
            if (currentLevel <= 0)
            {
                fontRenderer.drawString(I18n.format("gui.oneblockultima.unlock_cost") + ": " + set.unlockCost, contentLeft + 4, statusY, 0xC0C0C0);
            }
            else
            {
                BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
                if (nextLevel != null)
                {
                    fontRenderer.drawString(I18n.format("gui.oneblockultima.upgrade_cost") + ": " + nextLevel.upgradeCost, contentLeft + 4, statusY, 0xC0C0C0);
                }
                else
                {
                    fontRenderer.drawString(I18n.format("gui.oneblockultima.max_level"), contentLeft + 4, statusY, 0xC0C0C0);
                }
            }

            if (selectButton != null)
            {
                selectButton.text(currentLevel <= 0 ? I18n.format("gui.oneblockultima.locked") : (isActiveSet ? I18n.format("gui.oneblockultima.selected") : I18n.format("gui.oneblockultima.select")));
                selectButton.enabled(currentLevel > 0 && !isActiveSet);
            }
            if (upgradeButton != null)
            {
                BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
                if (currentLevel <= 0)
                {
                    upgradeButton.text(I18n.format("gui.oneblockultima.unlock"));
                    upgradeButton.enabled(true);
                }
                else if (nextLevel != null)
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

            boolean canShowCurrent = currentLevel > 0;
            BlockSetConfig.SetLevelDefinition currentDef = canShowCurrent ? set.getLevel(currentLevel) : null;
            int panelY = infoRowY + infoHeight;
            if (canShowCurrent)
            {
                renderLevelPanel(currentDef, leftPanelX, panelY, true, mouseX, mouseY);
            }
            int rightStartX = canShowCurrent ? rightPanelX : leftPanelX;
            BlockSetConfig.SetLevelDefinition nextDef = set.getLevel(currentLevel <= 0 ? 1 : currentLevel + 1);
            renderLevelPanel(nextDef, rightStartX, panelY, false, mouseX, mouseY);

            if (canShowCurrent && nextDef != null)
            {
                int separatorX = leftPanelX + panelWidth + panelGap / 2;
                int separatorTop = infoHeight + infoRowY + 5;
                int separatorBottom = ySize - 5;
                drawRect(separatorX, separatorTop, separatorX + 1, separatorBottom, 0xFF4C5560);
            }

            if (hoveredEntryLeft == null && hoveredStackLeft.isEmpty() && hoveredMobEntryLeft == null &&
                    hoveredEntryRight == null && hoveredStackRight.isEmpty() && hoveredMobEntryRight == null) {
                return;
            }

            if (!hoveredStackLeft.isEmpty())
            {
                java.util.List<String> tooltip = hoveredStackLeft.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
                tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + (hoveredEntryLeft != null ? hoveredEntryLeft.getChance() : 0) + "%");
                drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
            }
            else if (hoveredEntryLeft != null && hoveredEntryLeft.isFluid())
            {
                java.util.List<String> tooltip = BlockUtil.getTooltip(hoveredEntryLeft, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
                tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + hoveredEntryLeft.getChance() + "%");
                drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
            }
            else if (hoveredMobEntryLeft != null)
            {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
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
                java.util.List<String> tooltip = hoveredStackRight.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
                tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + (hoveredEntryRight != null ? hoveredEntryRight.getChance() : 0) + "%");
                drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
            }
            else if (hoveredEntryRight != null && hoveredEntryRight.isFluid())
            {
                java.util.List<String> tooltip = BlockUtil.getTooltip(hoveredEntryRight, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
                tooltip.add(I18n.format("gui.oneblockultima.chance") + ": " + hoveredEntryRight.getChance() + "%");
                drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
            }
            else if (hoveredMobEntryRight != null)
            {
                java.util.List<String> tooltip = new java.util.ArrayList<>();
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
    }

    private void drawUnlockConditions(BlockSetConfig.BlockSetDefinition set, int x, int y, int maxWidth, TileEntityOneBlockGenerator generator)
    {
        if (set == null || set.unlockConditions == null || set.unlockConditions.conditions == null ||
                set.unlockConditions.conditions.isEmpty())
        {
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(container.getPlayer());
        if (data == null) return;

        int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
        if (currentLevel > 0) return;

        String title = I18n.format("gui.oneblockultima.unlock_conditions");
        int titleWidth = fontRenderer.getStringWidth(title);
        int startX = x + (maxWidth - titleWidth) / 2;
        fontRenderer.drawString(title, startX, y, 0xA0B0C0);
        y += fontRenderer.FONT_HEIGHT + 2;

        for (BlockSetConfig.UnlockConditionDefinition condition : set.unlockConditions.conditions)
        {
            if (condition == null) continue;

            String conditionText = formatUnlockCondition(condition, data, generator);
            boolean satisfied = condition.isSatisfied(data, generator);
            int color = satisfied ? 0x7CEC9F : 0xFF7D7D;
            String status = satisfied ? " РЎвЂљР В¬Р Р€" : " РЎвЂљР В¬Р В§";
            String fullText = " - " + conditionText + status;

            int textWidth = fontRenderer.getStringWidth(fullText);
            int textX = x + (maxWidth - textWidth) / 2;
            fontRenderer.drawString(fullText, textX, y, color);
            y += fontRenderer.FONT_HEIGHT + 1;
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
        if (d != 0)
        {
            int delta = d > 0 ? -1 : 1;
            int mouseX = Mouse.getX() * width / mc.displayWidth;
            int mouseY = height - Mouse.getY() * height / mc.displayHeight - 1;

            int localMouseX = mouseX - guiLeft;
            int localMouseY = mouseY - guiTop;

            updateLayoutMetrics();

            if (!visibleSets.isEmpty())
            {
                BlockSetConfig.BlockSetDefinition set = visibleSets.get(selectedSetIndex);
                TileEntityOneBlockGenerator generator = container.getGenerator();
                int currentLevel = generator == null ? 0 : generator.getSetLevel(set.id);
                boolean canShowCurrent = currentLevel > 0;

                int panelY = infoRowY + infoHeight;

                int blocksAreaWidth = getBlocksAreaWidth();
                int mobsAreaWidth = getMobsAreaWidth();
                int gridStartY = getGridStartY(panelY);
                int currentAreaHeight = getAreaHeight();

                if (canShowCurrent)
                {
                    if (localMouseX >= leftPanelX + INNER_PADDING &&
                            localMouseX <= leftPanelX + INNER_PADDING + blocksAreaWidth &&
                            localMouseY >= gridStartY && localMouseY <= gridStartY + currentAreaHeight)
                    {
                        int maxScroll = getMaxBlockScroll(set, currentLevel);
                        blockScroll = Math.max(0, Math.min(blockScroll + delta, maxScroll));
                        return;
                    }

                    int mobsStartX = getMobsStartX(leftPanelX);
                    if (localMouseX >= mobsStartX &&
                            localMouseX <= mobsStartX + mobsAreaWidth &&
                            localMouseY >= gridStartY && localMouseY <= gridStartY + currentAreaHeight)
                    {
                        int maxScroll = getMaxMobScroll(set, currentLevel);
                        mobScroll = Math.max(0, Math.min(mobScroll + delta, maxScroll));
                        return;
                    }
                }

                int rightStartX = canShowCurrent ? rightPanelX : leftPanelX;
                int nextLevel = currentLevel <= 0 ? 1 : currentLevel + 1;

                if (localMouseX >= rightStartX + INNER_PADDING &&
                        localMouseX <= rightStartX + INNER_PADDING + blocksAreaWidth &&
                        localMouseY >= gridStartY && localMouseY <= gridStartY + currentAreaHeight)
                {
                    int maxScroll = getMaxBlockScroll(set, nextLevel);
                    blockScrollNext = Math.max(0, Math.min(blockScrollNext + delta, maxScroll));
                    return;
                }

                int rightMobsStartX = getMobsStartX(rightStartX);
                if (localMouseX >= rightMobsStartX &&
                        localMouseX <= rightMobsStartX + mobsAreaWidth &&
                        localMouseY >= gridStartY && localMouseY <= gridStartY + currentAreaHeight)
                {
                    int maxScroll = getMaxMobScroll(set, nextLevel);
                    mobScrollNext = Math.max(0, Math.min(mobScrollNext + delta, maxScroll));
                }
            }
        }
    }

    private int getMaxBlockScroll(BlockSetConfig.BlockSetDefinition set, int level)
    {
        if (set == null) return 0;
        BlockSetConfig.SetLevelDefinition levelDef = set.getLevel(level);
        if (levelDef == null || levelDef.blocks == null || levelDef.blocks.isEmpty()) return 0;

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
            catch (Exception e)
            {
                donateStatusMessage = I18n.format("gui.oneblockultima.donate.open_failed");
                donateStatusTicks = 200;
            }
        }
        else
        {
            GuiOneBlock.setClipboardString(method.value);
            donateStatusMessage = I18n.format("gui.oneblockultima.donate.copied", method.value);
            donateStatusTicks = 200;
        }
    }

    private void drawDonateView()
    {
        int y = infoRowY;
        int centerX = contentLeft + contentWidth / 2;

        String thankYou = I18n.format("gui.oneblockultima.donate.thank_you");
        drawCenteredString(fontRenderer, thankYou, centerX, y, 0x55FF55);
        y += rowInterval * 2;

        String line1 = I18n.format("gui.oneblockultima.donate.line1");
        drawCenteredString(fontRenderer, line1, centerX, y, 0xCCCCCC);
        y += rowInterval;

        String line2 = I18n.format("gui.oneblockultima.donate.line2");
        drawCenteredString(fontRenderer, line2, centerX, y, 0xCCCCCC);
        y += rowInterval;

        String line3 = I18n.format("gui.oneblockultima.donate.line3");
        drawCenteredString(fontRenderer, line3, centerX, y, 0xCCCCCC);
        y += rowInterval;

        int qrSize = 64;
        int qrX = contentLeft + 4;
        int qrY = y;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(SBP_TEXTURE);
        drawModalRectWithCustomSizedTexture(qrX, qrY, 0, 0, qrSize, qrSize, qrSize, qrSize);

        String sbpLabel = "\u0421\u0411\u041F";
        int labelWidth = fontRenderer.getStringWidth(sbpLabel);
        fontRenderer.drawStringWithShadow(sbpLabel, qrX + qrSize / 2.0F - labelWidth / 2.0F, qrY + qrSize + 4, 0xFFFFFF);

        if (donateStatusMessage != null && donateStatusTicks > 0)
        {
            int statusY = ySize - BUTTON_GAP - fontRenderer.FONT_HEIGHT;
            int statusColor = 0x55FF55;
            drawCenteredString(fontRenderer, donateStatusMessage, contentLeft + contentWidth / 2, statusY, statusColor);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        updateLayoutMetrics();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int titleBottom = guiTop + textHeight * 3;
        int tabsBottom = guiTop + tabRowY * 2;
        int infoBottom = guiTop + infoRowY + infoHeight;
        int setContentBottom = this.height - guiTop;

        if (activeView == VIEW_SETTINGS) {
            int remainingHeight = tabsBottom + (BUTTON_HEIGHT + BUTTON_GAP) * 3 + BUTTON_GAP - titleBottom;
            if (remainingHeight > 0) {
                renderProceduralBackground(
                        guiLeft,
                        titleBottom,
                        xSize,
                        remainingHeight
                );
            }

            drawRect(guiLeft, guiTop, guiLeft + xSize, titleBottom, 0xFF22272E);
            drawRect(guiLeft, titleBottom, guiLeft + xSize,
                    remainingHeight + titleBottom, 0xCC1F2328);
            drawRect(guiLeft + panelGap, titleBottom, guiLeft + xSize - panelGap, tabsBottom, 0xFF2E3A45);
        }
        else {
            int infoBgHeight = infoBottom - titleBottom;
            if (infoBgHeight > 0) {
                renderProceduralBackground(
                        guiLeft,
                        titleBottom,
                        xSize,
                        infoBgHeight
                );
            }

            int contentBgHeight = setContentBottom - infoBottom;
            if (contentBgHeight > 0) {
                renderProceduralBackground(
                        guiLeft,
                        infoBottom,
                        xSize,
                        contentBgHeight
                );
            }

            drawRect(guiLeft, guiTop, guiLeft + xSize, titleBottom, 0xFF22272E);
            drawRect(guiLeft, titleBottom, guiLeft + xSize, infoBottom, 0xCC1F2328);
            drawRect(guiLeft, infoBottom, guiLeft + xSize, setContentBottom, 0xCC1F2328);
            drawRect(guiLeft + panelGap, titleBottom, guiLeft + xSize - panelGap, tabsBottom, 0xFF2E3A45);
            drawRect(guiLeft, infoBottom, guiLeft + xSize, infoBottom + 1, 0xFF2E3A45);
        }

        if (tabs != null)
        {
            tabs.draw(fontRenderer, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (tabs != null && tabs.mouseClicked(mouseX, mouseY, mouseButton))
        {
            int clickedId = tabs.getActiveTabId();
            if (clickedId == BUTTON_TAB_SETS) activeView = VIEW_SETS;
            else if (clickedId == BUTTON_TAB_SETTINGS) activeView = VIEW_SETTINGS;
            else if (clickedId == BUTTON_TAB_DONATE) activeView = VIEW_DONATE;
            updateViewButtons();
        }
    }

    private void renderScrollBar(int scrollX, int scrollY, int scrollHeight, int currentScroll, int maxScroll)
    {
        if (maxScroll <= 0 || scrollHeight <= 0) return;

        int barHeight = Math.max(8, (scrollHeight * scrollHeight) / (scrollHeight + maxScroll * cellSize));
        if (barHeight > scrollHeight) barHeight = scrollHeight;
        int barY = scrollY + (currentScroll * (scrollHeight - barHeight)) / maxScroll;

        drawRect(scrollX, scrollY, scrollX + SCROLLBAR_WIDTH, scrollY + scrollHeight, 0xFF2A2F34);
        drawRect(scrollX, barY, scrollX + SCROLLBAR_WIDTH, barY + barHeight, 0xFF7A7F84);
    }

    private void renderMobScrollBar(int scrollX, int scrollY, int scrollHeight, int currentScroll, int maxScroll)
    {
        if (maxScroll <= 0 || scrollHeight <= 0) return;

        int barHeight = Math.max(8, (scrollHeight * scrollHeight) / (scrollHeight + maxScroll * cellSize));
        if (barHeight > scrollHeight) barHeight = scrollHeight;
        int barY = scrollY + (currentScroll * (scrollHeight - barHeight)) / maxScroll;

        drawRect(scrollX, scrollY, scrollX + SCROLLBAR_WIDTH, scrollY + scrollHeight, 0xFF2A2F34);
        drawRect(scrollX, barY, scrollX + SCROLLBAR_WIDTH, barY + barHeight, 0xFF7A7F84);
    }
}
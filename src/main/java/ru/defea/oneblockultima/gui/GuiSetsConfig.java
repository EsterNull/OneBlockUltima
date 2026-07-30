package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.gui.layout.*;
import ru.defea.oneblockultima.util.ModelUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.defea.oneblockultima.gui.containers.ContainerSetsConfig.*;

public class GuiSetsConfig extends GuiScreen
{
    private static final int BUTTON_ADD_SET = 0;
    private static final int BUTTON_SAVE = 1;
    private static final int BUTTON_BACK = 3;
    private static final int BUTTON_ADD_BLOCK = 4;
    private static final int BUTTON_ADD_MOB = 5;
    private static final int BUTTON_REMOVE_ENTRY = 6;
    private static final int BUTTON_CONFIRM_DELETE = 7;
    private static final int BUTTON_CANCEL = 2;
    private static final int BUTTON_RESET = 8;
    private static final int BUTTON_SAVE_CURRENCY = 10;
    private static final int BUTTON_CANCEL_CURRENCY = 11;
    private static final int BUTTON_EDIT_REQUIRED_MODS = 12;
    private static final int BUTTON_REQUIRED_MODS_TOGGLE = 13;
    private static final int BUTTON_REQUIRED_MODS_BACK = 14;
    private static final int BUTTON_REQUIRED_MODS_SAVE = 15;
    private static final int BUTTON_REQUIRED_MODS_DELETE = 16;
    private static final int BUTTON_REQUIRED_MODS_ADD = 17;
    private static final int BUTTON_UNLOCK_CONDITIONS_TOGGLE = 18;
    private static final int BUTTON_UNLOCK_CONDITIONS_BACK = 19;
    private static final int BUTTON_UNLOCK_CONDITIONS_SAVE = 20;
    private static final int BUTTON_UNLOCK_CONDITIONS_ADD = 21;
    private static final int BUTTON_UNLOCK_CONDITIONS_DELETE = 22;
    private static final int BUTTON_EDIT_UNLOCK_CONDITIONS = 23;
    private static final int BUTTON_UNLOCK_CONDITIONS_CYCLE_TYPE = 24;
    private static final int BUTTON_UNLOCK_CONDITIONS_CYCLE_SET = 25;
    private static final int ENTRY_HEIGHT = 22;

    private final GuiScreen parent;
    private final ContainerSetsConfig container;
    private ViewFactory factory;

    private GuiTextField searchField;
    private GuiTextField setNameField;
    private GuiTextField setIdField;
    private GuiTextField unlockCostField;
    private GuiTextField entrySearchField;
    private GuiTextField addLevelField;
    private GuiTextField addChanceField;
    private GuiTextField editLevelField;
    private GuiTextField editChanceField;
    private GuiTextField unlockConditionsLevelField;
    private GuiTextField unlockConditionsCountField;

    private TextFieldElement searchFieldElement;
    private TextFieldElement setNameElement;
    private TextFieldElement setIdElement;
    private TextFieldElement unlockCostElement;
    private TextFieldElement entrySearchElement;
    private TextFieldElement addLevelElement;
    private TextFieldElement addChanceElement;
    private TextFieldElement editLevelElement;
    private TextFieldElement editChanceElement;
    private TextFieldElement unlockLevelElement;
    private TextFieldElement unlockCountElement;

    private ScrollableListElement setsList;
    private TwoColumnListElement entriesList;
    private ScrollableListElement searchResultsList;
    private ScrollableListElement requiredModsList;
    private ScrollableListElement addModsList;
    private ScrollableListElement conditionsList;

    private int setsScrollOffset = 0;
    private int entriesScrollOffset = 0;
    private int searchScrollOffset = 0;
    private int modsScrollOffset = 0;
    private int addModsScrollOffset = 0;
    private int conditionsScrollOffset = 0;

    private boolean suppressMouseUntilRelease = false;
    private boolean suppressNextMouseClick = false;
    private String pendingAddEntrySearchText = "";

    public GuiSetsConfig(GuiScreen parent)
    {
        this.parent = parent;
        this.container = new ContainerSetsConfig();
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buildView();
    }

    private void buildView()
    {
        saveScrollOffsets();

        factory = new ViewFactory(width, height)
                .margin(8)
                .padding(4)
                .gap(6)
                .align(Alignment.CENTER)
                .panel(0xCC22272E, 0xFF3A3F44);

        switch (container.getCurrentView())
        {
            case VIEW_SETS: buildSetsView(); break;
            case VIEW_SET_DETAILS: buildSetDetailsView(); break;
            case VIEW_ADD_ENTRY: buildAddEntryView(); break;
            case VIEW_CONFIRM_DELETE: buildConfirmDeleteView(); break;
            case VIEW_EDIT: buildEditView(); break;
            case VIEW_REQUIRED_MODS_EDITOR: buildRequiredModsEditorView(); break;
            case VIEW_REQUIRED_MODS_ADD: buildRequiredModsAddView(); break;
            case VIEW_UNLOCK_CONDITIONS: buildUnlockConditionsView(); break;
        }

        factory.build(buttonList, fontRenderer);
    }

    private void saveScrollOffsets()
    {
        if (setsList != null) setsScrollOffset = setsList.getScrollOffset();
        if (entriesList != null) entriesScrollOffset = entriesList.getScrollOffset();
        if (searchResultsList != null) searchScrollOffset = searchResultsList.getScrollOffset();
        if (requiredModsList != null) modsScrollOffset = requiredModsList.getScrollOffset();
        if (addModsList != null) addModsScrollOffset = addModsList.getScrollOffset();
        if (conditionsList != null) conditionsScrollOffset = conditionsList.getScrollOffset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();

        if (factory != null) factory.draw(fontRenderer, mouseX, mouseY, partialTicks);

        if (suppressMouseUntilRelease && Mouse.isButtonDown(0))
        {
            for (GuiButton button : buttonList)
                button.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        else
        {
            if (suppressMouseUntilRelease) suppressMouseUntilRelease = false;
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        String status = container.getStatusMessage();
        if (status != null && !status.isEmpty() && container.getStatusTimer() > 0)
        {
            int color = status.contains("error") || status.contains("failed") ? 0xFFFF4444 : 0xFFA0A0A0;
            drawString(fontRenderer, status, width - fontRenderer.getStringWidth(status) - 10, 10, color);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (suppressNextMouseClick) { suppressNextMouseClick = false; return; }
        if (suppressMouseUntilRelease) { suppressMouseUntilRelease = false; return; }

        if (mouseButton == 0)
        {
            for (GuiButton button : new ArrayList<>(buttonList))
            {
                if (button.mousePressed(mc, mouseX, mouseY))
                {
                    button.playPressSound(mc.getSoundHandler());
                    actionPerformed(button);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (factory != null) factory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        if (factory != null) factory.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            handleBack();
            return;
        }

        if (!factory.keyTyped(typedChar, keyCode))
        {
            super.keyTyped(typedChar, keyCode);
        }

        int view = container.getCurrentView();
        if (view == VIEW_SETS && searchFieldElement != null && searchFieldElement.isFocused())
        {
            container.setSearchQuery(searchFieldElement.getText());
            container.updateFilteredSets();
            boolean wasFocused = searchFieldElement.isFocused();
            initGui();
            if (searchFieldElement != null) searchFieldElement.focused(wasFocused);
        }
        if (view == VIEW_ADD_ENTRY && entrySearchElement != null && entrySearchElement.isFocused())
        {
            pendingAddEntrySearchText = entrySearchElement.getText();
            container.performSearch(pendingAddEntrySearchText);
            searchScrollOffset = 0;
            boolean wasFocused = entrySearchElement.isFocused();
            initGui();
            if (entrySearchElement != null) entrySearchElement.focused(wasFocused);
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && factory != null)
        {
            factory.handleMouseInput(dWheel);
        }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (factory != null) factory.updateScreen();
        if (container.getStatusTimer() > 0)
        {
            container.setStatusTimer(container.getStatusTimer() - 1);
        }
    }

    private int getCurrentView() { return container.getCurrentView(); }

    private void changeView(int view)
    {
        int prev = getCurrentView();
        container.changeView(view);
        if (prev != view)
        {
            clearTextFieldFocus();
            initGui();
        }
    }

    private void clearTextFieldFocus()
    {
        if (searchFieldElement != null) searchFieldElement.focused(false);
        if (setNameElement != null) setNameElement.focused(false);
        if (setIdElement != null) setIdElement.focused(false);
        if (unlockCostElement != null) unlockCostElement.focused(false);
        if (entrySearchElement != null) entrySearchElement.focused(false);
        if (addLevelElement != null) addLevelElement.focused(false);
        if (addChanceElement != null) addChanceElement.focused(false);
        if (editLevelElement != null) editLevelElement.focused(false);
        if (editChanceElement != null) editChanceElement.focused(false);
        if (unlockLevelElement != null) unlockLevelElement.focused(false);
        if (unlockCountElement != null) unlockCountElement.focused(false);
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_BACK) { handleBack(); return; }
        if (button.id == BUTTON_SAVE) { handleSave(); return; }
        if (button.id == BUTTON_ADD_SET) { container.addNewSet(); changeView(VIEW_SET_DETAILS); return; }
        if (button.id == BUTTON_RESET) { container.resetToDefault(); initGui(); return; }
        if (button.id == BUTTON_ADD_BLOCK) { container.setCurrentEntryType(EntryType.BLOCK); container.setCurrentSearchType(SearchType.BLOCKS); changeView(VIEW_ADD_ENTRY); return; }
        if (button.id == BUTTON_ADD_MOB) { container.setCurrentEntryType(EntryType.MOB); container.setCurrentSearchType(SearchType.MOBS); changeView(VIEW_ADD_ENTRY); return; }
        if (button.id == BUTTON_REMOVE_ENTRY) { container.removeSelectedEntry(); initGui(); return; }
        if (button.id == BUTTON_CONFIRM_DELETE) { container.executeDeleteSet(); changeView(VIEW_SETS); return; }
        if (button.id == BUTTON_CANCEL) { changeView(container.getCurrentView() == VIEW_CONFIRM_DELETE ? VIEW_SETS : VIEW_SET_DETAILS); return; }
        if (button.id == BUTTON_SAVE_CURRENCY) { handleSaveCurrency(); return; }
        if (button.id == BUTTON_CANCEL_CURRENCY) { changeView(VIEW_SET_DETAILS); return; }
        if (button.id == BUTTON_EDIT_REQUIRED_MODS) { container.initRequiredModsEditor(); changeView(VIEW_REQUIRED_MODS_EDITOR); return; }
        if (button.id == BUTTON_REQUIRED_MODS_TOGGLE) { toggleRequiredModsType(); return; }
        if (button.id == BUTTON_REQUIRED_MODS_BACK) { handleRequiredModsBack(); return; }
        if (button.id == BUTTON_REQUIRED_MODS_SAVE) { handleRequiredModsSave(); return; }
        if (button.id == BUTTON_REQUIRED_MODS_DELETE) { container.deleteSelectedRequiredMods(); initGui(); return; }
        if (button.id == BUTTON_REQUIRED_MODS_ADD) { handleRequiredModsAdd(); return; }
        if (button.id == BUTTON_EDIT_UNLOCK_CONDITIONS) { container.initUnlockConditionsEditor(); changeView(VIEW_UNLOCK_CONDITIONS); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_TOGGLE) { toggleUnlockConditionsMode(); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_BACK) { handleUnlockConditionsBack(); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_SAVE) { handleUnlockConditionsSave(); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_ADD) { handleUnlockConditionsAdd(); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_DELETE) { handleUnlockConditionsDelete(); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_CYCLE_TYPE) { container.cycleUnlockConditionType(); initGui(); return; }
        if (button.id == BUTTON_UNLOCK_CONDITIONS_CYCLE_SET) { container.cycleUnlockConditionSet(); initGui(); return; }
    }

    private void handleBack()
    {
        int v = getCurrentView();
        if (v == VIEW_SET_DETAILS)
        {
            container.discardEditingSetChanges();
            changeView(VIEW_SETS);
        }
        else if (v == VIEW_ADD_ENTRY) changeView(VIEW_SET_DETAILS);
        else if (v == VIEW_REQUIRED_MODS_ADD) changeView(VIEW_REQUIRED_MODS_EDITOR);
        else mc.displayGuiScreen(parent);
    }

    private void handleSave()
    {
        if (container.getEditingSet() == null) return;
        String name = setNameElement != null ? setNameElement.getText().trim() : "";
        String id = setIdElement != null ? setIdElement.getText().trim() : "";
        String cost = unlockCostElement != null ? unlockCostElement.getText().trim() : "0";
        boolean saved = container.saveSetDetails(name, id, cost);
        if (saved) changeView(VIEW_SETS);
        else initGui();
    }

    private void handleSaveCurrency()
    {
        try
        {
            int level = Integer.parseInt(editLevelElement.getText().trim());
            int chance = Math.min(100, Math.max(1, Integer.parseInt(editChanceElement.getText().trim())));
            if (container.saveCurrency(level, chance)) changeView(VIEW_SET_DETAILS);
            else initGui();
        } catch (NumberFormatException e) { initGui(); }
    }

    private void handleRequiredModsBack()
    {
        if (container.getCurrentView() == VIEW_REQUIRED_MODS_ADD)
            changeView(VIEW_REQUIRED_MODS_EDITOR);
        else
            changeView(VIEW_SET_DETAILS);
    }

    private void handleRequiredModsSave()
    {
        container.applyRequiredModsToEditingSet();
        container.setRequiredModsEditorInitialized(false);
        changeView(VIEW_SET_DETAILS);
    }

    private void handleRequiredModsAdd()
    {
        if (container.getCurrentView() == VIEW_REQUIRED_MODS_EDITOR)
        {
            changeView(VIEW_REQUIRED_MODS_ADD);
        }
        else
        {
            container.addSelectedRequiredMods();
            changeView(VIEW_REQUIRED_MODS_EDITOR);
        }
    }

    private void handleUnlockConditionsBack()
    {
        changeView(VIEW_SET_DETAILS);
    }

    private void handleUnlockConditionsSave()
    {
        container.applyUnlockConditionsToEditingSet();
        changeView(VIEW_SET_DETAILS);
    }

    private void handleUnlockConditionsAdd()
    {
        String level = unlockLevelElement != null ? unlockLevelElement.getText() : "";
        String count = unlockCountElement != null ? unlockCountElement.getText() : "";
        container.addUnlockCondition(container.getNewConditionTypeToAdd(), container.getNewConditionSetId(), level, count);
        initGui();
    }

    private void handleUnlockConditionsDelete()
    {
        container.deleteSelectedUnlockCondition();
        initGui();
    }

    private void toggleRequiredModsType()
    {
        container.setRequiredModsEditorType(
                container.getRequiredModsEditorType() == BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL
                        ? BlockSetConfig.SetRequiredModsDefinition.TYPE.ANY
                        : BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL);
        initGui();
    }

    private void toggleUnlockConditionsMode()
    {
        container.setUnlockConditionsEditorMode(
                "any".equals(container.getUnlockConditionsEditorMode()) ? "all" : "any");
        initGui();
    }

    // ======== VIEW BUILDERS ========

    private void buildSetsView()
    {
        container.reloadConfig();
        container.updateFilteredSets();

        factory.title("gui.oneblockultima.config.sets_title");

        int searchWidth = Math.min(width / 3, 250);
        searchFieldElement = new TextFieldElement(searchWidth)
                .text(container.getSearchQuery())
                .focused(false);
        factory.add(searchFieldElement);

        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        List<BlockSetConfig.BlockSetDefinition> filtered = container.getFilteredSets();
        for (int i = 0; i < filtered.size(); i++)
        {
            final int idx = i;
            final BlockSetConfig.BlockSetDefinition set = filtered.get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    boolean isSelected = container.getSelectedSetIndex() == idx;
                    if (isSelected) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0xFF3F5060);
                    else if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);
                    String name = ContainerSetsConfig.getLocalizedSetName(set);
                    fr.drawString(name, x + 4, y + 2, 0xFFFFFF);
                    fr.drawString("ID: " + set.id, x + 4, y + 12, 0xA0A0A0);

                    String editLabel = I18n.format("gui.oneblockultima.config.edit");
                    String delLabel = I18n.format("gui.oneblockultima.config.delete_set");
                    int editW = fr.getStringWidth(editLabel) + 8;
                    int delW = fr.getStringWidth(delLabel) + 8;
                    int right = x + width - 4;
                    int btnY = y + (height - 14) / 2;

                    int delX = right - delW;
                    boolean delHov = mouseX >= delX && mouseX <= delX + delW && mouseY >= btnY && mouseY <= btnY + 14;
                    Gui.drawRect(delX, btnY, delX + delW, btnY + 14, delHov ? 0xFF6A3A3A : 0xFF3A2A2A);
                    drawCenteredString(fr, delLabel, delX + delW / 2, btnY + 3, 0xFFFF4444);

                    int editX = delX - 4 - editW;
                    boolean editHov = mouseX >= editX && mouseX <= editX + editW && mouseY >= btnY && mouseY <= btnY + 14;
                    Gui.drawRect(editX, btnY, editX + editW, btnY + 14, editHov ? 0xFF6A7A8A : 0xFF3A4A5A);
                    drawCenteredString(fr, editLabel, editX + editW / 2, btnY + 3, 0xFFFFFF);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    String editLabel = I18n.format("gui.oneblockultima.config.edit");
                    String delLabel = I18n.format("gui.oneblockultima.config.delete_set");
                    int editW = fontRenderer.getStringWidth(editLabel) + 8;
                    int delW = fontRenderer.getStringWidth(delLabel) + 8;
                    int right = entryWidth - 4;
                    int btnY = (entryHeight - 14) / 2;

                    int delX = right - delW;
                    int editX = delX - 4 - editW;

                    if (mouseXOffset >= delX && mouseXOffset <= delX + delW && mouseYOffset >= btnY && mouseYOffset <= btnY + 14)
                    {
                        container.confirmDeleteSet(idx);
                        changeView(VIEW_CONFIRM_DELETE);
                        return true;
                    }

                    if (mouseXOffset >= editX && mouseXOffset <= editX + editW && mouseYOffset >= btnY && mouseYOffset <= btnY + 14)
                    {
                        container.loadSetDetails(idx);
                        changeView(VIEW_SET_DETAILS);
                        return true;
                    }

                    return false;
                }
            });
        }

        setsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .scrollOffset(setsScrollOffset);
        setsList.flexible(true);
        setsList.visible(true);
        factory.add(setsList);

        RowElement topRow = new RowElement(Alignment.CENTER).gap(4).widthPercent(100);
        topRow.button(BUTTON_ADD_SET, I18n.format("gui.oneblockultima.config.add_set"));
        topRow.spacer(1);
        topRow.button(BUTTON_RESET, I18n.format("gui.oneblockultima.reset_default"));
        topRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.settings.back"));
        factory.add(topRow);
    }

    private void buildSetDetailsView()
    {
        BlockSetConfig.BlockSetDefinition editingSet = container.getEditingSet();
        if (editingSet == null) { changeView(VIEW_SETS); return; }

        factory.title(container.isNewSet()
                ? I18n.format("gui.oneblockultima.config.add_set")
                : ContainerSetsConfig.getLocalizedSetName(editingSet));

        int formMargin = Math.max(10, Math.min(24, width / 24));
        int formWidth = Math.max(220, width - formMargin * 2);
        int formLabelWidth = Math.max(90, Math.min(160, formWidth / 3));
        int fieldMaxWidth = factory.getContentWidth() - (formMargin - 8) - formLabelWidth - 12;
        int formFieldWidth = Math.max(120,
                Math.min(formWidth - formLabelWidth - 6, fieldMaxWidth));

        int leftPad = formMargin - 8;

        setNameElement = new TextFieldElement(formFieldWidth).text(
                container.isNewSet() ? container.getSavedNewSetName()
                        : ContainerSetsConfig.getLocalizedSetName(editingSet));
        RowElement nameRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        nameRow.add(new SpacerElement(leftPad, 20));
        nameRow.add(new LabelElement(I18n.format("gui.oneblockultima.config.set_name") + ":").color(0xA0A0A0).width(formLabelWidth).height(20));
        nameRow.add(setNameElement);
        factory.add(nameRow);

        setIdElement = new TextFieldElement(formFieldWidth).text(
                container.isNewSet() ? container.getSavedNewSetId() : editingSet.id)
                .enabled(container.isNewSet());
        RowElement idRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        idRow.add(new SpacerElement(leftPad, 20));
        idRow.add(new LabelElement(I18n.format("gui.oneblockultima.config.set_id") + ":").color(0xA0A0A0).width(formLabelWidth).height(20));
        idRow.add(setIdElement);
        factory.add(idRow);

        unlockCostElement = new TextFieldElement(formFieldWidth / 2).text(
                container.isNewSet() ? container.getSavedNewSetCost() : String.valueOf(editingSet.unlockCost));
        RowElement costRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        costRow.add(new SpacerElement(leftPad, 20));
        costRow.add(new LabelElement(I18n.format("gui.oneblockultima.config.unlock_cost") + ":").color(0xA0A0A0).width(formLabelWidth).height(20));
        costRow.add(unlockCostElement);
        factory.add(costRow);

        RowElement configBtns = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        configBtns.add(new SpacerElement(leftPad, 20));
        configBtns.add(new LabelElement("").width(formLabelWidth).height(20));
        configBtns.button(BUTTON_EDIT_REQUIRED_MODS, container.getRequiredModsButtonLabel());
        configBtns.button(BUTTON_EDIT_UNLOCK_CONDITIONS, container.getUnlockConditionsButtonLabel());
        factory.add(configBtns);

        factory.add(new SeparatorElement());

        List<TwoColumnListElement.TwoColumnEntry> leftEntries = new ArrayList<>();
        List<TwoColumnListElement.TwoColumnEntry> rightEntries = new ArrayList<>();

        if (editingSet.blocks != null)
        {
            List<BlockDisplayEntry> blockEntries = container.buildBlockDisplayEntries();
            for (final BlockDisplayEntry bde : blockEntries)
            {
                if (bde.blockIndex < 0 || bde.blockIndex >= editingSet.blocks.size()) continue;
                final BlockSetConfig.BlockElementDefinition block = editingSet.blocks.get(bde.blockIndex);
                leftEntries.add(new TwoColumnListElement.TwoColumnEntry() {
                    @Override
                    public void drawLeft(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                        boolean isSelected = container.getSelectedBlockIndex() == bde.blockIndex
                                && container.getSelectedBlockMeta() == bde.meta;
                        if (isSelected) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0xFF3F5060);

                        ItemStack stack = container.getItemStackFromEntry(block, bde.meta);
                        if (!stack.isEmpty())
                        {
                            GlStateManager.enableDepth();
                            RenderHelper.enableGUIStandardItemLighting();
                            GlStateManager.enableRescaleNormal();
                            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, x + 2, y + 2);
                            RenderHelper.disableStandardItemLighting();
                            GlStateManager.disableRescaleNormal();
                            GlStateManager.disableDepth();
                        }
                        else
                        {
                            Fluid fluid = container.getFluidForRegistry(block.registry);
                            if (fluid != null)
                            {
                                int iconSize = Math.min(16, height - 4);
                                ModelUtil.renderFluidSprite(fluid, x + 2, y + 2, iconSize, iconSize);
                            }
                        }

                        String name = container.getLocalizedNameForBlock(block, bde.meta);
                        int btnSize = height - 4;
                        int rightBound = x + width - btnSize - 4;
                        int maxNameW = Math.max(10, rightBound - (x + 20) - 6);
                        String displayName = name;
                        if (fr.getStringWidth(displayName) > maxNameW)
                            displayName = fr.trimStringToWidth(displayName, maxNameW - fr.getStringWidth("...")) + "...";
                        fr.drawString(displayName, x + 20, y + 2, 0xA0A0A0);
                        String levelInfo = I18n.format("gui.oneblockultima.config.base_level") + ": " + block.baseLevel;
                        fr.drawString(levelInfo, x + 20 + fr.getStringWidth(displayName) + 4, y + 2, 0x707070);
                        String registryInfo = block.registry + "  " + I18n.format("gui.oneblockultima.chance") + ": " + block.baseChance + "%";
                        int maxRegW = Math.max(10, rightBound - (x + 20));
                        if (fr.getStringWidth(registryInfo) > maxRegW)
                            registryInfo = fr.trimStringToWidth(registryInfo, maxRegW - fr.getStringWidth("...")) + "...";
                        fr.drawString(registryInfo, x + 20, y + 14, 0x808080);

                        int editX = x + width - btnSize - 2;
                        boolean editHov = mouseX >= editX && mouseX <= editX + btnSize && mouseY >= y + 2 && mouseY <= y + 2 + btnSize;
                        int editColor = editHov ? 0xFF6A7A8A : 0xFF3A4A5A;
                        Gui.drawRect(editX, y + 2, editX + btnSize, y + 2 + btnSize, editColor);
                        drawCenteredString(fr, "\u270E", editX + btnSize / 2, y + 2 + (btnSize - fr.FONT_HEIGHT) / 2, 0xFFFFFF);
                    }

                    @Override
                    public void drawRight(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {}

                    @Override
                    public boolean mouseClickedLeft(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                        int btnSize = entryHeight - 4;
                        int editX = entryWidth - btnSize - 2;

                        if (localX >= editX && localX <= editX + btnSize && localY >= 2 && localY <= 2 + btnSize)
                        {
                            container.setSelectedBlockIndex(bde.blockIndex);
                            container.setSelectedBlockMeta(bde.meta);
                            container.setSelectedMobIndex(-1);
                            container.editEntry(bde.blockIndex, EntryType.BLOCK);
                            changeView(VIEW_EDIT);
                            return true;
                        }
                        container.setSelectedBlockIndex(bde.blockIndex);
                        container.setSelectedBlockMeta(bde.meta);
                        container.setSelectedMobIndex(-1);
                        return true;
                    }

                    @Override
                    public boolean mouseClickedRight(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) { return false; }
                });
            }
        }

        if (editingSet.mobs != null)
        {
            for (int i = 0; i < editingSet.mobs.size(); i++)
            {
                final int mobIdx = i;
                final BlockSetConfig.MobElementDefinition mob = editingSet.mobs.get(i);
                rightEntries.add(new TwoColumnListElement.TwoColumnEntry() {
                    @Override
                    public void drawLeft(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {}

                    @Override
                    public void drawRight(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                        boolean isSelected = container.getSelectedMobIndex() == mobIdx;
                        if (isSelected) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0xFF3F5060);

                        int iconSize = Math.min(16, height - 4);
                        try {
                            World renderWorld = ModelUtil.getWorldOrCreateDummy();
                            Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(mob.registry), renderWorld);
                            if (entity != null)
                            {
                                if (entity.world == null) entity.world = renderWorld;
                                GlStateManager.pushMatrix();
                                try {
                                    int centerX = x + 2 + iconSize / 2;
                                    int centerY = y + 2 + iconSize * 3 / 4;
                                    ModelUtil.drawEntityOnScreen(centerX, centerY, entity, iconSize);
                                } finally {
                                    GlStateManager.popMatrix();
                                }
                            }
                            else
                            {
                                Gui.drawRect(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0xFF444444);
                                fr.drawString("M", x + 4, y + 4, 0xFFFFFF);
                            }
                        } catch (Exception ignored) {
                            Gui.drawRect(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0xFF444444);
                            fr.drawString("M", x + 4, y + 4, 0xFFFFFF);
                        }

                        String name = container.getLocalizedNameForMob(mob);
                        int btnSize = height - 4;
                        int rightBound = x + width - btnSize - 4;
                        int textX = x + iconSize + 6;
                        int maxNameW = Math.max(10, rightBound - textX - 6);
                        String displayName = name;
                        if (fr.getStringWidth(displayName) > maxNameW)
                            displayName = fr.trimStringToWidth(displayName, maxNameW - fr.getStringWidth("...")) + "...";
                        fr.drawString(displayName, textX, y + 2, 0xA0A0A0);
                        String levelInfo = I18n.format("gui.oneblockultima.config.base_level") + ": " + mob.baseLevel;
                        fr.drawString(levelInfo, textX + fr.getStringWidth(displayName) + 4, y + 2, 0x707070);
                        String chanceInfo = I18n.format("gui.oneblockultima.chance") + ": " + mob.baseChance + "%";
                        int maxInfoW = Math.max(10, rightBound - textX);
                        if (fr.getStringWidth(chanceInfo) > maxInfoW)
                            chanceInfo = fr.trimStringToWidth(chanceInfo, maxInfoW - fr.getStringWidth("...")) + "...";
                        fr.drawString(chanceInfo, textX, y + 14, 0x808080);

                        int editX = x + width - btnSize - 2;
                        boolean editHov = mouseX >= editX && mouseX <= editX + btnSize && mouseY >= y + 2 && mouseY <= y + 2 + btnSize;
                        int editColor = editHov ? 0xFF6A7A8A : 0xFF3A4A5A;
                        Gui.drawRect(editX, y + 2, editX + btnSize, y + 2 + btnSize, editColor);
                        drawCenteredString(fr, "\u270E", editX + btnSize / 2, y + 2 + (btnSize - fr.FONT_HEIGHT) / 2, 0xFFFFFF);
                    }

                    @Override
                    public boolean mouseClickedLeft(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) { return false; }

                    @Override
                    public boolean mouseClickedRight(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                        int btnSize = entryHeight - 4;
                        int editX = entryWidth - btnSize - 2;

                        if (localX >= editX && localX <= editX + btnSize && localY >= 2 && localY <= 2 + btnSize)
                        {
                            container.setSelectedMobIndex(mobIdx);
                            container.setSelectedBlockIndex(-1);
                            container.setSelectedBlockMeta(-1);
                            container.editEntry(mobIdx, EntryType.MOB);
                            changeView(VIEW_EDIT);
                            return true;
                        }
                        container.setSelectedMobIndex(mobIdx);
                        container.setSelectedBlockIndex(-1);
                        container.setSelectedBlockMeta(-1);
                        return true;
                    }
                });
            }
        }

        entriesList = new TwoColumnListElement(ENTRY_HEIGHT)
                .leftEntries(leftEntries)
                .rightEntries(rightEntries)
                .scrollOffset(entriesScrollOffset);
        entriesList.flexible(true);
        entriesList.visible(true);
        factory.add(entriesList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.settings.back"));
        btnRow.button(BUTTON_SAVE, I18n.format("gui.oneblockultima.save"));
        btnRow.button(BUTTON_ADD_BLOCK, I18n.format("gui.oneblockultima.config.add_block"));
        btnRow.button(BUTTON_ADD_MOB, I18n.format("gui.oneblockultima.config.add_mob"));
        btnRow.button(BUTTON_REMOVE_ENTRY, I18n.format("gui.oneblockultima.config.remove"));
        factory.add(btnRow);
    }

    private void buildAddEntryView()
    {
        container.setCurrentSearchType(container.getCurrentEntryType() == EntryType.BLOCK ? SearchType.BLOCKS : SearchType.MOBS);
        container.performSearch(pendingAddEntrySearchText);

        factory.title(container.getCurrentEntryType() == EntryType.BLOCK
                ? I18n.format("gui.oneblockultima.config.add_block")
                : I18n.format("gui.oneblockultima.config.add_mob"));

        entrySearchElement = new TextFieldElement(0).widthPercent(80).focused(true);
        if (!pendingAddEntrySearchText.isEmpty())
            entrySearchElement.text(pendingAddEntrySearchText);
        factory.add(entrySearchElement);

        String helpText = I18n.format("gui.oneblockultima.config.search.help");
        factory.add(new LabelElement(helpText).color(0x808080));

        int fieldWidth = Math.max(30, width * 3 / 100);
        RowElement fieldsRow = new RowElement(Alignment.CENTER).gap(4);
        fieldsRow.add(new LabelElement(I18n.format("gui.oneblockultima.config.base_level") + ": ").color(0xC0C0C0));
        addLevelElement = new TextFieldElement(fieldWidth).text("1");
        fieldsRow.add(addLevelElement);
        fieldsRow.add(new LabelElement(I18n.format("gui.oneblockultima.chance") + ": ").color(0xC0C0C0));
        addChanceElement = new TextFieldElement(fieldWidth).text("1");
        fieldsRow.add(addChanceElement);
        factory.add(fieldsRow);

        List<ScrollableListElement.ScrollableListEntry> searchEntries = new ArrayList<>();
        List<SearchResult> results = container.getSearchResults();
        for (int i = 0; i < results.size(); i++)
        {
            final int idx = i;
            final SearchResult result = results.get(i);
            searchEntries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);

                    int iconSize = Math.min(16, height - 4);
                    if (!result.isMob && !result.stack.isEmpty())
                    {
                        GlStateManager.enableDepth();
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableRescaleNormal();
                        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(result.stack, x + 2, y + 2);
                        RenderHelper.disableStandardItemLighting();
                        GlStateManager.disableRescaleNormal();
                        GlStateManager.disableDepth();
                    }
                    else if (result.isFluid && result.fluid != null)
                    {
                        ModelUtil.renderFluidSprite(result.fluid, x + 2, y + 2, iconSize, iconSize);
                    }
                    else if (result.isMob && result.entityClass != null)
                    {
                        try {
                            World renderWorld = ModelUtil.getWorldOrCreateDummy();
                            Entity entity = renderWorld != null ? EntityList.createEntityByIDFromName(new ResourceLocation(result.registry), renderWorld) : null;
                            if (entity == null) {
                                Gui.drawRect(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0xFF444444);
                                fr.drawString("M", x + 4, y + 4, 0xFFFFFF);
                            } else {
                                if (entity.world == null) entity.world = renderWorld;
                                GlStateManager.pushMatrix();
                                try {
                                    int centerX = x + 2 + iconSize / 2;
                                    int centerY = y + 2 + iconSize * 3 / 4;
                                    ModelUtil.drawEntityOnScreen(centerX, centerY, entity, iconSize);
                                } finally {
                                    GlStateManager.popMatrix();
                                }
                            }
                        } catch (Exception ignored) {
                            Gui.drawRect(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0xFF444444);
                            fr.drawString("M", x + 4, y + 4, 0xFFFFFF);
                        }
                    }

                    String displayName = result.name != null && !result.name.isEmpty() ? result.name : result.registry;
                    int textX = x + iconSize + 6;
                    fr.drawStringWithShadow(displayName, textX, y + 2, 0xFFFFFF);
                    fr.drawStringWithShadow(result.registry, textX, y + 12, 0x808080);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    int level = 1;
                    int chance = 1;
                    try { level = Integer.parseInt(addLevelElement.getText().trim()); } catch (NumberFormatException ignored) {}
                    try { chance = Math.min(100, Math.max(1, Integer.parseInt(addChanceElement.getText().trim()))); } catch (NumberFormatException ignored) {}
                    container.addEntryToCurrentSet(container.getCurrentEntryType(), result, level, chance);
                    initGui();
                    return true;
                }
            });
        }

        searchResultsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(searchEntries)
                .panelColor(0xFF1A1F24)
                .scrollOffset(searchScrollOffset);
        searchResultsList.flexible(true);
        searchResultsList.visible(true);
        factory.add(searchResultsList);

        if (searchEntries.isEmpty())
        {
            factory.add(new LabelElement(I18n.format("gui.oneblockultima.config.search.no_results")).color(0x808080).centered(true));
        }

        factory.button(BUTTON_BACK, I18n.format("gui.oneblockultima.settings.back"));
    }

    private void buildConfirmDeleteView()
    {
        List<BlockSetConfig.BlockSetDefinition> sets = container.getSets();
        int deleteTargetIndex = container.getDeleteTargetIndex();
        String deleteName = deleteTargetIndex >= 0 && deleteTargetIndex < sets.size()
                ? ContainerSetsConfig.getLocalizedSetName(sets.get(deleteTargetIndex))
                : "";

        factory.fitContent().centerVertical();
        factory.add(new LabelElement(I18n.format("gui.oneblockultima.config.confirm_delete_message", deleteName)).centered(true).color(0xFFFFFF));

        RowElement btnRow = factory.row(Alignment.CENTER).gap(8);
        btnRow.button(BUTTON_CONFIRM_DELETE, I18n.format("gui.oneblockultima.done"));
        btnRow.button(BUTTON_CANCEL, I18n.format("gui.oneblockultima.cancel"));
    }

    private void buildEditView()
    {
        BlockSetConfig.BlockSetDefinition editingSet = container.getEditingSet();
        int editingCurrencyIndex = container.getEditingCurrencyIndex();
        EntryType editingEntryType = container.getEditingEntryType();

        String entryName = "";
        int currentLevel = 1;
        int currentChance = 1;
        if (editingEntryType == EntryType.BLOCK && editingSet != null && editingSet.blocks != null
                && editingCurrencyIndex >= 0 && editingCurrencyIndex < editingSet.blocks.size())
        {
            BlockSetConfig.BlockElementDefinition entry = editingSet.blocks.get(editingCurrencyIndex);
            entryName = container.getLocalizedNameForBlock(entry, container.getSelectedBlockMeta() >= 0 ? container.getSelectedBlockMeta() : entry.meta);
            currentLevel = entry.baseLevel;
            currentChance = entry.baseChance;
        }
        else if (editingEntryType == EntryType.MOB && editingSet != null && editingSet.mobs != null
                && editingCurrencyIndex >= 0 && editingCurrencyIndex < editingSet.mobs.size())
        {
            BlockSetConfig.MobElementDefinition entry = editingSet.mobs.get(editingCurrencyIndex);
            entryName = container.getLocalizedNameForMob(entry);
            currentLevel = entry.baseLevel;
            currentChance = entry.baseChance;
        }

        factory.gap(3).centerVertical().fitContent();

        factory.add(new LabelElement(I18n.format("gui.oneblockultima.config.edit_title")).centered(true).color(0xFFFFFF));
        if (!entryName.isEmpty())
        {
            factory.add(new LabelElement(entryName).centered(true).color(0xC0C0C0));
        }

        int fieldWidth = Math.max(24, width * 2 / 100);

        ColumnElement labelCol = new ColumnElement().align(Alignment.RIGHT).gap(4);
        labelCol.add(new LabelElement(I18n.format("gui.oneblockultima.config.base_level") + ":").color(0xC0C0C0));
        labelCol.add(new LabelElement(I18n.format("gui.oneblockultima.chance") + ":").color(0xC0C0C0));

        ColumnElement fieldCol = new ColumnElement().gap(4);
        editLevelElement = new TextFieldElement(fieldWidth).text(String.valueOf(currentLevel)).focused(true);
        fieldCol.add(editLevelElement);
        editChanceElement = new TextFieldElement(fieldWidth).text(String.valueOf(currentChance));
        fieldCol.add(editChanceElement);

        RowElement formRow = factory.row(Alignment.CENTER).gap(10);
        formRow.add(labelCol);
        formRow.add(fieldCol);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6);
        btnRow.button(BUTTON_SAVE_CURRENCY, I18n.format("gui.oneblockultima.done"));
        btnRow.button(BUTTON_CANCEL_CURRENCY, I18n.format("gui.oneblockultima.cancel"));
    }

    private void buildRequiredModsEditorView()
    {
        if (container.getEditingSet() == null) { changeView(VIEW_SET_DETAILS); return; }
        if (!container.isRequiredModsEditorInitialized()) container.initRequiredModsEditor();

        factory.title("gui.oneblockultima.config.required_mods_title");

        factory.button(BUTTON_REQUIRED_MODS_TOGGLE, container.getRequiredModsEditorTypeLabel());

        String summary = container.getCurrentRequiredModEntries().isEmpty()
                ? I18n.format("gui.oneblockultima.config.required_mods_empty")
                : I18n.format("gui.oneblockultima.config.required_mods_selected", container.getCurrentRequiredModEntries().size());
        factory.add(new LabelElement(summary).color(0xA0A0A0));
        factory.add(new SeparatorElement());

        List<RequiredModEntry> currentMods = container.getCurrentRequiredModEntries();
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (final RequiredModEntry mod : currentMods)
        {
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    boolean isSel = container.getSelectedRequiredModsForRemoval().contains(mod.modId);
                    if (isSel) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0xFF3F5060);
                    else if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);
                    String label = mod.displayName.isEmpty() ? mod.modId : mod.displayName + " (" + mod.modId + ")";
                    fr.drawStringWithShadow(label, x + 4, y + 4, 0xFFFFFF);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.selectRequiredModForRemoval(mod.modId);
                    return true;
                }
            });
        }

        requiredModsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .panelColor(0xFF1A1F24)
                .scrollOffset(modsScrollOffset);
        requiredModsList.flexible(true);
        requiredModsList.visible(true);
        factory.add(requiredModsList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_REQUIRED_MODS_BACK, I18n.format("gui.oneblockultima.settings.back"));
        btnRow.button(BUTTON_REQUIRED_MODS_ADD, I18n.format("gui.oneblockultima.config.add"));
        btnRow.button(BUTTON_REQUIRED_MODS_DELETE, I18n.format("gui.oneblockultima.config.remove"));
        btnRow.button(BUTTON_REQUIRED_MODS_SAVE, I18n.format("gui.oneblockultima.done"));
        factory.add(btnRow);
    }

    private void buildRequiredModsAddView()
    {
        container.getSelectedRequiredModsToAdd().clear();

        factory.title("gui.oneblockultima.config.required_mods_add_title");

        String summary = I18n.format("gui.oneblockultima.config.required_mods_add_hint");
        factory.add(new LabelElement(summary).color(0xA0A0A0));

        List<RequiredModEntry> availableMods = container.getAvailableRequiredModEntries();
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (final RequiredModEntry mod : availableMods)
        {
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    boolean isSel = container.getSelectedRequiredModsToAdd().contains(mod.modId);
                    if (isSel) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0xFF3F5060);
                    else if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);
                    String label = mod.displayName.isEmpty() ? mod.modId : mod.displayName + " (" + mod.modId + ")";
                    fr.drawStringWithShadow(label, x + 4, y + 4, 0xFFFFFF);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.selectRequiredModToAdd(mod.modId);
                    return true;
                }
            });
        }

        addModsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .panelColor(0xFF1A1F24)
                .scrollOffset(addModsScrollOffset);
        addModsList.flexible(true);
        addModsList.visible(true);
        factory.add(addModsList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_REQUIRED_MODS_BACK, I18n.format("gui.oneblockultima.settings.back"));
        btnRow.button(BUTTON_REQUIRED_MODS_ADD, I18n.format("gui.oneblockultima.config.add"));
        factory.add(btnRow);
    }

    private void buildUnlockConditionsView()
    {
        if (container.getEditingSet() == null) { changeView(VIEW_SET_DETAILS); return; }

        factory.title("gui.oneblockultima.unlock_conditions");

        String toggleLabel = "any".equalsIgnoreCase(container.getUnlockConditionsEditorMode())
                ? I18n.format("gui.oneblockultima.config.any")
                : I18n.format("gui.oneblockultima.config.all");
        factory.button(BUTTON_UNLOCK_CONDITIONS_TOGGLE, toggleLabel);

        String type = container.getNewConditionTypeToAdd();
        String typeLabelText = I18n.format("gui.oneblockultima.config.unlock_conditions_type_" + type);

        RowElement addRow = new RowElement(Alignment.CENTER).gap(4);
        addRow.button(BUTTON_UNLOCK_CONDITIONS_CYCLE_TYPE, typeLabelText);

        if ("set_level".equals(type) || "broken_blocks".equals(type))
        {
            String setId = container.getNewConditionSetId();
            boolean hasSet = setId != null && !setId.isEmpty();
            String setLabel;
            if (hasSet)
            {
                setLabel = ContainerSetsConfig.getLocalizedSetName(
                        container.getAvailableSetsForConditions().stream()
                                .filter(s -> s.id.equals(setId)).findFirst().orElse(null));
                if (setLabel.equals(setId)) setLabel = setId;
            }
            else setLabel = "-";
            addRow.button(BUTTON_UNLOCK_CONDITIONS_CYCLE_SET, setLabel).enabled(hasSet);
        }

        int fieldWidth = Math.max(40, width * 4 / 100);
        if ("set_level".equals(type))
        {
            addRow.add(new LabelElement(I18n.format("gui.oneblockultima.config.unlock_conditions_level") + ":").color(0xC0C0C0));
            unlockLevelElement = new TextFieldElement(fieldWidth).text("1");
            addRow.add(unlockLevelElement);
        }
        else
        {
            addRow.add(new LabelElement(I18n.format("gui.oneblockultima.config.unlock_conditions_count") + ":").color(0xC0C0C0));
            unlockCountElement = new TextFieldElement(fieldWidth).text("1");
            addRow.add(unlockCountElement);
        }
        factory.add(addRow);
        factory.add(new SeparatorElement());

        List<BlockSetConfig.UnlockConditionDefinition> conditions = container.getUnlockConditionsEditorConditions();
        List<BlockSetConfig.BlockSetDefinition> availableSets = container.getAvailableSetsForConditions();
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++)
        {
            final int condIdx = i;
            final BlockSetConfig.UnlockConditionDefinition cond = conditions.get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    boolean isSel = container.getSelectedUnlockConditionIndex() == condIdx;
                    if (isSel) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0xFF3F5060);
                    else if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);

                    String typeName = I18n.format("gui.oneblockultima.config.unlock_conditions_type_" + cond.type);
                    String info = typeName;
                    if (cond.setId != null)
                    {
                        String setName = ContainerSetsConfig.getLocalizedSetName(
                                availableSets.stream().filter(s -> s.id.equals(cond.setId)).findFirst().orElse(null));
                        info += " [" + setName + "]";
                    }
                    if (cond.level > 0) info += " " + I18n.format("gui.oneblockultima.lv") + ":" + cond.level;
                    if (cond.count > 0) info += " x" + cond.count;
                    fr.drawStringWithShadow(info, x + 4, y + 4, 0xFFFFFF);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.setSelectedUnlockConditionIndex(condIdx);
                    return true;
                }
            });
        }

        conditionsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .panelColor(0xFF1A1F24)
                .scrollOffset(conditionsScrollOffset);
        conditionsList.flexible(true);
        conditionsList.visible(true);
        factory.add(conditionsList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_UNLOCK_CONDITIONS_BACK, I18n.format("gui.oneblockultima.settings.back"));
        btnRow.button(BUTTON_UNLOCK_CONDITIONS_SAVE, I18n.format("gui.oneblockultima.done"));
        btnRow.button(BUTTON_UNLOCK_CONDITIONS_ADD, I18n.format("gui.oneblockultima.config.add"));
        btnRow.button(BUTTON_UNLOCK_CONDITIONS_DELETE, I18n.format("gui.oneblockultima.config.remove"));
        factory.add(btnRow);
    }
}

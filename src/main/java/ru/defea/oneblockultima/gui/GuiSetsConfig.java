package ru.defea.oneblockultima.gui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.registries.ForgeRegistries;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.containers.ContainerSetsConfig;
import ru.defea.oneblockultima.gui.layout.*;
import ru.defea.oneblockultima.util.ModelUtil;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.nbt.Tag.*;
import static ru.defea.oneblockultima.Constants.*;
import static ru.defea.oneblockultima.gui.containers.ContainerSetsConfig.*;

public class GuiSetsConfig extends ModScreen
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
    private static final int BUTTON_EDIT_NBT = 26;
    private static final int BUTTON_NBT_ADD = 27;
    private static final int BUTTON_NBT_BACK = 28;
    private static final int BUTTON_NBT_DONE = 29;
    private static final int BUTTON_NBT_CANCEL = 30;
    private static final int BUTTON_NBT_CYCLE_TYPE = 31;
    private static final int BUTTON_NBT_CYCLE_ELEM_TYPE = 32;
    private static final int BUTTON_DELETE_ENTRY = 33;
    private static final int BUTTON_EDIT_CASE = 34;
    private static final int BUTTON_CASE_ENABLED_TOGGLE = 35;
    private static final int BUTTON_CASE_ADD = 36;
    private static final int BUTTON_CASE_ENTRY_SAVE = 37;
    private static final int BUTTON_CASE_ENTRY_CANCEL = 38;
    private static final int BUTTON_CASE_ENTRY_DELETE = 39;
    private static final int BUTTON_CASE_ADD_LOOT = 40;
    private static final int BUTTON_CASE_LOOT_PICK = 41;
    private static final int ENTRY_HEIGHT = 22;
    private static final int NBT_ROW_PADDING = 4;
    private static final int NBT_ROW_BUTTON_HEIGHT = 14;
    private static final int NBT_ROW_TRASH_WIDTH = 18;
    private static final int NBT_ROW_BUTTON_TEXT_PADDING = 10;
    private static final int NBT_ROW_TEXT_TOP = 2;
    private static final int NBT_ROW_TEXT_LINE_GAP = 1;
    private static final int NBT_ROW_TRUNCATION_SLACK = 30;
    private static final int ROW_HEIGHT = 20;
    private static final int PREVIEW_PADDING = 2;

    private final Screen parent;
    private final ContainerSetsConfig container;
    private ViewFactory factory;
    private ViewFactory rootFactory;
    private ViewSwitcherElement switcher;
    private GuiGraphics gfx;

    private TextFieldElement searchFieldElement;
    private TextFieldElement setNameElement;
    private TextFieldElement setIdElement;
    private TextFieldElement unlockCostElement;
    private TextFieldElement entrySearchElement;
    private TextFieldElement editLevelElement;
    private TextFieldElement editChanceElement;
    private TextFieldElement unlockLevelElement;
    private TextFieldElement unlockCountElement;
    private TextFieldElement nbtKeyElement;
    private TextFieldElement nbtValueElement;
    private TextFieldElement caseCountElement;
    private TextFieldElement caseWeightElement;

    private ScrollableListElement setsList;
    private TwoColumnListElement entriesList;
    private ScrollableListElement searchResultsList;
    private ScrollableListElement requiredModsList;
    private ScrollableListElement addModsList;
    private ScrollableListElement conditionsList;
    private ScrollableListElement nbtList;
    private ScrollableListElement caseEntriesList;
    private ScrollableListElement caseLootList;

    private int setsScrollOffset = 0;
    private int entriesScrollOffset = 0;
    private int searchScrollOffset = 0;
    private int modsScrollOffset = 0;
    private int addModsScrollOffset = 0;
    private int conditionsScrollOffset = 0;
    private int nbtScrollOffset = 0;
    private int caseScrollOffset = 0;
    private int caseLootScrollOffset = 0;

    private boolean suppressMouseUntilRelease = false;
    private boolean suppressNextMouseClick = false;
    private String pendingAddEntrySearchText = "";

    private final java.util.Map<String, Entity> mobEntityCache = new java.util.HashMap<>();

    public GuiSetsConfig(Screen parent)
    {
        super(net.minecraft.network.chat.Component.literal("Sets Config"));
        this.parent = parent;
        this.container = new ContainerSetsConfig();
    }

    @Override
    public void init()
    {

        
        this.renderables.clear();
        this.children().clear();
        mobEntityCache.clear();
        buildView();
    }

    private Entity resolveEntity(String registry)
    {
        return resolveEntity(registry, null);
    }

    private Entity resolveEntity(String registry, net.minecraft.nbt.CompoundTag nbtTags)
    {
        if (registry == null || registry.isEmpty())
        {
            return null;
        }

        String cacheKey = registry + "#" + (nbtTags == null ? "" : nbtTags.toString());
        Entity cached = mobEntityCache.get(cacheKey);
        if (cached != null)
        {
            return cached;
        }

        try
        {
            Level renderWorld = this.minecraft.level;
            String normalized = ru.defea.oneblockultima.util.BlockUtil.normalizeLegacyId(registry);
            EntityType<?> type = EntityType.byString(normalized).orElse(null);
            if (type == null) type = resolveEntityType(normalized);
            Entity entity = type != null ? type.create(renderWorld) : null;
            if (entity != null)
            {
                if (nbtTags != null && !nbtTags.isEmpty())
                {
                    ru.defea.oneblockultima.util.BlockUtil.applyNbtToEntity(entity, nbtTags);
                }
                if (entity instanceof net.minecraft.world.entity.monster.Slime slime)
                {
                    slime.setSize(3, false);
                }
                mobEntityCache.put(cacheKey, entity);
            }
            return entity;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private EntityType<?> resolveEntityType(String registry)
    {
        if (registry == null || registry.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(registry);
        if (rl == null) return null;
        EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(rl);
        if (t == null) t = ForgeRegistries.ENTITY_TYPES.getValue(rl);
        return t;
    }

    private void buildView()
    {
        saveScrollOffsets();

        if (rootFactory == null || rootFactory.getScreenWidth() != width || rootFactory.getScreenHeight() != height)
        {
            rootFactory = new ViewFactory(width, height)
                    .margin(2)
                    .padding(4)
                    .gap(6)
                    .align(Alignment.CENTER)
                    .panel(0, 0);
            switcher = new ViewSwitcherElement().widthPercent(100).flexible(true);
            rootFactory.add(switcher);
        }

        factory = new ViewFactory(width, height)
                .margin(2)
                .padding(4)
                .gap(6)
                .align(Alignment.CENTER);

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
            case VIEW_EDIT_NBT: buildEditNbtView(); break;
            case VIEW_NBT_ADD: buildNbtAddView(); break;
            case VIEW_CASE_EDITOR: buildCaseEditorView(); break;
            case VIEW_CASE_ENTRY_ADD: buildCaseEntryAddView(); break;
            case VIEW_CASE_ENTRY_EDIT: buildCaseEntryEditView(); break;
            case VIEW_CASE_LOOT_PICK: buildCaseLootPickView(); break;
        }

        int view = container.getCurrentView();
        switcher.replaceView(view, new ViewFactoryElement(factory));
        switcher.setView(view);
        rootFactory.build(this, Minecraft.getInstance().font);
    }

    private void saveScrollOffsets()
    {
        if (setsList != null) setsScrollOffset = setsList.getScrollOffset();
        if (entriesList != null) entriesScrollOffset = entriesList.getScrollOffset();
        if (searchResultsList != null) searchScrollOffset = searchResultsList.getScrollOffset();
        if (requiredModsList != null) modsScrollOffset = requiredModsList.getScrollOffset();
        if (addModsList != null) addModsScrollOffset = addModsList.getScrollOffset();
        if (conditionsList != null) conditionsScrollOffset = conditionsList.getScrollOffset();
        if (nbtList != null) nbtScrollOffset = nbtList.getScrollOffset();
        if (caseEntriesList != null) caseScrollOffset = caseEntriesList.getScrollOffset();
        if (caseLootList != null) caseLootScrollOffset = caseLootList.getScrollOffset();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        this.gfx = g;
        drawModBackground(g);

        if (factory != null) rootFactory.draw(g, Minecraft.getInstance().font, mouseX, mouseY, partialTicks);

        super.render(g, mouseX, mouseY, partialTicks);

        String status = container.getStatusMessage();
        if (status != null && !status.isEmpty() && container.getStatusTimer() > 0)
        {
            int color = status.contains("error") || status.contains("failed") ? REDDISH_COLOR : GRAY_COLOR_5;
            g.drawString(Minecraft.getInstance().font, status, width - Minecraft.getInstance().font.width(status) - 10, 10, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        if (suppressNextMouseClick) { suppressNextMouseClick = false; return true; }
        if (suppressMouseUntilRelease) { suppressMouseUntilRelease = false; return true; }

        boolean handled = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!handled && rootFactory != null) handled = rootFactory.mouseClicked((int) mouseX, (int) mouseY, mouseButton);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        if (!handled && rootFactory != null) handled = rootFactory.mouseReleased((int) mouseX, (int) mouseY, button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (!handled && rootFactory != null) handled = rootFactory.mouseClickMove((int) mouseX, (int) mouseY, button, 0L);
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            handleBack();
            return true;
        }

        boolean handled = rootFactory.keyTyped((char) keyCode, keyCode);
        if (!handled) handled = super.keyPressed(keyCode, scanCode, modifiers);

        int view = container.getCurrentView();
        if (view == VIEW_SETS && searchFieldElement != null && searchFieldElement.isFocused())
        {
            String query = searchFieldElement.getText();
            if (!query.equals(container.getSearchQuery()))
            {
                container.setSearchQuery(query);
                container.updateFilteredSets();
                int cursor = searchFieldElement.getTextField().getCursorPosition();
                int selection = searchFieldElement.getTextField().getCursorPosition();
                init();
                if (searchFieldElement != null)
                {
                    searchFieldElement.focused(true);
                    if (searchFieldElement.getTextField() != null)
                    {
                        searchFieldElement.getTextField().setCursorPosition(cursor);
                        searchFieldElement.getTextField().setCursorPosition(selection);
                    }
                }
            }
        }
        if ((view == VIEW_ADD_ENTRY || view == VIEW_CASE_ENTRY_ADD) && entrySearchElement != null && entrySearchElement.isFocused())
        {
            String text = entrySearchElement.getText();
            if (!text.equals(pendingAddEntrySearchText))
            {
                pendingAddEntrySearchText = text;
                container.performSearch(pendingAddEntrySearchText);
                searchScrollOffset = 0;
                int cursor = entrySearchElement.getTextField().getCursorPosition();
                int selection = entrySearchElement.getTextField().getCursorPosition();
                init();
                if (entrySearchElement != null)
                {
                    entrySearchElement.focused(true);
                    if (entrySearchElement.getTextField() != null)
                    {
                        entrySearchElement.getTextField().setCursorPosition(cursor);
                        entrySearchElement.getTextField().setCursorPosition(selection);
                    }
                }
            }
        }
        if (view == VIEW_NBT_ADD)
        {
            if (nbtKeyElement != null && nbtKeyElement.isFocused()) container.setNbtEditorKeyText(nbtKeyElement.getText());
            if (nbtValueElement != null && nbtValueElement.isFocused()) container.setNbtEditorValueText(nbtValueElement.getText());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        boolean handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!handled && rootFactory != null) handled = rootFactory.handleMouseInput((int) scrollY);
        return handled;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (rootFactory != null) rootFactory.updateScreen();
        if (container.getStatusTimer() > 0)
        {
            container.setStatusTimer(container.getStatusTimer() - 1);
        }
    }

    private int getCurrentView() { return container.getCurrentView(); }

    private void changeView(int view)
    {
        saveCurrentFormState();
        int prev = getCurrentView();
        container.changeView(view);
        if (prev != view)
        {
            clearTextFieldFocus();
            init();
        }
    }

    private void clearTextFieldFocus()
    {
        if (searchFieldElement != null) searchFieldElement.focused(false);
        if (setNameElement != null) setNameElement.focused(false);
        if (setIdElement != null) setIdElement.focused(false);
        if (unlockCostElement != null) unlockCostElement.focused(false);
        if (entrySearchElement != null) entrySearchElement.focused(false);
        if (editLevelElement != null) editLevelElement.focused(false);
        if (editChanceElement != null) editChanceElement.focused(false);
        if (unlockLevelElement != null) unlockLevelElement.focused(false);
        if (unlockCountElement != null) unlockCountElement.focused(false);
        if (nbtKeyElement != null) nbtKeyElement.focused(false);
        if (nbtValueElement != null) nbtValueElement.focused(false);
        if (caseCountElement != null) caseCountElement.focused(false);
        if (caseWeightElement != null) caseWeightElement.focused(false);
    }

    @Override
    public void onClose()
    {
        saveCurrentFormState();

    }

    private void actionPerformed(int id)
    {
        if (id == BUTTON_BACK) { handleBack(); return; }
        if (id == BUTTON_SAVE) { handleSave(); return; }
        if (id == BUTTON_ADD_SET) { container.addNewSet(); changeView(VIEW_SET_DETAILS); return; }
        if (id == BUTTON_RESET) { container.resetToDefault(); init(); return; }
        if (id == BUTTON_ADD_BLOCK) { saveCurrentFormState(); container.setCurrentEntryType(EntryType.BLOCK); container.setCurrentSearchType(SearchType.BLOCKS); changeView(VIEW_ADD_ENTRY); return; }
        if (id == BUTTON_ADD_MOB) { saveCurrentFormState(); container.setCurrentEntryType(EntryType.MOB); container.setCurrentSearchType(SearchType.MOBS); changeView(VIEW_ADD_ENTRY); return; }
        if (id == BUTTON_REMOVE_ENTRY) { saveCurrentFormState(); container.removeSelectedEntry(); init(); return; }
        if (id == BUTTON_CONFIRM_DELETE) { container.executeDeleteSet(); changeView(VIEW_SETS); return; }
        if (id == BUTTON_CANCEL) { changeView(container.getCurrentView() == VIEW_CONFIRM_DELETE ? VIEW_SETS : VIEW_SET_DETAILS); return; }
        if (id == BUTTON_SAVE_CURRENCY) { handleSaveCurrency(); return; }
        if (id == BUTTON_CANCEL_CURRENCY) { container.cancelPendingAdd(); changeView(VIEW_SET_DETAILS); return; }
        if (id == BUTTON_DELETE_ENTRY) { container.removeSelectedEntry(); changeView(VIEW_SET_DETAILS); return; }
        if (id == BUTTON_EDIT_REQUIRED_MODS) { saveCurrentFormState(); container.initRequiredModsEditor(); changeView(VIEW_REQUIRED_MODS_EDITOR); return; }
        if (id == BUTTON_REQUIRED_MODS_TOGGLE) { toggleRequiredModsType(); return; }
        if (id == BUTTON_REQUIRED_MODS_BACK) { handleRequiredModsBack(); return; }
        if (id == BUTTON_REQUIRED_MODS_SAVE) { handleRequiredModsSave(); return; }
        if (id == BUTTON_REQUIRED_MODS_DELETE) { container.deleteSelectedRequiredMods(); init(); return; }
        if (id == BUTTON_REQUIRED_MODS_ADD) { handleRequiredModsAdd(); return; }
        if (id == BUTTON_EDIT_UNLOCK_CONDITIONS) { saveCurrentFormState(); container.initUnlockConditionsEditor(); changeView(VIEW_UNLOCK_CONDITIONS); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_TOGGLE) { toggleUnlockConditionsMode(); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_BACK) { handleUnlockConditionsBack(); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_SAVE) { handleUnlockConditionsSave(); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_ADD) { handleUnlockConditionsAdd(); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_DELETE) { handleUnlockConditionsDelete(); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_CYCLE_TYPE) { container.cycleUnlockConditionType(); init(); return; }
        if (id == BUTTON_UNLOCK_CONDITIONS_CYCLE_SET) { container.cycleUnlockConditionSet(); init(); return; }
        if (id == BUTTON_EDIT_NBT) { container.nbtEditorStartAdd(); changeView(VIEW_EDIT_NBT); return; }
        if (id == BUTTON_NBT_ADD) { container.nbtEditorStartAdd(); changeView(VIEW_NBT_ADD); return; }
        if (id == BUTTON_NBT_BACK) { handleNbtBack(); return; }
        if (id == BUTTON_NBT_DONE) { handleNbtSave(); return; }
        if (id == BUTTON_NBT_CANCEL) { changeView(VIEW_EDIT_NBT); return; }
        if (id == BUTTON_NBT_CYCLE_TYPE) { container.cycleNbtEditorAddType(); init(); return; }
        if (id == BUTTON_NBT_CYCLE_ELEM_TYPE) { container.cycleNbtEditorListElementType(); init(); }
        if (id == BUTTON_EDIT_CASE) { saveCurrentFormState(); container.initCaseEditor(); changeView(VIEW_CASE_EDITOR); return; }
        if (id == BUTTON_CASE_ENABLED_TOGGLE) { container.toggleCaseEnabled(); init(); return; }
        if (id == BUTTON_CASE_ADD) { saveCurrentFormState(); container.setCurrentEntryType(EntryType.BLOCK); container.setCurrentSearchType(SearchType.BLOCKS); pendingAddEntrySearchText = ""; changeView(VIEW_CASE_ENTRY_ADD); return; }
        if (id == BUTTON_CASE_ADD_LOOT) { container.stageCaseLootAdd(); changeView(VIEW_CASE_ENTRY_EDIT); return; }
        if (id == BUTTON_CASE_ENTRY_SAVE) { handleCaseEntrySave(); return; }
        if (id == BUTTON_CASE_ENTRY_CANCEL) { container.cancelCaseEntryAdd(); changeView(VIEW_CASE_EDITOR); return; }
        if (id == BUTTON_CASE_LOOT_PICK) { changeView(VIEW_CASE_LOOT_PICK); return; }
        if (id == BUTTON_CASE_ENTRY_DELETE)
        {
            container.removeCaseEntry();
            if (container.getCurrentView() == VIEW_CASE_EDITOR) init();
            else changeView(VIEW_CASE_EDITOR);
        }
    }

    private void handleBack()
    {
        saveCurrentFormState();
        int v = getCurrentView();
        if (v == VIEW_SET_DETAILS)
        {
            container.discardEditingSetChanges();
            changeView(VIEW_SETS);
        }
        else if (v == VIEW_ADD_ENTRY) changeView(VIEW_SET_DETAILS);
        else if (v == VIEW_EDIT) { container.cancelPendingAdd(); changeView(VIEW_SET_DETAILS); }
        else if (v == VIEW_REQUIRED_MODS_ADD) changeView(VIEW_REQUIRED_MODS_EDITOR);
        else if (v == VIEW_EDIT_NBT) handleNbtBack();
        else if (v == VIEW_NBT_ADD) changeView(VIEW_EDIT_NBT);
        else if (v == VIEW_CASE_EDITOR) { changeView(VIEW_SET_DETAILS); }
        else if (v == VIEW_CASE_ENTRY_ADD) changeView(VIEW_CASE_EDITOR);
        else if (v == VIEW_CASE_ENTRY_EDIT) { container.cancelCaseEntryAdd(); changeView(VIEW_CASE_EDITOR); }
        else if (v == VIEW_CASE_LOOT_PICK) { changeView(VIEW_CASE_ENTRY_EDIT); }
        else Minecraft.getInstance().setScreen(parent);
    }

    private void handleSave()
    {
        if (container.getEditingSet() == null) return;
        String name = setNameElement != null ? setNameElement.getText().trim() : "";
        String id = setIdElement != null ? setIdElement.getText().trim() : "";
        String cost = unlockCostElement != null ? unlockCostElement.getText().trim() : "0";
        boolean saved = container.saveSetDetails(name, id, cost);
        if (saved) changeView(VIEW_SETS);
        else init();
    }

    private void saveCurrentFormState()
    {
        if (getCurrentView() == VIEW_SET_DETAILS)
        {
            if (setNameElement != null)
            {
                container.setSavedNewSetName(setNameElement.getText());
            }
            if (setIdElement != null)
            {
                container.setSavedNewSetId(setIdElement.getText());
            }
            if (unlockCostElement != null)
            {
                container.setSavedNewSetCost(unlockCostElement.getText());
            }
        }
        if (getCurrentView() == VIEW_CASE_ENTRY_EDIT)
        {
            if (caseCountElement != null) container.setCaseCountText(caseCountElement.getText());
            if (caseWeightElement != null) container.setCaseWeightText(caseWeightElement.getText());
        }
    }

    private void handleSaveCurrency()
    {
        try
        {
            int level = Integer.parseInt(editLevelElement.getText().trim());
            int chance = Math.min(100, Integer.parseInt(editChanceElement.getText().trim()));
            if (container.saveCurrency(level, chance))
            {
                container.clearPendingAdd();
                changeView(VIEW_SET_DETAILS);
            }
            else init();
        } catch (NumberFormatException e) { init(); }
    }

    private void handleNbtBack()
    {
        if (container.nbtEditorAtRoot())
        {
            changeView(VIEW_EDIT);
        }
        else
        {
            container.nbtEditorPop();
            init();
        }
    }

    private void handleNbtSave()
    {
        String key = nbtKeyElement != null ? nbtKeyElement.getText() : "";
        String value = nbtValueElement != null ? nbtValueElement.getText() : "";
        if (container.nbtEditorApply(key, value))
        {
            changeView(VIEW_EDIT_NBT);
        }
        else
        {
            if (key != null && !key.trim().isEmpty()) container.setNbtEditorValueText("");
            init();
        }
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
        init();
    }

    private void handleUnlockConditionsDelete()
    {
        container.deleteSelectedUnlockCondition();
        init();
    }

    private void toggleRequiredModsType()
    {
        container.setRequiredModsEditorType(
                container.getRequiredModsEditorType() == BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL
                        ? BlockSetConfig.SetRequiredModsDefinition.TYPE.ANY
                        : BlockSetConfig.SetRequiredModsDefinition.TYPE.ALL);
        init();
    }

    private void toggleUnlockConditionsMode()
    {
        container.setUnlockConditionsEditorMode(
                "any".equals(container.getUnlockConditionsEditorMode()) ? "all" : "any");
        init();
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
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    boolean isSelected = container.getSelectedSetIndex() == idx;
                    if (isSelected) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);
                    else if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);
                    String name = ContainerSetsConfig.getLocalizedSetName(set);
                    gfx.drawString(fr, name, x + 4, y + 2, WHITE_COLOR_1);
                    gfx.drawString(fr, "ID: " + set.id, x + 4, y + 12, GRAY_COLOR_5);

                    String editLabel = I18n.get("gui.oneblockultima.config.edit");
                    String delLabel = I18n.get("gui.oneblockultima.config.delete_set");
                    int editW = fr.width(editLabel) + 8;
                    int delW = fr.width(delLabel) + 8;
                    int right = x + width - 4;
                    int btnY = y + (height - 14) / 2;

                    int delX = right - delW;
                    boolean delHov = mouseX >= delX && mouseX <= delX + delW && mouseY >= btnY && mouseY <= btnY + 14;
                    gfx.fill(delX, btnY, delX + delW, btnY + 14, delHov ? DARK_RED_COLOR_1 : DARK_RED_COLOR_2);
                    gfx.drawCenteredString(fr, delLabel, delX + delW / 2, btnY + 3, REDDISH_COLOR);

                    int editX = delX - 4 - editW;
                    boolean editHov = mouseX >= editX && mouseX <= editX + editW && mouseY >= btnY && mouseY <= btnY + 14;
                    gfx.fill(editX, btnY, editX + editW, btnY + 14, editHov ? BLUE_GRAY_COLOR : DARK_BLUE_GRAY_COLOR_1);
                    gfx.drawCenteredString(fr, editLabel, editX + editW / 2, btnY + 3, WHITE_COLOR_1);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    String editLabel = I18n.get("gui.oneblockultima.config.edit");
                    String delLabel = I18n.get("gui.oneblockultima.config.delete_set");
                    int editW = Minecraft.getInstance().font.width(editLabel) + 8;
                    int delW = Minecraft.getInstance().font.width(delLabel) + 8;
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
        topRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK)).onPress(() -> actionPerformed(BUTTON_BACK));
        topRow.button(BUTTON_RESET, I18n.get("gui.oneblockultima.reset_default")).onPress(() -> actionPerformed(BUTTON_RESET)).onPress(() -> actionPerformed(BUTTON_RESET));
        topRow.button(BUTTON_ADD_SET, I18n.get("gui.oneblockultima.config.add_set")).onPress(() -> actionPerformed(BUTTON_ADD_SET));
        factory.add(topRow);
    }

    private void buildSetDetailsView()
    {
        BlockSetConfig.BlockSetDefinition editingSet = container.getEditingSet();
        if (editingSet == null) { changeView(VIEW_SETS); return; }

        factory.title(container.isNewSet()
                ? I18n.get("gui.oneblockultima.config.add_set")
                : ContainerSetsConfig.getLocalizedSetName(editingSet));

        int contentWidth = rootFactory.getContentWidth() - 2 * rootFactory.getContentX();
        int formMargin = Math.max(10, Math.min(24, contentWidth / 24));
        int formLabelWidth = Math.max(90, Math.min(160, contentWidth / 3));
        int formFieldWidth = Math.max(120, contentWidth - formMargin - formLabelWidth - 12);

        setNameElement = new TextFieldElement(formFieldWidth).text(
                container.isNewSet() ? container.getSavedNewSetName()
                        : ContainerSetsConfig.getLocalizedSetName(editingSet));
        RowElement nameRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        nameRow.add(new SpacerElement(formMargin, 20));
        nameRow.add(new LabelElement(I18n.get("gui.oneblockultima.config.set_name") + ":").color(GRAY_COLOR_5).width(formLabelWidth).height(20));
        nameRow.add(setNameElement);
        factory.add(nameRow);

        setIdElement = new TextFieldElement(formFieldWidth).text(
                container.isNewSet() ? container.getSavedNewSetId() : editingSet.id)
                .enabled(container.isNewSet());
        RowElement idRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        idRow.add(new SpacerElement(formMargin, 20));
        idRow.add(new LabelElement(I18n.get("gui.oneblockultima.config.set_id") + ":").color(GRAY_COLOR_5).width(formLabelWidth).height(20));
        idRow.add(setIdElement);
        factory.add(idRow);

        unlockCostElement = new TextFieldElement(formFieldWidth / 2).text(
                container.isNewSet() ? container.getSavedNewSetCost() : String.valueOf(editingSet.unlockCost));
        RowElement costRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        costRow.add(new SpacerElement(formMargin, 20));
        costRow.add(new LabelElement(I18n.get("gui.oneblockultima.unlock_cost") + ":").color(GRAY_COLOR_5).width(formLabelWidth).height(20));
        costRow.add(unlockCostElement);
        factory.add(costRow);

        RowElement configButtons = new RowElement(Alignment.CENTER).gap(6).widthPercent(100);
        configButtons.button(BUTTON_EDIT_REQUIRED_MODS, container.getRequiredModsButtonLabel()).onPress(() -> actionPerformed(BUTTON_EDIT_REQUIRED_MODS));
        configButtons.button(BUTTON_EDIT_UNLOCK_CONDITIONS, container.getUnlockConditionsButtonLabel()).onPress(() -> actionPerformed(BUTTON_EDIT_UNLOCK_CONDITIONS));
        configButtons.button(BUTTON_EDIT_CASE, container.getCaseButtonLabel()).onPress(() -> actionPerformed(BUTTON_EDIT_CASE));
        factory.add(configButtons);

        factory.add(new SeparatorElement());

        List<TwoColumnListElement.TwoColumnEntry> leftEntries = new ArrayList<>();
        List<TwoColumnListElement.TwoColumnEntry> rightEntries = new ArrayList<>();

        double caseDropPercent = 0.0D;
        if (editingSet.hasCaseEntries())
        {
            caseDropPercent = ModSettings.get().getCaseDropPercent();
            if (caseDropPercent < 0.0D) caseDropPercent = 0.0D;
            if (caseDropPercent > 100.0D) caseDropPercent = 100.0D;
        }
        final double caseScaleFactor = (100.0D - caseDropPercent) / 100.0D;

        if (editingSet.blocks != null)
        {
            List<BlockDisplayEntry> blockEntries = container.buildBlockDisplayEntries();
            for (final BlockDisplayEntry bde : blockEntries)
            {
                if (bde.blockIndex < 0 || bde.blockIndex >= editingSet.blocks.size()) continue;
                final BlockSetConfig.BlockElementDefinition block = editingSet.blocks.get(bde.blockIndex);
                final InlineClickableElement editButton = new InlineClickableElement("\u270E", DARK_BLUE_GRAY_COLOR_1, BLUE_GRAY_COLOR, (mx, my, mb) -> {
                    container.setSelectedBlockIndex(bde.blockIndex);
                    container.setSelectedBlockMeta(bde.meta);
                    container.setSelectedMobIndex(-1);
                    container.editEntry(bde.blockIndex, EntryType.BLOCK);
                    changeView(VIEW_EDIT);
                    return true;
                });
                leftEntries.add(new TwoColumnListElement.TwoColumnEntry() {
                    @Override
                    public void drawLeft(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                        boolean isSelected = container.getSelectedBlockIndex() == bde.blockIndex
                                && container.getSelectedBlockMeta() == bde.meta;
                        if (isSelected) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);

                        ItemStack stack = container.getItemStackFromEntry(block, bde.meta);

                        final int cellPadding = 2;
                        final int itemIconSize = 16;
                        final int textGap = 4;
                        final int fontHeight = fr.lineHeight;
                        final int badgeTextPadding = 2;

                        if (!stack.isEmpty())
                        {
                                                                                                                gfx.renderItem(stack, x + cellPadding, y + cellPadding);
                                                                                                            }
                        else
                        {
                            Fluid fluid = container.getFluidForRegistry(block.registry);
                            if (fluid != null)
                            {
                                int iconSize = Math.min(itemIconSize, height - 2 * cellPadding);
                                FluidElement fluidIcon = new FluidElement(fluid).size(iconSize);
                                fluidIcon.setComputedPosition(x + cellPadding, y + cellPadding);
                                fluidIcon.setComputedSize(iconSize, iconSize);
                                fluidIcon.draw(gfx, fr, mouseX, mouseY, 0);
                            }
                        }

                        String name = container.getLocalizedNameForBlock(block, bde.meta);
                        int btnSize = height - 2 * cellPadding;
                        int rightBound = x + width - btnSize - 2 * cellPadding;
                        int textX = x + cellPadding + itemIconSize + cellPadding;
                        int maxNameW = Math.max(10, rightBound - textX - textGap - cellPadding);
                        String displayName = name;
                        if (fr.width(displayName) > maxNameW)
                            displayName = fr.plainSubstrByWidth(displayName, maxNameW - fr.width("...")) + "...";
                        gfx.drawString(fr, displayName, textX, y + cellPadding, GRAY_COLOR_5);
                        String levelInfo = I18n.get("gui.oneblockultima.config.base_level") + ": " + block.baseLevel;
                        gfx.drawString(fr, levelInfo, textX + fr.width(displayName) + textGap, y + cellPadding, GRAY_COLOR_7);
                        int infoX = textX;
                        boolean hasNbt = block.nbtTags != null && !block.nbtTags.isEmpty();
                        if (hasNbt)
                        {
                            String nbtLabel = "NBT";
                            int badgeW = fr.width(nbtLabel) + 2 * badgeTextPadding;
                            int badgeY = y + height - fontHeight;
                            gfx.fill(infoX, badgeY, infoX + badgeW, badgeY + fontHeight, DARK_BLUE_GRAY_COLOR_1);
                            gfx.drawString(fr, nbtLabel, infoX + badgeTextPadding, badgeY + 1, GOLD_COLOR);
                            infoX += badgeW + 2 * badgeTextPadding;
                        }
                        String registryInfo = block.registry + "  " + I18n.get("gui.oneblockultima.chance") + ": "
                                + Math.round(block.baseChance * caseScaleFactor) + "%";
                        int maxRegW = Math.max(10, rightBound - infoX);
                        if (fr.width(registryInfo) > maxRegW)
                            registryInfo = fr.plainSubstrByWidth(registryInfo, maxRegW - fr.width("...")) + "...";
                        gfx.drawString(fr, registryInfo, infoX, y + height - fontHeight + 1, GRAY_COLOR_1);

                        int editX = x + width - btnSize - cellPadding;
                        editButton.setComputedPosition(editX, y + cellPadding);
                        editButton.setComputedSize(btnSize, btnSize);
                        editButton.draw(gfx, fr, mouseX, mouseY, 0);
                    }

                    @Override
                    public void drawRight(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {}

                    @Override
                    public boolean mouseClickedLeft(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                        int btnSize = entryHeight - 4;
                        editButton.setComputedPosition(mouseX - localX + entryWidth - btnSize - 2, mouseY - localY + 2);
                        editButton.setComputedSize(btnSize, btnSize);
                        if (editButton.mouseClicked(mouseX, mouseY, mouseButton)) return true;
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
                final InlineClickableElement editButton = new InlineClickableElement("\u270E", DARK_BLUE_GRAY_COLOR_1, BLUE_GRAY_COLOR, (mx, my, mb) -> {
                    container.setSelectedMobIndex(mobIdx);
                    container.setSelectedBlockIndex(-1);
                    container.setSelectedBlockMeta(-1);
                    container.editEntry(mobIdx, EntryType.MOB);
                    changeView(VIEW_EDIT);
                    return true;
                });
                rightEntries.add(new TwoColumnListElement.TwoColumnEntry() {
                    @Override
                    public void drawLeft(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {}

                    @Override
                    public void drawRight(int x, int y, int width, int height, boolean hovered, int index, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                        boolean isSelected = container.getSelectedMobIndex() == mobIdx;
                        if (isSelected) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);

                        int iconSize = Math.max(4, height - 8);
                        EntityRendererElement mobIcon = new EntityRendererElement(resolveEntity(mob.registry, mob.nbtTags), resolveEntityType(mob.registry));
                        mobIcon.scale(iconSize * 2);
                        mobIcon.setComputedPosition(x + 2, y + 2);
                        mobIcon.setComputedSize(iconSize, iconSize);
                        mobIcon.draw(gfx, fr, mouseX, mouseY, 0);

                        String name = container.getLocalizedNameForMob(mob);
                        int btnSize = height - 4;
                        int rightBound = x + width - btnSize - 4;
                        int textX = x + iconSize + 6;
                        int maxNameW = Math.max(10, rightBound - textX - 6);
                        String displayName = name;
                        if (fr.width(displayName) > maxNameW)
                            displayName = fr.plainSubstrByWidth(displayName, maxNameW - fr.width("...")) + "...";
                        gfx.drawString(fr, displayName, textX, y + 2, GRAY_COLOR_5);
                        String levelInfo = I18n.get("gui.oneblockultima.config.base_level") + ": " + mob.baseLevel;
                        gfx.drawString(fr, levelInfo, textX + fr.width(displayName) + 4, y + 2, GRAY_COLOR_7);
                        String chanceInfo = I18n.get("gui.oneblockultima.chance") + ": " + mob.baseChance + "%";
                        int infoX = textX;
                        boolean hasNbt = mob.nbtTags != null && !mob.nbtTags.isEmpty();
                        if (hasNbt)
                        {
                            String nbtLabel = "NBT";
                            int badgeW = fr.width(nbtLabel) + 2 * 2;
                            int badgeY = y + height - fr.lineHeight;
                            gfx.fill(infoX, badgeY, infoX + badgeW, badgeY + fr.lineHeight, DARK_BLUE_GRAY_COLOR_1);
                            gfx.drawString(fr, nbtLabel, infoX + 2, badgeY + 1, GOLD_COLOR);
                            infoX += badgeW + 4;
                        }
                        int maxInfoW = Math.max(10, rightBound - infoX);
                        if (fr.width(chanceInfo) > maxInfoW)
                            chanceInfo = fr.plainSubstrByWidth(chanceInfo, maxInfoW - fr.width("...")) + "...";
                        gfx.drawString(fr, chanceInfo, infoX, y + 14, GRAY_COLOR_1);

                        int editX = x + width - btnSize - 2;
                        editButton.setComputedPosition(editX, y + 2);
                        editButton.setComputedSize(btnSize, btnSize);
                        editButton.draw(gfx, fr, mouseX, mouseY, 0);
                    }

                    @Override
                    public boolean mouseClickedLeft(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) { return false; }

                    @Override
                    public boolean mouseClickedRight(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                        int btnSize = entryHeight - 4;
                        editButton.setComputedPosition(mouseX - localX + entryWidth - btnSize - 2, mouseY - localY + 2);
                        editButton.setComputedSize(btnSize, btnSize);
                        if (editButton.mouseClicked(mouseX, mouseY, mouseButton)) return true;
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
        btnRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK)).onPress(() -> actionPerformed(BUTTON_BACK));
        btnRow.add(new DangerButtonElement(BUTTON_REMOVE_ENTRY, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_REMOVE_ENTRY)));
        btnRow.button(BUTTON_ADD_BLOCK, I18n.get("gui.oneblockultima.config.add_block")).onPress(() -> actionPerformed(BUTTON_ADD_BLOCK));
        btnRow.button(BUTTON_ADD_MOB, I18n.get("gui.oneblockultima.config.add_mob")).onPress(() -> actionPerformed(BUTTON_ADD_MOB));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.get("gui.oneblockultima.save")).onPress(() -> actionPerformed(BUTTON_SAVE)));
        factory.add(btnRow);
    }

    private void buildAddEntryView()
    {
        container.setCurrentSearchType(container.getCurrentEntryType() == EntryType.BLOCK ? SearchType.BLOCKS : SearchType.MOBS);
        container.performSearch(pendingAddEntrySearchText);

        factory.title(container.getCurrentEntryType() == EntryType.BLOCK
                ? I18n.get("gui.oneblockultima.config.add_block")
                : I18n.get("gui.oneblockultima.config.add_mob"));

        entrySearchElement = new TextFieldElement(0).widthPercent(80).focused(true);
        if (!pendingAddEntrySearchText.isEmpty())
            entrySearchElement.text(pendingAddEntrySearchText);
        factory.add(entrySearchElement);

        String helpText = I18n.get("gui.oneblockultima.config.search.help");
        factory.add(new LabelElement(helpText).color(GRAY_COLOR_1));

        List<ScrollableListElement.ScrollableListEntry> searchEntries = new ArrayList<>();
        List<SearchResult> results = container.getSearchResults();
        for (final SearchResult result : results) {
            searchEntries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    int iconSize = Math.min(16, height - 4);
                    if (!result.isMob && !result.stack.isEmpty()) {
                                                                                                gfx.renderItem(result.stack, x + 2, y + 2);
                                                                                            } else if (result.isFluid && result.fluid != null) {
                        FluidElement fluidIcon = new FluidElement(result.fluid).size(iconSize);
                        fluidIcon.setComputedPosition(x + 2, y + 2);
                        fluidIcon.setComputedSize(iconSize, iconSize);
                        fluidIcon.draw(gfx, fr, mouseX, mouseY, 0);
                    } else if (result.isMob && result.entityClass != null) {
                        EntityRendererElement mobIcon = new EntityRendererElement(resolveEntity(result.registry), result.entityClass);
                        mobIcon.scale(iconSize * 2);
                        mobIcon.setComputedPosition(x + 2, y + 2);
                        mobIcon.setComputedSize(iconSize, iconSize);
                        mobIcon.draw(gfx, fr, mouseX, mouseY, 0);
                    }

                    String displayName = result.name != null && !result.name.isEmpty() ? result.name : result.registry;
                    int textX = x + iconSize + 6;
                    gfx.drawString(fr, displayName, textX, y + 2, WHITE_COLOR_1, true);
                    gfx.drawString(fr, result.registry, textX, y + 12, GRAY_COLOR_1, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    pendingAddEntrySearchText = entrySearchElement != null ? entrySearchElement.getText() : "";
                    container.stagePendingAdd(container.getCurrentEntryType(), result);
                    changeView(VIEW_EDIT);
                    return true;
                }
            });
        }

        searchResultsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(searchEntries)
                .scrollOffset(searchScrollOffset);
        searchResultsList.flexible(true);
        searchResultsList.visible(true);
        factory.add(searchResultsList);

        if (searchEntries.isEmpty())
        {
            factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.search.no_results")).color(GRAY_COLOR_1).centered());
        }

        factory.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK)).onPress(() -> actionPerformed(BUTTON_BACK));
    }

    private void buildConfirmDeleteView()
    {
        List<BlockSetConfig.BlockSetDefinition> sets = container.getSets();
        int deleteTargetIndex = container.getDeleteTargetIndex();
        String deleteName = deleteTargetIndex >= 0 && deleteTargetIndex < sets.size()
                ? ContainerSetsConfig.getLocalizedSetName(sets.get(deleteTargetIndex))
                : "";

        factory.fitContent().centerVertical();
        factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.confirm_delete_message", deleteName)).centered());

        RowElement btnRow = factory.row(Alignment.CENTER).gap(8);
        btnRow.button(BUTTON_CANCEL, I18n.get("gui.oneblockultima.cancel")).onPress(() -> actionPerformed(BUTTON_CANCEL)).onPress(() -> actionPerformed(BUTTON_CANCEL));
        btnRow.add(new DangerButtonElement(BUTTON_CONFIRM_DELETE, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_CONFIRM_DELETE)));
    }

    private void buildEditView()
    {
        BlockSetConfig.BlockSetDefinition editingSet = container.getEditingSet();
        int editingCurrencyIndex = container.getEditingCurrencyIndex();
        EntryType editingEntryType = container.getEditingEntryType();

        String entryName = container.getEditingEntryDisplayName();
        int currentLevel = 1;
        int currentChance = 1;
        if (editingEntryType == EntryType.BLOCK && editingSet != null && editingSet.blocks != null
                && editingCurrencyIndex >= 0 && editingCurrencyIndex < editingSet.blocks.size())
        {
            BlockSetConfig.BlockElementDefinition entry = editingSet.blocks.get(editingCurrencyIndex);
            currentLevel = entry.baseLevel;
            currentChance = entry.baseChance;
        }
        else if (editingEntryType == EntryType.MOB && editingSet != null && editingSet.mobs != null
                && editingCurrencyIndex >= 0 && editingCurrencyIndex < editingSet.mobs.size())
        {
            BlockSetConfig.MobElementDefinition entry = editingSet.mobs.get(editingCurrencyIndex);
            currentLevel = entry.baseLevel;
            currentChance = entry.baseChance;
        }

        factory.gap(3).centerVertical().fitContent();

        factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.edit_title")).centered());
        if (!entryName.isEmpty())
        {
            factory.add(new LabelElement(entryName).centered());
        }

        ViewElement<?> preview = buildEditPreview(editingSet, editingCurrencyIndex, editingEntryType,
                ROW_HEIGHT - 2 * PREVIEW_PADDING);
        if (preview != null)
        {
            RowElement previewRow = factory.row(Alignment.CENTER).gap(6).height(ROW_HEIGHT);
            previewRow.add(preview);
        }

        int fieldWidth = Math.max(24, width * 2 / 100);

        ColumnElement labelCol = new ColumnElement().align(Alignment.RIGHT).gap(4);
        labelCol.add(new LabelElement(I18n.get("gui.oneblockultima.config.base_level") + ":"));
        labelCol.add(new LabelElement(I18n.get("gui.oneblockultima.chance") + ":"));

        ColumnElement fieldCol = new ColumnElement().gap(4);
        editLevelElement = new TextFieldElement(fieldWidth).text(String.valueOf(currentLevel)).focused(true);
        fieldCol.add(editLevelElement);
        editChanceElement = new TextFieldElement(fieldWidth).text(String.valueOf(currentChance));
        fieldCol.add(editChanceElement);

        RowElement formRow = factory.row(Alignment.CENTER).gap(10).height(ROW_HEIGHT);
        formRow.add(labelCol);
        formRow.add(fieldCol);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6).height(ROW_HEIGHT);
        btnRow.button(BUTTON_CANCEL_CURRENCY, I18n.get("gui.oneblockultima.cancel")).onPress(() -> actionPerformed(BUTTON_CANCEL_CURRENCY));
        btnRow.add(new DangerButtonElement(BUTTON_DELETE_ENTRY, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_DELETE_ENTRY)));
        btnRow.button(BUTTON_EDIT_NBT, I18n.get("gui.oneblockultima.config.nbt_edit")).onPress(() -> actionPerformed(BUTTON_EDIT_NBT));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE_CURRENCY, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_SAVE_CURRENCY)));
    }

    private ViewElement<?> buildEditPreview(BlockSetConfig.BlockSetDefinition editingSet, int editingCurrencyIndex, EntryType editingEntryType, @SuppressWarnings("SameParameterValue") int previewSize)
    {
        if (editingEntryType == EntryType.BLOCK && editingSet != null && editingSet.blocks != null
                && editingCurrencyIndex >= 0 && editingCurrencyIndex < editingSet.blocks.size())
        {
            BlockSetConfig.BlockElementDefinition entry = editingSet.blocks.get(editingCurrencyIndex);
            if (entry == null || entry.registry == null || entry.registry.isEmpty())
            {
                return null;
            }

            int meta = container.getSelectedBlockMeta();
            if (meta < 0)
            {
                meta = entry.meta;
            }
            ItemStack stack = container.getItemStackFromEntry(entry, meta);
            if (!stack.isEmpty())
            {
                return new ItemStackElement(stack).size(previewSize);
            }

            Fluid fluid = container.getFluidForRegistry(entry.registry);
            if (fluid != null)
            {
                return new FluidElement(fluid).size(previewSize);
            }
            return null;
        }

        if (editingEntryType == EntryType.MOB && editingSet != null && editingSet.mobs != null
                && editingCurrencyIndex >= 0 && editingCurrencyIndex < editingSet.mobs.size())
        {
            BlockSetConfig.MobElementDefinition entry = editingSet.mobs.get(editingCurrencyIndex);
            if (entry == null || entry.registry == null || entry.registry.isEmpty())
            {
                return null;
            }

            Entity entity = resolveEntity(entry.registry, entry.nbtTags);
            if (entity != null)
            {
                return new CustomDrawCallbackElement((g, x, y, w, h, fr, mx, my, pt) ->
                {
                    if (!(entity instanceof LivingEntity))
                    {
                        return;
                    }
                    boolean drawn = false;
                    try
                    {
                        float[] units = ModelUtil.getModelUnits(entity);
                        if (units[0] > 0.0F && units[1] > 0.0F)
                        {
                            float finalScale = Math.min(previewSize / units[0], previewSize / units[1]);
                            int ox = x + w / 2 - Math.round(units[2] * finalScale);
                            int oy = y + h / 2 + Math.round(units[3] * finalScale);
                            ModelUtil.drawEntityOnScreenScaled(gfx, ox, oy, entity, finalScale);
                            drawn = true;
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                    if (!drawn)
                    {
                        Item egg = SpawnEggItem.byId(entity.getType());
                        if (egg != null)
                        {
                            int ds = Math.max(16, previewSize / 2);
                            gfx.renderFakeItem(new ItemStack(egg), x + (w - ds) / 2, y + (h - ds) / 2);
                        }
                    }
                }, previewSize, previewSize);
            }
            return null;
        }

        return null;
    }

    private void buildEditNbtView()
    {
        String entryName = container.getEditingEntryDisplayName();
        boolean atRoot = container.nbtEditorAtRoot();
        boolean atList = container.nbtEditorIsListContext();
        boolean atArray = container.nbtEditorIsArrayContext();
        boolean atContainer = atList || atArray;

        if (atRoot)
        {
            factory.title("gui.oneblockultima.config.nbt_title");
            if (!entryName.isEmpty())
            {
                factory.add(new LabelElement(entryName).centered());
            }
        }
        else
        {
            factory.title(I18n.get("gui.oneblockultima.config.nbt_subtitle", container.nbtEditorPathLabel()));
        }

        List<NbtTagEntry> tags = container.getNbtTags();
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (final NbtTagEntry tag : tags)
        {
            entries.add(new ScrollableListElement.ScrollableListEntry()
            {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY)
                {
                    boolean navigable = tag.isCompound() || tag.isList() || tag.isArray();
                    String typeLabel = getNbtTypeLabel(tag.getTypeId());
                    String preview = nbtValuePreview(tag.value);

                    int btnY = y + (height - NBT_ROW_BUTTON_HEIGHT) / 2;
                    int trashX = x + width - NBT_ROW_PADDING - NBT_ROW_TRASH_WIDTH;
                    boolean trashHov = mouseX >= trashX && mouseX <= trashX + NBT_ROW_TRASH_WIDTH
                            && mouseY >= btnY && mouseY <= btnY + NBT_ROW_BUTTON_HEIGHT;

                    String actionLabel = navigable
                            ? I18n.get("gui.oneblockultima.config.nbt_open")
                            : I18n.get("gui.oneblockultima.config.nbt_edit_value");
                    int actionW = fr.width(actionLabel) + NBT_ROW_BUTTON_TEXT_PADDING;
                    int actionX = trashX - NBT_ROW_PADDING - actionW;
                    boolean actionHov = mouseX >= actionX && mouseX <= actionX + actionW
                            && mouseY >= btnY && mouseY <= btnY + NBT_ROW_BUTTON_HEIGHT;

                    if (navigable && hovered && !trashHov && !actionHov)
                    {
                        gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);
                    }

                    gfx.drawString(fr, tag.key, x + NBT_ROW_PADDING, y + NBT_ROW_TEXT_TOP, navigable ? WHITE_COLOR_1 : WHITE_COLOR_2);
                    String detail = typeLabel + ": " + preview;
                    int maxDetailW = width - actionW - NBT_ROW_TRASH_WIDTH - NBT_ROW_TRUNCATION_SLACK;
                    if (fr.width(detail) > maxDetailW)
                    {
                        detail = fr.plainSubstrByWidth(detail, maxDetailW) + "...";
                    }
                    gfx.drawString(fr, detail, x + NBT_ROW_PADDING, y + NBT_ROW_TEXT_TOP + fr.lineHeight + NBT_ROW_TEXT_LINE_GAP, GRAY_COLOR_5);

                    gfx.fill(trashX, btnY, trashX + NBT_ROW_TRASH_WIDTH, btnY + NBT_ROW_BUTTON_HEIGHT, trashHov ? DARK_RED_COLOR_1 : DARK_RED_COLOR_2);
                    drawXIcon(trashX, btnY, trashHov ? REDDISH_COLOR : GRAY_COLOR_5);

                    gfx.fill(actionX, btnY, actionX + actionW, btnY + NBT_ROW_BUTTON_HEIGHT, actionHov ? BLUE_GRAY_COLOR : DARK_BLUE_GRAY_COLOR_1);
                    gfx.drawCenteredString(fr, actionLabel, actionX + actionW / 2, btnY + (NBT_ROW_BUTTON_HEIGHT - fr.lineHeight) / 2 + 1, WHITE_COLOR_1);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton)
                {
                    if (mouseButton != 0) return false;
                    int btnY = (entryHeight - NBT_ROW_BUTTON_HEIGHT) / 2;
                    int trashX = entryWidth - NBT_ROW_PADDING - NBT_ROW_TRASH_WIDTH;

                    if (mouseXOffset >= trashX && mouseXOffset <= trashX + NBT_ROW_TRASH_WIDTH
                            && mouseYOffset >= btnY && mouseYOffset <= btnY + NBT_ROW_BUTTON_HEIGHT)
                    {
                        if (tag.index >= 0) container.nbtEditorRemoveIndex(tag.index);
                        else container.nbtEditorRemove(tag.key);
                        init();
                        return true;
                    }

                    if (tag.isCompound() || tag.isList() || tag.isArray())
                    {
                        if (tag.index >= 0) container.nbtEditorPushIndex(tag.index);
                        else container.nbtEditorPush(tag.key);
                        init();
                        return true;
                    }

                    if (tag.index >= 0) container.nbtEditorStartEditIndex(tag.index);
                    else container.nbtEditorStartEdit(tag.key);
                    changeView(VIEW_NBT_ADD);
                    return true;
                }
            });
        }

        nbtList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .scrollOffset(nbtScrollOffset);
        nbtList.flexible(true);
        factory.add(nbtList);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6);
        btnRow.button(BUTTON_NBT_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_NBT_BACK));
        btnRow.button(BUTTON_NBT_ADD, I18n.get(atContainer
                ? "gui.oneblockultima.config.nbt_add_element"
                : "gui.oneblockultima.config.nbt_add_tag")).onPress(() -> actionPerformed(BUTTON_NBT_ADD));
    }

    private void buildNbtAddView()
    {
        factory.fitContent();
        factory.centerVertical();

        boolean atList = container.nbtEditorIsListContext();
        boolean atArray = container.nbtEditorIsArrayContext();
        boolean atContainer = atList || atArray;
        boolean editing = container.nbtEditorIsEditing();

        if (editing)
        {
            factory.title(I18n.get("gui.oneblockultima.config.nbt_edit_value"));
        }
        else if (atContainer)
        {
            factory.title(I18n.get("gui.oneblockultima.config.nbt_add_element"));
        }
        else
        {
            factory.title(I18n.get("gui.oneblockultima.config.nbt_add_tag"));
        }

        int formWidth = Math.max(160, Math.min(280, width / 2));

        boolean showKeyField = !atContainer;
        boolean focusKey = showKeyField && !editing;

        String keyLabel = I18n.get("gui.oneblockultima.config.nbt_key") + ":";
        String valueLabel = I18n.get("gui.oneblockultima.config.nbt_value") + ":";
        String typeLabel = I18n.get("gui.oneblockultima.config.nbt_type") + ":";
        String elementTypeLabel = I18n.get("gui.oneblockultima.config.nbt_element_type") + ":";
        String[] labels = new String[]{
                keyLabel,
                valueLabel,
                typeLabel,
                elementTypeLabel
        };
        int formLabelWidth = 0;
        for (String s : labels) {
            formLabelWidth = Math.max(formLabelWidth, Minecraft.getInstance().font.width(s));
        }

        if (showKeyField)
        {
            nbtKeyElement = new TextFieldElement(formWidth)
                    .text(container.nbtEditorGetKeyText())
                    .fitToText()
                    .focused(focusKey);
            RowElement keyRow = factory.row(Alignment.LEFT).gap(6).align(Alignment.LEFT);
            keyRow.add(new LabelElement(keyLabel).color(GRAY_COLOR_5).width(formLabelWidth));
            keyRow.add(nbtKeyElement);
        }
        else
        {
            nbtKeyElement = null;
        }

        int valueTypeId;
        boolean typeFixed;
        boolean showValueField;
        if (editing)
        {
            valueTypeId = container.nbtEditorGetEditingTypeId();
            typeFixed = true;
        }
        else if (atArray)
        {
            valueTypeId = container.nbtEditorGetArrayElementType();
            typeFixed = true;
        }
        else if (atList)
        {
            valueTypeId = container.getNbtEditorListElementType();
            typeFixed = container.nbtEditorListTypeIsFixed();
        }
        else
        {
            valueTypeId = container.getNbtEditorAddType();
            typeFixed = false;
        }
        showValueField = isScalarNbtType(valueTypeId);

        RowElement typeRow = factory.row(Alignment.LEFT).gap(6).align(Alignment.LEFT);
        if (typeFixed)
        {
            typeRow.add(new LabelElement(typeLabel).color(GRAY_COLOR_5).width(formLabelWidth));
            typeRow.add(new LabelElement(getNbtTypeLabel(valueTypeId)).color(WHITE_COLOR_1));
        }
        else if (atList)
        {
            typeRow.add(new LabelElement(elementTypeLabel).color(GRAY_COLOR_5).width(formLabelWidth));
            typeRow.button(BUTTON_NBT_CYCLE_ELEM_TYPE, getNbtTypeLabel(container.getNbtEditorListElementType())).onPress(() -> actionPerformed(BUTTON_NBT_CYCLE_ELEM_TYPE));
        }
        else
        {
            typeRow.add(new LabelElement(typeLabel).color(GRAY_COLOR_5).width(formLabelWidth));
            typeRow.button(BUTTON_NBT_CYCLE_TYPE, getNbtTypeLabel(container.getNbtEditorAddType())).onPress(() -> actionPerformed(BUTTON_NBT_CYCLE_TYPE));
        }

        if (showValueField)
        {
            nbtValueElement = new TextFieldElement(formWidth)
                    .text(container.nbtEditorGetValueText())
                    .fitToText()
                    .focused(!focusKey);
            RowElement valueRow = factory.row(Alignment.LEFT).gap(6).align(Alignment.LEFT);
            valueRow.add(new LabelElement(valueLabel).color(GRAY_COLOR_5).width(formLabelWidth));
            valueRow.add(nbtValueElement);
        }
        else
        {
            nbtValueElement = null;
            factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.nbt_hint_container"))
                    .color(GRAY_COLOR_5).centered());
        }

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6);
        btnRow.button(BUTTON_NBT_CANCEL, I18n.get("gui.oneblockultima.cancel")).onPress(() -> actionPerformed(BUTTON_NBT_CANCEL));
        btnRow.add(new SuccessButtonElement(BUTTON_NBT_DONE, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_NBT_DONE)));
    }

    private static boolean isScalarNbtType(int typeId)
    {
        return typeId != TAG_COMPOUND && typeId != TAG_LIST
                && typeId != TAG_BYTE_ARRAY && typeId != TAG_INT_ARRAY;
    }

    private void drawXIcon(int x, int y, int color)
    {
        int cx = x + GuiSetsConfig.NBT_ROW_TRASH_WIDTH / 2;
        int cy = y + GuiSetsConfig.NBT_ROW_BUTTON_HEIGHT / 2;
        for (int i = 0; i < 5; i++)
        {
            gfx.fill(cx - 3 + i, cy - 3 + i, cx - 2 + i, cy - 2 + i, color);
            gfx.fill(cx + 2 - i, cy - 3 + i, cx + 3 - i, cy - 2 + i, color);
        }
    }

    private void buildRequiredModsEditorView()
    {
        if (container.getEditingSet() == null) { changeView(VIEW_SET_DETAILS); return; }
        if (!container.isRequiredModsEditorInitialized()) container.initRequiredModsEditor();
        container.getSelectedRequiredModsForRemoval().clear();

        factory.title("gui.oneblockultima.config.required_mods");

        factory.button(BUTTON_REQUIRED_MODS_TOGGLE, container.getRequiredModsEditorTypeLabel()).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_TOGGLE));

        String summary = container.getCurrentRequiredModEntries().isEmpty()
                ? I18n.get("gui.oneblockultima.config.required_mods_empty")
                : I18n.get("gui.oneblockultima.config.required_mods_selected", container.getCurrentRequiredModEntries().size());
        factory.add(new LabelElement(summary).color(GRAY_COLOR_5));
        factory.add(new SeparatorElement());

        List<RequiredModEntry> currentMods = container.getCurrentRequiredModEntries();
        List<ScrollableListElement.ScrollableListEntry> entries = getListModsForRemoval(currentMods);

        requiredModsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .scrollOffset(modsScrollOffset);
        requiredModsList.flexible(true);
        requiredModsList.visible(true);
        factory.add(requiredModsList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_REQUIRED_MODS_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_BACK));
        btnRow.add(new DangerButtonElement(BUTTON_REQUIRED_MODS_DELETE, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_DELETE)));
        btnRow.button(BUTTON_REQUIRED_MODS_ADD, I18n.get("gui.oneblockultima.config.add")).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_ADD));
        btnRow.add(new SuccessButtonElement(BUTTON_REQUIRED_MODS_SAVE, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_SAVE)));
        factory.add(btnRow);
    }

    private void buildRequiredModsAddView()
    {
        container.getSelectedRequiredModsToAdd().clear();

        factory.title("gui.oneblockultima.config.required_mods_add_title");

        String summary = I18n.get("gui.oneblockultima.config.required_mods_add_hint");
        factory.add(new LabelElement(summary).color(GRAY_COLOR_5));

        List<RequiredModEntry> availableMods = container.getAvailableRequiredModEntries();
        List<ScrollableListElement.ScrollableListEntry> entries = getListMods(availableMods);

        addModsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .scrollOffset(addModsScrollOffset);
        addModsList.flexible(true);
        addModsList.visible(true);
        factory.add(addModsList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_REQUIRED_MODS_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_BACK));
        btnRow.button(BUTTON_REQUIRED_MODS_ADD, I18n.get("gui.oneblockultima.config.add")).onPress(() -> actionPerformed(BUTTON_REQUIRED_MODS_ADD));
        factory.add(btnRow);
    }

    private List<ScrollableListElement.ScrollableListEntry> getListMods(List<RequiredModEntry> availableMods) {
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (final RequiredModEntry mod : availableMods)
        {
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    boolean isSel = container.getSelectedRequiredModsToAdd().contains(mod.modId);
                    if (isSel) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);
                    else if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);
                    String label = mod.displayName.isEmpty() ? mod.modId : mod.displayName + " (" + mod.modId + ")";
                    gfx.drawString(fr, label, x + 4, y + 4, WHITE_COLOR_1, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.selectRequiredModToAdd(mod.modId);
                    return true;
                }
            });
        }
        return entries;
    }

    private List<ScrollableListElement.ScrollableListEntry> getListModsForRemoval(List<RequiredModEntry> mods)
    {
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (final RequiredModEntry mod : mods)
        {
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    boolean isSel = container.getSelectedRequiredModsForRemoval().contains(mod.modId);
                    if (isSel) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);
                    else if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);
                    String label = mod.displayName.isEmpty() ? mod.modId : mod.displayName + " (" + mod.modId + ")";
                    gfx.drawString(fr, label, x + 4, y + 4, WHITE_COLOR_1, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.selectRequiredModToRemove(mod.modId);
                    return true;
                }
            });
        }
        return entries;
    }

    private void buildUnlockConditionsView()
    {
        if (container.getEditingSet() == null) { changeView(VIEW_SET_DETAILS); return; }

        factory.title("gui.oneblockultima.unlock_conditions");

        String toggleLabel = "any".equalsIgnoreCase(container.getUnlockConditionsEditorMode())
                ? I18n.get("gui.oneblockultima.config.any")
                : I18n.get("gui.oneblockultima.config.all");
        factory.button(BUTTON_UNLOCK_CONDITIONS_TOGGLE, toggleLabel).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_TOGGLE));

        String type = container.getNewConditionTypeToAdd();
        String typeLabelText = I18n.get("gui.oneblockultima.config.unlock_conditions_type_" + type);

        RowElement addRow = new RowElement(Alignment.CENTER).gap(4);
        addRow.button(BUTTON_UNLOCK_CONDITIONS_CYCLE_TYPE, typeLabelText).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_CYCLE_TYPE));

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
            addRow.button(BUTTON_UNLOCK_CONDITIONS_CYCLE_SET, setLabel).enabled(hasSet).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_CYCLE_SET));
        }

        int fieldWidth = Math.max(40, width * 4 / 100);
        if ("set_level".equals(type))
        {
            addRow.add(new LabelElement(I18n.get("gui.oneblockultima.config.base_level") + ":"));
            unlockLevelElement = new TextFieldElement(fieldWidth).text("1");
            addRow.add(unlockLevelElement);
        }
        else
        {
            addRow.add(new LabelElement(I18n.get("gui.oneblockultima.config.unlock_conditions_count") + ":"));
            unlockCountElement = new TextFieldElement(fieldWidth).text("1");
            addRow.add(unlockCountElement);
        }
        factory.add(addRow);
        factory.add(new SeparatorElement());

        List<ScrollableListElement.ScrollableListEntry> entries = getListConditions();

        conditionsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .scrollOffset(conditionsScrollOffset);
        conditionsList.flexible(true);
        conditionsList.visible(true);
        factory.add(conditionsList);

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_UNLOCK_CONDITIONS_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_BACK));
        btnRow.add(new DangerButtonElement(BUTTON_UNLOCK_CONDITIONS_DELETE, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_DELETE)));
        btnRow.button(BUTTON_UNLOCK_CONDITIONS_ADD, I18n.get("gui.oneblockultima.config.add")).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_ADD));
        btnRow.add(new SuccessButtonElement(BUTTON_UNLOCK_CONDITIONS_SAVE, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_UNLOCK_CONDITIONS_SAVE)));
        factory.add(btnRow);
    }

    private List<ScrollableListElement.ScrollableListEntry> getListConditions() {
        List<BlockSetConfig.UnlockConditionDefinition> conditions = container.getUnlockConditionsEditorConditions();
        List<BlockSetConfig.BlockSetDefinition> availableSets = container.getAvailableSetsForConditions();
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++)
        {
            final int condIdx = i;
            final BlockSetConfig.UnlockConditionDefinition cond = conditions.get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    boolean isSel = container.getSelectedUnlockConditionIndex() == condIdx;
                    if (isSel) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);
                    else if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    String info = I18n.get("gui.oneblockultima.config.unlock_conditions_type_" + cond.type);
                    if (cond.setId != null)
                    {
                        String setName = ContainerSetsConfig.getLocalizedSetName(
                                availableSets.stream().filter(s -> s.id.equals(cond.setId)).findFirst().orElse(null));
                        info += " [" + setName + "]";
                    }
                    if (cond.level > 0) info += ": " + cond.level + " " + I18n.get("gui.oneblockultima.lv").toLowerCase();
                    else if (cond.count > 0) info += ": x" + cond.count;
                    gfx.drawString(fr, info, x + 4, y + 4, WHITE_COLOR_1, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.setSelectedUnlockConditionIndex(condIdx);
                    return true;
                }
            });
        }
        return entries;
    }

    private void buildCaseEditorView()
    {
        if (container.getEditingSet() == null) { changeView(VIEW_SET_DETAILS); return; }
        if (!container.isCaseEditorInitialized()) container.initCaseEditor();

        factory.title("gui.oneblockultima.config.case_editor");

        RowElement toggleRow = new RowElement(Alignment.LEFT).gap(6).widthPercent(100);
        toggleRow.add(new LabelElement(I18n.get("gui.oneblockultima.config.case") + ":").color(GRAY_COLOR_5));
        toggleRow.button(BUTTON_CASE_ENABLED_TOGGLE, container.isCaseEnabled()
                ? I18n.get("gui.oneblockultima.config.case_on")
                : I18n.get("gui.oneblockultima.config.case_off")).onPress(() -> actionPerformed(BUTTON_CASE_ENABLED_TOGGLE));
        factory.add(toggleRow);

        factory.add(new SeparatorElement());

        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        List<BlockSetConfig.CaseEntryDefinition> caseEntries = container.getCaseEntries();
        for (int i = 0; i < caseEntries.size(); i++)
        {
            final int idx = i;
            final BlockSetConfig.CaseEntryDefinition entry = caseEntries.get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    boolean isSelected = container.getSelectedCaseEntryIndex() == idx;
                    if (isSelected) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);
                    else if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    ItemStack stack = container.getItemStackFromCaseEntry(entry);
                    final int cellPadding = 2;
                    final int itemIconSize = 16;
                    if (!stack.isEmpty())
                    {
                                                                                                gfx.renderItem(stack, x + cellPadding, y + cellPadding);
                                                                                            }

                    String name = container.getLocalizedNameForCaseEntry(entry);
                    int btnSize = height - 2 * cellPadding;
                    int rightBound = x + width - btnSize - 2 * cellPadding;
                    int textX = x + cellPadding + itemIconSize + cellPadding;
                    int maxNameW = Math.max(10, rightBound - textX - 6);
                    String displayName = name;
                    if (fr.width(displayName) > maxNameW)
                        displayName = fr.plainSubstrByWidth(displayName, maxNameW - fr.width("...")) + "...";
                    gfx.drawString(fr, displayName, textX, y + 2, GRAY_COLOR_5);
                    String info = container.getCaseEntryInfoLine(entry);
                    int maxInfoW = Math.max(10, rightBound - textX);
                    if (fr.width(info) > maxInfoW)
                        info = fr.plainSubstrByWidth(info, maxInfoW - fr.width("...")) + "...";
                    gfx.drawString(fr, info, textX, y + 12, GRAY_COLOR_1);

                    int editX = x + width - btnSize - cellPadding;
                    gfx.fill(editX, y + cellPadding, editX + btnSize, y + cellPadding + btnSize, DARK_BLUE_GRAY_COLOR_1);
                    gfx.drawString(fr, "\u270E", editX + btnSize / 2 - 4, y + cellPadding + 3, WHITE_COLOR_1);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    int btnSize = entryHeight - 4;
                    int editX = entryWidth - btnSize - 2;
                    if (mouseXOffset >= editX && mouseXOffset <= editX + btnSize
                            && mouseYOffset >= 2 && mouseYOffset <= 2 + btnSize)
                    {
                        container.startEditingCaseEntry(idx);
                        changeView(VIEW_CASE_ENTRY_EDIT);
                        return true;
                    }
                    container.setSelectedCaseEntryIndex(idx);
                    return true;
                }
            });
        }

        caseEntriesList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(entries)
                .scrollOffset(caseScrollOffset);
        caseEntriesList.flexible(true);
        caseEntriesList.visible(true);
        factory.add(caseEntriesList);

        if (caseEntries.isEmpty())
        {
            factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.case_no_entries")).color(GRAY_COLOR_1).centered());
        }

        RowElement btnRow = new RowElement(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK)).onPress(() -> actionPerformed(BUTTON_BACK));
        btnRow.add(new DangerButtonElement(BUTTON_CASE_ENTRY_DELETE, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_CASE_ENTRY_DELETE)));
        btnRow.button(BUTTON_CASE_ADD_LOOT, I18n.get("gui.oneblockultima.config.case_add_loot")).onPress(() -> actionPerformed(BUTTON_CASE_ADD_LOOT));
        btnRow.button(BUTTON_CASE_ADD, I18n.get("gui.oneblockultima.config.add")).onPress(() -> actionPerformed(BUTTON_CASE_ADD));
        factory.add(btnRow);
    }

    private void buildCaseEntryAddView()
    {
        container.setCurrentSearchType(SearchType.BLOCKS);
        container.performSearch(pendingAddEntrySearchText);

        factory.title("gui.oneblockultima.config.case_add_title");

        entrySearchElement = new TextFieldElement(0).widthPercent(80).focused(true);
        if (!pendingAddEntrySearchText.isEmpty())
            entrySearchElement.text(pendingAddEntrySearchText);
        factory.add(entrySearchElement);

        String helpText = I18n.get("gui.oneblockultima.config.search.help");
        factory.add(new LabelElement(helpText).color(GRAY_COLOR_1));

        List<ScrollableListElement.ScrollableListEntry> searchEntries = new ArrayList<>();
        List<SearchResult> results = container.getSearchResults();
        for (final SearchResult result : results)
        {
            if (result.isFluid) continue;
            searchEntries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);
                    int iconSize = Math.min(16, height - 4);
                    if (!result.stack.isEmpty())
                    {
                                                                                                gfx.renderItem(result.stack, x + 2, y + 2);
                                                                                            }
                    String displayName = result.name != null && !result.name.isEmpty() ? result.name : result.registry;
                    int textX = x + iconSize + 6;
                    gfx.drawString(fr, displayName, textX, y + 2, WHITE_COLOR_1, true);
                    gfx.drawString(fr, result.registry, textX, y + 12, GRAY_COLOR_1, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    pendingAddEntrySearchText = entrySearchElement != null ? entrySearchElement.getText() : "";
                    container.stageCaseEntryAdd(result);
                    changeView(VIEW_CASE_ENTRY_EDIT);
                    return true;
                }
            });
        }

        searchResultsList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(searchEntries)
                .scrollOffset(searchScrollOffset);
        searchResultsList.flexible(true);
        searchResultsList.visible(true);
        factory.add(searchResultsList);

        if (searchEntries.isEmpty())
        {
            factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.search.no_results")).color(GRAY_COLOR_1).centered());
        }

        factory.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK)).onPress(() -> actionPerformed(BUTTON_BACK));
    }

    private void buildCaseEntryEditView()
    {
        BlockSetConfig.CaseEntryDefinition entry = container.getEditingCaseEntry();
        if (entry == null) { changeView(VIEW_CASE_EDITOR); return; }

        boolean isLootEntry = entry.item == null || entry.item.isEmpty();

        factory.title(isLootEntry
                ? I18n.get("gui.oneblockultima.config.case_loot_edit_title")
                : I18n.get("gui.oneblockultima.config.case_entry_edit_title"));

        if (!isLootEntry)
        {
            String entryName = container.getLocalizedNameForCaseEntry(entry);
            if (!entryName.isEmpty()) factory.add(new LabelElement(entryName).centered());
        }

        factory.gap(3).centerVertical().fitContent();

        int fieldWidth = Math.max(40, width * 5 / 100);
        String chanceLabel = I18n.get("gui.oneblockultima.config.case_chance") + ":";
        int labelWidth = Minecraft.getInstance().font.width(chanceLabel);
        String countLabel = I18n.get("gui.oneblockultima.config.case_count") + ":";
        if (Minecraft.getInstance().font.width(countLabel) > labelWidth) Minecraft.getInstance().font.width(countLabel);

        ColumnElement labelCol = new ColumnElement().align(Alignment.RIGHT).gap(4);
        ColumnElement fieldCol = new ColumnElement().gap(4);

        if (isLootEntry)
        {
            labelCol.add(new LabelElement(I18n.get("gui.oneblockultima.config.case_loot_table") + ":").color(GRAY_COLOR_5));
            String lootTableName = container.getCaseLootTableText().trim();
            String lootButtonLabel = lootTableName.isEmpty()
                    ? I18n.get("gui.oneblockultima.config.case_loot_none")
                    : lootTableName;
            fieldCol.add(new ButtonElement<>(BUTTON_CASE_LOOT_PICK, lootButtonLabel).onPress(() -> actionPerformed(BUTTON_CASE_LOOT_PICK)));

            labelCol.add(new LabelElement(chanceLabel).color(GRAY_COLOR_5));
            caseWeightElement = new TextFieldElement(fieldWidth).text(container.getCaseWeightText());
            fieldCol.add(caseWeightElement);
        }
        else
        {
            labelCol.add(new LabelElement(countLabel).color(GRAY_COLOR_5));
            caseCountElement = new TextFieldElement(fieldWidth).text(container.getCaseCountText());
            fieldCol.add(caseCountElement);

            labelCol.add(new LabelElement(chanceLabel).color(GRAY_COLOR_5));
            caseWeightElement = new TextFieldElement(fieldWidth).text(container.getCaseWeightText());
            fieldCol.add(caseWeightElement);
        }

        RowElement formRow = factory.row(Alignment.CENTER).gap(10).height(ROW_HEIGHT);
        formRow.add(labelCol);
        formRow.add(fieldCol);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6).height(ROW_HEIGHT);
        btnRow.button(BUTTON_CASE_ENTRY_CANCEL, I18n.get("gui.oneblockultima.cancel")).onPress(() -> actionPerformed(BUTTON_CASE_ENTRY_CANCEL));
        btnRow.add(new DangerButtonElement(BUTTON_CASE_ENTRY_DELETE, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_CASE_ENTRY_DELETE)));
        btnRow.add(new SuccessButtonElement(BUTTON_CASE_ENTRY_SAVE, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_CASE_ENTRY_SAVE)));
    }

    private void handleCaseEntrySave()
    {
        saveCurrentFormState();
        try
        {
            int count = caseCountElement != null && !caseCountElement.getText().trim().isEmpty()
                    ? Integer.parseInt(caseCountElement.getText().trim()) : 1;
            int chance = caseWeightElement != null && !caseWeightElement.getText().trim().isEmpty()
                    ? Integer.parseInt(caseWeightElement.getText().trim()) : 1;
            String lootTable = container.getCaseLootTableText();
            if (container.saveCaseEntry(count, chance, lootTable))
            {
                changeView(VIEW_CASE_EDITOR);
            }
            else init();
        }
        catch (NumberFormatException e) { init(); }
    }

    private void buildCaseLootPickView()
    {
        factory.title("gui.oneblockultima.config.case_loot_pick_title");

        List<ScrollableListElement.ScrollableListEntry> lootEntries = new ArrayList<>();
        List<ResourceLocation> tables = container.getAvailableLootTables();
        String current = container.getCaseLootTableText().trim();
        for (final ResourceLocation location : tables)
        {
            final String tableName = location.toString();
            final boolean isCurrent = tableName.equalsIgnoreCase(current);
            lootEntries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.Font fr, int mouseX, int mouseY) {
                    if (isCurrent) gfx.fill(x + 1, y, x + width - 1, y + height, DARK_BLUE_GRAY_COLOR_1);
                    else if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);
                    gfx.drawString(fr, tableName, x + 4, y + (float) (height - fr.lineHeight) / 2, isCurrent ? WHITE_COLOR_1 : GRAY_COLOR_5, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
                    container.setCaseLootTableText(tableName);
                    changeView(VIEW_CASE_ENTRY_EDIT);
                    return true;
                }
            });
        }

        caseLootList = new ScrollableListElement(ENTRY_HEIGHT)
                .entries(lootEntries)
                .scrollOffset(caseLootScrollOffset);
        caseLootList.flexible(true);
        caseLootList.visible(true);
        factory.add(caseLootList);

        if (lootEntries.isEmpty())
        {
            factory.add(new LabelElement(I18n.get("gui.oneblockultima.config.search.no_results")).color(GRAY_COLOR_1).centered());
        }

        factory.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK)).onPress(() -> actionPerformed(BUTTON_BACK));
    }
}

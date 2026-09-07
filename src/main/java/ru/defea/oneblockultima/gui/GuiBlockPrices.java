package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.gui.containers.ContainerBlockPrices;
import ru.defea.oneblockultima.gui.layout.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

public class GuiBlockPrices extends ModScreen
{
    private static final int VIEW_PRICES = 0;
    private static final int VIEW_ADD_BLOCK = 1;
    private static final int VIEW_EDIT_PRICE = 2;

    private static final int BUTTON_ADD = 0;
    private static final int BUTTON_SAVE = 1;
    private static final int BUTTON_BACK = 2;
    private static final int BUTTON_BALANCE_MODE = 3;
    private static final int BUTTON_SET_COST_MODE = 4;
    private static final int BUTTON_DELETE = 5;
    private static final int BUTTON_RESET = 6;

    private static final int ENTRY_HEIGHT = 24;

    private final Screen parent;
    private final ContainerBlockPrices container;
    private int currentView = VIEW_PRICES;
    private int scrollOffset = 0;
    private int searchScrollOffset = 0;

    private ViewFactory factory;
    private GuiGraphics gfx;
    private ViewSwitcherElement switcher;
    private ScrollableListElement priceList;
    private ScrollableListElement searchList;
    private TextFieldElement searchFieldElement;
    private LabelElement searchNoResultsLabel;
    private DoubleStepperElement priceStepper;
    private DoubleStepperElement setCostStepper;
    private StatusBarElement statusBar;
    private boolean statusBarActive = false;

    public GuiBlockPrices(Screen parent)
    {
        super(Component.literal(I18n.get("gui.oneblockultima.prices.title")));
        this.parent = parent;
        this.container = new ContainerBlockPrices();
    }

    @Override
    public void init()
    {
        this.renderables.clear();
        this.children().clear();
        buildView();
    }

    private void changeView(int view)
    {
        if (currentView != view)
        {
            currentView = view;
            init();
        }
    }

    private void buildView()
    {
        this.renderables.clear();
        this.children().clear();
        if (factory == null || factory.getScreenWidth() != width || factory.getScreenHeight() != height)
        {
            factory = new ViewFactory(width, height)
                .margin(8).padding(6)
                .gap(6)
                .align(Alignment.CENTER);
            switcher = new ViewSwitcherElement().widthPercent(100).flexible(true);
            factory.add(switcher);
        }

        ColumnElement view = new ColumnElement().gap(6).align(Alignment.CENTER);
        if (currentView == VIEW_PRICES)
        {
            container.reloadPriceEntries();
            buildPricesView(view);
        }
        else if (currentView == VIEW_ADD_BLOCK)
        {
            buildAddBlockView(view);
        }
        else if (currentView == VIEW_EDIT_PRICE)
        {
            buildEditPriceView(view);
        }

        switcher.replaceView(currentView, view);
        switcher.setView(currentView);
        switcher.fitContent(currentView == VIEW_EDIT_PRICE);
        if (currentView == VIEW_EDIT_PRICE)
        {
            factory.panel(0, 0);
        }
        else
        {
            factory.panel(TRANSPARENT_DARK_GRAY_COLOR_1, DARK_GRAY_COLOR_1);
        }
        factory.build(this, font);
        if (currentView == VIEW_ADD_BLOCK && searchFieldElement != null)
        {
            this.setFocused(searchFieldElement.getTextField());
        }
        statusBarActive = statusBar != null && statusBar.isActive();
    }

    private void buildPricesView(ColumnElement view)
    {
        view.title("gui.oneblockultima.prices.title");

        String balanceModeLabel = container.getCurrentBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK
                ? I18n.get("gui.oneblockultima.mod_settings.balance_mode.break_block")
                : I18n.get("gui.oneblockultima.mod_settings.balance_mode.sell_block");
        String balanceToggleText = I18n.get("gui.oneblockultima.mod_settings.balance_mode") + ": " + balanceModeLabel;
        RowElement balanceRow = view.row(Alignment.LEFT).gap(4).widthPercent(100);
        balanceRow.button(BUTTON_BALANCE_MODE, balanceToggleText).onPress(() -> actionPerformed(BUTTON_BALANCE_MODE));

        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (int i = 0; i < container.getFilteredEntries().size(); i++)
        {
            final int idx = i;
            final java.util.Map.Entry<String, Double> entry = container.getFilteredEntries().get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry()
            {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, Font fr, int mouseX, int mouseY)
                {
                    if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    net.minecraft.world.item.ItemStack stack = BlockPriceConfig.createItemStack(
                        ContainerBlockPrices.parseRegistryFromKey(entry.getKey()),
                        ContainerBlockPrices.parseMetaFromKey(entry.getKey()));
                    if (!stack.isEmpty())
                    {
                        gfx.renderFakeItem(stack, x + 4, y + 4);
                    }

                    String name = container.getBlockDisplayName(
                        ContainerBlockPrices.parseRegistryFromKey(entry.getKey()),
                        ContainerBlockPrices.parseMetaFromKey(entry.getKey()));
                    gfx.drawString(fr, Component.literal(name), x + 24, y + 2, WHITE_COLOR_1, true);
                    gfx.drawString(fr, Component.literal(entry.getKey()), x + 24, y + 12, GRAY_COLOR_1, true);

                    String priceStr = ContainerBlockPrices.formatPrice(entry.getValue());
                    String editText = I18n.get("gui.oneblockultima.config.edit");
                    String delText = I18n.get("gui.oneblockultima.config.remove");
                    int editW = fr.width(editText) + 8;
                    int delW = fr.width(delText) + 8;
                    int btnH = 14;
                    int btnGap = 3;
                    int usableRight = x + width - 10;

                    int delBtnX = usableRight - delW;
                    int delBtnY = y + (height - btnH) / 2;
                    boolean delHovered = mouseX >= delBtnX && mouseX <= delBtnX + delW && mouseY >= delBtnY && mouseY <= delBtnY + btnH;
                    gfx.fill(delBtnX, delBtnY, delBtnX + delW, delBtnY + btnH, delHovered ? DARK_RED_COLOR_3 : DARK_RED_COLOR_1);
                    gfx.drawCenteredString(fr, Component.literal(delText), delBtnX + delW / 2, delBtnY + 3, REDDISH_COLOR);

                    int editBtnX = delBtnX - btnGap - editW;
                    int editBtnY = y + (height - btnH) / 2;
                    boolean editHovered = mouseX >= editBtnX && mouseX <= editBtnX + editW && mouseY >= editBtnY && mouseY <= editBtnY + btnH;
                    gfx.fill(editBtnX, editBtnY, editBtnX + editW, editBtnY + btnH, editHovered ? GREEN : DARK_GREEN);
                    gfx.drawCenteredString(fr, Component.literal(editText), editBtnX + editW / 2, editBtnY + 3, SUCCESS_COLOR);

                    int priceW = fr.width(priceStr);
                    int priceX = editBtnX - btnGap - priceW;
                    int priceY = y + (height - 8) / 2;
                    gfx.drawString(fr, Component.literal(priceStr), priceX, priceY, GOLD_COLOR, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton)
                {
                    String editText = I18n.get("gui.oneblockultima.config.edit");
                    String delText = I18n.get("gui.oneblockultima.config.remove");
                    int editW = font.width(editText) + 8;
                    int delW = font.width(delText) + 8;
                    int btnH = 14;
                    int btnGap = 3;
                    int usableRight = entryWidth - 10;

                    int delBtnX = usableRight - delW;
                    int btnY = (entryHeight - btnH) / 2;
                    int editBtnX = delBtnX - btnGap - editW;

                    if (localX >= editBtnX && localX <= editBtnX + editW && localY >= btnY && localY <= btnY + btnH)
                    {
                        container.selectPriceEntry(idx);
                        changeView(VIEW_EDIT_PRICE);
                        return true;
                    }

                    if (localX >= delBtnX && localX <= delBtnX + delW && localY >= btnY && localY <= btnY + btnH)
                    {
                        container.deletePriceEntry(idx);
                        if (statusBar != null) statusBar.text(I18n.get("gui.oneblockultima.prices.price_removed"), 60);
                        buildView();
                        return true;
                    }
                    return false;
                }
            });
        }

        priceList = new ScrollableListElement(ENTRY_HEIGHT)
            .entries(entries)
            .scrollOffset(scrollOffset);
        priceList.visible(!entries.isEmpty());
        priceList.flexible(true);
        view.add(priceList);

        if (statusBar == null) statusBar = new StatusBarElement();
        statusBar.visible(statusBar.isActive());
        view.add(statusBar);
        String setCostModeLabel = container.isSetCostMultiplierMode()
                ? I18n.get("gui.oneblockultima.mod_settings.set_cost_increase.multiplier")
                : I18n.get("gui.oneblockultima.mod_settings.set_cost_increase.fixed");
        String setCostToggleText = I18n.get("gui.oneblockultima.mod_settings.set_cost_increase") + ": " + setCostModeLabel;
        view.button(BUTTON_SET_COST_MODE, setCostToggleText).onPress(() -> actionPerformed(BUTTON_SET_COST_MODE));

        String valueLabelKey = container.isSetCostMultiplierMode()
                ? "gui.oneblockultima.mod_settings.set_cost_increase.value.multiplier"
                : "gui.oneblockultima.mod_settings.set_cost_increase.value.fixed";
        RowElement setCostRow = view.row(Alignment.CENTER).gap(6);
        setCostRow.add(new LabelElement(I18n.get(valueLabelKey)).color(GRAY_COLOR_1));
        setCostStepper = new DoubleStepperElement()
            .value(container.getSetCostIncreaseValue())
            .min(container.isSetCostMultiplierMode() ? 1.0 : 0)
            .step(0.1)
            .fieldWidth(70);
        setCostRow.add(setCostStepper);

        RowElement btnRow = view.row(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK));
        btnRow.button(BUTTON_RESET, I18n.get("gui.oneblockultima.reset_default")).onPress(() -> actionPerformed(BUTTON_RESET));
        btnRow.button(BUTTON_ADD, I18n.get("gui.oneblockultima.config.add")).onPress(() -> actionPerformed(BUTTON_ADD));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.get("gui.oneblockultima.save")).onPress(() -> actionPerformed(BUTTON_SAVE)));
    }

    private void buildAddBlockView(ColumnElement view)
    {
        view.title("gui.oneblockultima.config.add");

        container.performSearch();

        searchFieldElement = new TextFieldElement(0)
            .text(container.getSearchQuery())
            .focused(true)
            .widthPercent(60);
        view.add(searchFieldElement);

        view.add(new LabelElement(I18n.get("gui.oneblockultima.config.search.help")).color(GRAY_COLOR_1).centered());

        List<ScrollableListElement.ScrollableListEntry> searchEntries = buildSearchEntries();

        searchList = new ScrollableListElement(20)
            .entries(searchEntries)
            .scrollOffset(searchScrollOffset);
        searchList.visible(!searchEntries.isEmpty());
        searchList.flexible(true);
        view.add(searchList);

        searchNoResultsLabel = new LabelElement(I18n.get("gui.oneblockultima.prices.search.no_results")).color(GRAY_COLOR_1).centered();
        searchNoResultsLabel.visible(searchEntries.isEmpty());
        view.add(searchNoResultsLabel);

        view.button(BUTTON_BACK, I18n.get("gui.oneblockultima.back")).onPress(() -> actionPerformed(BUTTON_BACK));
    }

    private List<ScrollableListElement.ScrollableListEntry> buildSearchEntries()
    {
        List<ScrollableListElement.ScrollableListEntry> searchEntries = new ArrayList<>();
        for (int i = 0; i < container.getSearchResults().size(); i++)
        {
            final int idx = i;
            final ContainerBlockPrices.SearchResult result = container.getSearchResults().get(i);
            searchEntries.add(new ScrollableListElement.ScrollableListEntry()
            {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, Font fr, int mouseX, int mouseY)
                {
                    if (hovered) gfx.fill(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    if (!result.stack.isEmpty())
                    {
                        gfx.renderFakeItem(result.stack, x + 2, y + 2);
                    }

                    String displayName = result.name != null && !result.name.isEmpty() ? result.name : result.registry;
                    gfx.drawString(fr, Component.literal(displayName), x + 20, y + 2, WHITE_COLOR_1, true);
                    gfx.drawString(fr, Component.literal(result.registry), x + 20, y + 12, GRAY_COLOR_1, true);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton)
                {
                    container.selectSearchResult(idx);
                    changeView(VIEW_EDIT_PRICE);
                    return true;
                }
            });
        }
        return searchEntries;
    }

    private void refreshSearchResults()
    {
        if (searchFieldElement == null) return;
        String newQuery = searchFieldElement.getText();
        if (newQuery.equals(container.getSearchQuery())) return;
        container.setSearchQuery(newQuery);
        searchScrollOffset = 0;
        container.performSearch();
        if (searchList != null)
        {
            List<ScrollableListElement.ScrollableListEntry> searchEntries = buildSearchEntries();
            searchList.entries(searchEntries);
            searchList.scrollOffset(0);
            searchList.visible(!searchEntries.isEmpty());
        }
        if (searchNoResultsLabel != null)
        {
            searchNoResultsLabel.visible(container.getSearchResults().isEmpty());
        }
    }

    private void buildEditPriceView(ColumnElement view)
    {
        view.centerVertical();
        view.title("gui.oneblockultima.config.edit_title");

        net.minecraft.world.item.ItemStack stack = BlockPriceConfig.createItemStack(container.getEditingRegistry(), container.getEditingMeta());
        if (!stack.isEmpty())
        {
            RowElement infoRow = view.row(Alignment.CENTER).gap(6);
            infoRow.add(new ItemStackElement(stack).size(24));

            ColumnElement textCol = new ColumnElement().gap(0).align(Alignment.LEFT);
            String displayName = container.getEditingName() != null && !container.getEditingName().isEmpty()
                    ? container.getEditingName() : container.getEditingRegistry();
            textCol.add(new LabelElement(displayName).color(WHITE_COLOR_1));
            textCol.add(new LabelElement(container.getEditingRegistry()).color(GRAY_COLOR_1));
            infoRow.add(textCol);
        }
        else
        {
            String displayName = container.getEditingName() != null && !container.getEditingName().isEmpty()
                    ? container.getEditingName() : container.getEditingRegistry();
            view.add(new LabelElement(displayName).color(WHITE_COLOR_1).centered());
            view.add(new LabelElement(container.getEditingRegistry()).color(GRAY_COLOR_1).centered());
        }


        RowElement priceRow = view.row(Alignment.CENTER).gap(8);
        priceRow.add(new LabelElement(I18n.get("gui.oneblockultima.prices.price")).color(GRAY_COLOR_1));
        priceStepper = new DoubleStepperElement()
            .value(container.getEditingPrice())
            .min(0)
            .step(0.1)
            .fieldWidth(70)
            .focused(true);
        priceRow.add(priceStepper);

        RowElement btnRow = view.row(Alignment.CENTER).gap(8);
        btnRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.cancel")).onPress(() -> actionPerformed(BUTTON_BACK));
        btnRow.add(new DangerButtonElement(BUTTON_DELETE, I18n.get("gui.oneblockultima.config.remove")).onPress(() -> actionPerformed(BUTTON_DELETE)));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.get("gui.oneblockultima.done")).onPress(() -> actionPerformed(BUTTON_SAVE)));
    }

    private void actionPerformed(int id)
    {
        if (id == BUTTON_BACK)
        {
            if (currentView == VIEW_PRICES)
            {
                minecraft.setScreen(parent);
            }
            else
            {
                changeView(VIEW_PRICES);
            }
            return;
        }

        if (id == BUTTON_ADD)
        {
            container.startAddBlock();
            searchScrollOffset = 0;
            changeView(VIEW_ADD_BLOCK);
            return;
        }

        if (id == BUTTON_RESET && currentView == VIEW_PRICES)
        {
            container.resetToDefault();
            if (statusBar != null) statusBar.text(I18n.get("gui.oneblockultima.status.reset_success"), 60);
            buildView();
            return;
        }

        if (id == BUTTON_BALANCE_MODE && currentView == VIEW_PRICES)
        {
            container.toggleBalanceMode();
            init();
            return;
        }

        if (id == BUTTON_SET_COST_MODE && currentView == VIEW_PRICES)
        {
            if (setCostStepper != null)
            {
                setCostStepper.commit();
                container.setSetCostIncreaseValue(setCostStepper.getValue());
            }
            container.toggleSetCostMode();
            init();
            return;
        }

        if (id == BUTTON_SAVE && currentView == VIEW_EDIT_PRICE)
        {
            if (priceStepper != null)
            {
                priceStepper.commit();
                container.savePrice(priceStepper.getValue());
            }
            if (statusBar != null) statusBar.text(I18n.get("gui.oneblockultima.prices.price_saved"), 60);
            changeView(VIEW_PRICES);
            return;
        }

        if (id == BUTTON_DELETE && currentView == VIEW_EDIT_PRICE)
        {
            container.deleteEditingPrice();
            if (statusBar != null) statusBar.text(I18n.get("gui.oneblockultima.prices.price_removed"), 60);
            changeView(VIEW_PRICES);
            return;
        }

        if (id == BUTTON_SAVE && currentView == VIEW_PRICES)
        {
            if (setCostStepper != null)
            {
                setCostStepper.commit();
                container.setSetCostIncreaseValue(setCostStepper.getValue());
            }
            container.flushToConfig();
            if (statusBar != null) statusBar.text(I18n.get("gui.oneblockultima.prices.table_saved"), 60);
            buildView();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        boolean handled = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!handled && factory != null) handled = factory.mouseClicked((int) mouseX, (int) mouseY, mouseButton);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        if (!handled && factory != null) handled = factory.mouseReleased((int) mouseX, (int) mouseY, button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (!handled && factory != null) handled = factory.mouseClickMove((int) mouseX, (int) mouseY, button, 0L);
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            if (currentView == VIEW_PRICES)
            {
                minecraft.setScreen(parent);
            }
            else
            {
                changeView(VIEW_PRICES);
            }
            return true;
        }

        boolean handled = factory != null && factory.keyTyped((char) keyCode, keyCode);
        if (!handled) handled = super.keyPressed(keyCode, scanCode, modifiers);

        if (currentView == VIEW_ADD_BLOCK && searchFieldElement != null && searchFieldElement.isFocused())
        {
            refreshSearchResults();
        }

        return true;
    }

    @Override
    public boolean charTyped(char typedChar, int modifiers)
    {
        boolean handled = super.charTyped(typedChar, modifiers);
        if (currentView == VIEW_ADD_BLOCK && searchFieldElement != null && searchFieldElement.isFocused())
        {
            refreshSearchResults();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        boolean handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!handled && factory != null) handled = factory.handleMouseInput((int) scrollY);
        return handled;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (factory != null) factory.updateScreen();

        boolean statusActive = statusBar != null && statusBar.isActive();
        if (statusActive != statusBarActive)
        {
            statusBarActive = statusActive;
            if (statusBar != null) statusBar.visible(statusActive);
            if (factory != null)
            {
                factory.build(this, font);
            }
        }

        if (factory != null && (factory.getScreenWidth() != width || factory.getScreenHeight() != height))
        {
            buildView();
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks)
    {
        this.gfx = gg;
        this.drawModBackground(gg);
        if (factory != null) factory.draw(gg, font, mouseX, mouseY, partialTicks);
        super.render(gg, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose()
    {
        super.onClose();
    }

    private int getMaxVisibleEntries()
    {
        return Math.max(1, (height - 100) / ENTRY_HEIGHT);
    }

    private int getMaxVisibleSearchResults()
    {
        return Math.max(1, (height - 110) / 20);
    }
}

package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.gui.containers.ContainerBlockPrices;
import ru.defea.oneblockultima.gui.layout.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

public class GuiBlockPrices extends GuiScreen
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

    private static final int ENTRY_HEIGHT = 24;

    private final GuiScreen parent;
    private final ContainerBlockPrices container;
    private int currentView = VIEW_PRICES;
    private int scrollOffset = 0;
    private int searchScrollOffset = 0;

    private ViewFactory factory;
    private ViewSwitcherElement switcher;
    private ScrollableListElement priceList;
    private ScrollableListElement searchList;
    private TextFieldElement searchFieldElement;
    private DoubleStepperElement priceStepper;
    private DoubleStepperElement setCostStepper;
    private StatusBarElement statusBar;
    private boolean statusBarActive = false;
    private boolean inClickDispatch = false;
    private boolean pendingReinit = false;

    public GuiBlockPrices(GuiScreen parent)
    {
        this.parent = parent;
        this.container = new ContainerBlockPrices();
    }

    @Override
    public void initGui()
    {
        if (inClickDispatch)
        {
            pendingReinit = true;
            return;
        }
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buildView();
    }

    private void changeView(int view)
    {
        if (currentView != view)
        {
            currentView = view;
            initGui();
        }
    }

    private void buildView()
    {
        buttonList.clear();
        if (priceList != null) scrollOffset = priceList.getScrollOffset();
        if (searchList != null) searchScrollOffset = searchList.getScrollOffset();

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
        if (currentView == VIEW_EDIT_PRICE) {
            factory.panel(0, 0);
        } else {
            factory.panel(TRANSPARENT_DARK_GRAY_COLOR_1, DARK_GRAY_COLOR_1);
        }
        factory.build(buttonList, fontRenderer);
        statusBarActive = statusBar != null && statusBar.isActive();
    }

    private void buildPricesView(ColumnElement view)
    {
        view.title("gui.oneblockultima.prices.title");

        String balanceModeLabel = container.getCurrentBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK
                ? I18n.format("gui.oneblockultima.mod_settings.balance_mode.break_block")
                : I18n.format("gui.oneblockultima.mod_settings.balance_mode.sell_block");
        String balanceToggleText = I18n.format("gui.oneblockultima.mod_settings.balance_mode") + ": " + balanceModeLabel;
        RowElement balanceRow = view.row(Alignment.LEFT).gap(4).widthPercent(100);
        balanceRow.button(BUTTON_BALANCE_MODE, balanceToggleText);

        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (int i = 0; i < container.getFilteredEntries().size(); i++)
        {
            final int idx = i;
            final java.util.Map.Entry<String, Double> entry = container.getFilteredEntries().get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    net.minecraft.item.ItemStack stack = BlockPriceConfig.createItemStack(
                        ContainerBlockPrices.parseRegistryFromKey(entry.getKey()),
                        ContainerBlockPrices.parseMetaFromKey(entry.getKey()));
                    if (!stack.isEmpty()) {
                        GlStateManager.enableDepth();
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableRescaleNormal();
                        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, x + 4, y + 4);
                        RenderHelper.disableStandardItemLighting();
                        GlStateManager.disableRescaleNormal();
                        GlStateManager.disableDepth();
                    }

                    String name = container.getBlockDisplayName(
                        ContainerBlockPrices.parseRegistryFromKey(entry.getKey()),
                        ContainerBlockPrices.parseMetaFromKey(entry.getKey()));
                    fr.drawStringWithShadow(name, x + 24, y + 2, WHITE_COLOR_1);
                    fr.drawStringWithShadow(entry.getKey(), x + 24, y + 12, GRAY_COLOR_1);

                    String priceStr = ContainerBlockPrices.formatPrice(entry.getValue());
                    String editText = I18n.format("gui.oneblockultima.config.edit");
                    String delText = I18n.format("gui.oneblockultima.config.remove");
                    int editW = fr.getStringWidth(editText) + 8;
                    int delW = fr.getStringWidth(delText) + 8;
                    int btnH = 14;
                    int btnGap = 3;
                    int usableRight = x + width - 10;

                    int delBtnX = usableRight - delW;
                    int delBtnY = y + (height - btnH) / 2;
                    boolean delHovered = mouseX >= delBtnX && mouseX <= delBtnX + delW && mouseY >= delBtnY && mouseY <= delBtnY + btnH;
                    Gui.drawRect(delBtnX, delBtnY, delBtnX + delW, delBtnY + btnH, delHovered ? DARK_RED_COLOR_3 : DARK_RED_COLOR_1);
                    drawCenteredString(fr, delText, delBtnX + delW / 2, delBtnY + 3, REDDISH_COLOR);

                    int editBtnX = delBtnX - btnGap - editW;
                    int editBtnY = y + (height - btnH) / 2;
                    boolean editHovered = mouseX >= editBtnX && mouseX <= editBtnX + editW && mouseY >= editBtnY && mouseY <= editBtnY + btnH;
                    Gui.drawRect(editBtnX, editBtnY, editBtnX + editW, editBtnY + btnH, editHovered ? GREEN : DARK_GREEN);
                    drawCenteredString(fr, editText, editBtnX + editW / 2, editBtnY + 3, SUCCESS_COLOR);

                    int priceW = fr.getStringWidth(priceStr);
                    int priceX = editBtnX - btnGap - priceW;
                    int priceY = y + (height - 8) / 2;
                    fr.drawStringWithShadow(priceStr, priceX, priceY, GOLD_COLOR);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                    String editText = I18n.format("gui.oneblockultima.config.edit");
                    String delText = I18n.format("gui.oneblockultima.config.remove");
                    int editW = fontRenderer.getStringWidth(editText) + 8;
                    int delW = fontRenderer.getStringWidth(delText) + 8;
                    int btnH = 14;
                    int btnGap = 3;
                    int usableRight = entryWidth - 10;

                    int delBtnX = usableRight - delW;
                    int btnY = (entryHeight - btnH) / 2;
                    int editBtnX = delBtnX - btnGap - editW;

                    if (localX >= editBtnX && localX <= editBtnX + editW && localY >= btnY && localY <= btnY + btnH) {
                        container.selectPriceEntry(idx);
                        changeView(VIEW_EDIT_PRICE);
                        return true;
                    }

                    if (localX >= delBtnX && localX <= delBtnX + delW && localY >= btnY && localY <= btnY + btnH) {
                        container.deletePriceEntry(idx);
                        if (statusBar != null) statusBar.text(I18n.format("gui.oneblockultima.prices.price_removed"), 60);
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
                ? I18n.format("gui.oneblockultima.mod_settings.set_cost_increase.multiplier")
                : I18n.format("gui.oneblockultima.mod_settings.set_cost_increase.fixed");
        String setCostToggleText = I18n.format("gui.oneblockultima.mod_settings.set_cost_increase") + ": " + setCostModeLabel;
        view.button(BUTTON_SET_COST_MODE, setCostToggleText);

        String valueLabelKey = container.isSetCostMultiplierMode()
                ? "gui.oneblockultima.mod_settings.set_cost_increase.value.multiplier"
                : "gui.oneblockultima.mod_settings.set_cost_increase.value.fixed";
        RowElement setCostRow = view.row(Alignment.CENTER).gap(6);
        setCostRow.add(new LabelElement(I18n.format(valueLabelKey)).color(GRAY_COLOR_1));
        setCostStepper = new DoubleStepperElement()
            .value(container.getSetCostIncreaseValue())
            .min(container.isSetCostMultiplierMode() ? 1.0 : 0)
            .step(1)
            .fieldWidth(70);
        setCostRow.add(setCostStepper);

        RowElement btnRow = view.row(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.back"));
        btnRow.button(BUTTON_ADD, I18n.format("gui.oneblockultima.config.add"));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.format("gui.oneblockultima.save")));
    }

    private void buildAddBlockView(ColumnElement view)
    {
        view.title("gui.oneblockultima.config.add");

        searchFieldElement = new TextFieldElement(0)
            .text(container.getSearchQuery())
            .focused(true)
            .widthPercent(60);
        view.add(searchFieldElement);

        view.add(new LabelElement(I18n.format("gui.oneblockultima.config.search.help")).color(GRAY_COLOR_1).centered());

        List<ScrollableListElement.ScrollableListEntry> searchEntries = new ArrayList<>();
        for (int i = 0; i < container.getSearchResults().size(); i++)
        {
            final int idx = i;
            final ContainerBlockPrices.SearchResult result = container.getSearchResults().get(i);
            searchEntries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, TRANSPARENT_WHITE);

                    if (!result.stack.isEmpty()) {
                        GlStateManager.enableDepth();
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableRescaleNormal();
                        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(result.stack, x + 2, y + 2);
                        RenderHelper.disableStandardItemLighting();
                        GlStateManager.disableRescaleNormal();
                        GlStateManager.disableDepth();
                    }

                    String displayName = result.name != null && !result.name.isEmpty() ? result.name : result.registry;
                    fr.drawStringWithShadow(displayName, x + 20, y + 2, WHITE_COLOR_1);
                    fr.drawStringWithShadow(result.registry, x + 20, y + 12, GRAY_COLOR_1);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                    container.selectSearchResult(idx);
                    changeView(VIEW_EDIT_PRICE);
                    return true;
                }
            });
        }

        searchList = new ScrollableListElement(20)
            .entries(searchEntries)
            .scrollOffset(searchScrollOffset);
        searchList.visible(!searchEntries.isEmpty());
        searchList.flexible(true);
        view.add(searchList);

        if (searchEntries.isEmpty())
        {
            view.add(new LabelElement(I18n.format("gui.oneblockultima.prices.search.no_results")).color(GRAY_COLOR_1).centered());
        }

        view.button(BUTTON_BACK, I18n.format("gui.oneblockultima.back"));
    }

    private void buildEditPriceView(ColumnElement view)
    {
        view.centerVertical();
        view.title("gui.oneblockultima.config.edit_title");

        net.minecraft.item.ItemStack stack = BlockPriceConfig.createItemStack(container.getEditingRegistry(), container.getEditingMeta());
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
        priceRow.add(new LabelElement(I18n.format("gui.oneblockultima.prices.price")).color(GRAY_COLOR_1));
        priceStepper = new DoubleStepperElement()
            .value(container.getEditingPrice())
            .min(0)
            .step(1)
            .fieldWidth(70)
            .focused(true);
        priceRow.add(priceStepper);

        RowElement btnRow = view.row(Alignment.CENTER).gap(8);
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));
        btnRow.add(new DangerButtonElement(BUTTON_DELETE, I18n.format("gui.oneblockultima.config.remove")));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.format("gui.oneblockultima.done")));
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button)
    {
        factory.actionPerformed(button);

        if (button.id == BUTTON_BACK)
        {
            if (currentView == VIEW_PRICES)
            {
                Keyboard.enableRepeatEvents(false);
                mc.displayGuiScreen(parent);
            }
            else
            {
                changeView(VIEW_PRICES);
            }
            return;
        }

        if (button.id == BUTTON_ADD)
        {
            container.startAddBlock();
            searchScrollOffset = 0;
            changeView(VIEW_ADD_BLOCK);
            return;
        }

        if (button.id == BUTTON_BALANCE_MODE && currentView == VIEW_PRICES)
        {
            container.toggleBalanceMode();
            initGui();
            return;
        }

        if (button.id == BUTTON_SET_COST_MODE && currentView == VIEW_PRICES)
        {
            if (setCostStepper != null)
            {
                setCostStepper.commit();
                container.setSetCostIncreaseValue(setCostStepper.getValue());
            }
            container.toggleSetCostMode();
            initGui();
            return;
        }

        if (button.id == BUTTON_SAVE && currentView == VIEW_EDIT_PRICE)
        {
            if (priceStepper != null)
            {
                priceStepper.commit();
                container.savePrice(priceStepper.getValue());
            }
            if (statusBar != null) statusBar.text(I18n.format("gui.oneblockultima.prices.price_saved"), 60);
            changeView(VIEW_PRICES);
            return;
        }

        if (button.id == BUTTON_DELETE && currentView == VIEW_EDIT_PRICE)
        {
            container.deleteEditingPrice();
            if (statusBar != null) statusBar.text(I18n.format("gui.oneblockultima.prices.price_removed"), 60);
            changeView(VIEW_PRICES);
            return;
        }

        if (button.id == BUTTON_SAVE && currentView == VIEW_PRICES)
        {
            if (setCostStepper != null)
            {
                setCostStepper.commit();
                container.setSetCostIncreaseValue(setCostStepper.getValue());
            }
            container.flushToConfig();
            if (statusBar != null) statusBar.text(I18n.format("gui.oneblockultima.prices.table_saved"), 60);
            buildView();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        inClickDispatch = true;
        try
        {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        finally
        {
            inClickDispatch = false;
        }
        if (pendingReinit)
        {
            pendingReinit = false;
            initGui();
        }
        factory.mouseClicked(mouseX, mouseY, mouseButton);

        if (currentView == VIEW_ADD_BLOCK && searchFieldElement != null)
        {
            container.setSearchQuery(searchFieldElement.getText());
            searchScrollOffset = 0;
            container.performSearch();
            buildView();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (!factory.keyTyped(typedChar, keyCode))
        {
            super.keyTyped(typedChar, keyCode);
        }

        if (currentView == VIEW_ADD_BLOCK && searchFieldElement != null)
        {
            String newQuery = searchFieldElement.getText();
            if (!newQuery.equals(container.getSearchQuery()))
            {
                container.setSearchQuery(newQuery);
                searchScrollOffset = 0;
                container.performSearch();
                buildView();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0)
        {
            int delta = dWheel > 0 ? -1 : 1;
            if (currentView == VIEW_PRICES && priceList != null)
            {
                scrollOffset = priceList.getScrollOffset();
                scrollOffset += delta;
                scrollOffset = Math.max(0, Math.min(Math.max(0, container.getFilteredEntries().size() - getMaxVisibleEntries()), scrollOffset));
                priceList.scrollOffset(scrollOffset);
            }
            else if (currentView == VIEW_ADD_BLOCK && searchList != null)
            {
                searchScrollOffset = searchList.getScrollOffset();
                searchScrollOffset += delta;
                searchScrollOffset = Math.max(0, Math.min(Math.max(0, container.getSearchResults().size() - getMaxVisibleSearchResults()), searchScrollOffset));
                searchList.scrollOffset(searchScrollOffset);
            }
        }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        factory.updateScreen();

        boolean statusActive = statusBar != null && statusBar.isActive();
        if (statusActive != statusBarActive)
        {
            buildView();
            return;
        }

        if (factory != null && (factory.getScreenWidth() != width || factory.getScreenHeight() != height))
        {
            buildView();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        factory.draw(fontRenderer, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
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

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiBlockPrices extends GuiScreen
{
    private static final int VIEW_PRICES = 0;
    private static final int VIEW_ADD_BLOCK = 1;
    private static final int VIEW_EDIT_PRICE = 2;

    private static final int BUTTON_ADD = 0;
    private static final int BUTTON_SAVE = 1;
    private static final int BUTTON_BACK = 2;
    private static final int BUTTON_BALANCE_MODE = 3;

    private final GuiScreen parent;
    private final ContainerBlockPrices container;
    private int currentView = VIEW_PRICES;
    private int scrollOffset = 0;
    private final int entryHeight = 24;
    private String statusMessage = "";
    private int statusTimer = 0;
    private int searchScrollOffset = 0;

    private ViewFactory factory;
    private ScrollableListElement priceList;
    private ScrollableListElement searchList;
    private TextFieldElement searchFieldElement;
    private TextFieldElement priceFieldElement;
    private StatusBarElement statusBar;

    public GuiBlockPrices(GuiScreen parent)
    {
        this.parent = parent;
        this.container = new ContainerBlockPrices();
    }

    @Override
    public void initGui()
    {
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
        if (priceList != null) scrollOffset = priceList.getScrollOffset();
        if (searchList != null) searchScrollOffset = searchList.getScrollOffset();
        buttonList.clear();

        if (currentView == VIEW_PRICES)
        {
            container.reloadPriceEntries();
            buildPricesView();
        }
        else if (currentView == VIEW_ADD_BLOCK)
        {
            buildAddBlockView();
        }
        else if (currentView == VIEW_EDIT_PRICE)
        {
            buildEditPriceView();
        }
    }

    private void buildPricesView()
    {
        factory = new ViewFactory(width, height)
            .margin(8).padding(2)
            .gap(6)
            .align(Alignment.CENTER)
            .panel(0xCC22272E, 0xFF3A3F44);

        factory.title("gui.oneblockultima.prices.title");

        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (int i = 0; i < container.getFilteredEntries().size(); i++)
        {
            final int idx = i;
            final java.util.Map.Entry<String, Double> entry = container.getFilteredEntries().get(i);
            entries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);

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
                    fr.drawStringWithShadow(name, x + 24, y + 2, 0xFFFFFF);
                    fr.drawStringWithShadow(entry.getKey(), x + 24, y + 12, 0x808080);

                    String priceStr = ContainerBlockPrices.formatPrice(entry.getValue());
                    String editText = I18n.format("gui.oneblockultima.prices.edit");
                    String delText = I18n.format("gui.oneblockultima.prices.delete");
                    int editW = fr.getStringWidth(editText) + 8;
                    int delW = fr.getStringWidth(delText) + 8;
                    int btnH = 14;
                    int btnGap = 3;
                    int usableRight = x + width - 10;

                    int delBtnX = usableRight - delW;
                    int delBtnY = y + (height - btnH) / 2;
                    boolean delHovered = mouseX >= delBtnX && mouseX <= delBtnX + delW && mouseY >= delBtnY && mouseY <= delBtnY + btnH;
                    Gui.drawRect(delBtnX, delBtnY, delBtnX + delW, delBtnY + btnH, delHovered ? 0xFF8B4A4A : 0xFF6B3A3A);
                    drawCenteredString(fr, delText, delBtnX + delW / 2, delBtnY + 3, 0xFF5555);

                    int editBtnX = delBtnX - btnGap - editW;
                    int editBtnY = y + (height - btnH) / 2;
                    boolean editHovered = mouseX >= editBtnX && mouseX <= editBtnX + editW && mouseY >= editBtnY && mouseY <= editBtnY + btnH;
                    Gui.drawRect(editBtnX, editBtnY, editBtnX + editW, editBtnY + btnH, editHovered ? 0xFF4A8B4A : 0xFF3A6B3A);
                    drawCenteredString(fr, editText, editBtnX + editW / 2, editBtnY + 3, 0x55FF55);

                    int priceW = fr.getStringWidth(priceStr);
                    int priceX = editBtnX - btnGap - priceW;
                    int priceY = y + (height - 8) / 2;
                    fr.drawStringWithShadow(priceStr, priceX, priceY, 0xFFD700);
                }

                @Override
                public boolean mouseClicked(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
                    String editText = I18n.format("gui.oneblockultima.prices.edit");
                    String delText = I18n.format("gui.oneblockultima.prices.delete");
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
                        statusMessage = I18n.format("gui.oneblockultima.prices.price_removed");
                        statusTimer = 60;
                        buildView();
                        return true;
                    }
                    return false;
                }
            });
        }

        priceList = new ScrollableListElement(entryHeight)
            .entries(entries)
            .scrollOffset(scrollOffset);
        priceList.visible(!entries.isEmpty());
        priceList.flexible(true);
        factory.add(priceList);

        statusBar = new StatusBarElement();
        factory.add(statusBar);

        String balanceModeLabel = container.getCurrentBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK
                ? I18n.format("gui.oneblockultima.mod_settings.balance_mode.break_block")
                : I18n.format("gui.oneblockultima.mod_settings.balance_mode.sell_block");
        String balanceToggleText = I18n.format("gui.oneblockultima.mod_settings.balance_mode") + ": " + balanceModeLabel;
        factory.button(BUTTON_BALANCE_MODE, balanceToggleText);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_ADD, I18n.format("gui.oneblockultima.prices.add"));
        btnRow.button(BUTTON_SAVE, I18n.format("gui.oneblockultima.save"));
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));

        factory.build(buttonList, fontRenderer);
    }

    private void buildAddBlockView()
    {
        factory = new ViewFactory(width, height)
            .margin(8).padding(2)
            .gap(6)
            .align(Alignment.CENTER)
            .panel(0xCC22272E, 0xFF3A3F44);

        factory.title("gui.oneblockultima.prices.add");

        searchFieldElement = new TextFieldElement(0)
            .text(container.getSearchQuery())
            .focused(true)
            .widthPercent(60);
        factory.add(searchFieldElement);

        factory.add(new LabelElement(I18n.format("gui.oneblockultima.prices.search.help")).color(0x808080).centered(true));

        List<ScrollableListElement.ScrollableListEntry> searchEntries = new ArrayList<>();
        for (int i = 0; i < container.getSearchResults().size(); i++)
        {
            final int idx = i;
            final ContainerBlockPrices.SearchResult result = container.getSearchResults().get(i);
            searchEntries.add(new ScrollableListElement.ScrollableListEntry() {
                @Override
                public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, net.minecraft.client.gui.FontRenderer fr, int mouseX, int mouseY) {
                    if (hovered) Gui.drawRect(x + 1, y, x + width - 1, y + height, 0x33FFFFFF);

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
                    fr.drawStringWithShadow(displayName, x + 20, y + 2, 0xFFFFFF);
                    fr.drawStringWithShadow(result.registry, x + 20, y + 12, 0x808080);
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
        factory.add(searchList);

        if (searchEntries.isEmpty())
        {
            factory.add(new LabelElement(I18n.format("gui.oneblockultima.prices.search.no_results")).color(0x808080).centered(true));
        }

        factory.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));

        factory.build(buttonList, fontRenderer);
    }

    private void buildEditPriceView()
    {
        factory = new ViewFactory(width, height)
            .margin(8).padding(2)
            .gap(6)
            .align(Alignment.CENTER)
            .panel(0xCC22272E, 0xFF3A3F44)
            .centerVertical();

        factory.title("gui.oneblockultima.prices.edit_title");

        net.minecraft.item.ItemStack stack = BlockPriceConfig.createItemStack(container.getEditingRegistry(), container.getEditingMeta());
        if (!stack.isEmpty())
        {
            factory.add(new ItemStackElement(stack).size(16).align(Alignment.CENTER));
        }

        String displayName = container.getEditingName() != null && !container.getEditingName().isEmpty()
                ? container.getEditingName() : container.getEditingRegistry();
        factory.add(new LabelElement(displayName).centered(true));
        factory.add(new LabelElement(container.getEditingRegistry()).color(0x808080).centered(true));

        factory.add(new SpacerElement(10));
        factory.add(new LabelElement(I18n.format("gui.oneblockultima.prices.price")).color(0xC0C0C0).centered(true));

        priceFieldElement = new TextFieldElement(0)
            .text(ContainerBlockPrices.formatPrice(container.getEditingPrice()))
            .focused(true)
            .widthPercent(30);
        factory.add(priceFieldElement);

        factory.add(new SpacerElement(10));
        RowElement btnRow = factory.row(Alignment.CENTER).gap(8);
        btnRow.button(BUTTON_SAVE, I18n.format("gui.oneblockultima.done"));
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));

        factory.build(buttonList, fontRenderer);
    }

    @Override
    protected void actionPerformed(GuiButton button)
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

        if (button.id == BUTTON_SAVE && currentView == VIEW_EDIT_PRICE)
        {
            if (priceFieldElement != null) container.savePrice(priceFieldElement.getText());
            statusMessage = I18n.format("gui.oneblockultima.prices.price_saved");
            statusTimer = 60;
            changeView(VIEW_PRICES);
            return;
        }

        if (button.id == BUTTON_SAVE && currentView == VIEW_PRICES)
        {
            container.flushToConfig();
            statusMessage = I18n.format("gui.oneblockultima.prices.table_saved");
            statusTimer = 60;
            if (statusBar != null) statusBar.text(statusMessage, statusTimer);
            return;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
        if (statusTimer > 0) statusTimer--;
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
        return Math.max(1, (height - 100) / entryHeight);
    }

    private int getMaxVisibleSearchResults()
    {
        return Math.max(1, (height - 110) / 20);
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }
}

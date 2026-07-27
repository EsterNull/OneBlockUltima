package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.config.BlockPriceConfig;

import java.io.IOException;
import java.util.*;

public class GuiBlockPrices extends GuiScreen
{
    private static final int VIEW_PRICES = 0;
    private static final int VIEW_ADD_BLOCK = 1;
    private static final int VIEW_EDIT_PRICE = 2;

    private static final int BUTTON_ADD = 0;
    private static final int BUTTON_SAVE = 1;
    private static final int BUTTON_BACK = 2;
    private static final int BUTTON_BALANCE_MODE = 3;
    private static final int BUTTON_EDIT_BASE = 100;
    private static final int BUTTON_DELETE_BASE = 200;

    private final GuiScreen parent;
    private int currentView = VIEW_PRICES;
    private int scrollOffset = 0;
    private int entryHeight = 24;
    private String statusMessage = "";
    private int statusTimer = 0;

    private List<Map.Entry<String, Double>> priceEntries = new ArrayList<>();
    private List<Map.Entry<String, Double>> filteredEntries = new ArrayList<>();
    private Map<String, Double> stagedPrices = new LinkedHashMap<>();

    private GuiTextField searchField;
    private GuiTextField priceField;
    private String searchQuery = "";
    private List<SearchResult> searchResults = new ArrayList<>();
    private int searchScrollOffset = 0;

    private String editingRegistry = "";
    private String editingName = "";
    private int editingMeta = 0;
    private double editingPrice = 0;
    private boolean editingExisting = false;
    private int deleteTargetIndex = -1;
    private BlockPriceConfig.BalanceMode currentBalanceMode;

    private static class SearchResult
    {
        final String registry;
        final String name;
        final ItemStack stack;
        final int meta;

        SearchResult(String registry, String name, ItemStack stack)
        {
            this.registry = registry;
            this.name = name;
            this.stack = stack;
            this.meta = stack.isEmpty() ? 0 : stack.getMetadata();
        }
    }

    public GuiBlockPrices(GuiScreen parent)
    {
        this.parent = parent;
        this.currentBalanceMode = BlockPriceConfig.get().getBalanceMode();
        stagedPrices.clear();
        stagedPrices.putAll(BlockPriceConfig.get().getPrices());
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        int centerX = width / 2;
        int btnWidth = 80;
        int btnHeight = 20;

        if (currentView == VIEW_PRICES)
        {
            reloadPriceEntries();
            buttonList.clear();
            int topY = 35;
            int bottomY = height - 30;

            String balanceModeLabel = currentBalanceMode == BlockPriceConfig.BalanceMode.BREAK_BLOCK
                    ? I18n.format("gui.oneblockultima.mod_settings.balance_mode.break_block")
                    : I18n.format("gui.oneblockultima.mod_settings.balance_mode.sell_block");
            String balanceToggleText = I18n.format("gui.oneblockultima.mod_settings.balance_mode") + ": " + balanceModeLabel;
            int bmBtnWidth = Math.min(250, fontRenderer.getStringWidth(balanceToggleText) + 20);
            buttonList.add(new GuiButton(BUTTON_BALANCE_MODE, centerX - bmBtnWidth / 2, bottomY - btnHeight - 4, bmBtnWidth, btnHeight, balanceToggleText));

            buttonList.add(new GuiButton(BUTTON_ADD, centerX - btnWidth * 3 / 2 - 2, bottomY, btnWidth, btnHeight, I18n.format("gui.oneblockultima.prices.add")));
            buttonList.add(new GuiButton(BUTTON_SAVE, centerX - btnWidth / 2, bottomY, btnWidth, btnHeight, I18n.format("gui.oneblockultima.save")));
            buttonList.add(new GuiButton(BUTTON_BACK, centerX + btnWidth / 2 + 2, bottomY, btnWidth, btnHeight, I18n.format("gui.oneblockultima.cancel")));
        }
        else if (currentView == VIEW_ADD_BLOCK)
        {
            buttonList.clear();
            searchField = new GuiTextField(0, fontRenderer, centerX - 100, 32, 200, 14);
            searchField.setFocused(true);
            searchField.setText(searchQuery);
            searchField.setMaxStringLength(256);

            int bottomY = height - 30;
            buttonList.add(new GuiButton(BUTTON_BACK, centerX - 40, bottomY, 80, 20, I18n.format("gui.oneblockultima.cancel")));

            performSearch();
        }
        else if (currentView == VIEW_EDIT_PRICE)
        {
            buttonList.clear();
            priceField = new GuiTextField(1, fontRenderer, centerX - 60, height / 2 + 10, 120, 14);
            priceField.setFocused(true);
            priceField.setText(formatPrice(editingPrice));
            priceField.setMaxStringLength(10);
            priceField.setEnableBackgroundDrawing(false);

            int bottomY = height - 30;
            int bw = 80;
            buttonList.add(new GuiButton(BUTTON_SAVE, centerX - bw - 4, bottomY, bw, 20, I18n.format("gui.oneblockultima.save")));
            buttonList.add(new GuiButton(BUTTON_BACK, centerX + 4, bottomY, bw, 20, I18n.format("gui.oneblockultima.cancel")));
        }
    }

    private void changeView(int view)
    {
        if (currentView != view)
        {
            currentView = view;
            initGui();
        }
    }

    private void reloadPriceEntries()
    {
        priceEntries = new ArrayList<>(stagedPrices.entrySet());
        updateFilteredEntries();
    }

    private void updateFilteredEntries()
    {
        filteredEntries = new ArrayList<>(priceEntries);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_BACK)
        {
            if (currentView == VIEW_PRICES)
            {
                Keyboard.enableRepeatEvents(false);
                mc.displayGuiScreen(parent);
            }
            else if (currentView == VIEW_ADD_BLOCK || currentView == VIEW_EDIT_PRICE)
            {
                changeView(VIEW_PRICES);
            }
            return;
        }

        if (button.id == BUTTON_ADD)
        {
            editingExisting = false;
            editingRegistry = "";
            editingName = "";
            editingMeta = 0;
            editingPrice = 0;
            searchQuery = "";
            changeView(VIEW_ADD_BLOCK);
            return;
        }

        if (button.id == BUTTON_BALANCE_MODE && currentView == VIEW_PRICES)
        {
            currentBalanceMode = (currentBalanceMode == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
                    ? BlockPriceConfig.BalanceMode.SELL_BLOCK
                    : BlockPriceConfig.BalanceMode.BREAK_BLOCK;
            initGui();
            return;
        }

        if (button.id == BUTTON_SAVE && currentView == VIEW_EDIT_PRICE)
        {
            savePrice();
            return;
        }

        if (button.id == BUTTON_SAVE && currentView == VIEW_PRICES)
        {
            flushToConfig();
            BlockPriceConfig.get().setBalanceMode(currentBalanceMode);
            statusMessage = I18n.format("gui.oneblockultima.prices.table_saved");
            statusTimer = 60;
            return;
        }

        if (button.id >= BUTTON_DELETE_BASE && currentView == VIEW_PRICES)
        {
            int index = button.id - BUTTON_DELETE_BASE + scrollOffset;
            if (index >= 0 && index < filteredEntries.size())
            {
                Map.Entry<String, Double> entry = filteredEntries.get(index);
                stagedPrices.remove(entry.getKey());
                statusMessage = I18n.format("gui.oneblockultima.prices.price_removed");
                statusTimer = 60;
                reloadPriceEntries();
            }
            return;
        }

        if (button.id >= BUTTON_EDIT_BASE && currentView == VIEW_PRICES)
        {
            int index = button.id - BUTTON_EDIT_BASE + scrollOffset;
            if (index >= 0 && index < filteredEntries.size())
            {
                Map.Entry<String, Double> entry = filteredEntries.get(index);
                String entryKey = entry.getKey();
                int entryMeta = parseMetaFromKey(entryKey);
                editingRegistry = entryMeta > 0 ? entryKey.substring(0, entryKey.lastIndexOf(':')) : entryKey;
                editingMeta = entryMeta;
                editingPrice = entry.getValue();
                editingExisting = true;
                editingName = getBlockDisplayName(editingRegistry, editingMeta);
                changeView(VIEW_EDIT_PRICE);
            }
        }
    }

    private void savePrice()
    {
        double price = 0;
        try
        {
            String text = priceField.getText().trim().replace(',', '.');
            price = Double.parseDouble(text);
        }
        catch (NumberFormatException e)
        {
        }
        price = Math.max(0, price);

        if (editingRegistry.isEmpty())
        {
            return;
        }

        String priceKey = editingMeta > 0 ? editingRegistry + ":" + editingMeta : editingRegistry;
        stagedPrices.put(priceKey, price);
        statusMessage = I18n.format("gui.oneblockultima.prices.price_saved");
        statusTimer = 60;
        changeView(VIEW_PRICES);
    }

    private void flushToConfig()
    {
        BlockPriceConfig.get().replaceAll(stagedPrices);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (currentView == VIEW_ADD_BLOCK && searchField != null)
        {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (currentView == VIEW_EDIT_PRICE && priceField != null)
        {
            priceField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (currentView == VIEW_ADD_BLOCK)
        {
            int listX = width / 2 - 100;
            int listY = 65;
            int listW = 200;
            int listH = height - 110;
            int itemH = 20;

            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH)
            {
                int clickedRow = (mouseY - listY) / itemH;
                int index = clickedRow + searchScrollOffset;
                if (index >= 0 && index < searchResults.size())
                {
                    SearchResult result = searchResults.get(index);
                    editingRegistry = result.registry;
                    editingMeta = result.meta;
                    editingName = result.name;
                    String priceKey = editingMeta > 0 ? editingRegistry + ":" + editingMeta : editingRegistry;
                    editingPrice = stagedPrices.getOrDefault(priceKey, 0.0);
                    editingExisting = stagedPrices.containsKey(priceKey);
                    changeView(VIEW_EDIT_PRICE);
                    return;
                }
            }
        }

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
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (currentView == VIEW_ADD_BLOCK && searchField != null && searchField.isFocused())
        {
            searchField.textboxKeyTyped(typedChar, keyCode);
            searchQuery = searchField.getText();
            searchScrollOffset = 0;
            performSearch();
        }
        else if (currentView == VIEW_EDIT_PRICE && priceField != null && priceField.isFocused())
        {
            priceField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0)
        {
            int delta = dWheel > 0 ? -1 : 1;
            if (currentView == VIEW_PRICES)
            {
                int maxScroll = Math.max(0, filteredEntries.size() - getMaxVisibleEntries());
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + delta));
            }
            else if (currentView == VIEW_ADD_BLOCK)
            {
                int maxScroll = Math.max(0, searchResults.size() - getMaxVisibleSearchResults());
                searchScrollOffset = Math.max(0, Math.min(maxScroll, searchScrollOffset + delta));
            }
        }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (searchField != null) searchField.updateCursorCounter();
        if (priceField != null) priceField.updateCursorCounter();
        if (statusTimer > 0) statusTimer--;
    }

    private static final int LIST_BOTTOM_MARGIN = 130;

    private int getMaxVisibleEntries()
    {
        int listH = height - LIST_BOTTOM_MARGIN;
        return Math.max(1, listH / entryHeight);
    }

    private int getMaxVisibleSearchResults()
    {
        int listH = height - 110;
        return Math.max(1, listH / 20);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();

        if (currentView == VIEW_PRICES)
        {
            drawPricesView(mouseX, mouseY, partialTicks);
        }
        else if (currentView == VIEW_ADD_BLOCK)
        {
            drawAddBlockView(mouseX, mouseY, partialTicks);
        }
        else if (currentView == VIEW_EDIT_PRICE)
        {
            drawEditPriceView(mouseX, mouseY, partialTicks);
        }

        if (statusTimer > 0 && !statusMessage.isEmpty())
        {
            int bmBtnY = height - 30 - 20 - 4;
            drawCenteredString(fontRenderer, statusMessage, width / 2, bmBtnY - 12, 0x55FF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static final int SCROLLBAR_W = 4;

    private void drawPricesView(int mouseX, int mouseY, float partialTicks)
    {
        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.prices.title"), width / 2, 14, 0xFFFFFF);

        int listX = width / 2 - 120;
        int listY = 35;
        int listW = 240;
        int listH = height - LIST_BOTTOM_MARGIN;

        Gui.drawRect(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, 0xFF555555);
        Gui.drawRect(listX, listY, listX + listW, listY + listH, 0xAA111111);

        String editText = I18n.format("gui.oneblockultima.prices.edit");
        String delText = I18n.format("gui.oneblockultima.prices.delete");
        int editW = fontRenderer.getStringWidth(editText) + 8;
        int delW = fontRenderer.getStringWidth(delText) + 8;
        int btnH = 14;
        int btnGap = 3;
        int usableRight = listX + listW - SCROLLBAR_W;

        int maxVisible = getMaxVisibleEntries();
        for (int i = 0; i < maxVisible && i + scrollOffset < filteredEntries.size(); i++)
        {
            int idx = i + scrollOffset;
            Map.Entry<String, Double> entry = filteredEntries.get(idx);
            int ey = listY + i * entryHeight;

            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= ey && mouseY < ey + entryHeight;
            if (hovered)
            {
                Gui.drawRect(listX + 1, ey, listX + listW - 1, ey + entryHeight, 0x33FFFFFF);
            }

            ItemStack stack = BlockPriceConfig.createItemStack(parseRegistryFromKey(entry.getKey()), parseMetaFromKey(entry.getKey()));
            if (!stack.isEmpty())
            {
                GlStateManager.enableDepth();
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, listX + 4, ey + 4);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableDepth();
            }

            String name = getBlockDisplayName(parseRegistryFromKey(entry.getKey()), parseMetaFromKey(entry.getKey()));
            fontRenderer.drawStringWithShadow(name, listX + 24, ey + 2, 0xFFFFFF);
            fontRenderer.drawStringWithShadow(entry.getKey(), listX + 24, ey + 12, 0x808080);

            String priceStr = formatPrice(entry.getValue());
            int priceW = fontRenderer.getStringWidth(priceStr);
            int priceX = usableRight - priceW - btnGap - editW - btnGap - delW - 4;
            int priceY = ey + (entryHeight - 8) / 2;
            fontRenderer.drawStringWithShadow(priceStr, priceX, priceY, 0xFFD700);

            int delBtnX = usableRight - delW;
            int delBtnY = ey + (entryHeight - btnH) / 2;
            Gui.drawRect(delBtnX, delBtnY, delBtnX + delW, delBtnY + btnH, 0xFF6B3A3A);
            drawCenteredString(fontRenderer, delText, delBtnX + delW / 2, delBtnY + 3, 0xFF5555);

            int editBtnX = delBtnX - btnGap - editW;
            int editBtnY = ey + (entryHeight - btnH) / 2;
            Gui.drawRect(editBtnX, editBtnY, editBtnX + editW, editBtnY + btnH, 0xFF3A6B3A);
            drawCenteredString(fontRenderer, editText, editBtnX + editW / 2, editBtnY + 3, 0x55FF55);
        }

        if (filteredEntries.size() > maxVisible)
        {
            int scrollX = listX + listW - SCROLLBAR_W;
            int scrollAreaH = listH;
            int scrollAreaY = listY;
            int thumbH = Math.max(10, scrollAreaH * maxVisible / filteredEntries.size());
            int thumbY = scrollAreaY + (int)((float)scrollOffset / Math.max(1, filteredEntries.size() - maxVisible) * (scrollAreaH - thumbH));

            Gui.drawRect(scrollX, scrollAreaY, scrollX + 3, scrollAreaY + scrollAreaH, 0xFF2A2F34);
            Gui.drawRect(scrollX, thumbY, scrollX + 3, thumbY + thumbH, 0xFF7A7F84);
        }

        drawHorizontalLine(listX, listX + listW, listY - 1, 0xFF555555);
        drawHorizontalLine(listX, listX + listW, listY + listH, 0xFF555555);

        for (GuiButton btn : buttonList)
        {
            if (btn.id >= BUTTON_EDIT_BASE || btn.id >= BUTTON_DELETE_BASE) continue;
            btn.drawButton(mc, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (currentView == VIEW_PRICES)
        {
            int listX = width / 2 - 120;
            int listY = 35;
            int listW = 240;
            int maxVisible = getMaxVisibleEntries();

            String editText = I18n.format("gui.oneblockultima.prices.edit");
            String delText = I18n.format("gui.oneblockultima.prices.delete");
            int editW = fontRenderer.getStringWidth(editText) + 8;
            int delW = fontRenderer.getStringWidth(delText) + 8;
            int btnH = 14;
            int btnGap = 3;
            int usableRight = listX + listW - SCROLLBAR_W;

            for (int i = 0; i < maxVisible && i + scrollOffset < filteredEntries.size(); i++)
            {
                int idx = i + scrollOffset;
                int ey = listY + i * entryHeight;
                int editBtnX = usableRight - delW - btnGap - editW;
                int editBtnY = ey + (entryHeight - btnH) / 2;
                int delBtnX = usableRight - delW;
                int delBtnY = ey + (entryHeight - btnH) / 2;

                if (mouseX >= editBtnX && mouseX <= editBtnX + editW && mouseY >= editBtnY && mouseY <= editBtnY + btnH)
                {
                    Map.Entry<String, Double> entry = filteredEntries.get(idx);
                    String entryKey = entry.getKey();
                    int entryMeta = parseMetaFromKey(entryKey);
                    editingRegistry = entryMeta > 0 ? entryKey.substring(0, entryKey.lastIndexOf(':')) : entryKey;
                    editingMeta = entryMeta;
                    editingPrice = entry.getValue();
                    editingExisting = true;
                    editingName = getBlockDisplayName(editingRegistry, editingMeta);
                    changeView(VIEW_EDIT_PRICE);
                    return;
                }

                if (mouseX >= delBtnX && mouseX <= delBtnX + delW && mouseY >= delBtnY && mouseY <= delBtnY + btnH)
                {
                    Map.Entry<String, Double> entry = filteredEntries.get(idx);
                    stagedPrices.remove(entry.getKey());
                    statusMessage = I18n.format("gui.oneblockultima.prices.price_removed");
                    statusTimer = 60;
                    reloadPriceEntries();
                    return;
                }
            }
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    private void drawAddBlockView(int mouseX, int mouseY, float partialTicks)
    {
        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.prices.add"), width / 2, 14, 0xFFFFFF);

        if (searchField != null)
        {
            searchField.drawTextBox();
        }

        String hint = I18n.format("gui.oneblockultima.prices.search.help");
        drawCenteredString(fontRenderer, hint, width / 2, 50, 0x808080);

        int listX = width / 2 - 100;
        int listY = 65;
        int listW = 200;
        int listH = height - 110;
        int itemH = 20;

        Gui.drawRect(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, 0xFF555555);
        Gui.drawRect(listX, listY, listX + listW, listY + listH, 0xAA111111);

        if (searchResults.isEmpty())
        {
            drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.prices.search.no_results"), width / 2, listY + listH / 2 - 4, 0x808080);
        }
        else
        {
            int maxVisible = getMaxVisibleSearchResults();
            for (int i = 0; i < maxVisible && i + searchScrollOffset < searchResults.size(); i++)
            {
                int idx = i + searchScrollOffset;
                SearchResult result = searchResults.get(idx);
                int ey = listY + i * itemH;

                boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= ey && mouseY < ey + itemH;
                if (hovered)
                {
                    Gui.drawRect(listX + 1, ey, listX + listW - 1, ey + itemH, 0x33FFFFFF);
                }

                if (!result.stack.isEmpty())
                {
                    GlStateManager.enableDepth();
                    RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.enableRescaleNormal();
                    Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(result.stack, listX + 2, ey + 2);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableRescaleNormal();
                    GlStateManager.disableDepth();
                }

                String displayName = result.name != null && !result.name.isEmpty() ? result.name : result.registry;
                fontRenderer.drawStringWithShadow(displayName, listX + 20, ey + 2, 0xFFFFFF);
                fontRenderer.drawStringWithShadow(result.registry, listX + 20, ey + 12, 0x808080);
            }

            if (searchResults.size() > maxVisible)
            {
                int scrollX = listX + listW - 4;
                int scrollAreaH = listH;
                int scrollAreaY = listY;
                int totalH = searchResults.size() * itemH;
                int thumbH = Math.max(10, scrollAreaH * maxVisible / searchResults.size());
                int thumbY = scrollAreaY + (int)((float)searchScrollOffset / Math.max(1, searchResults.size() - maxVisible) * (scrollAreaH - thumbH));

                Gui.drawRect(scrollX, scrollAreaY, scrollX + 3, scrollAreaY + scrollAreaH, 0xFF2A2F34);
                Gui.drawRect(scrollX, thumbY, scrollX + 3, thumbY + thumbH, 0xFF7A7F84);
            }
        }

        for (GuiButton btn : buttonList)
        {
            btn.drawButton(mc, mouseX, mouseY, partialTicks);
        }
    }

    private void drawEditPriceView(int mouseX, int mouseY, float partialTicks)
    {
        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.prices.edit_title"), width / 2, 14, 0xFFFFFF);

        int centerX = width / 2;
        int centerY = height / 2 - 20;

        ItemStack stack = BlockPriceConfig.createItemStack(editingRegistry, editingMeta);
        if (!stack.isEmpty())
        {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, centerX - 8, centerY - 30);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableDepth();
        }

        String displayName = editingName != null && !editingName.isEmpty() ? editingName : editingRegistry;
        drawCenteredString(fontRenderer, displayName, centerX, centerY + 2, 0xFFFFFF);
        drawCenteredString(fontRenderer, editingRegistry, centerX, centerY + 14, 0x808080);

        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.prices.price"), centerX, centerY + 35, 0xC0C0C0);
        if (priceField != null)
        {
            int pad = 2;
            Gui.drawRect(priceField.x - pad, priceField.y - pad, priceField.x + priceField.width + pad, priceField.y + priceField.height + pad, 0xFF1A1D21);
            drawHorizontalLine(priceField.x - pad, priceField.x + priceField.width + pad, priceField.y - pad, 0xFF444444);
            drawHorizontalLine(priceField.x - pad, priceField.x + priceField.width + pad, priceField.y + priceField.height + pad, 0xFF444444);
            drawVerticalLine(priceField.x - pad, priceField.y - pad, priceField.y + priceField.height + pad, 0xFF444444);
            drawVerticalLine(priceField.x + priceField.width + pad, priceField.y - pad, priceField.y + priceField.height + pad, 0xFF444444);
            priceField.drawTextBox();
        }

        for (GuiButton btn : buttonList)
        {
            btn.drawButton(mc, mouseX, mouseY, partialTicks);
        }
    }

    private void performSearch()
    {
        searchResults.clear();
        String query = searchQuery == null ? "" : searchQuery;
        boolean emptyQuery = query.isEmpty();

        String[] parts = emptyQuery ? new String[0] : query.split(" ");
        List<String> searchTerms = new ArrayList<>();
        String modFilter = null;
        String idFilter = null;

        for (String part : parts)
        {
            if (part.isEmpty()) continue;
            if (part.startsWith("@")) modFilter = part.substring(1).toLowerCase(Locale.ROOT);
            else if (part.startsWith("&")) idFilter = part.substring(1).toLowerCase(Locale.ROOT);
            else searchTerms.add(part.toLowerCase(Locale.ROOT));
        }

        for (net.minecraft.block.Block block : ForgeRegistries.BLOCKS)
        {
            ResourceLocation reg = block.getRegistryName();
            if (reg == null) continue;

            String registry = reg.toString();
            String registryId = reg.getResourcePath();
            String modId = reg.getResourceDomain();

            if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
            if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

            Item item = Item.getItemFromBlock(block);
            if (item == null || item == Items.AIR) continue;

            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);
            if (subItems.isEmpty())
            {
                subItems.add(new ItemStack(item, 1, 0));
            }

            for (ItemStack subStack : subItems)
            {
                if (subStack.isEmpty() || subStack.getItem() != item) continue;

                int meta = subStack.getMetadata();
                String key = registry + ":" + meta;
                if (stagedPrices.containsKey(key) || (meta == 0 && stagedPrices.containsKey(registry))) continue;

                String name = "";
                try { name = subStack.getDisplayName(); } catch (Exception ignored) {}

                if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;

                searchResults.add(new SearchResult(registry, name, subStack.copy()));
            }
        }

        // Add non-block items (e.g. diamonds from ore drops)
        for (Item item : ForgeRegistries.ITEMS)
        {
            if (item == null || item == Items.AIR) continue;
            if (item instanceof ItemBlock) continue;

            ResourceLocation reg = item.getRegistryName();
            if (reg == null) continue;

            String registry = reg.toString();
            String registryId = reg.getResourcePath();
            String modId = reg.getResourceDomain();

            if (modFilter != null && !modId.toLowerCase(Locale.ROOT).contains(modFilter)) continue;
            if (idFilter != null && !registryId.toLowerCase(Locale.ROOT).contains(idFilter)) continue;

            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);
            if (subItems.isEmpty())
            {
                subItems.add(new ItemStack(item, 1, 0));
            }

            for (ItemStack subStack : subItems)
            {
                if (subStack.isEmpty() || subStack.getItem() != item) continue;

                int meta = subStack.getMetadata();
                String key = registry + ":" + meta;
                if (stagedPrices.containsKey(key) || (meta == 0 && stagedPrices.containsKey(registry))) continue;

                String name = "";
                try { name = subStack.getDisplayName(); } catch (Exception ignored) {}

                if (!emptyQuery && !searchTerms.isEmpty() && !matchesSearchTerms(name, searchTerms)) continue;

                searchResults.add(new SearchResult(registry, name, subStack.copy()));
            }
        }

        searchResults.sort(Comparator.comparing(r -> r.name.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesSearchTerms(String name, List<String> terms)
    {
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String term : terms)
        {
            if (!lowerName.contains(term)) return false;
        }
        return true;
    }

    private String getBlockDisplayName(String registry, int meta)
    {
        ItemStack stack = BlockPriceConfig.createItemStack(registry, meta);
        if (!stack.isEmpty())
        {
            try { return stack.getDisplayName(); } catch (Exception ignored) {}
        }
        return registry;
    }

    private static int parseMetaFromKey(String key)
    {
        if (key == null) return 0;
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0) return 0;
        String suffix = key.substring(lastColon + 1);
        try
        {
            return Integer.parseInt(suffix);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private static String parseRegistryFromKey(String key)
    {
        if (key == null) return "";
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0) return key;
        String suffix = key.substring(lastColon + 1);
        try
        {
            Integer.parseInt(suffix);
            return key.substring(0, lastColon);
        }
        catch (NumberFormatException e)
        {
            return key;
        }
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return true;
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    private static String formatPrice(double price)
    {
        if (price == (long) price)
        {
            return String.valueOf((long) price);
        }
        return String.valueOf(price);
    }
}

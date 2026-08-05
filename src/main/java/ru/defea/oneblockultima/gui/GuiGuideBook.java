package ru.defea.oneblockultima.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.defea.oneblockultima.guide.GuideBookContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.defea.oneblockultima.Constants.*;

public class GuiGuideBook extends GuiScreen
{
    private static final int BOOK_W = 480;
    private static final int BOOK_H = 300;

    private static final int LEFT_PAGE_X = 28;
    private static final int RIGHT_PAGE_X = 256;
    private static final int PAGE_W = 196;
    private static final int PAGE_Y = 40;
    private static final int PAGE_H = 224;
    private static final int LINE_H = 9;
    private static final int GAP = 3;

    private static final float CONTENT_SCALE = 1.25F;
    private static final int PAD = 8;
    private static final int CONTENT_W = (int) ((PAGE_W - 2 * PAD) / CONTENT_SCALE);
    private static final int CONTENT_H = (int) ((PAGE_H - 2 * PAD) / CONTENT_SCALE);

    private static final int TOC_ROW_H = 16;
    private static final int TOC_TOP = 6;

    private static final int NAV_Y = 270;
    private static final int NAV_BTN_W = 48;
    private static final int NAV_BTN_H = 22;
    private static final int NAV_PREV_X = 20;
    private static final int NAV_NEXT_X = BOOK_W - NAV_BTN_W - 20;
    private static final int NAV_HOME_X = 14;
    private static final int NAV_HOME_Y = 10;
    private static final int NAV_HOME_W = NAV_BTN_W;
    private static final int NAV_HOME_H = NAV_BTN_H;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_INNER = 0xFF8B8B8B;

    private float scale = 1.0F;
    private int bookX;
    private int bookY;

    private int currentSpread = 0;

    private final List<BookPage> pages = new ArrayList<>();
    private BookPage tocPage;

    private ItemStack hoverStack = ItemStack.EMPTY;
    private float hoverContentX;
    private float hoverContentY;
    private int hoverPageX = LEFT_PAGE_X;

    private final List<GuideBookContent.CommandInfo> commands = new ArrayList<>();
    private final List<GuideBookContent.Recipe> recipes = new ArrayList<>();

    private static class Placed
    {
        final PageElement element;
        final int y;

        Placed(PageElement element, int y)
        {
            this.element = element;
            this.y = y;
        }
    }

    private static class BookPage
    {
        final List<Placed> placed = new ArrayList<>();
    }

    private abstract static class PageElement
    {
        @SuppressWarnings("SameParameterValue")
        abstract int measure(FontRenderer fr, int width);

        @SuppressWarnings("SameParameterValue")
        abstract void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY);
    }

    private static class TitleElement extends PageElement
    {
        final String text;
        final int color;

        TitleElement(String text, int color)
        {
            this.text = text;
            this.color = color;
        }

        TitleElement(String key)
        {
            this(I18n.format(key), GOLD_COLOR);
        }

        @Override
        int measure(FontRenderer fr, int width)
        {
            return LINE_H + 4;
        }

        @Override
        void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY)
        {
            int textWidth = fr.getStringWidth(text);
            int tx = x + (width - textWidth) / 2;
            fr.drawString(text, tx, y, color);
            Gui.drawRect(x, y + LINE_H + 3, x + width, y + LINE_H + 4, GOLD_COLOR);
        }
    }

    private static class TextElement extends PageElement
    {
        final List<String> lines;
        final int color;

        TextElement(FontRenderer fr, String text, int color, int width)
        {
            this.color = color;
            this.lines = wrapText(fr, text, width);
        }

        TextElement(TextElement source, int fromLine, int count)
        {
            this.color = source.color;
            this.lines = source.lines.subList(fromLine, fromLine + count);
        }

        @Override
        int measure(FontRenderer fr, int width)
        {
            return lines.size() * LINE_H;
        }

        @Override
        void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY)
        {
            int cy = y;
            for (String line : lines)
            {
                fr.drawString(line, x, cy, color);
                cy += LINE_H;
            }
        }
    }

    private static class IconLineElement extends PageElement
    {
        final ResourceLocation texture;
        final List<String> lines;
        final int color;

        IconLineElement(ResourceLocation texture, String text, int color)
        {
            this.texture = texture;
            this.color = color;
            this.lines = wrapText(Minecraft.getMinecraft().fontRenderer, text, CONTENT_W - 20);
        }

        @Override
        int measure(FontRenderer fr, int width)
        {
            return Math.max(16, lines.size() * LINE_H);
        }

        @Override
        void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY)
        {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
            int cy = y + (16 - lines.size() * LINE_H) / 2;
            for (String line : lines)
            {
                fr.drawString(line, x + 20, cy, color);
                cy += LINE_H;
            }
        }
    }

    private static class CommandElement extends PageElement
    {
        final GuideBookContent.CommandInfo command;
        final List<String> nameLines;
        final List<String> usageLines;
        final List<String> descLines;
        final String cheatMarker;

        CommandElement(FontRenderer fr, GuideBookContent.CommandInfo command)
        {
            this.command = command;
            this.cheatMarker = command.cheat ? "[" + I18n.format("book.oneblockultima.command.cheat") + "] " : "";
            this.nameLines = wrapText(fr, "/" + command.name, CONTENT_W);
            this.usageLines = wrapText(fr, command.usage, CONTENT_W);
            this.descLines = wrapText(fr, I18n.format(command.descriptionKey), CONTENT_W);
        }

        @Override
        int measure(FontRenderer fr, int width)
        {
            return (nameLines.size() + usageLines.size() + descLines.size()) * LINE_H + 4;
        }

        @Override
        void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY)
        {
            int cy = y;
            for (String line : nameLines)
            {
                int nameX = x;
                if (!cheatMarker.isEmpty())
                {
                    fr.drawString(cheatMarker, x, cy, REDDISH_COLOR);
                    nameX += fr.getStringWidth(cheatMarker);
                }
                fr.drawString(line, nameX, cy, GOLD_COLOR);
                cy += LINE_H;
            }
            for (String line : usageLines)
            {
                fr.drawString(line, x, cy, GRAY_COLOR_5);
                cy += LINE_H;
            }
            for (String line : descLines)
            {
                fr.drawString(line, x, cy, LIGHT_GRAY_COLOR_1);
                cy += LINE_H;
            }
        }
    }

    private class RecipeElement extends PageElement
    {
        private static final int SLOT = 18;
        private static final int GRID_SIZE = SLOT * 3;
        private static final int ARROW_X = GRID_SIZE + 4;
        private static final int ARROW_W = 14;
        private static final int RESULT_X = ARROW_X + ARROW_W + 6;

        final GuideBookContent.Recipe recipe;
        final List<String> nameLines;

        RecipeElement(FontRenderer fr, GuideBookContent.Recipe recipe)
        {
            this.recipe = recipe;
            this.nameLines = wrapText(fr, recipe.result.getDisplayName(), CONTENT_W);
        }

        @Override
        int measure(FontRenderer fr, int width)
        {
            return GRID_SIZE + 4 + nameLines.size() * LINE_H + 6;
        }

        @Override
        void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY)
        {
            for (int i = 0; i < 9; i++)
            {
                int cx = x + (i % 3) * SLOT;
                int cy = y + (i / 3) * SLOT;
                drawSlot(cx, cy);
                ItemStack stack = recipe.grid[i];
                if (!stack.isEmpty())
                {
                    drawItemStack(stack, cx + 1, cy + 1);
                    if (mouseIn(mouseX, mouseY, cx, cy, SLOT, SLOT))
                    {
                        setHoverContent(stack, cx + (float) SLOT / 2, cy + (float) SLOT / 2);
                    }
                }
            }

            drawCraftingArrow(x + ARROW_X, y + (GRID_SIZE - 9) / 2, ARROW_W, 9, GRAY_COLOR_5);

            int rx = x + RESULT_X;
            int ry = y + (GRID_SIZE - SLOT) / 2;
            drawSlot(rx, ry);
            drawItemStack(recipe.result, rx + 1, ry + 1);
            if (mouseIn(mouseX, mouseY, rx, ry, SLOT, SLOT))
            {
                setHoverContent(recipe.result, rx + (float) SLOT / 2, ry + (float) SLOT / 2);
            }

            int ny = y + GRID_SIZE + 4;
            for (String line : nameLines)
            {
                fr.drawString(line, x, ny, WHITE_COLOR_1);
                ny += LINE_H;
            }
        }
    }

    private static class TocEntryElement extends PageElement
    {
        final String label;
        final int pageNumber;
        final int targetPage;
        int hitX;
        int hitY;
        int hitW;
        int hitH;

        TocEntryElement(String label, int pageNumber, int targetPage)
        {
            this.label = label;
            this.pageNumber = pageNumber;
            this.targetPage = targetPage;
        }

        @Override
        int measure(FontRenderer fr, int width)
        {
            return TOC_ROW_H;
        }

        @Override
        void draw(FontRenderer fr, int x, int y, int width, float mouseX, float mouseY)
        {
            hitX = x;
            hitY = y;
            hitW = width;
            hitH = TOC_ROW_H;
            boolean hovered = mouseIn(mouseX, mouseY, x, y, width, TOC_ROW_H);
            fr.drawString(label, x, y + 4, hovered ? WHITE_COLOR_1 : LIGHT_GRAY_COLOR_1);
            String number = String.valueOf(pageNumber);
            fr.drawString(number, x + width - fr.getStringWidth(number), y + 4, GOLD_COLOR);
        }

        boolean hit(float mouseX, float mouseY)
        {
            return mouseX >= hitX && mouseX < hitX + hitW && mouseY >= hitY && mouseY < hitY + hitH;
        }
    }

    @Override
    public void initGui()
    {
        super.initGui();
        buttonList.clear();

        commands.clear();
        recipes.clear();
        commands.addAll(GuideBookContent.getCommands());
        recipes.addAll(GuideBookContent.getRecipes());

        float fitScale = Math.min(
                (float) (width - 24) / BOOK_W,
                (float) (height - 24) / BOOK_H);
        scale = Math.min(1.0F, fitScale);
        int bookW = (int) (BOOK_W * scale);
        int bookH = (int) (BOOK_H * scale);
        bookX = (width - bookW) / 2;
        bookY = (height - bookH) / 2;

        buildBook();
    }

    private void buildBook()
    {
        List<BookPage> overviewPages = buildOverviewPages();
        List<BookPage> commandPages = buildCommandPages();
        List<BookPage> recipePages = buildRecipePages();
        List<BookPage> mechanicPages = buildMechanicPages();

        Map<String, Integer> sectionStart = getSectionStart(overviewPages, commandPages, recipePages);

        pages.clear();
        tocPage = buildTocPage(sectionStart);
        pages.add(tocPage);
        pages.add(new BookPage());
        pages.addAll(overviewPages);
        pages.addAll(commandPages);
        pages.addAll(recipePages);
        pages.addAll(mechanicPages);

        if (currentSpread > maxSpread())
        {
            currentSpread = maxSpread();
        }
    }

    private static Map<String, Integer> getSectionStart(List<BookPage> overviewPages, List<BookPage> commandPages, List<BookPage> recipePages) {
        Map<String, Integer> sectionStart = new LinkedHashMap<>();
        // The opening spread shows only the table of contents, so content starts
        // at page index 2 (index 1 is a filler that is never rendered).
        int offset = 2;
        sectionStart.put("book.oneblockultima.overview.title", offset);
        offset += overviewPages.size();
        sectionStart.put("book.oneblockultima.tab.commands", offset);
        offset += commandPages.size();
        sectionStart.put("book.oneblockultima.tab.recipes", offset);
        offset += recipePages.size();
        sectionStart.put("book.oneblockultima.tab.mechanics", offset);
        return sectionStart;
    }

    private int maxSpread()
    {
        return Math.max(0, (pages.size() - 1) / 2);
    }

    private List<BookPage> buildOverviewPages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.overview.title"));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.overview.line1"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.overview.line2"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.overview.line3"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new IconLineElement(COIN_TEXTURE, I18n.format("book.oneblockultima.overview.currency"), GOLD_COLOR));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.overview.sets"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.overview.tip"), GRAY_COLOR_5, CONTENT_W));
        return paginate(elements);
    }

    private List<BookPage> buildCommandPages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.tab.commands"));
        for (GuideBookContent.CommandInfo command : commands)
        {
            elements.add(new CommandElement(fontRenderer, command));
        }
        return paginate(elements);
    }

    private List<BookPage> buildRecipePages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.tab.recipes"));
        if (recipes.isEmpty())
        {
            elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.recipes.empty"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        }
        else
        {
            elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.recipes_count", recipes.size()), GRAY_COLOR_5, CONTENT_W));
            for (GuideBookContent.Recipe recipe : recipes)
            {
                elements.add(new RecipeElement(fontRenderer, recipe));
            }
        }
        return paginate(elements);
    }

    private List<BookPage> buildMechanicPages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.tab.mechanics"));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.intro"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.break"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.mobs"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.currency"), GOLD_COLOR, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.mode"), GOLD_COLOR, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.sets"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.conditions"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.mechanics.invites"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(fontRenderer, I18n.format("book.oneblockultima.overview.tip"), GRAY_COLOR_5, CONTENT_W));
        return paginate(elements);
    }

    private BookPage buildTocPage(Map<String, Integer> sectionStart)
    {
        BookPage page = new BookPage();
        int y = TOC_TOP;
        page.placed.add(new Placed(new TitleElement("book.oneblockultima.toc.title"), y));
        y += LINE_H + 4 + 12;

        for (Map.Entry<String, Integer> entry : sectionStart.entrySet())
        {
            String label = I18n.format(entry.getKey());
            int targetPage = entry.getValue();
            page.placed.add(new Placed(new TocEntryElement(label, targetPage + 1, targetPage), y));
            y += TOC_ROW_H;
        }
        return page;
    }

    private List<BookPage> paginate(List<PageElement> elements)
    {
        List<BookPage> result = new ArrayList<>();
        BookPage page = new BookPage();
        int y = 0;

        for (PageElement element : elements)
        {
            if (element instanceof TextElement)
            {
                TextElement text = (TextElement) element;
                int lineIndex = 0;
                while (lineIndex < text.lines.size())
                {
                    if (y > CONTENT_H - LINE_H)
                    {
                        result.add(page);
                        page = new BookPage();
                        y = 0;
                    }
                    int spaceLeft = CONTENT_H - y;
                    int fit = spaceLeft / LINE_H;
                    if (fit <= 0)
                    {
                        fit = CONTENT_H / LINE_H;
                    }
                    int take = Math.min(fit, text.lines.size() - lineIndex);
                    page.placed.add(new Placed(new TextElement(text, lineIndex, take), y));
                    y += take * LINE_H;
                    lineIndex += take;
                    if (lineIndex < text.lines.size())
                    {
                        result.add(page);
                        page = new BookPage();
                        y = 0;
                    }
                }
                y += GAP;
            }
            else
            {
                int height = element.measure(fontRenderer, CONTENT_W);
                if (y + height > CONTENT_H)
                {
                    result.add(page);
                    page = new BookPage();
                    y = 0;
                }
                page.placed.add(new Placed(element, y));
                y += height + GAP;
            }
        }

        if (!page.placed.isEmpty() || result.isEmpty())
        {
            result.add(page);
        }
        return result;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        hoverStack = ItemStack.EMPTY;

        float localMouseX = (mouseX - bookX) / scale;
        float localMouseY = (mouseY - bookY) / scale;

        GlStateManager.pushMatrix();
        GlStateManager.translate(bookX, bookY, 0);
        GlStateManager.scale(scale, scale, 1.0F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(GUIDE_BOOK_BG_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        int leftIndex = currentSpread * 2;
        if (leftIndex < pages.size())
        {
            drawPage(pages.get(leftIndex), LEFT_PAGE_X, localMouseX, localMouseY);
        }
        int rightIndex = leftIndex + 1;
        if (rightIndex < pages.size() && !(currentSpread == 0 && rightIndex == 1))
        {
            drawPage(pages.get(rightIndex), RIGHT_PAGE_X, localMouseX, localMouseY);
        }

        drawPageNumbers(leftIndex, rightIndex);
        drawNavigation(localMouseX, localMouseY);

        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (!hoverStack.isEmpty())
        {
            int tipX = bookX + (int) ((hoverPageX + PAD + hoverContentX * CONTENT_SCALE) * scale) + 8;
            int tipY = bookY + (int) ((PAGE_Y + PAD + hoverContentY * CONTENT_SCALE) * scale);
            List<String> tooltip = hoverStack.getTooltip(mc.player,
                    mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            drawHoveringText(tooltip, tipX, tipY, fontRenderer);
        }
    }

    private void drawPage(BookPage page, int pageX, float localMouseX, float localMouseY)
    {
        float contentMouseX = (localMouseX - (pageX + PAD)) / CONTENT_SCALE;
        float contentMouseY = (localMouseY - (PAGE_Y + PAD)) / CONTENT_SCALE;

        GlStateManager.pushMatrix();
        GlStateManager.translate(pageX + PAD, PAGE_Y + PAD, 0);
        GlStateManager.scale(CONTENT_SCALE, CONTENT_SCALE, 1.0F);

        ItemStack hoverBefore = hoverStack;
        for (Placed placed : page.placed)
        {
            placed.element.draw(fontRenderer, 0, placed.y, CONTENT_W, contentMouseX, contentMouseY);
        }
        if (hoverStack != hoverBefore)
        {
            hoverPageX = pageX;
        }

        GlStateManager.popMatrix();
    }

    private void drawPageNumbers(int leftIndex, int rightIndex)
    {
        if (leftIndex < pages.size())
        {
            drawCenteredString(fontRenderer, String.valueOf(leftIndex + 1), LEFT_PAGE_X + PAGE_W / 2, PAGE_Y + PAGE_H + 1, GRAY_COLOR_5);
        }
        if (rightIndex < pages.size() && !(currentSpread == 0 && rightIndex == 1))
        {
            drawCenteredString(fontRenderer, String.valueOf(rightIndex + 1), RIGHT_PAGE_X + PAGE_W / 2, PAGE_Y + PAGE_H + 1, GRAY_COLOR_5);
        }
    }

    private void drawNavigation(float localMouseX, float localMouseY)
    {
        boolean canPrev = currentSpread > 0;
        boolean canNext = currentSpread < maxSpread();

        boolean prevHover = canPrev && mouseIn(localMouseX, localMouseY, NAV_PREV_X, NAV_Y, NAV_BTN_W, NAV_BTN_H);
        boolean nextHover = canNext && mouseIn(localMouseX, localMouseY, NAV_NEXT_X, NAV_Y, NAV_BTN_W, NAV_BTN_H);
        boolean homeHover = currentSpread != 0 && mouseIn(localMouseX, localMouseY, NAV_HOME_X, NAV_HOME_Y, NAV_HOME_W, NAV_HOME_H);

        int prevColor = prevHover ? WHITE_COLOR_1 : (canPrev ? GOLD_COLOR : GRAY_COLOR_4);
        int nextColor = nextHover ? WHITE_COLOR_1 : (canNext ? GOLD_COLOR : GRAY_COLOR_4);
        int homeColor = homeHover ? WHITE_COLOR_1 : (currentSpread != 0 ? GOLD_COLOR : GRAY_COLOR_4);

        int arrowSize = 14;
        int arrowY = NAV_Y + (NAV_BTN_H - arrowSize) / 2;
        drawLeftArrow(NAV_PREV_X + NAV_BTN_W / 2 - arrowSize / 2, arrowY, arrowSize, prevColor);
        drawRightArrow(NAV_NEXT_X + NAV_BTN_W / 2 - arrowSize / 2, arrowY, arrowSize, nextColor);

        drawHomeArrow(NAV_HOME_X + NAV_HOME_W / 2 - 8, NAV_HOME_Y + (NAV_HOME_H - 16) / 2, 16, homeColor);
    }

    private static void drawLeftArrow(int x, int y, int size, int color)
    {
        int midY = y + size / 2;
        int headLen = 4;
        int headHalf = 4;
        for (int px = x; px <= x + headLen; px++)
        {
            int h = (headHalf * (px - x) + headHalf / 2) / headHalf;
            Gui.drawRect(px, midY - h, px + 1, midY + h + 1, color);
        }
        Gui.drawRect(x + headLen, midY - 1, x + size, midY + 1, color);
    }

    private static void drawRightArrow(int x, int y, int size, int color)
    {
        int midY = y + size / 2;
        int headLen = 4;
        int headHalf = 4;
        for (int px = x + size - headLen - 1; px <= x + size - 1; px++)
        {
            int h = (headHalf * (x + size - 1 - px) + headHalf / 2) / headHalf;
            Gui.drawRect(px, midY - h, px + 1, midY + h + 1, color);
        }
        Gui.drawRect(x, midY - 1, x + size - headLen - 1, midY + 1, color);
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawHomeArrow(int x, int y, int size, int color)
    {
        int midY = y + size / 2;
        int headLen = 5;
        int headHalf = 5;
        for (int px = x; px <= x + headLen; px++)
        {
            int h = (headHalf * (px - x) + headHalf / 2) / headHalf;
            Gui.drawRect(px, midY - h, px + 1, midY + h + 1, color);
        }
        Gui.drawRect(x + headLen, midY - 2, x + size, midY + 2, color);
        Gui.drawRect(x + size - 3, midY - 2, x + size, midY + 6, color);
        Gui.drawRect(x + size - 6, midY + 4, x + size, midY + 6, color);
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawCraftingArrow(int x, int y, int w, int h, int color)
    {
        Gui.drawRect(x, y + h / 2 - 1, x + w - h / 2 - 1, y + h / 2 + 2, color);
        for (int k = 0; k < h; k++)
        {
            int dy = Math.abs(k - h / 2);
            int start = x + w - h / 2 - 1 + dy;
            if (start < x + w)
            {
                Gui.drawRect(start, y + k, x + w, y + k + 1, color);
            }
        }
    }

    private static void drawSlot(int x, int y)
    {
        Gui.drawRect(x, y, x + 18, y + 18, SLOT_BORDER);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, SLOT_INNER);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 2, 0xFFA6A6A6);
    }

    private void setHoverContent(ItemStack stack, float contentX, float contentY)
    {
        if (!stack.isEmpty())
        {
            hoverStack = stack;
            hoverContentX = contentX;
            hoverContentY = contentY;
        }
    }

    private static List<String> wrapText(FontRenderer fr, String text, int width)
    {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty())
        {
            lines.add("");
            return lines;
        }
        for (String line : fr.listFormattedStringToWidth(text, width))
        {
            if (fr.getStringWidth(line) <= width)
            {
                lines.add(line);
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < line.length(); i++)
            {
                current.append(line.charAt(i));
                if (fr.getStringWidth(current.toString()) <= width)
                {
                    continue;
                }
                current.deleteCharAt(current.length() - 1);
                if (current.length() > 0)
                {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(line.charAt(i));
            }
            if (current.length() > 0)
            {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private static boolean mouseIn(float mouseX, float mouseY, int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void drawItemStack(ItemStack stack, int x, int y)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0)
        {
            return;
        }

        float localMouseX = (mouseX - bookX) / scale;
        float localMouseY = (mouseY - bookY) / scale;

        if (currentSpread > 0 && mouseIn(localMouseX, localMouseY, NAV_PREV_X, NAV_Y, NAV_BTN_W, NAV_BTN_H))
        {
            currentSpread--;
            return;
        }
        if (currentSpread < maxSpread() && mouseIn(localMouseX, localMouseY, NAV_NEXT_X, NAV_Y, NAV_BTN_W, NAV_BTN_H))
        {
            currentSpread++;
            return;
        }
        if (currentSpread != 0 && mouseIn(localMouseX, localMouseY, NAV_HOME_X, NAV_HOME_Y, NAV_HOME_W, NAV_HOME_H))
        {
            currentSpread = 0;
            return;
        }

        if (currentSpread == 0 && tocPage != null)
        {
            float contentMouseX = (localMouseX - (LEFT_PAGE_X + PAD)) / CONTENT_SCALE;
            float contentMouseY = (localMouseY - (PAGE_Y + PAD)) / CONTENT_SCALE;
            for (Placed placed : tocPage.placed)
            {
                if (placed.element instanceof TocEntryElement)
                {
                    TocEntryElement entry = (TocEntryElement) placed.element;
                    if (entry.hit(contentMouseX, contentMouseY))
                    {
                        currentSpread = entry.targetPage / 2;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel > 0 && currentSpread > 0)
        {
            currentSpread--;
        }
        else if (wheel < 0 && currentSpread < maxSpread())
        {
            currentSpread++;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_LEFT && currentSpread > 0)
        {
            currentSpread--;
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT && currentSpread < maxSpread())
        {
            currentSpread++;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}

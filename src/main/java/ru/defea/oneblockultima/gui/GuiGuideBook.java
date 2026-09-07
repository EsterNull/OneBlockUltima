package ru.defea.oneblockultima.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.lwjgl.glfw.GLFW;
import ru.defea.oneblockultima.guide.GuideBookContent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.defea.oneblockultima.Constants.*;

public class GuiGuideBook extends ModScreen
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

    private static final float CONTENT_SCALE = 1.0F;
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

    public GuiGuideBook()
    {
        super(Component.literal(""));
    }

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
        abstract int measure(Font fr, int width);

        @SuppressWarnings("SameParameterValue")
        abstract void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY);
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
            this(I18n.get(key), GOLD_COLOR);
        }

        @Override
        int measure(Font fr, int width)
        {
            return LINE_H + 4;
        }

        @Override
        void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY)
        {
            int textWidth = fr.width(text);
            int tx = x + (width - textWidth) / 2;
            g.drawString(fr, text, tx, y, color);
            g.fill(x, y + LINE_H + 3, x + width, y + LINE_H + 4, GOLD_COLOR);
        }
    }

    private static class TextElement extends PageElement
    {
        final List<String> lines;
        final int color;

        TextElement(Font fr, String text, int color, int width)
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
        int measure(Font fr, int width)
        {
            return lines.size() * LINE_H;
        }

        @Override
        void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY)
        {
            int cy = y;
            for (String line : lines)
            {
                g.drawString(fr, line, x, cy, color);
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
            this.lines = wrapText(Minecraft.getInstance().font, text, CONTENT_W - 20);
        }

        @Override
        int measure(Font fr, int width)
        {
            return Math.max(16, lines.size() * LINE_H);
        }

        @Override
        void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY)
        {
            g.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
            int cy = y + (16 - lines.size() * LINE_H) / 2;
            for (String line : lines)
            {
                g.drawString(fr, line, x + 20, cy, color);
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

        CommandElement(Font fr, GuideBookContent.CommandInfo command)
        {
            this.command = command;
            this.cheatMarker = command.cheat ? "[" + I18n.get("book.oneblockultima.command.cheat") + "] " : "";
            this.nameLines = wrapText(fr, "/" + command.name, CONTENT_W);
            this.usageLines = wrapText(fr, command.usage, CONTENT_W);
            this.descLines = wrapText(fr, I18n.get(command.descriptionKey), CONTENT_W);
        }

        @Override
        int measure(Font fr, int width)
        {
            return (nameLines.size() + usageLines.size() + descLines.size()) * LINE_H + 4;
        }

        @Override
        void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY)
        {
            int cy = y;
            for (String line : nameLines)
            {
                int nameX = x;
                if (!cheatMarker.isEmpty())
                {
                    g.drawString(fr, cheatMarker, x, cy, REDDISH_COLOR);
                    nameX += fr.width(cheatMarker);
                }
                g.drawString(fr, line, nameX, cy, GOLD_COLOR);
                cy += LINE_H;
            }
            for (String line : usageLines)
            {
                g.drawString(fr, line, x, cy, GRAY_COLOR_5);
                cy += LINE_H;
            }
            for (String line : descLines)
            {
                g.drawString(fr, line, x, cy, LIGHT_GRAY_COLOR_1);
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

        RecipeElement(Font fr, GuideBookContent.Recipe recipe)
        {
            this.recipe = recipe;
            this.nameLines = wrapText(fr, recipe.result.getHoverName().getString(), CONTENT_W);
        }

        @Override
        int measure(Font fr, int width)
        {
            return GRID_SIZE + 4 + nameLines.size() * LINE_H + 6;
        }

        @Override
        void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY)
        {
            for (int i = 0; i < 9; i++)
            {
                int cx = x + (i % 3) * SLOT;
                int cy = y + (i / 3) * SLOT;
                drawSlot(g, cx, cy);
                ItemStack stack = recipe.grid[i];
                if (!stack.isEmpty())
                {
                    drawItemStack(g, stack, cx + 1, cy + 1);
                    if (mouseIn(mouseX, mouseY, cx, cy, SLOT, SLOT))
                    {
                        setHoverContent(stack, cx + (float) SLOT / 2, cy + (float) SLOT / 2);
                    }
                }
            }

            drawCraftingArrow(g, x + ARROW_X, y + (GRID_SIZE - 9) / 2, ARROW_W, 9, GRAY_COLOR_5);

            int rx = x + RESULT_X;
            int ry = y + (GRID_SIZE - SLOT) / 2;
            drawSlot(g, rx, ry);
            drawItemStack(g, recipe.result, rx + 1, ry + 1);
            if (mouseIn(mouseX, mouseY, rx, ry, SLOT, SLOT))
            {
                setHoverContent(recipe.result, rx + (float) SLOT / 2, ry + (float) SLOT / 2);
            }

            int ny = y + GRID_SIZE + 4;
            for (String line : nameLines)
            {
                g.drawString(fr, line, x, ny, WHITE_COLOR_1);
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
        int measure(Font fr, int width)
        {
            return TOC_ROW_H;
        }

        @Override
        void draw(GuiGraphics g, Font fr, int x, int y, int width, float mouseX, float mouseY)
        {
            hitX = x;
            hitY = y;
            hitW = width;
            hitH = TOC_ROW_H;
            boolean hovered = mouseIn(mouseX, mouseY, x, y, width, TOC_ROW_H);
            g.drawString(fr, label, x, y + 4, hovered ? WHITE_COLOR_1 : LIGHT_GRAY_COLOR_1);
            String number = String.valueOf(pageNumber);
            g.drawString(fr, number, x + width - fr.width(number), y + 4, GOLD_COLOR);
        }

        boolean hit(float mouseX, float mouseY)
        {
            return mouseX >= hitX && mouseX < hitX + hitW && mouseY >= hitY && mouseY < hitY + hitH;
        }
    }

    @Override
    public void init()
    {
        super.init();
        

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
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.overview.line1"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.overview.line2"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.overview.line3"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new IconLineElement(COIN_TEXTURE, I18n.get("book.oneblockultima.overview.currency"), GOLD_COLOR));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.overview.sets"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.overview.tip"), GRAY_COLOR_5, CONTENT_W));
        return paginate(elements);
    }

    private List<BookPage> buildCommandPages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.tab.commands"));
        for (GuideBookContent.CommandInfo command : commands)
        {
            elements.add(new CommandElement(Minecraft.getInstance().font, command));
        }
        return paginate(elements);
    }

    private List<BookPage> buildRecipePages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.tab.recipes"));
        if (recipes.isEmpty())
        {
            elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.recipes.empty"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        }
        else
        {
            elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.recipes_count", recipes.size()), GRAY_COLOR_5, CONTENT_W));
            for (GuideBookContent.Recipe recipe : recipes)
            {
                elements.add(new RecipeElement(Minecraft.getInstance().font, recipe));
            }
        }
        return paginate(elements);
    }

    private List<BookPage> buildMechanicPages()
    {
        List<PageElement> elements = new ArrayList<>();
        elements.add(new TitleElement("book.oneblockultima.tab.mechanics"));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.intro"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.break"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.mobs"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.currency"), GOLD_COLOR, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.mode"), GOLD_COLOR, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.sets"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.conditions"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.claim"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.invites"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.mechanics.settings"), LIGHT_GRAY_COLOR_1, CONTENT_W));
        elements.add(new TextElement(Minecraft.getInstance().font, I18n.get("book.oneblockultima.overview.tip"), GRAY_COLOR_5, CONTENT_W));
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
            String label = I18n.get(entry.getKey());
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
                int height = element.measure(Minecraft.getInstance().font, CONTENT_W);
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        drawModBackground(g);
        hoverStack = ItemStack.EMPTY;

        float localMouseX = (mouseX - bookX) / scale;
        float localMouseY = (mouseY - bookY) / scale;

        g.pose().pushPose();
        g.pose().translate(bookX, bookY, 0);
        g.pose().scale(scale, scale, 1.0F);

        RenderSystem.setShaderTexture(0, GUIDE_BOOK_BG_TEXTURE);
        AbstractTexture guideTexture = Minecraft.getInstance().getTextureManager().getTexture(GUIDE_BOOK_BG_TEXTURE);
        if (guideTexture != null)
        {
            guideTexture.setFilter(false, false);
        }

        g.blit(GUIDE_BOOK_BG_TEXTURE, 0, 0, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        int leftIndex = currentSpread * 2;
        if (leftIndex < pages.size())
        {
            drawPage(g, pages.get(leftIndex), LEFT_PAGE_X, localMouseX, localMouseY);
        }
        int rightIndex = leftIndex + 1;
        if (rightIndex < pages.size() && !(currentSpread == 0 && rightIndex == 1))
        {
            drawPage(g, pages.get(rightIndex), RIGHT_PAGE_X, localMouseX, localMouseY);
        }

        drawPageNumbers(g, leftIndex, rightIndex);
        drawNavigation(g, localMouseX, localMouseY);

        g.pose().popPose();

        super.render(g, mouseX, mouseY, partialTicks);

        if (!hoverStack.isEmpty())
        {
            int tipX = bookX + (int) ((hoverPageX + PAD + hoverContentX * CONTENT_SCALE) * scale) + 8;
            int tipY = bookY + (int) ((PAGE_Y + PAD + hoverContentY * CONTENT_SCALE) * scale);
            g.renderTooltip(Minecraft.getInstance().font, hoverStack, tipX, tipY);
        }
    }

    private void drawPage(GuiGraphics g, BookPage page, int pageX, float localMouseX, float localMouseY)
    {
        float contentMouseX = (localMouseX - (pageX + PAD)) / CONTENT_SCALE;
        float contentMouseY = (localMouseY - (PAGE_Y + PAD)) / CONTENT_SCALE;

        g.pose().pushPose();
        g.pose().translate(pageX + PAD, PAGE_Y + PAD, 0);
        g.pose().scale(CONTENT_SCALE, CONTENT_SCALE, 1.0F);

        ItemStack hoverBefore = hoverStack;
        for (Placed placed : page.placed)
        {
            placed.element.draw(g, Minecraft.getInstance().font, 0, placed.y, CONTENT_W, contentMouseX, contentMouseY);
        }
        if (hoverStack != hoverBefore)
        {
            hoverPageX = pageX;
        }

        g.pose().popPose();
    }

    private void drawPageNumbers(GuiGraphics g, int leftIndex, int rightIndex)
    {
        if (leftIndex < pages.size())
        {
            g.drawCenteredString(Minecraft.getInstance().font, String.valueOf(leftIndex + 1), LEFT_PAGE_X + PAGE_W / 2, PAGE_Y + PAGE_H + 1, GRAY_COLOR_5);
        }
        if (rightIndex < pages.size() && !(currentSpread == 0 && rightIndex == 1))
        {
            g.drawCenteredString(Minecraft.getInstance().font, String.valueOf(rightIndex + 1), RIGHT_PAGE_X + PAGE_W / 2, PAGE_Y + PAGE_H + 1, GRAY_COLOR_5);
        }
    }

    private void drawNavigation(GuiGraphics g, float localMouseX, float localMouseY)
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
        drawLeftArrow(g, NAV_PREV_X + NAV_BTN_W / 2 - arrowSize / 2, arrowY, arrowSize, prevColor);
        drawRightArrow(g, NAV_NEXT_X + NAV_BTN_W / 2 - arrowSize / 2, arrowY, arrowSize, nextColor);

        drawHomeArrow(g, NAV_HOME_X + NAV_HOME_W / 2 - 8, NAV_HOME_Y + (NAV_HOME_H - 16) / 2, 16, homeColor);
    }

    private static void drawLeftArrow(GuiGraphics g, int x, int y, int size, int color)
    {
        int midY = y + size / 2;
        int headLen = 4;
        int headHalf = 4;
        for (int px = x; px <= x + headLen; px++)
        {
            int h = (headHalf * (px - x) + headHalf / 2) / headHalf;
            g.fill(px, midY - h, px + 1, midY + h + 1, color);
        }
        g.fill(x + headLen, midY - 1, x + size, midY + 1, color);
    }

    private static void drawRightArrow(GuiGraphics g, int x, int y, int size, int color)
    {
        int midY = y + size / 2;
        int headLen = 4;
        int headHalf = 4;
        for (int px = x + size - headLen - 1; px <= x + size - 1; px++)
        {
            int h = (headHalf * (x + size - 1 - px) + headHalf / 2) / headHalf;
            g.fill(px, midY - h, px + 1, midY + h + 1, color);
        }
        g.fill(x, midY - 1, x + size - headLen - 1, midY + 1, color);
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawHomeArrow(GuiGraphics g, int x, int y, int size, int color)
    {
        int midY = y + size / 2;
        int headLen = 5;
        int headHalf = 5;
        for (int px = x; px <= x + headLen; px++)
        {
            int h = (headHalf * (px - x) + headHalf / 2) / headHalf;
            g.fill(px, midY - h, px + 1, midY + h + 1, color);
        }
        g.fill(x + headLen, midY - 2, x + size, midY + 2, color);
        g.fill(x + size - 3, midY - 2, x + size, midY + 6, color);
        g.fill(x + size - 6, midY + 4, x + size, midY + 6, color);
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawCraftingArrow(GuiGraphics g, int x, int y, int w, int h, int color)
    {
        g.fill(x, y + h / 2 - 1, x + w - h / 2 - 1, y + h / 2 + 2, color);
        for (int k = 0; k < h; k++)
        {
            int dy = Math.abs(k - h / 2);
            int start = x + w - h / 2 - 1 + dy;
            if (start < x + w)
            {
                g.fill(start, y + k, x + w, y + k + 1, color);
            }
        }
    }

    private static void drawSlot(GuiGraphics g, int x, int y)
    {
        g.fill(x, y, x + 18, y + 18, SLOT_BORDER);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_INNER);
        g.fill(x + 1, y + 1, x + 17, y + 2, 0xFFA6A6A6);
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

    private static List<String> wrapText(Font fr, String text, int width)
    {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty())
        {
            lines.add("");
            return lines;
        }
        String[] paragraphs = text.replace("\\n", "\n").split("\n", -1);
        for (String paragraph : paragraphs)
        {
            if (paragraph.isEmpty())
            {
                continue;
            }
            if (!lines.isEmpty())
            {
                lines.add("");
            }
            lines.addAll(wrapLine(fr, paragraph, width));
        }
        if (lines.isEmpty())
        {
            lines.add("");
        }
        return lines;
    }

    private static List<String> wrapLine(Font fr, String line, int width)
    {
        List<String> result = new ArrayList<>();
        if (line == null || line.isEmpty())
        {
            result.add("");
            return result;
        }
        StringBuilder current = new StringBuilder();
        String[] words = line.split(" ", -1);
        for (String word : words)
        {
            String candidate = current.length() == 0 ? word : current.toString() + " " + word;
            if (fr.width(candidate) <= width)
            {
                current.setLength(0);
                current.append(candidate);
            }
            else
            {
                if (current.length() > 0)
                {
                    result.add(current.toString());
                    current.setLength(0);
                }
                if (fr.width(word) <= width)
                {
                    current.append(word);
                }
                else
                {
                    StringBuilder piece = new StringBuilder();
                    for (int i = 0; i < word.length(); i++)
                    {
                        if (piece.length() > 0 && fr.width(piece.toString() + word.charAt(i)) > width)
                        {
                            result.add(piece.toString());
                            piece.setLength(0);
                        }
                        piece.append(word.charAt(i));
                    }
                    current.append(piece);
                }
            }
        }
        if (current.length() > 0)
        {
            result.add(current.toString());
        }
        return result;
    }

    private static boolean mouseIn(float mouseX, float mouseY, int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void drawItemStack(GuiGraphics g, ItemStack stack, int x, int y)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }
        g.renderItem(stack, x, y);
        g.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        if (mouseButton != 0)
        {
            return false;
        }

        float localMouseX = (float) ((mouseX - bookX) / scale);
        float localMouseY = (float) ((mouseY - bookY) / scale);

        if (currentSpread > 0 && mouseIn(localMouseX, localMouseY, NAV_PREV_X, NAV_Y, NAV_BTN_W, NAV_BTN_H))
        {
            currentSpread--;
            return true;
        }
        if (currentSpread < maxSpread() && mouseIn(localMouseX, localMouseY, NAV_NEXT_X, NAV_Y, NAV_BTN_W, NAV_BTN_H))
        {
            currentSpread++;
            return true;
        }
        if (currentSpread != 0 && mouseIn(localMouseX, localMouseY, NAV_HOME_X, NAV_HOME_Y, NAV_HOME_W, NAV_HOME_H))
        {
            currentSpread = 0;
            return true;
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
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (scrollY > 0 && currentSpread > 0)
        {
            currentSpread--;
            return true;
        }
        else if (scrollY < 0 && currentSpread < maxSpread())
        {
            currentSpread++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_LEFT && currentSpread > 0)
        {
            currentSpread--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && currentSpread < maxSpread())
        {
            currentSpread++;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

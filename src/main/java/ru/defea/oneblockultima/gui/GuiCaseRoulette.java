package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.defea.oneblockultima.Constants;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketOpenCase;
import ru.defea.oneblockultima.util.CaseUtil;

import java.util.List;
import java.util.Random;

public class GuiCaseRoulette extends GuiScreen
{
    private static GuiCaseRoulette openInstance;
    private static Integer pendingResult;

    public static void receiveResult(int index)
    {
        if (openInstance != null)
        {
            openInstance.setResult(index);
        }
        else
        {
            pendingResult = index;
        }
    }

    private final ItemStack caseStack;
    private final List<CaseUtil.WeightedStack> contents;
    private int targetIndex = -1;
    private double scrollPos;
    private double startScroll;
    private double targetScroll;
    private int animTicks;
    private int animDuration;
    private boolean finishing;
    private int finishTicks;
    private String rewardName;
    private boolean skipRequested;
    private GuiButton skipButton;
    private final Random rand = new Random();
    private long spinStartNanos;

    private static final int ROW_COUNT = 7;
    private static final int CENTER_ROW = 3;
    private static final int SLOT_SIZE = 44;
    private static final int GAP = 4;
    private static final int ROW_HEIGHT = SLOT_SIZE + GAP;
    private static final int STRIP_HEIGHT = ROW_COUNT * ROW_HEIGHT + 14;
    private static final int STRIP_WIDTH = SLOT_SIZE + 36;

    private int rowCount = ROW_COUNT;
    private int centerRow = CENTER_ROW;
    private int stripHeight = STRIP_HEIGHT;

    public GuiCaseRoulette(ItemStack caseStack)
    {
        this.caseStack = caseStack;
        this.contents = CaseUtil.readContents(caseStack);
    }

    @Override
    public void initGui()
    {
        super.initGui();
        openInstance = this;

        if (pendingResult != null)
        {
            targetIndex = pendingResult;
            pendingResult = null;
        }

        if (contents.isEmpty())
        {
            if (mc.player != null)
            {
                mc.player.sendMessage(new TextComponentTranslation("gui.oneblockultima.case.empty"));
            }
            mc.displayGuiScreen(null);
            return;
        }

        if (targetIndex < 0)
        {
            targetIndex = rand.nextInt(contents.size());
        }

        int avail = height / 2;
        rowCount = Math.max(3, (avail - 14) / ROW_HEIGHT);
        if (rowCount % 2 == 0) rowCount--;
        centerRow = rowCount / 2;
        stripHeight = rowCount * ROW_HEIGHT + 14;

        startSpin();

        int buttonWidth = 110;
        int buttonHeight = 20;
        skipButton = new GuiButton(0, width / 2 - buttonWidth / 2, height - 32, buttonWidth, buttonHeight, I18n.format("gui.oneblockultima.case.skip"));
        buttonList.clear();
        buttonList.add(skipButton);

        NBTTagCompound nbt = caseStack.hasTagCompound() ? caseStack.getTagCompound() : new NBTTagCompound();
        ModMessages.sendToServer(new PacketOpenCase(nbt));
    }

    private void startSpin()
    {
        int size = contents.size();
        // Longer run-up: cross more items for a longer, more impressive spin
        int base = size * 24 + rand.nextInt(size * 6);
        int mod = ((base % size) - targetIndex + size) % size;
        targetScroll = base - mod;
        int spins = 30 + rand.nextInt(6);
        startScroll = targetScroll - (double) size * spins;
        scrollPos = startScroll;
        animTicks = 0;
        spinStartNanos = System.nanoTime();
        animDuration = 160 + rand.nextInt(10);
        finishing = false;
        finishTicks = 0;
        rewardName = null;
        skipRequested = false;
        updateSkipButtonLabel();
    }

    public void setResult(int index)
    {
        if (contents.isEmpty())
        {
            return;
        }
        int size = contents.size();
        int clamped = ((index % size) + size) % size;
        if (targetIndex == clamped && animTicks >= animDuration)
        {
            return;
        }
        targetIndex = clamped;

        double current = scrollPos;
        int currentSlot = (int) Math.floor(current);
        int currentIndex = ((currentSlot % size) + size) % size;
        int steps = ((clamped - currentIndex + size) % size);
        if (steps == 0)
        {
            steps = size;
        }
        steps += size * (5 + rand.nextInt(3));
        targetScroll = currentSlot + steps;
        startScroll = current;
        animTicks = 0;
        spinStartNanos = System.nanoTime();
        animDuration = 160;
        finishing = false;
        rewardName = null;
        skipRequested = false;
        updateSkipButtonLabel();
    }

    private void updateSkipButtonLabel()
    {
        if (skipButton != null)
        {
            skipButton.displayString = I18n.format("gui.oneblockultima.case.skip");
        }
    }

    private static double smoothEase(double t)
    {
        if (t <= 0.0D)
        {
            return 0.0D;
        }
        if (t >= 1.0D)
        {
            return 1.0D;
        }
        // Ease-out quartic: fast start with a long, smooth deceleration
        double u = 1.0D - t;
        return 1.0D - u * u * u * u;
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (contents.isEmpty())
        {
            return;
        }

        if (animTicks < animDuration)
        {
            if (skipRequested)
            {
                animTicks = animDuration;
            }
            else
            {
                animTicks++;
            }
            double t = Math.min(1.0, (double) animTicks / animDuration);
            double eased = smoothEase(t);
            scrollPos = startScroll + (targetScroll - startScroll) * eased;
            if (animTicks >= animDuration)
            {
                int size = contents.size();
                int slot = (int) Math.floor(targetScroll);
                int landed = ((slot % size) + size) % size;
                rewardName = contents.get(landed).stack.getDisplayName();
                finishing = true;
                finishTicks = 0;
                if (skipButton != null)
                {
                    skipButton.visible = false;
                }
            }
        }
        else if (finishing)
        {
            finishTicks++;
            if (finishTicks >= 80)
            {
                mc.displayGuiScreen(null);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();

        int cx = width / 2;
        drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.case.title"), cx, 24, Constants.WHITE_COLOR_2);

        if (contents.isEmpty())
        {
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int size = contents.size();
        double scroll = renderScroll(partialTicks);
        int top = height / 2 - stripHeight / 2;

        // Name of the item currently pointed at by the arrows:
        // switches when the slot's middle crosses the pointer's middle
        int curSlot = (int) Math.round(scroll);
        int curIdx = ((curSlot % size) + size) % size;
        ItemStack curStack = contents.get(curIdx).stack;
        String previewName = curStack.getDisplayName();
        if (curStack.getCount() > 1)
        {
            previewName += " x" + curStack.getCount();
        }
        drawOutlinedCenteredString(previewName, cx, top - 14);

        // Panel background: vertical gradient with gold corner accents
        int stripL = cx - STRIP_WIDTH / 2;
        int stripR = cx + STRIP_WIDTH / 2;
        GlStateManager.disableTexture2D();
        drawGradientRect(stripL, top, stripR, top + stripHeight, Constants.CASE_BG_GRADIENT_TOP, Constants.CASE_BG_GRADIENT_BOTTOM);
        GlStateManager.enableTexture2D();
        drawRect(stripL, top, stripL + 6, top + 2, Constants.GOLD_COLOR);
        drawRect(stripL, top, stripL + 2, top + 6, Constants.GOLD_COLOR);
        drawRect(stripR - 6, top, stripR, top + 2, Constants.GOLD_COLOR);
        drawRect(stripR - 2, top, stripR, top + 6, Constants.GOLD_COLOR);
        drawRect(stripL, top + stripHeight - 2, stripL + 6, top + stripHeight, Constants.GOLD_COLOR);
        drawRect(stripL, top + stripHeight - 6, stripL + 2, top + stripHeight, Constants.GOLD_COLOR);
        drawRect(stripR - 6, top + stripHeight - 2, stripR, top + stripHeight, Constants.GOLD_COLOR);
        drawRect(stripR - 2, top + stripHeight - 6, stripR, top + stripHeight, Constants.GOLD_COLOR);

        int stripLeft = cx - STRIP_WIDTH / 2;
        int innerTop = top + 7;
        int x = cx - SLOT_SIZE / 2;
        int clipHeight = (rowCount - 1) * ROW_HEIGHT + SLOT_SIZE;
        long tMs = System.currentTimeMillis();

        // Sparkles on the gray background only: drawn before the cells, which
        // then overdraw anything beneath the slot area
        if (!finishing && animTicks < animDuration)
        {
            java.util.Random sparkRand = new java.util.Random(tMs / 90L);
            for (int i = 0; i < 12; i++)
            {
                int sx = stripLeft + 3 + sparkRand.nextInt(Math.max(1, STRIP_WIDTH - 6));
                int sy = innerTop + 2 + sparkRand.nextInt(Math.max(1, clipHeight - 4));
                int ssz = 1 + sparkRand.nextInt(2);
                int sa = 120 + sparkRand.nextInt(120);
                int scol = (i % 3 == 0) ? Constants.CASE_SPARK_WHITE : Constants.CASE_GOLD_RGB;
                drawRect(sx, sy, sx + ssz, sy + ssz, (sa << 24) | scol);
            }
        }

        // Continuously sliding cells, clipped exactly to the fixed slots' extent
        enableStripScissor(stripLeft + 2, innerTop, STRIP_WIDTH - 4, clipHeight);
        double frac = scroll - Math.floor(scroll);
        float dy = (float) (frac * (double) ROW_HEIGHT);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, dy, 0.0F);

        for (int r = -1; r <= rowCount; r++)
        {
            int index = (int) Math.floor(scroll) + (centerRow - r);
            int slotIdx = ((index % size) + size) % size;
            int y = innerTop + r * ROW_HEIGHT;

            drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, Constants.DARK_GRAY_COLOR_2);
            drawRect(x, y, x + SLOT_SIZE, y + 1, Constants.GRAY_COLOR_8);
            drawRect(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, Constants.GRAY_COLOR_8);
            drawRect(x, y, x + 1, y + SLOT_SIZE, Constants.GRAY_COLOR_8);
            drawRect(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, Constants.GRAY_COLOR_8);

            ItemStack stack = contents.get(slotIdx).stack;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.enableGUIStandardItemLighting();
            int ix = x + (SLOT_SIZE - 16) / 2;
            int iy = y + (SLOT_SIZE - 16) / 2;
            mc.getRenderItem().renderItemIntoGUI(stack, ix, iy);
            RenderHelper.disableStandardItemLighting();

            if (stack.getCount() > 1)
            {
                String cnt = "x" + stack.getCount();
                drawOutlinedString(cnt, x + SLOT_SIZE - fontRenderer.getStringWidth(cnt) - 3, y + SLOT_SIZE - 11, Constants.WHITE_COLOR_1);
            }
        }

        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Static frames above the sliding cells. Gui.drawRect hardcodes z=0 while items
        // render at z~50+, so depth testing must be off here for draw order to win
        GlStateManager.disableDepth();

        // Pulsing golden glow inside the center slot
        int centerY = innerTop + centerRow * ROW_HEIGHT;
        float pulse = 0.5F + 0.5F * (float) Math.sin(tMs % 100000L / 180.0D);
        int glowA = finishing ? 0x46 : (int) (0x20 + 0x18 * pulse);
        drawRect(x, centerY, x + SLOT_SIZE, centerY + SLOT_SIZE, (glowA << 24) | Constants.CASE_GOLD_RGB);

        drawRectOutline(x - 2, innerTop - 2, x + SLOT_SIZE + 2, innerTop + SLOT_SIZE + 2, 2, Constants.GRAY_COLOR_1);
        drawRectOutline(x - 2, innerTop + (rowCount - 1) * ROW_HEIGHT - 2, x + SLOT_SIZE + 2, innerTop + (rowCount - 1) * ROW_HEIGHT + SLOT_SIZE + 2, 2, Constants.GRAY_COLOR_1);
        drawRectOutline(x - 2, centerY - 2, x + SLOT_SIZE + 2, centerY + SLOT_SIZE + 2, 2, Constants.GOLD_COLOR);
        GlStateManager.enableDepth();

        // Golden flash when the reward is revealed
        if (finishing && finishTicks < 15)
        {
            int fa = (14 - finishTicks) * 12;
            drawRect(stripL, top, stripR, top + stripHeight, (fa << 24) | Constants.CASE_GOLD_RGB);
        }

        // Arrow indicators on the sides, pointing into the center slot
        int arrowY = centerY + (SLOT_SIZE - 9) / 2;
        drawCenteredString(fontRenderer, "\u25B6", x - 11, arrowY, Constants.GOLD_COLOR);
        drawCenteredString(fontRenderer, "\u25C0", x + SLOT_SIZE + 11, arrowY, Constants.GOLD_COLOR);

        if (finishing && rewardName != null)
        {
            String reward = I18n.format("gui.oneblockultima.case.reward", rewardName);
            drawCenteredString(fontRenderer, reward, cx, top + stripHeight + 10, Constants.SUCCESS_COLOR);
            drawCenteredString(fontRenderer, I18n.format("gui.oneblockultima.case.click_close"), cx, top + stripHeight + 22, Constants.WHITE_COLOR_2);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static void drawRectOutline(int left, int top, int right, int bottom, @SuppressWarnings("SameParameterValue") int thickness, int color)
    {
        drawRect(left, top, right, top + thickness, color);
        drawRect(left, bottom - thickness, right, bottom, color);
        drawRect(left, top + thickness, left + thickness, bottom - thickness, color);
        drawRect(right - thickness, top + thickness, right, bottom - thickness, color);
    }

    private void enableStripScissor(int x, int y, @SuppressWarnings("SameParameterValue") int w, int h)
    {
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, w * f, h * f);
    }

    private double renderScroll(float partialTicks)
    {
        if (animTicks >= animDuration)
        {
            return targetScroll;
        }
        double elapsedMs = (System.nanoTime() - spinStartNanos) / 1.0E6D + partialTicks * 50.0D;
        double t = Math.min(1.0D, elapsedMs / ((double) animDuration * 50.0D));
        return startScroll + (targetScroll - startScroll) * smoothEase(t);
    }

    private void drawOutlinedString(String text, float tx, int y, int color)
    {
        String outline = "\u00A70" + text;
        fontRenderer.drawString(outline, tx - 1.0F, y, Constants.WHITE_COLOR_1, false);
        fontRenderer.drawString(outline, tx + 1.0F, y, Constants.WHITE_COLOR_1, false);
        fontRenderer.drawString(outline, tx, y - 1.0F, Constants.WHITE_COLOR_1, false);
        fontRenderer.drawString(outline, tx, y + 1.0F, Constants.WHITE_COLOR_1, false);
        fontRenderer.drawString(text, tx, y, color, true);
    }

    private void drawOutlinedCenteredString(String text, int cx, int y)
    {
        drawOutlinedString(text, cx - fontRenderer.getStringWidth(text) / 2.0F, y, Constants.GOLD_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException
    {
        if (finishing && mouseButton == 0)
        {
            mc.displayGuiScreen(null);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws java.io.IOException
    {
        if (button.id == 0)
        {
            skipRequested = true;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E)
        {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_SPACE && !finishing)
        {
            skipRequested = true;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed()
    {
        if (openInstance == this)
        {
            openInstance = null;
        }
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
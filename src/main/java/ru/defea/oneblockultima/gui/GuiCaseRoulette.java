package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

import ru.defea.oneblockultima.Constants;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketOpenCase;
import ru.defea.oneblockultima.util.CaseUtil;

public class GuiCaseRoulette extends ModScreen
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
    private Button skipButton;
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
        super(Component.literal("case_roulette"));
        this.caseStack = caseStack;
        this.contents = CaseUtil.readContents(caseStack, net.minecraft.client.Minecraft.getInstance().level.registryAccess());
    }

    @Override
    public void init()
    {
        super.init();
        openInstance = this;

        if (pendingResult != null)
        {
            targetIndex = pendingResult;
            pendingResult = null;
        }

        if (contents.isEmpty())
        {
            if (this.minecraft.player != null)
            {
                this.minecraft.player.sendSystemMessage(Component.translatable("gui.oneblockultima.case.empty"));
            }
            this.minecraft.setScreen(null);
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
        skipButton = Button.builder(Component.literal(I18n.get("gui.oneblockultima.case.skip")), b -> skipRequested = true)
                .bounds(width / 2 - buttonWidth / 2, height - 32, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(skipButton);

        CompoundTag nbt = (CompoundTag) caseStack.saveOptional(net.minecraft.core.RegistryAccess.EMPTY);
        ModMessages.sendToServer(new PacketOpenCase(nbt));
    }

    private void startSpin()
    {
        int size = contents.size();
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
            skipButton.setMessage(Component.literal(I18n.get("gui.oneblockultima.case.skip")));
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
        double u = 1.0D - t;
        return 1.0D - u * u * u * u;
    }

    @Override
    public void tick()
    {
        super.tick();
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
                rewardName = contents.get(landed).stack.getHoverName().getString();
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
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        this.drawModBackground(g);

        int cx = width / 2;
        g.drawCenteredString(this.font, I18n.get("gui.oneblockultima.case.title"), cx, 24, Constants.WHITE_COLOR_2);

        if (contents.isEmpty())
        {
            super.render(g, mouseX, mouseY, partialTicks);
            return;
        }

        int size = contents.size();
        double scroll = renderScroll(partialTicks);
        int top = height / 2 - stripHeight / 2;

        int curSlot = (int) Math.round(scroll);
        int curIdx = ((curSlot % size) + size) % size;
        ItemStack curStack = contents.get(curIdx).stack;
        String previewName = curStack.getHoverName().getString();
        if (curStack.getCount() > 1)
        {
            previewName += " x" + curStack.getCount();
        }
        drawOutlinedCenteredString(g, previewName, cx, top - 14);

        int stripL = cx - STRIP_WIDTH / 2;
        int stripR = cx + STRIP_WIDTH / 2;
        g.fillGradient(stripL, top, stripR, top + stripHeight, Constants.CASE_BG_GRADIENT_TOP, Constants.CASE_BG_GRADIENT_BOTTOM);
        g.fill(stripL, top, stripL + 6, top + 2, Constants.GOLD_COLOR);
        g.fill(stripL, top, stripL + 2, top + 6, Constants.GOLD_COLOR);
        g.fill(stripR - 6, top, stripR, top + 2, Constants.GOLD_COLOR);
        g.fill(stripR - 2, top, stripR, top + 6, Constants.GOLD_COLOR);
        g.fill(stripL, top + stripHeight - 2, stripL + 6, top + stripHeight, Constants.GOLD_COLOR);
        g.fill(stripL, top + stripHeight - 6, stripL + 2, top + stripHeight, Constants.GOLD_COLOR);
        g.fill(stripR - 6, top + stripHeight - 2, stripR, top + stripHeight, Constants.GOLD_COLOR);
        g.fill(stripR - 2, top + stripHeight - 6, stripR, top + stripHeight, Constants.GOLD_COLOR);

        int stripLeft = cx - STRIP_WIDTH / 2;
        int innerTop = top + 7;
        int x = cx - SLOT_SIZE / 2;
        int clipHeight = (rowCount - 1) * ROW_HEIGHT + SLOT_SIZE;
        long tMs = System.currentTimeMillis();

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
                g.fill(sx, sy, sx + ssz, sy + ssz, (sa << 24) | scol);
            }
        }

        g.enableScissor(stripLeft + 2, innerTop, stripLeft + 2 + STRIP_WIDTH - 4, innerTop + clipHeight);
        double frac = scroll - Math.floor(scroll);
        float dy = (float) (frac * (double) ROW_HEIGHT);

        g.pose().pushPose();
        g.pose().translate(0.0D, dy, 0.0D);

        for (int r = -1; r <= rowCount; r++)
        {
            int index = (int) Math.floor(scroll) + (centerRow - r);
            int slotIdx = ((index % size) + size) % size;
            int y = innerTop + r * ROW_HEIGHT;

            g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, Constants.DARK_GRAY_COLOR_2);
            g.fill(x, y, x + SLOT_SIZE, y + 1, Constants.GRAY_COLOR_8);
            g.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, Constants.GRAY_COLOR_8);
            g.fill(x, y, x + 1, y + SLOT_SIZE, Constants.GRAY_COLOR_8);
            g.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, Constants.GRAY_COLOR_8);

            ItemStack stack = contents.get(slotIdx).stack;
            int ix = x + (SLOT_SIZE - 16) / 2;
            int iy = y + (SLOT_SIZE - 16) / 2;
            g.renderFakeItem(stack, ix, iy);

            if (stack.getCount() > 1)
            {
                String cnt = "x" + stack.getCount();
                drawOutlinedString(g, cnt, x + SLOT_SIZE - this.font.width(cnt) - 3, y + SLOT_SIZE - 11, Constants.WHITE_COLOR_1);
            }
        }

        g.pose().popPose();
        g.disableScissor();

        RenderSystem.disableDepthTest();

        int centerY = innerTop + centerRow * ROW_HEIGHT;
        float pulse = 0.5F + 0.5F * (float) Math.sin(tMs % 100000L / 180.0D);
        int glowA = finishing ? 0x46 : (int) (0x20 + 0x18 * pulse);
        g.fill(x, centerY, x + SLOT_SIZE, centerY + SLOT_SIZE, (glowA << 24) | Constants.CASE_GOLD_RGB);

        drawRectOutline(g, x - 2, innerTop - 2, x + SLOT_SIZE + 2, innerTop + SLOT_SIZE + 2, 2, Constants.GRAY_COLOR_1);
        drawRectOutline(g, x - 2, innerTop + (rowCount - 1) * ROW_HEIGHT - 2, x + SLOT_SIZE + 2, innerTop + (rowCount - 1) * ROW_HEIGHT + SLOT_SIZE + 2, 2, Constants.GRAY_COLOR_1);
        drawRectOutline(g, x - 2, centerY - 2, x + SLOT_SIZE + 2, centerY + SLOT_SIZE + 2, 2, Constants.GOLD_COLOR);
        RenderSystem.enableDepthTest();

        if (finishing && finishTicks < 15)
        {
            int fa = (14 - finishTicks) * 12;
            g.fill(stripL, top, stripR, top + stripHeight, (fa << 24) | Constants.CASE_GOLD_RGB);
        }

        int arrowY = centerY + (SLOT_SIZE - 9) / 2;
        g.drawCenteredString(this.font, "\u25B6", x - 11, arrowY, Constants.GOLD_COLOR);
        g.drawCenteredString(this.font, "\u25C0", x + SLOT_SIZE + 11, arrowY, Constants.GOLD_COLOR);

        if (finishing && rewardName != null)
        {
            int rewardCount = contents.get(targetIndex).stack.getCount();
            String reward = I18n.get("gui.oneblockultima.case.reward", rewardName, String.valueOf(rewardCount));
            g.drawCenteredString(this.font, reward, cx, top + stripHeight + 10, Constants.SUCCESS_COLOR);
            g.drawCenteredString(this.font, I18n.get("gui.oneblockultima.case.click_close"), cx, top + stripHeight + 22, Constants.WHITE_COLOR_2);
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    private static void drawRectOutline(GuiGraphics g, int left, int top, int right, int bottom, int thickness, int color)
    {
        g.fill(left, top, right, top + thickness, color);
        g.fill(left, bottom - thickness, right, bottom, color);
        g.fill(left, top + thickness, left + thickness, bottom - thickness, color);
        g.fill(right - thickness, top + thickness, right, bottom - thickness, color);
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

    private void drawOutlinedString(GuiGraphics g, String text, float tx, int y, int color)
    {
        String outline = "\u00A70" + text;
        g.drawString(this.font, outline, (int) (tx - 1.0F), y, Constants.WHITE_COLOR_1, false);
        g.drawString(this.font, outline, (int) (tx + 1.0F), y, Constants.WHITE_COLOR_1, false);
        g.drawString(this.font, outline, (int) tx, y - 1, Constants.WHITE_COLOR_1, false);
        g.drawString(this.font, outline, (int) tx, y + 1, Constants.WHITE_COLOR_1, false);
        g.drawString(this.font, text, (int) tx, y, color, true);
    }

    private void drawOutlinedCenteredString(GuiGraphics g, String text, int cx, int y)
    {
        drawOutlinedString(g, text, cx - this.font.width(text) / 2.0F, y, Constants.GOLD_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        if (finishing && mouseButton == 0)
        {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E)
        {
            this.minecraft.setScreen(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE && !finishing)
        {
            skipRequested = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose()
    {
        if (openInstance == this)
        {
            openInstance = null;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}

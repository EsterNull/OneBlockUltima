package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.layout.*;

import java.io.IOException;

public class GuiMiscSettings extends GuiScreen {
    private static final int BUTTON_BACK = 0;
    private static final int BUTTON_SAVE = 1;
    private static final int BUTTON_MOB_WORLD_GENERATION = 2;
    private static final int BUTTON_DEBUG_MODE = 3;
    private static final int BUTTON_RESET = 4;

    private static final int MIN_INVITE_TICKS = 20;
    private static final int MAX_INVITE_TICKS = 72000;
    private static final int INVITE_STEP_TICKS = 20;
    private static final int MIN_BREAK_COOLDOWN_TICKS = 0;
    private static final int MAX_BREAK_COOLDOWN_TICKS = 12000;
    private static final int BREAK_COOLDOWN_STEP_TICKS = 1;
    private static final int MIN_MOB_SPAWN_PERCENT = 0;
    private static final int MAX_MOB_SPAWN_PERCENT = 100;
    private static final int MOB_SPAWN_STEP_PERCENT = 1;
    private static final int MIN_GENERATOR_MEMBERS = 0;
    private static final int MAX_GENERATOR_MEMBERS = 100;
    private static final int GENERATOR_MEMBERS_STEP = 1;

    private final GuiScreen parent;
    private ViewFactory factory;
    private ModSettings settings;

    private boolean mobWorldGeneration;
    private boolean debugMode;
    private int inviteDurationTicks;
    private int breakCooldownTicks;
    private int mobSpawnPercent;
    private int generatorMembers;

    private ButtonToggleElement mobWorldGenerationToggle;
    private ButtonToggleElement debugModeToggle;
    private StepperElement inviteDurationStepper;
    private StepperElement breakCooldownStepper;
    private StepperElement mobSpawnPercentStepper;
    private StepperElement generatorMembersStepper;

    public GuiMiscSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        settings = ModSettings.get();
        mobWorldGeneration = settings.getMobWorldGeneration();
        debugMode = settings.isDebugMode();
        inviteDurationTicks = settings.getInviteDurationTicks();
        breakCooldownTicks = settings.getNonPlayerBreakCooldownTicks();
        mobSpawnPercent = settings.getMaxMobSpawnPercent();
        generatorMembers = settings.getMaxGeneratorMembers();

        buildView();
    }

    private void buildView()
    {
        factory = new ViewFactory(width, height)
                .margin(8).padding(2)
                .gap(6)
                .align(Alignment.CENTER)
                .centerVertical()
                .fitContent();

        factory.title("gui.oneblockultima.misc.title");

        RowElement firstLineControls = factory.row(Alignment.LEFT).gap(8).align(Alignment.LEFT);
        mobWorldGenerationToggle = firstLineControls.buttonToggle(BUTTON_MOB_WORLD_GENERATION, mobWorldGeneration)
                .label(I18n.format("gui.oneblockultima.misc.mob_world_generation"));

        RowElement debugModeControls = factory.row(Alignment.LEFT).gap(8).align(Alignment.LEFT);
        debugModeToggle = debugModeControls.buttonToggle(BUTTON_DEBUG_MODE, debugMode)
                .label(I18n.format("gui.oneblockultima.misc.debug_mode"));

        String inviteControlString = I18n.format("gui.oneblockultima.misc.invite_duration");
        RowElement inviteControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        inviteControls.label(inviteControlString);
        inviteDurationStepper = new StepperElement()
                .value(inviteDurationTicks)
                .min(MIN_INVITE_TICKS)
                .max(MAX_INVITE_TICKS)
                .step(INVITE_STEP_TICKS)
                .fieldWidth(50)
                .gap(4);
        inviteControls.add(inviteDurationStepper);

        RowElement breakCooldownControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        breakCooldownControls.label(I18n.format("gui.oneblockultima.misc.non_player_break_cooldown"));
        breakCooldownStepper = new StepperElement()
                .value(breakCooldownTicks)
                .min(MIN_BREAK_COOLDOWN_TICKS)
                .max(MAX_BREAK_COOLDOWN_TICKS)
                .step(BREAK_COOLDOWN_STEP_TICKS)
                .fieldWidth(50)
                .gap(4);
        breakCooldownControls.add(breakCooldownStepper);

        RowElement mobSpawnPercentControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        mobSpawnPercentControls.label(I18n.format("gui.oneblockultima.misc.max_mob_spawn_percent"));
        mobSpawnPercentStepper = new StepperElement()
                .value(mobSpawnPercent)
                .min(MIN_MOB_SPAWN_PERCENT)
                .max(MAX_MOB_SPAWN_PERCENT)
                .step(MOB_SPAWN_STEP_PERCENT)
                .fieldWidth(50)
                .gap(4);
        mobSpawnPercentControls.add(mobSpawnPercentStepper);

        RowElement generatorMembersControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        generatorMembersControls.label(I18n.format("gui.oneblockultima.misc.max_generator_members"));
        generatorMembersStepper = new StepperElement()
                .value(generatorMembers)
                .min(MIN_GENERATOR_MEMBERS)
                .max(MAX_GENERATOR_MEMBERS)
                .step(GENERATOR_MEMBERS_STEP)
                .fieldWidth(50)
                .gap(4);
        generatorMembersControls.add(generatorMembersStepper);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.format("gui.oneblockultima.cancel"));
        btnRow.button(BUTTON_RESET, I18n.format("gui.oneblockultima.reset_default"));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.format("gui.oneblockultima.save")));

        factory.build(buttonList, fontRenderer);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == BUTTON_BACK)
        {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_SAVE) {
            settings.setMobWorldGeneration(mobWorldGeneration);
            settings.setDebugMode(debugMode);
            settings.setInviteDurationTicks(inviteDurationStepper.getValue());
            settings.setNonPlayerBreakCooldownTicks(breakCooldownStepper.getValue());
            settings.setMaxMobSpawnPercent(mobSpawnPercentStepper.getValue());
            settings.setMaxGeneratorMembers(generatorMembersStepper.getValue());
            BlockSetConfig.invalidateComputedLevels();
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_MOB_WORLD_GENERATION) {
            mobWorldGeneration = !mobWorldGeneration;
            mobWorldGenerationToggle.toggle();
        }
        if (button.id == BUTTON_DEBUG_MODE) {
            debugMode = !debugMode;
            debugModeToggle.toggle();
        }
        if (button.id == BUTTON_RESET) {
            mobWorldGeneration = false;
            debugMode = false;
            inviteDurationTicks = 1200;
            breakCooldownTicks = 20;
            mobSpawnPercent = 10;
            generatorMembers = 0;
            if (mobWorldGenerationToggle != null) mobWorldGenerationToggle.stateTriggered(false);
            if (debugModeToggle != null) debugModeToggle.stateTriggered(false);
            if (inviteDurationStepper != null) inviteDurationStepper.setValue(inviteDurationTicks);
            if (breakCooldownStepper != null) breakCooldownStepper.setValue(breakCooldownTicks);
            if (mobSpawnPercentStepper != null) mobSpawnPercentStepper.setValue(mobSpawnPercent);
            if (generatorMembersStepper != null) generatorMembersStepper.setValue(generatorMembers);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (factory != null) factory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (factory != null) factory.updateScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        if (factory != null) factory.draw(fontRenderer, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

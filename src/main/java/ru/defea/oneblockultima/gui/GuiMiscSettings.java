package ru.defea.oneblockultima.gui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.layout.*;

import java.io.IOException;

public class GuiMiscSettings extends ModScreen {
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

    private final Screen parent;
    private ViewFactory factory;
    private ModSettings settings;
    private GuiGraphics gfx;

    private boolean mobWorldGeneration;
    private boolean debugMode;
    private int inviteDurationTicks;
    private int breakCooldownTicks;
    private int mobSpawnPercent;
    private int generatorMembers;
    private double caseDropPercent;

    private ButtonToggleElement mobWorldGenerationToggle;
    private ButtonToggleElement debugModeToggle;
    private StepperElement inviteDurationStepper;
    private StepperElement breakCooldownStepper;
    private StepperElement mobSpawnPercentStepper;
    private StepperElement generatorMembersStepper;
    private DoubleStepperElement caseDropPercentStepper;

    public GuiMiscSettings(Screen parent)
    {
        super(Component.literal(""));
        this.parent = parent;
    }

    @Override
    public void init()
    {
        this.renderables.clear();
        this.children().clear();

        settings = ModSettings.get();
        mobWorldGeneration = settings.getMobWorldGeneration();
        debugMode = settings.isDebugMode();
        inviteDurationTicks = settings.getInviteDurationTicks();
        breakCooldownTicks = settings.getNonPlayerBreakCooldownTicks();
        mobSpawnPercent = settings.getMaxMobSpawnPercent();
        generatorMembers = settings.getMaxGeneratorMembers();
        caseDropPercent = settings.getCaseDropPercent();

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
                .label(I18n.get("gui.oneblockultima.misc.mob_world_generation"))
                .onPress(() -> actionPerformed(BUTTON_MOB_WORLD_GENERATION));

        RowElement debugModeControls = factory.row(Alignment.LEFT).gap(8).align(Alignment.LEFT);
        debugModeToggle = debugModeControls.buttonToggle(BUTTON_DEBUG_MODE, debugMode)
                .label(I18n.get("gui.oneblockultima.misc.debug_mode"))
                .onPress(() -> actionPerformed(BUTTON_DEBUG_MODE));

        String inviteControlString = I18n.get("gui.oneblockultima.misc.invite_duration");
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
        breakCooldownControls.label(I18n.get("gui.oneblockultima.misc.non_player_break_cooldown"));
        breakCooldownStepper = new StepperElement()
                .value(breakCooldownTicks)
                .min(MIN_BREAK_COOLDOWN_TICKS)
                .max(MAX_BREAK_COOLDOWN_TICKS)
                .step(BREAK_COOLDOWN_STEP_TICKS)
                .fieldWidth(50)
                .gap(4);
        breakCooldownControls.add(breakCooldownStepper);

        RowElement mobSpawnPercentControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        mobSpawnPercentControls.label(I18n.get("gui.oneblockultima.misc.max_mob_spawn_percent"));
        mobSpawnPercentStepper = new StepperElement()
                .value(mobSpawnPercent)
                .min(MIN_MOB_SPAWN_PERCENT)
                .max(MAX_MOB_SPAWN_PERCENT)
                .step(MOB_SPAWN_STEP_PERCENT)
                .fieldWidth(50)
                .gap(4);
        mobSpawnPercentControls.add(mobSpawnPercentStepper);

        RowElement generatorMembersControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        generatorMembersControls.label(I18n.get("gui.oneblockultima.misc.max_generator_members"));
        generatorMembersStepper = new StepperElement()
                .value(generatorMembers)
                .min(MIN_GENERATOR_MEMBERS)
                .max(MAX_GENERATOR_MEMBERS)
                .step(GENERATOR_MEMBERS_STEP)
                .fieldWidth(50)
                .gap(4);
        generatorMembersControls.add(generatorMembersStepper);

        RowElement caseDropPercentControls = factory.row(Alignment.SPACE_BETWEEN).stretchToContent();
        caseDropPercentControls.label(I18n.get("gui.oneblockultima.misc.case_drop_percent"));
        caseDropPercentStepper = new DoubleStepperElement()
                .value(caseDropPercent)
                .min(0.0)
                .max(100.0)
                .step(1.0)
                .decimals(0)
                .fieldWidth(50)
                .gap(4);
        caseDropPercentControls.add(caseDropPercentStepper);

        RowElement btnRow = factory.row(Alignment.CENTER).gap(4);
        btnRow.button(BUTTON_BACK, I18n.get("gui.oneblockultima.cancel")).onPress(() -> actionPerformed(BUTTON_BACK));
        btnRow.button(BUTTON_RESET, I18n.get("gui.oneblockultima.reset_default")).onPress(() -> actionPerformed(BUTTON_RESET));
        btnRow.add(new SuccessButtonElement(BUTTON_SAVE, I18n.get("gui.oneblockultima.save")).onPress(() -> actionPerformed(BUTTON_SAVE)));

        factory.build(this, Minecraft.getInstance().font);
    }

    private void actionPerformed(int id)
    {
        if (id == BUTTON_BACK)
        {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        if (id == BUTTON_SAVE) {
            settings.setMobWorldGeneration(mobWorldGeneration);
            settings.setDebugMode(debugMode);
            settings.setInviteDurationTicks(inviteDurationStepper.getValue());
            settings.setNonPlayerBreakCooldownTicks(breakCooldownStepper.getValue());
            settings.setMaxMobSpawnPercent(mobSpawnPercentStepper.getValue());
            settings.setMaxGeneratorMembers(generatorMembersStepper.getValue());
            settings.setCaseDropPercent(caseDropPercentStepper.getValue());
            BlockSetConfig.invalidateComputedLevels();
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        if (id == BUTTON_MOB_WORLD_GENERATION) {
            mobWorldGeneration = !mobWorldGeneration;
            mobWorldGenerationToggle.toggle();
        }
        if (id == BUTTON_DEBUG_MODE) {
            debugMode = !debugMode;
            debugModeToggle.toggle();
        }
        if (id == BUTTON_RESET) {
            mobWorldGeneration = false;
            debugMode = false;
            inviteDurationTicks = 1200;
            breakCooldownTicks = 20;
            mobSpawnPercent = 10;
            generatorMembers = 0;
            caseDropPercent = 1.0;
            if (mobWorldGenerationToggle != null) mobWorldGenerationToggle.stateTriggered(false);
            if (debugModeToggle != null) debugModeToggle.stateTriggered(false);
            if (inviteDurationStepper != null) inviteDurationStepper.setValue(inviteDurationTicks);
            if (breakCooldownStepper != null) breakCooldownStepper.setValue(breakCooldownTicks);
            if (mobSpawnPercentStepper != null) mobSpawnPercentStepper.setValue(mobSpawnPercent);
            if (generatorMembersStepper != null) generatorMembersStepper.setValue(generatorMembers);
            if (caseDropPercentStepper != null) caseDropPercentStepper.setValue(caseDropPercent);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        if (factory != null) factory.mouseClicked((int) mouseX, (int) mouseY, mouseButton);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void tick()
    {
        if (factory != null) factory.updateScreen();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        this.gfx = g;
        drawModBackground(g);
        if (factory != null) factory.draw(g, Minecraft.getInstance().font, mouseX, mouseY, partialTicks);
        super.render(g, mouseX, mouseY, partialTicks);
    }
}

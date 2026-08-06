package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.ModSettings;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

public class ModSettingsTest {

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    private ModSettings newInstance() throws Exception {
        Constructor<ModSettings> constructor = ModSettings.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    // --- BalancePosition enum ---

    @Test
    public void balancePositionHasEightValues() {
        assertEquals(8, ModSettings.BalancePosition.values().length);
    }

    @Test
    public void balancePositionContainsAllCorners() {
        assertNotNull(ModSettings.BalancePosition.TOP_LEFT);
        assertNotNull(ModSettings.BalancePosition.TOP_RIGHT);
        assertNotNull(ModSettings.BalancePosition.BOTTOM_LEFT);
        assertNotNull(ModSettings.BalancePosition.BOTTOM_RIGHT);
    }

    @Test
    public void balancePositionContainsAllSides() {
        assertNotNull(ModSettings.BalancePosition.TOP);
        assertNotNull(ModSettings.BalancePosition.BOTTOM);
        assertNotNull(ModSettings.BalancePosition.LEFT);
        assertNotNull(ModSettings.BalancePosition.RIGHT);
    }

    // --- Default values ---

    @Test
    public void defaultBalancePositionIsTopRight() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(ModSettings.BalancePosition.TOP_RIGHT, settings.getBalancePosition());
    }

    @Test
    public void defaultHOffsetIsFive() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(5, settings.getHOffset());
    }

    @Test
    public void defaultVOffsetIsThree() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(3, settings.getVOffset());
    }

    // --- Setters and getters ---

    @Test
    public void setBalancePositionChangesValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setBalancePosition(ModSettings.BalancePosition.BOTTOM_LEFT);
        assertEquals(ModSettings.BalancePosition.BOTTOM_LEFT, settings.getBalancePosition());
    }

    @Test
    public void setHOffsetChangesValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setHOffset(20);
        assertEquals(20, settings.getHOffset());
    }

    @Test
    public void setVOffsetChangesValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setVOffset(15);
        assertEquals(15, settings.getVOffset());
    }

    @Test
    public void setNegativeOffsets() throws Exception {
        ModSettings settings = newInstance();
        settings.setHOffset(-10);
        settings.setVOffset(-5);
        assertEquals(-10, settings.getHOffset());
        assertEquals(-5, settings.getVOffset());
    }

    @Test
    public void setZeroOffsets() throws Exception {
        ModSettings settings = newInstance();
        settings.setHOffset(0);
        settings.setVOffset(0);
        assertEquals(0, settings.getHOffset());
        assertEquals(0, settings.getVOffset());
    }

    // --- Per-position offsets ---

    @Test
    public void offsetsAreStoredPerPositionIndependently() throws Exception {
        ModSettings settings = newInstance();
        settings.setHOffset(ModSettings.BalancePosition.TOP_LEFT, 7);
        settings.setVOffset(ModSettings.BalancePosition.TOP_LEFT, 4);
        settings.setHOffset(ModSettings.BalancePosition.BOTTOM_RIGHT, 30);
        settings.setVOffset(ModSettings.BalancePosition.BOTTOM_RIGHT, -15);

        assertEquals(7, settings.getHOffset(ModSettings.BalancePosition.TOP_LEFT));
        assertEquals(4, settings.getVOffset(ModSettings.BalancePosition.TOP_LEFT));
        assertEquals(30, settings.getHOffset(ModSettings.BalancePosition.BOTTOM_RIGHT));
        assertEquals(-15, settings.getVOffset(ModSettings.BalancePosition.BOTTOM_RIGHT));
    }

    @Test
    public void unsetPositionReturnsDefaultOffsets() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(5, settings.getHOffset(ModSettings.BalancePosition.BOTTOM));
        assertEquals(3, settings.getVOffset(ModSettings.BalancePosition.BOTTOM));
    }

    @Test
    public void currentPositionOffsetsArePreservedWhenSwitchingPositions() throws Exception {
        ModSettings settings = newInstance();
        settings.setBalancePosition(ModSettings.BalancePosition.TOP);
        settings.setHOffset(10);
        settings.setVOffset(6);

        settings.setBalancePosition(ModSettings.BalancePosition.BOTTOM);
        settings.setHOffset(40);
        settings.setVOffset(20);

        settings.setBalancePosition(ModSettings.BalancePosition.TOP);
        assertEquals(10, settings.getHOffset());
        assertEquals(6, settings.getVOffset());

        settings.setBalancePosition(ModSettings.BalancePosition.BOTTOM);
        assertEquals(40, settings.getHOffset());
        assertEquals(20, settings.getVOffset());
    }

    // --- mobWorldGeneration ---

    @Test
    public void defaultMobWorldGenerationIsFalse() throws Exception {
        ModSettings settings = newInstance();
        assertFalse(settings.getMobWorldGeneration());
    }

    @Test
    public void setMobWorldGenerationTrueChangesValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setMobWorldGeneration(true);
        assertTrue(settings.getMobWorldGeneration());
    }

    @Test
    public void setMobWorldGenerationFalseAfterTrue() throws Exception {
        ModSettings settings = newInstance();
        settings.setMobWorldGeneration(true);
        settings.setMobWorldGeneration(false);
        assertFalse(settings.getMobWorldGeneration());
    }

    // --- inviteDurationTicks ---

    @Test
    public void defaultInviteDurationTicksIsTwelveHundred() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(1200, settings.getInviteDurationTicks());
    }

    @Test
    public void setInviteDurationTicksStoresValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setInviteDurationTicks(600);
        assertEquals(600, settings.getInviteDurationTicks());
    }

    // --- nonPlayerBreakCooldownTicks ---

    @Test
    public void defaultNonPlayerBreakCooldownTicksIsTwenty() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(20, settings.getNonPlayerBreakCooldownTicks());
    }

    @Test
    public void setNonPlayerBreakCooldownTicksStoresValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setNonPlayerBreakCooldownTicks(40);
        assertEquals(40, settings.getNonPlayerBreakCooldownTicks());
    }

    // --- maxMobSpawnPercent ---

    @Test
    public void defaultMaxMobSpawnPercentIsTen() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(10, settings.getMaxMobSpawnPercent());
    }

    @Test
    public void setMaxMobSpawnPercentStoresValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setMaxMobSpawnPercent(25);
        assertEquals(25, settings.getMaxMobSpawnPercent());
    }

    // --- maxGeneratorMembers ---

    @Test
    public void defaultMaxGeneratorMembersIsZero() throws Exception {
        ModSettings settings = newInstance();
        assertEquals(0, settings.getMaxGeneratorMembers());
    }

    @Test
    public void setMaxGeneratorMembersStoresValue() throws Exception {
        ModSettings settings = newInstance();
        settings.setMaxGeneratorMembers(5);
        assertEquals(5, settings.getMaxGeneratorMembers());
    }
}

package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.ModSettings;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ModSettingsTest {

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    private ModSettings newInstance() throws Exception {
        Constructor<ModSettings> ctor = ModSettings.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
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
}

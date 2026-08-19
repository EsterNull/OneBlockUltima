package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.config.BlockSetConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OneBlockPlayerDataTest {

    private static final double DELTA = 0.0001;

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
        BlockSetConfig.reset();
    }

    private OneBlockPlayerData newData() {
        return new OneBlockPlayerData();
    }

    @Test
    public void freshPlayerDataHasZeroBalance() {
        OneBlockPlayerData data = newData();
        assertEquals(0, data.getCurrency(), DELTA);
    }

    @Test
    public void addingCurrencyIncreasesBalance() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void addingNegativeOrZeroCurrencyIsIgnored() {
        OneBlockPlayerData data = newData();
        data.addCurrency(-5);
        assertEquals(0, data.getCurrency(), DELTA);
        data.addCurrency(0);
        assertEquals(0, data.getCurrency(), DELTA);
    }

    @Test
    public void spendingCurrencyDecreasesBalance() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertTrue(data.spendCurrency(50));
        assertEquals(50, data.getCurrency(), DELTA);
    }

    @Test
    public void spendingMoreThanBalanceFails() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertFalse(data.spendCurrency(150));
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void spendingNegativeAmountFails() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertFalse(data.spendCurrency(-10));
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void exactSpendDrainsBalance() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertTrue(data.spendCurrency(100));
        assertEquals(0, data.getCurrency(), DELTA);
    }

    @Test
    public void breakingBlocksIncrementsTotalAndPerSet() {
        OneBlockPlayerData data = newData();
        data.addBrokenBlocks("classic", 5);
        assertEquals(5, data.getBrokenBlocksCount());
        assertEquals(5, data.getBrokenBlocksCount("classic"));
    }

    @Test
    public void breakingBlocksMultipleSets() {
        OneBlockPlayerData data = newData();
        data.addBrokenBlocks("classic", 3);
        data.addBrokenBlocks("nether", 7);
        assertEquals(10, data.getBrokenBlocksCount());
        assertEquals(3, data.getBrokenBlocksCount("classic"));
        assertEquals(7, data.getBrokenBlocksCount("nether"));
    }

    @Test
    public void breakingZeroOrNegativeBlocksIsIgnored() {
        OneBlockPlayerData data = newData();
        data.addBrokenBlocks("classic", 0);
        assertEquals(0, data.getBrokenBlocksCount());
        data.addBrokenBlocks("classic", -5);
        assertEquals(0, data.getBrokenBlocksCount());
    }

    @Test
    public void upgradeSetIncreasesLevelAndSpendsCurrency() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertTrue(data.upgradeSet("classic", 50, 10));
        assertEquals(2, data.getSetLevel("classic"));
        assertEquals(50, data.getCurrency(), DELTA);
    }

    @Test
    public void upgradeSetFailsAtMaxLevel() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertFalse(data.upgradeSet("classic", 50, 1));
        assertEquals(1, data.getSetLevel("classic"));
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void upgradeSetFailsWithInsufficientFunds() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertFalse(data.upgradeSet("classic", 200, 10));
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void upgradeSetFailsWithZeroCost() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertTrue(data.upgradeSet("classic", 0, 10));
        assertEquals(2, data.getSetLevel("classic"));
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void copyFromCopiesAllFields() {
        OneBlockPlayerData source = newData();
        source.setCurrency(500);
        source.addBrokenBlocks("classic", 30);
        source.addBrokenBlocks("nether", 20);
        source.upgradeSet("classic", 0, 10);
        source.upgradeSet("classic", 0, 10);
        source.upgradeSet("nether", 0, 10);

        OneBlockPlayerData target = newData();
        target.copyFrom(source);

        assertEquals(500, target.getCurrency(), DELTA);
        assertEquals(50, target.getBrokenBlocksCount());
        assertEquals(30, target.getBrokenBlocksCount("classic"));
        assertEquals(20, target.getBrokenBlocksCount("nether"));
        assertEquals(3, target.getSetLevel("classic"));
        assertEquals(1, target.getSetLevel("nether"));
    }

    @Test
    public void copyFromWithNullIsNoOp() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        data.copyFrom(null);
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void setCurrencyClampsNegative() {
        OneBlockPlayerData data = newData();
        data.setCurrency(-10);
        assertEquals(0, data.getCurrency(), DELTA);
    }

    @Test
    public void setCurrencyClampsAboveMax() {
        OneBlockPlayerData data = newData();
        data.setCurrency(OneBlockPlayerData.MAX_CURRENCY * 10);
        assertEquals(OneBlockPlayerData.MAX_CURRENCY, data.getCurrency(), DELTA);
    }

    @Test
    public void setCurrencyClampsTheReportedCorruptValue() {
        OneBlockPlayerData data = newData();
        data.setCurrency(92233720368547760.0);
        assertTrue("corrupt balance must be clamped below the long-overflow display threshold",
                data.getCurrency() <= OneBlockPlayerData.MAX_CURRENCY);
    }

    @Test
    public void setCurrencyIgnoresNaN() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        data.setCurrency(Double.NaN);
        assertEquals(0, data.getCurrency(), DELTA);
    }

    @Test
    public void addCurrencyClampsToMax() {
        OneBlockPlayerData data = newData();
        data.addCurrency(1e300);
        assertEquals(OneBlockPlayerData.MAX_CURRENCY, data.getCurrency(), DELTA);
    }

    @Test
    public void addCurrencyClampsSumAboveMax() {
        OneBlockPlayerData data = newData();
        data.addCurrency(OneBlockPlayerData.MAX_CURRENCY - 5);
        data.addCurrency(10);
        assertEquals(OneBlockPlayerData.MAX_CURRENCY, data.getCurrency(), DELTA);
    }

    @Test
    public void addCurrencyIgnoresNaN() {
        OneBlockPlayerData data = newData();
        data.addCurrency(Double.NaN);
        assertEquals(0, data.getCurrency(), DELTA);
    }

    @Test
    public void spendCurrencyFailsOnNaN() {
        OneBlockPlayerData data = newData();
        data.addCurrency(100);
        assertFalse(data.spendCurrency(Double.NaN));
        assertEquals(100, data.getCurrency(), DELTA);
    }

    @Test
    public void setBrokenBlocksBySetClampsNegative() {
        OneBlockPlayerData data = newData();
        Map<String, Integer> blocks = new HashMap<>();
        blocks.put("classic", -5);
        blocks.put("nether", 10);
        data.setBrokenBlocksBySet(blocks);
        assertEquals(0, data.getBrokenBlocksCount("classic"));
        assertEquals(10, data.getBrokenBlocksCount("nether"));
    }

    @Test
    public void getSetLevelReturnsOneForDefaultSet() {
        OneBlockPlayerData data = newData();
        assertEquals(1, data.getSetLevel("classic"));
    }

    @Test
    public void getSetLevelReturnsZeroForUnknownSet() {
        OneBlockPlayerData data = newData();
        assertEquals(0, data.getSetLevel("nonexistent"));
    }

    @Test
    public void addingFractionalCurrency() {
        OneBlockPlayerData data = newData();
        data.addCurrency(0.5);
        assertEquals(0.5, data.getCurrency(), DELTA);
        data.addCurrency(0.25);
        assertEquals(0.75, data.getCurrency(), DELTA);
    }

    @Test
    public void spendingFractionalCurrency() {
        OneBlockPlayerData data = newData();
        data.addCurrency(10);
        assertTrue(data.spendCurrency(0.5));
        assertEquals(9.5, data.getCurrency(), DELTA);
    }
}

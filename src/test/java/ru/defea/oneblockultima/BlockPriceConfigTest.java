package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockPriceConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BlockPriceConfigTest {

    private static final double DELTA = 0.0001;

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
        BlockPriceConfig.reset();
    }

    private BlockPriceConfig newConfig() throws Exception {
        BlockPriceConfig config = new BlockPriceConfig();
        Field pricesField = BlockPriceConfig.class.getDeclaredField("prices");
        pricesField.setAccessible(true);
        pricesField.set(config, new LinkedHashMap<String, Double>());
        return config;
    }

    // --- getPrice ---

    @Test
    public void getPriceReturnsZeroForUnknown() throws Exception {
        BlockPriceConfig config = newConfig();
        assertEquals(0, config.getPrice("minecraft:stone"), DELTA);
    }

    @Test
    public void getPriceReturnsZeroForNull() throws Exception {
        BlockPriceConfig config = newConfig();
        assertEquals(0, config.getPrice(null), DELTA);
    }

    @Test
    public void getPriceReturnsCorrectValue() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:dirt", 5);
        assertEquals(5, config.getPrice("minecraft:dirt"), DELTA);
    }

    @Test
    public void getPriceReturnsFractionalValue() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:dirt", 0.5);
        assertEquals(0.5, config.getPrice("minecraft:dirt"), DELTA);
    }

    @Test
    public void getPriceReturnsZeroAfterRemove() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.removePrice("minecraft:stone");
        assertEquals(0, config.getPrice("minecraft:stone"), DELTA);
    }

    // --- hasPrice ---

    @Test
    public void hasPriceReturnsFalseForUnknown() throws Exception {
        BlockPriceConfig config = newConfig();
        assertFalse(config.hasPrice("minecraft:stone"));
    }

    @Test
    public void hasPriceReturnsFalseForNull() throws Exception {
        BlockPriceConfig config = newConfig();
        assertFalse(config.hasPrice(null));
    }

    @Test
    public void hasPriceReturnsTrueAfterSet() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:diamond", 100);
        assertTrue(config.hasPrice("minecraft:diamond"));
    }

    @Test
    public void hasPriceReturnsFalseAfterRemove() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:diamond", 100);
        config.removePrice("minecraft:diamond");
        assertFalse(config.hasPrice("minecraft:diamond"));
    }

    // --- setPrice ---

    @Test
    public void setPriceAddsNewEntry() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        assertEquals(10, config.getPrice("minecraft:stone"), DELTA);
    }

    @Test
    public void setPriceOverwritesExisting() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.setPrice("minecraft:stone", 25);
        assertEquals(25, config.getPrice("minecraft:stone"), DELTA);
    }

    @Test
    public void setPriceWithZeroValue() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 0);
        assertTrue(config.hasPrice("minecraft:stone"));
        assertEquals(0, config.getPrice("minecraft:stone"), DELTA);
    }

    @Test
    public void setPriceWithNullRegistryIsNoOp() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice(null, 10);
        assertFalse(config.hasPrice(null));
    }

    @Test
    public void setPriceWithNegativeValue() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", -5);
        assertEquals(-5, config.getPrice("minecraft:stone"), DELTA);
    }

    @Test
    public void setPriceWithFractionalValue() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 0.25);
        assertEquals(0.25, config.getPrice("minecraft:stone"), DELTA);
    }

    // --- removePrice ---

    @Test
    public void removePriceRemovesEntry() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.removePrice("minecraft:stone");
        assertFalse(config.hasPrice("minecraft:stone"));
    }

    @Test
    public void removePriceOnUnknownIsNoOp() throws Exception {
        BlockPriceConfig config = newConfig();
        config.removePrice("minecraft:nonexistent");
        assertFalse(config.hasPrice("minecraft:nonexistent"));
    }

    @Test
    public void removePriceWithNullIsNoOp() throws Exception {
        BlockPriceConfig config = newConfig();
        config.removePrice(null);
    }

    // --- replaceAll ---

    @Test
    public void replaceAllReplacesAllPrices() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.setPrice("minecraft:dirt", 5);

        Map<String, Double> newPrices = new LinkedHashMap<>();
        newPrices.put("minecraft:diamond", 100.0);
        newPrices.put("minecraft:emerald", 50.0);
        config.replaceAll(newPrices);

        assertFalse(config.hasPrice("minecraft:stone"));
        assertFalse(config.hasPrice("minecraft:dirt"));
        assertEquals(100, config.getPrice("minecraft:diamond"), DELTA);
        assertEquals(50, config.getPrice("minecraft:emerald"), DELTA);
    }

    @Test
    public void replaceAllWithEmptyMapClearsAll() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.replaceAll(new HashMap<String, Double>());
        assertFalse(config.hasPrice("minecraft:stone"));
        assertEquals(0, config.getPrices().size());
    }

    @Test
    public void replaceAllWithNullIsNoOp() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.replaceAll(null);
        assertTrue(config.hasPrice("minecraft:stone"));
    }

    // --- getPrices ---

    @Test
    public void getPricesReturnsUnmodifiableMap() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        Map<String, Double> prices = config.getPrices();
        try {
            prices.put("minecraft:dirt", 5.0);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void getPricesReturnsCorrectSize() throws Exception {
        BlockPriceConfig config = newConfig();
        assertEquals(0, config.getPrices().size());
        config.setPrice("minecraft:stone", 10);
        assertEquals(1, config.getPrices().size());
        config.setPrice("minecraft:dirt", 5);
        assertEquals(2, config.getPrices().size());
    }

    // --- getPricesList ---

    @Test
    public void getPricesListReturnsModifiableCopy() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.setPrice("minecraft:dirt", 5);

        java.util.List<Map.Entry<String, Double>> list = config.getPricesList();
        assertEquals(2, list.size());
        list.clear();
        assertEquals(2, config.getPrices().size());
    }

    @Test
    public void getPricesListPreservesInsertionOrder() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:diamond", 100);
        config.setPrice("minecraft:stone", 10);
        config.setPrice("minecraft:dirt", 5);

        java.util.List<Map.Entry<String, Double>> list = config.getPricesList();
        assertEquals("minecraft:diamond", list.get(0).getKey());
        assertEquals("minecraft:stone", list.get(1).getKey());
        assertEquals("minecraft:dirt", list.get(2).getKey());
    }

    // --- createItemStack ---

    @Test
    public void createItemStackReturnsEmptyForNull() {
        assertTrue(BlockPriceConfig.createItemStack(null).isEmpty());
    }

    @Test
    public void createItemStackReturnsEmptyForUnknownRegistry() {
        assertTrue(BlockPriceConfig.createItemStack("nonexistent:fake_item").isEmpty());
    }

    @Test
    public void createItemStackReturnsDirtForValidBlock() {
        net.minecraft.item.ItemStack stack = BlockPriceConfig.createItemStack("minecraft:dirt");
        assertFalse(stack.isEmpty());
        assertEquals(1, stack.getCount());
    }

    @Test
    public void createItemStackReturnsStoneForValidBlock() {
        net.minecraft.item.ItemStack stack = BlockPriceConfig.createItemStack("minecraft:stone");
        assertFalse(stack.isEmpty());
        assertEquals(net.minecraft.init.Blocks.STONE, ((net.minecraft.item.ItemBlock) stack.getItem()).getBlock());
    }

    @Test
    public void createItemStackReturnsEmptyForAirRegistry() {
        net.minecraft.item.ItemStack stack = BlockPriceConfig.createItemStack("minecraft:air");
        assertTrue(stack.isEmpty());
    }

    // --- Multiple operations ---

    @Test
    public void multipleSetAndRemoveOperations() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.setPrice("minecraft:dirt", 5);
        config.setPrice("minecraft:cobblestone", 3);
        assertEquals(3, config.getPrices().size());

        config.removePrice("minecraft:dirt");
        assertEquals(2, config.getPrices().size());
        assertEquals(10, config.getPrice("minecraft:stone"), DELTA);
        assertEquals(0, config.getPrice("minecraft:dirt"), DELTA);
        assertEquals(3, config.getPrice("minecraft:cobblestone"), DELTA);
    }

    @Test
    public void setPriceZeroThenCheckHasPrice() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setPrice("minecraft:stone", 10);
        config.setPrice("minecraft:stone", 0);
        assertTrue(config.hasPrice("minecraft:stone"));
        assertEquals(0, config.getPrice("minecraft:stone"), DELTA);
    }

    // --- BalanceMode ---

    @Test
    public void defaultBalanceModeIsBreakBlock() throws Exception {
        BlockPriceConfig config = newConfig();
        assertEquals(BlockPriceConfig.BalanceMode.BREAK_BLOCK, config.getBalanceMode());
    }

    @Test
    public void setBalanceModeChangesValue() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setBalanceMode(BlockPriceConfig.BalanceMode.SELL_BLOCK);
        assertEquals(BlockPriceConfig.BalanceMode.SELL_BLOCK, config.getBalanceMode());
    }

    @Test
    public void setBalanceModeBackToBreakBlock() throws Exception {
        BlockPriceConfig config = newConfig();
        config.setBalanceMode(BlockPriceConfig.BalanceMode.SELL_BLOCK);
        config.setBalanceMode(BlockPriceConfig.BalanceMode.BREAK_BLOCK);
        assertEquals(BlockPriceConfig.BalanceMode.BREAK_BLOCK, config.getBalanceMode());
    }

    @Test
    public void balanceModeHasTwoValues() {
        assertEquals(2, BlockPriceConfig.BalanceMode.values().length);
    }

    @Test
    public void balanceModeValueOf() {
        assertEquals(BlockPriceConfig.BalanceMode.BREAK_BLOCK, BlockPriceConfig.BalanceMode.valueOf("BREAK_BLOCK"));
        assertEquals(BlockPriceConfig.BalanceMode.SELL_BLOCK, BlockPriceConfig.BalanceMode.valueOf("SELL_BLOCK"));
    }
}

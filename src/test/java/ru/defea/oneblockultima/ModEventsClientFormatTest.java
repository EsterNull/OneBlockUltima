package ru.defea.oneblockultima;

import org.junit.Test;
import ru.defea.oneblockultima.event.ModEventsClient;

import static org.junit.Assert.assertEquals;

/**
 * Regression checks for {@link ModEventsClient#formatCurrency(double)}.
 *
 * <p>The 1.7.10 formatter rounded {@code value * 100} into a {@code long}; balances near
 * {@code Long.MAX_VALUE / 100} (e.g. the corrupt {@code 92233720368547760}) overflowed that
 * product and produced garbage. The formatter must render any finite balance, including the
 * maximum allowed one, as plain decimal text without overflowing.
 */
public class ModEventsClientFormatTest {

    @Test
    public void formatsWholeNumbers() {
        assertEquals("0", ModEventsClient.formatCurrency(0));
        assertEquals("100", ModEventsClient.formatCurrency(100));
        assertEquals("92233720368547760", ModEventsClient.formatCurrency(92233720368547760.0));
    }

    @Test
    public void formatsFractionsToTwoDecimals() {
        assertEquals("0.5", ModEventsClient.formatCurrency(0.5));
        assertEquals("1234.5", ModEventsClient.formatCurrency(1234.5));
        assertEquals("0.01", ModEventsClient.formatCurrency(0.005));
        assertEquals("0.75", ModEventsClient.formatCurrency(0.75));
    }

    @Test
    public void formatsMaxCurrencyWithoutLongOverflow() {
        assertEquals("1000000000000000", ModEventsClient.formatCurrency(1_000_000_000_000_000.0));
    }

    @Test
    public void handlesNonFiniteValues() {
        assertEquals("0", ModEventsClient.formatCurrency(Double.NaN));
        assertEquals("0", ModEventsClient.formatCurrency(Double.POSITIVE_INFINITY));
        assertEquals("0", ModEventsClient.formatCurrency(Double.NEGATIVE_INFINITY));
    }
}

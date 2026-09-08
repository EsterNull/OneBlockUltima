package ru.defea.oneblockultima.util;

public final class CurrencyUtil
{
    public static final long CENT_SCALE = 100L;

    private CurrencyUtil()
    {
    }

    public static long toCents(double value)
    {
        if (Double.isFinite(value))
        {
            return Math.round(value * CENT_SCALE);
        }
        return 0L;
    }

    public static double fromCents(long cents)
    {
        return cents / (double) CENT_SCALE;
    }

    public static double roundToCents(double value)
    {
        return fromCents(toCents(value));
    }
}
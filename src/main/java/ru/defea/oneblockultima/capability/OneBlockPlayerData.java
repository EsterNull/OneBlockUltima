package ru.defea.oneblockultima.capability;

import ru.defea.oneblockultima.config.BlockSetConfig;

import java.util.HashMap;
import java.util.Map;

public class OneBlockPlayerData implements IOneBlockPlayerData
{
    /**
     * Upper bound for the currency. The 1.7.10 balance is displayed by rounding
     * {@code value * 100} into a {@code long}; values around {@code Long.MAX_VALUE / 100}
     * (e.g. {@code 92233720368547760}) made that product overflow and produced a corrupt
     * balance. The cap keeps the balance well below the overflow threshold.
     */
    public static final double MAX_CURRENCY = 1_000_000_000_000_000.0;

    private double currency;
    private final Map<String, Integer> setLevels = new HashMap<>();
    private final Map<String, Integer> brokenBlocksBySet = new HashMap<>();
    private int brokenBlocksTotal;

    @Override
    public double getCurrency()
    {
        return currency;
    }

    @Override
    public void addCurrency(double amount)
    {
        if (amount <= 0 || Double.isNaN(amount))
        {
            return;
        }

        double sum = currency + amount;
        this.currency = Double.isInfinite(sum) || sum > MAX_CURRENCY ? MAX_CURRENCY : sum;
    }

    @Override
    public boolean spendCurrency(double amount)
    {
        if (amount < 0 || Double.isNaN(amount) || currency < amount)
        {
            return false;
        }

        currency -= amount;
        return true;
    }

    @Override
    public int getSetLevel(String setId)
    {
        Integer level = setLevels.get(setId);
        if (level != null)
        {
            return level;
        }

        BlockSetConfig config = BlockSetConfig.get();
        if (config == null)
        {
            return 0;
        }

        String defaultSetId = config.getDefaultSetId();
        return setId != null && setId.equals(defaultSetId) ? 1 : 0;
    }

    @Override
    public boolean upgradeSet(String setId, int cost, int maxLevel)
    {
        int currentLevel = getSetLevel(setId);
        if (currentLevel >= maxLevel || !spendCurrency(cost))
        {
            return false;
        }

        setLevels.put(setId, currentLevel + 1);
        return true;
    }

    public Map<String, Integer> getSetLevels()
    {
        return setLevels;
    }

    public Map<String, Integer> getBrokenBlocksBySet()
    {
        return brokenBlocksBySet;
    }

    @Override
    public int getBrokenBlocksCount()
    {
        return this.brokenBlocksTotal;
    }

    @Override
    public int getBrokenBlocksCount(String setId)
    {
        Integer count = this.brokenBlocksBySet.get(setId);
        return count == null ? 0 : count;
    }

    @Override
    public void addBrokenBlocks(String setId, int amount)
    {
        if (amount <= 0)
        {
            return;
        }

        brokenBlocksTotal += amount;
        brokenBlocksBySet.put(setId, getBrokenBlocksCount(setId) + amount);
    }

    @Override
    public void copyFrom(IOneBlockPlayerData other)
    {
        if (other == null)
        {
            return;
        }

        setCurrency(other.getCurrency());
        setBrokenBlocksTotal(other.getBrokenBlocksCount());

        if (other instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData otherData = (OneBlockPlayerData) other;
            setBrokenBlocksBySet(otherData.getBrokenBlocksBySet());
            getSetLevels().clear();
            getSetLevels().putAll(otherData.getSetLevels());
            return;
        }

        getSetLevels().clear();
    }

    public void setCurrency(double currency)
    {
        if (Double.isNaN(currency))
        {
            currency = 0;
        }
        this.currency = Math.min(Math.max(0, currency), MAX_CURRENCY);
    }

    public void setBrokenBlocksTotal(int brokenBlocksTotal)
    {
        this.brokenBlocksTotal = Math.max(0, brokenBlocksTotal);
    }

    public void setBrokenBlocksBySet(Map<String, Integer> brokenBlocksBySet)
    {
        this.brokenBlocksBySet.clear();
        if (brokenBlocksBySet != null)
        {
            for (Map.Entry<String, Integer> entry : brokenBlocksBySet.entrySet())
            {
                this.brokenBlocksBySet.put(entry.getKey(), Math.max(0, entry.getValue()));
            }
        }
    }
}

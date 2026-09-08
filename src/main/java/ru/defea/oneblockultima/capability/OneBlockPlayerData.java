package ru.defea.oneblockultima.capability;

import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.util.CurrencyUtil;

import java.util.HashMap;
import java.util.Map;

public class OneBlockPlayerData implements IOneBlockPlayerData
{
    private long currencyCents;
    private final Map<String, Integer> setLevels = new HashMap<String, Integer>();
    private final Map<String, Integer> brokenBlocksBySet = new HashMap<String, Integer>();
    private int brokenBlocksTotal;

    @Override
    public double getCurrency()
    {
        return CurrencyUtil.fromCents(currencyCents);
    }

    public long getCurrencyCents()
    {
        return currencyCents;
    }

    public void setCurrencyCents(long cents)
    {
        this.currencyCents = Math.max(0L, cents);
    }

    @Override
    public void addCurrency(double amount)
    {
        if (Double.isFinite(amount) && amount > 0)
        {
            long cents = CurrencyUtil.toCents(amount);
            if (cents > 0)
            {
                currencyCents += cents;
                if (currencyCents < 0)
                {
                    currencyCents = Long.MAX_VALUE;
                }
            }
        }
    }

    @Override
    public boolean spendCurrency(double amount)
    {
        if (!Double.isFinite(amount) || amount < 0)
        {
            return false;
        }

        long cents = CurrencyUtil.toCents(amount);
        if (currencyCents < cents)
        {
            return false;
        }

        currencyCents -= cents;
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
        setCurrencyCents(Double.isFinite(currency) ? CurrencyUtil.toCents(currency) : 0L);
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

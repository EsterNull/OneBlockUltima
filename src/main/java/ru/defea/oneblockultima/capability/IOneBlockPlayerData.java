package ru.defea.oneblockultima.capability;

public interface IOneBlockPlayerData
{
    double getCurrency();

    void addCurrency(double amount);

    boolean spendCurrency(double amount);

    int getSetLevel(String setId);

    boolean upgradeSet(String setId, int cost, int maxLevel);

    int getBrokenBlocksCount();

    int getBrokenBlocksCount(String setId);

    void addBrokenBlocks(String setId, int amount);

    void copyFrom(IOneBlockPlayerData other);
}

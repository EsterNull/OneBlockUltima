package ru.defea.oneblockultima.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import ru.defea.oneblockultima.OneBlockUltima;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ModSettings
{
    public enum BalancePosition
    {
        TOP_LEFT,
        TOP,
        TOP_RIGHT,
        RIGHT,
        BOTTOM_RIGHT,
        BOTTOM,
        BOTTOM_LEFT,
        LEFT
    }

    public static class PositionOffsets
    {
        public int hOffset;
        public int vOffset;

        public PositionOffsets(int hOffset, int vOffset)
        {
            this.hOffset = hOffset;
            this.vOffset = vOffset;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "oneblockultima_mod_settings.json";
    private static ModSettings instance;
    private static volatile boolean debugEnabled;

    private BalancePosition balancePosition = BalancePosition.TOP_RIGHT;
    private final int hOffset = 5;
    private final int vOffset = 3;
    private Map<BalancePosition, PositionOffsets> positionOffsets = new HashMap<>();
    private boolean isShowBalance = true;
    private boolean mobWorldGeneration = false;
    private boolean debugMode = false;
    private int inviteDurationTicks = 1200;
    private int nonPlayerBreakCooldownTicks = 20;
    private int maxMobSpawnPercent = 10;
    private int maxGeneratorMembers = 0;
    private double caseDropPercent = 5.0;
    private boolean isShowInfoPanel = true;
    private boolean showFluids = true;
    private boolean showMobs = true;
    private boolean showChests = true;
    private boolean showSaplings = true;

    public static ModSettings get()
    {
        if (instance == null)
        {
            instance = load();
        }
        debugEnabled = instance.debugMode;
        return instance;
    }

    public static int[] computeBalanceBox(BalancePosition pos, int hOffset, int vOffset,
                                          int boxW, int boxH, int areaW, int areaH)
    {
        int bx;
        int by;
        switch (pos)
        {
            case TOP_LEFT:
                bx = areaW * hOffset / 100;
                by = areaH * vOffset / 100;
                break;
            case TOP:
                bx = areaW / 2 - boxW / 2 + areaW * hOffset / 100;
                by = areaH * vOffset / 100;
                break;
            case LEFT:
                bx = areaW * hOffset / 100;
                by = areaH / 2 - boxH / 2 + areaH * vOffset / 100;
                break;
            case RIGHT:
                bx = areaW - boxW - areaW * hOffset / 100;
                by = areaH / 2 - boxH / 2 + areaH * vOffset / 100;
                break;
            case BOTTOM_LEFT:
                bx = areaW * hOffset / 100;
                by = areaH - boxH - areaH * vOffset / 100;
                break;
            case BOTTOM:
                bx = areaW / 2 - boxW / 2 + areaW * hOffset / 100;
                by = areaH - boxH - areaH * vOffset / 100;
                break;
            case BOTTOM_RIGHT:
                bx = areaW - boxW - areaW * hOffset / 100;
                by = areaH - boxH - areaH * vOffset / 100;
                break;
            default:
                bx = areaW - boxW - areaW * hOffset / 100;
                by = areaH * vOffset / 100;
                break;
        }
        bx = Math.max(0, Math.min(bx, Math.max(0, areaW - boxW)));
        by = Math.max(0, Math.min(by, Math.max(0, areaH - boxH)));
        return new int[]{bx, by, boxW, boxH};
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public BalancePosition getBalancePosition() { return balancePosition; }
    public int getHOffset() { return getHOffset(balancePosition); }
    public int getVOffset() { return getVOffset(balancePosition); }
    public int getHOffset(BalancePosition pos) { return getOffsets(pos).hOffset; }
    public int getVOffset(BalancePosition pos) { return getOffsets(pos).vOffset; }
    public boolean isShowBalance() { return isShowBalance; }
    public boolean getMobWorldGeneration() { return mobWorldGeneration; }
    public boolean isDebugMode() { return debugMode; }
    public int getInviteDurationTicks() { return inviteDurationTicks; }
    public int getNonPlayerBreakCooldownTicks() { return nonPlayerBreakCooldownTicks; }
    public int getMaxMobSpawnPercent() { return maxMobSpawnPercent; }
    public int getMaxGeneratorMembers() { return maxGeneratorMembers; }
    public double getCaseDropPercent() { return caseDropPercent; }
    public boolean isNotShowInfoPanel() { return !isShowInfoPanel; }
    public boolean isShowFluids() { return showFluids; }
    public boolean isShowMobs() { return showMobs; }
    public boolean isShowChests() { return showChests; }
    public boolean isShowSaplings() { return showSaplings; }
    public void toggleFluids() { this.showFluids = !this.showFluids; save(); }
    public void toggleMobs() { this.showMobs = !this.showMobs; save(); }
    public void toggleChests() { this.showChests = !this.showChests; save(); }
    public void toggleSaplings() { this.showSaplings = !this.showSaplings; save(); }

    public void setBalancePosition(BalancePosition pos) { this.balancePosition = pos; save(); }
    public void setHOffset(int offset) { setHOffset(balancePosition, offset); }
    public void setVOffset(int offset) { setVOffset(balancePosition, offset); }
    public void setHOffset(BalancePosition pos, int offset) { getOffsets(pos).hOffset = offset; save(); }
    public void setVOffset(BalancePosition pos, int offset) { getOffsets(pos).vOffset = offset; save(); }
    public void setAllPositionOffsets(int[] hOffsets, int[] vOffsets)
    {
        BalancePosition[] positions = BalancePosition.values();
        for (int i = 0; i < positions.length; i++)
        {
            PositionOffsets off = getOffsets(positions[i]);
            off.hOffset = hOffsets[i];
            off.vOffset = vOffsets[i];
        }
        save();
    }
    public void setShowBalance(boolean isShowBalance) { this.isShowBalance = isShowBalance; save(); }
    public void setMobWorldGeneration(boolean mobWorldGeneration) { this.mobWorldGeneration = mobWorldGeneration; save(); }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; debugEnabled = debugMode; save(); }
    public void setInviteDurationTicks(int inviteDurationTicks) { this.inviteDurationTicks = inviteDurationTicks; save(); }
    public void setNonPlayerBreakCooldownTicks(int nonPlayerBreakCooldownTicks) { this.nonPlayerBreakCooldownTicks = nonPlayerBreakCooldownTicks; save(); }
    public void setMaxMobSpawnPercent(int maxMobSpawnPercent) { this.maxMobSpawnPercent = maxMobSpawnPercent; save(); }
    public void setMaxGeneratorMembers(int maxGeneratorMembers) { this.maxGeneratorMembers = maxGeneratorMembers; save(); }
    public void setCaseDropPercent(double caseDropPercent) { this.caseDropPercent = caseDropPercent; save(); }
    public void setShowInfoPanel(boolean showInfoPanel) { this.isShowInfoPanel = showInfoPanel; save(); }

    private static Path getFilePath()
    {
        return FMLPaths.CONFIGDIR.get().resolve(OneBlockUltima.MODID).resolve(FILE_NAME);
    }

    private static ModSettings load()
    {
        Path file = getFilePath();
        if (!Files.exists(file))
        {
            return new ModSettings();
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))
        {
            ModSettings loaded = GSON.fromJson(reader, ModSettings.class);
            if (loaded == null) return new ModSettings();
            loaded.migrate();
            return loaded;
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load mod settings", e);
            return new ModSettings();
        }
    }

    private void migrate()
    {
        if (positionOffsets == null) positionOffsets = new HashMap<>();
        for (BalancePosition p : BalancePosition.values())
        {
            PositionOffsets off = positionOffsets.get(p);
            if (off == null)
            {
                positionOffsets.put(p, new PositionOffsets(defaultHOffsetFor(p), defaultVOffsetFor(p)));
            }
            else
            {
                if (off.hOffset == hOffset) off.hOffset = defaultHOffsetFor(p);
                if (off.vOffset == vOffset) off.vOffset = defaultVOffsetFor(p);
            }
        }
    }

    private int defaultHOffsetFor(BalancePosition pos)
    {
        return (pos == BalancePosition.TOP || pos == BalancePosition.BOTTOM) ? 0 : hOffset;
    }

    private int defaultVOffsetFor(BalancePosition pos)
    {
        return (pos == BalancePosition.LEFT || pos == BalancePosition.RIGHT) ? 0 : vOffset;
    }

    private PositionOffsets getOffsets(BalancePosition pos)
    {
        if (pos == null) pos = balancePosition;
        return positionOffsets.computeIfAbsent(pos, k -> new PositionOffsets(defaultHOffsetFor(k), defaultVOffsetFor(k)));
    }

    private void save()
    {
        Path file = getFilePath();
        try
        {
            if (file.getParent() != null)
            {
                Files.createDirectories(file.getParent());
            }
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8))
            {
                GSON.toJson(this, writer);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to save mod settings", e);
        }
    }
}

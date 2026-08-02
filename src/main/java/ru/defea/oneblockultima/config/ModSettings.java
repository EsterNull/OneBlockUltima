package ru.defea.oneblockultima.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.common.Loader;
import ru.defea.oneblockultima.OneBlockUltima;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "oneblockultima_mod_settings.json";
    private static ModSettings instance;

    private BalancePosition balancePosition = BalancePosition.TOP_RIGHT;
    private int hOffset = 5;
    private int vOffset = 3;
    private boolean isShowBalance = true;
    private boolean mobWorldGeneration = false;
    private int inviteDurationTicks = 1200;
    private int nonPlayerBreakCooldownTicks = 20;

    public static ModSettings get()
    {
        if (instance == null)
        {
            instance = load();
        }
        return instance;
    }

    public BalancePosition getBalancePosition() { return balancePosition; }
    public int getHOffset() { return hOffset; }
    public int getVOffset() { return vOffset; }
    public boolean isShowBalance() { return isShowBalance; }
    public boolean getMobWorldGeneration() { return mobWorldGeneration; }
    public int getInviteDurationTicks() { return inviteDurationTicks; }
    public int getNonPlayerBreakCooldownTicks() { return nonPlayerBreakCooldownTicks; }

    public void setBalancePosition(BalancePosition pos) { this.balancePosition = pos; save(); }
    public void setHOffset(int offset) { this.hOffset = offset; save(); }
    public void setVOffset(int offset) { this.vOffset = offset; save(); }
    public void setShowBalance(boolean isShowBalance) { this.isShowBalance = isShowBalance; save(); }
    public void setMobWorldGeneration(boolean mobWorldGeneration) { this.mobWorldGeneration = mobWorldGeneration; save(); }
    public void setInviteDurationTicks(int inviteDurationTicks) { this.inviteDurationTicks = inviteDurationTicks; save(); }
    public void setNonPlayerBreakCooldownTicks(int nonPlayerBreakCooldownTicks) { this.nonPlayerBreakCooldownTicks = nonPlayerBreakCooldownTicks; save(); }

    private static File getFile()
    {
        if (Loader.instance().getConfigDir() != null)
        {
            return new File(Loader.instance().getConfigDir(), FILE_NAME);
        }
        return null;
    }

    private static ModSettings load()
    {
        File file = getFile();
        if (file == null || !file.exists())
        {
            return new ModSettings();
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))
        {
            ModSettings loaded = GSON.fromJson(reader, ModSettings.class);
            return loaded != null ? loaded : new ModSettings();
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to load mod settings", e);
            return new ModSettings();
        }
    }

    private void save()
    {
        File file = getFile();
        if (file == null) return;
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))
        {
            GSON.toJson(this, writer);
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("Failed to save mod settings", e);
        }
    }
}

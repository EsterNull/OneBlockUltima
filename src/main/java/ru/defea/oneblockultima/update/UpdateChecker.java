package ru.defea.oneblockultima.update;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import ru.defea.oneblockultima.OneBlockUltima;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UpdateChecker
{
    private static final String VERSIONS_URL = "https://raw.githubusercontent.com/XZSt4nce/OneBlockUltima/main/versions.json";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "OneBlockUltima-UpdateChecker");
        t.setDaemon(true);
        return t;
    });

    private static String cachedRecommendedVersion;
    private static String cachedReleaseUrl;
    private static boolean checkDone = false;
    private static boolean updateAvailable = false;

    public static void checkForUpdates(ServerPlayer player)
    {
        if (checkDone)
        {
            if (updateAvailable && player != null)
            {
                notifyPlayer(player);
            }
            return;
        }

        executor.submit(() ->
        {
            try
            {
                String mcVersion = net.minecraft.SharedConstants.getCurrentVersion().getName();
                String versionKey = mcVersion + "-recommended";
                String releaseKey = mcVersion + "-recommended_release";

        IModInfo mod = ModList.get().getMods().stream()
                .filter(m -> m.getModId().equals(OneBlockUltima.MODID))
                .findFirst().orElse(null);
        if (mod == null) return;
        String currentVersion = mod.getVersion().toString();

                URL url = new URL(VERSIONS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "OneBlockUltima/" + currentVersion);

                if (conn.getResponseCode() != 200)
                {
                    OneBlockUltima.logDebugWarn("[UpdateChecker] Failed to fetch versions.json: HTTP {}", conn.getResponseCode());
                    checkDone = true;
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JsonObject root = new JsonParser().parse(sb.toString()).getAsJsonObject();
                JsonObject promos = root.getAsJsonObject("promos");
                if (promos == null || !promos.has(versionKey))
                {
                    OneBlockUltima.logDebugWarn("[UpdateChecker] No promo key '{}' found", versionKey);
                    checkDone = true;
                    return;
                }

                String recommendedVersion = promos.get(versionKey).getAsString();
                String releaseUrl = promos.has(releaseKey) ? promos.get(releaseKey).getAsString() :
                        root.has("homepage") ? root.get("homepage").getAsString() : null;

                cachedRecommendedVersion = recommendedVersion;
                cachedReleaseUrl = releaseUrl;
                checkDone = true;

                if (!currentVersion.equals(recommendedVersion))
                {
                    OneBlockUltima.logDebug("[UpdateChecker] New version available: {} (current: {})", recommendedVersion, currentVersion);
                    updateAvailable = true;
                    if (player != null)
                    {
                        notifyPlayer(player);
                    }
                }
                else
                {
                    OneBlockUltima.logDebug("[UpdateChecker] Mod is up to date: {}", currentVersion);
                }
            }
            catch (Exception e)
            {
                OneBlockUltima.logDebugWarn("[UpdateChecker] Failed to check for updates: {}", e.getMessage());
                checkDone = true;
            }
        });
    }

    private static void notifyPlayer(ServerPlayer player)
    {
        String currentVersion;
        IModInfo mod = ModList.get().getMods().stream()
                .filter(m -> m.getModId().equals(OneBlockUltima.MODID))
                .findFirst().orElse(null);
        if (mod != null)
        {
            currentVersion = mod.getVersion().toString();
        }
        else
        {
            currentVersion = "???";
        }

        player.sendSystemMessage(Component.translatable(
                "oneblockultima.update.available",
                cachedRecommendedVersion,
                currentVersion));

        if (cachedReleaseUrl != null)
        {
            net.minecraft.network.chat.MutableComponent linkMessage = Component.translatable(
                    "oneblockultima.update.link",
                    cachedRecommendedVersion);

            linkMessage = linkMessage.withStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, cachedReleaseUrl)));

            player.sendSystemMessage(linkMessage);
        }
    }

    public static void shutdown()
    {
        executor.shutdown();
        try
        {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException ignored)
        {
        }
    }
}

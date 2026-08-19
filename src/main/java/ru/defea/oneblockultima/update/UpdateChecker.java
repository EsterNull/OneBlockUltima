package ru.defea.oneblockultima.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
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

    public static void checkForUpdates(EntityPlayerMP player)
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
                String mcVersion = Loader.instance().getMCVersionString().substring(10);
                String versionKey = mcVersion + "-recommended";
                String releaseKey = mcVersion + "-recommended_release";

                ModContainer mod = Loader.instance().getIndexedModList().get(OneBlockUltima.MODID);
                if (mod == null) return;
                String currentVersion = mod.getVersion();

                URL url = new URL(VERSIONS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "OneBlockUltima/" + currentVersion);

                if (conn.getResponseCode() != 200)
                {
                    OneBlockUltima.getLogger().warn("[UpdateChecker] Failed to fetch versions.json: HTTP {}", conn.getResponseCode());
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
                    OneBlockUltima.getLogger().warn("[UpdateChecker] No promo key '{}' found", versionKey);
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
                    OneBlockUltima.getLogger().info("[UpdateChecker] New version available: {} (current: {})", recommendedVersion, currentVersion);
                    updateAvailable = true;
                    if (player != null)
                    {
                        notifyPlayer(player);
                    }
                }
                else
                {
                    OneBlockUltima.getLogger().info("[UpdateChecker] Mod is up to date: {}", currentVersion);
                }
            }
            catch (Exception e)
            {
                OneBlockUltima.getLogger().warn("[UpdateChecker] Failed to check for updates: {}", e.getMessage());
                checkDone = true;
            }
        });
    }

    private static void notifyPlayer(EntityPlayerMP player)
    {
        String currentVersion;
        ModContainer mod = Loader.instance().getIndexedModList().get(OneBlockUltima.MODID);
        if (mod != null)
        {
            currentVersion = mod.getVersion();
        }
        else
        {
            currentVersion = "???";
        }

        player.addChatMessage(new ChatComponentTranslation(
                "oneblockultima.update.available",
                cachedRecommendedVersion,
                currentVersion));

        if (cachedReleaseUrl != null)
        {
            ChatComponentTranslation linkMessage = new ChatComponentTranslation(
                    "oneblockultima.update.link",
                    cachedRecommendedVersion);

            linkMessage.setChatStyle(new ChatStyle()
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, cachedReleaseUrl)));

            player.addChatMessage(linkMessage);
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

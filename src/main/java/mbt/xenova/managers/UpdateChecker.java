package mbt.xenova.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mbt.xenova.MineBoost;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    private static final int RESOURCE_ID = 137356;

    private static final String VERSIONS_LATEST_URL = "https://api.spiget.org/v2/resources/%d/versions/latest";

    private static final String RESOURCE_PAGE_URL = "https://www.spigotmc.org/resources/%d";

    public void checkAsync() {
        MineBoost plugin = MineBoost.getInstance();

        if (RESOURCE_ID <= 0) {
            plugin.getLogger().warning("Update checking is enabled but no Spigot resource ID is configured yet; skipping check.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = fetchLatestVersionJson();
                String remoteVersion = extractVersionName(json);
                if (remoteVersion == null) {
                    plugin.getLogger().warning("Could not read the latest version from Spiget's response.");
                    return;
                }

                String currentVersion = plugin.getPluginMeta().getVersion();

                if (!remoteVersion.equalsIgnoreCase(currentVersion)) {
                    plugin.getLogger().warning("A new version of MineBoost is available: " + remoteVersion + " (running " + currentVersion + "). Download: " + String.format(RESOURCE_PAGE_URL, RESOURCE_ID));
                } else {
                    plugin.getLogger().info("MineBoost is up to date (" + currentVersion + ").");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    private String fetchLatestVersionJson() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
                String.format(VERSIONS_LATEST_URL, RESOURCE_ID)).toURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "MineBoost-UpdateChecker");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            connection.disconnect();
        }

        return response.toString();
    }

    private String extractVersionName(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("name")) return null;
        return root.get("name").getAsString();
    }
}
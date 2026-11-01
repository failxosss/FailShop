package cz.failshop.update;

import cz.failshop.FailShop;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks Spigot (via the Spiget API) for a newer FailShop version and, if enabled,
 * downloads it into the server's update folder. The server swaps the jar on the next restart.
 */
public final class UpdateChecker implements Listener {

    private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private final FailShop plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String newVersion;
    private volatile String downloadedVersion;
    private int resourceId;

    public UpdateChecker(FailShop plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("update-checker.enabled", true)) return;

        resourceId = c.getInt("update-checker.resource-id", 0);
        if (resourceId <= 0) {
            plugin.getLogger().info("Update checker is idle: set update-checker.resource-id in config.yml "
                    + "after publishing FailShop on Spigot.");
            return;
        }

        long hours = Math.max(1, c.getLong("update-checker.check-interval-hours", 6));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::check, 20L * 30, hours * 60L * 60L * 20L);
    }

    private String api() {
        return "https://api.spiget.org/v2/resources/" + resourceId;
    }

    private void check() {
        try {
            HttpResponse<String> res = http.send(request(api() + "/versions/latest"),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return;

            Matcher m = NAME.matcher(res.body());
            if (!m.find()) return;

            String latest = m.group(1).trim();
            String current = plugin.getDescription().getVersion();
            if (!isNewer(latest, current)) {
                newVersion = null;
                return;
            }
            if (latest.equals(downloadedVersion)) return;

            newVersion = latest;
            plugin.getLogger().info("A new version of FailShop is available: " + latest + " (current: " + current + ")");

            if (plugin.getConfig().getBoolean("update-checker.auto-download", true)) {
                download(latest);
            } else {
                notifyAdmins(plugin.msg("update-available", "version", latest));
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Update check failed: " + e.getMessage());
        }
    }

    private void download(String version) {
        try {
            HttpResponse<byte[]> res = http.send(request(api() + "/download"),
                    HttpResponse.BodyHandlers.ofByteArray());
            byte[] data = res.body();

            // must be a real jar (zip header "PK"), not an HTML error page
            if (res.statusCode() != 200 || data.length < 10_000 || data[0] != 'P' || data[1] != 'K') {
                plugin.getLogger().warning("Auto-update: the downloaded file is not a valid jar, skipping.");
                return;
            }

            File updateDir = Bukkit.getUpdateFolderFile();
            if (!updateDir.exists() && !updateDir.mkdirs()) return;

            File target = new File(updateDir, plugin.getPluginFile().getName());
            File tmp = new File(updateDir, target.getName() + ".tmp");
            Files.write(tmp.toPath(), data);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

            downloadedVersion = version;
            plugin.getLogger().info("Version " + version + " was downloaded. Restart the server to apply the update.");
            notifyAdmins(plugin.msg("update-downloaded", "version", version));
        } catch (Exception e) {
            plugin.getLogger().warning("Auto-update download failed: " + e.getMessage());
        }
    }

    private HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "FailShop-UpdateChecker")
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
    }

    private void notifyAdmins(String text) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("failshop.admin")) p.sendMessage(text);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String version = newVersion;
        Player p = event.getPlayer();
        if (version == null || !p.hasPermission("failshop.admin")) return;

        String text = version.equals(downloadedVersion)
                ? plugin.msg("update-downloaded", "version", version)
                : plugin.msg("update-available", "version", version);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) p.sendMessage(text);
        }, 40L);
    }

    private static boolean isNewer(String latest, String current) {
        String[] a = latest.split("[^0-9]+");
        String[] b = current.split("[^0-9]+");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length && !a[i].isEmpty() ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length && !b[i].isEmpty() ? Integer.parseInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }
}

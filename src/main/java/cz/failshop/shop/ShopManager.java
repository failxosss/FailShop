package cz.failshop.shop;

import cz.failshop.FailShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads shops.yml, keeps dynamic price state (data.yml) and answers price questions. */
public final class ShopManager {

    private final FailShop plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private final Map<String, ShopItem> items = new HashMap<>();
    private final Map<Material, ShopItem> sellIndex = new EnumMap<>(Material.class);
    private final Map<Material, ShopItem> materialIndex = new EnumMap<>(Material.class);
    private final Map<String, Double> demand = new HashMap<>();

    private File dataFile;
    private YamlConfiguration data = new YamlConfiguration();

    public ShopManager(FailShop plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- loading

    public void load() {
        categories.clear();
        items.clear();
        sellIndex.clear();
        materialIndex.clear();

        File file = new File(plugin.getDataFolder(), "shops.yml");
        if (!file.exists()) plugin.saveResource("shops.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection cs = y.getConfigurationSection("categories");
        if (cs != null) {
            for (String cid : cs.getKeys(false)) {
                ConfigurationSection c = cs.getConfigurationSection(cid);
                if (c == null) continue;
                loadCategory(cid, c);
            }
        }
        loadData();
    }

    private void loadCategory(String cid, ConfigurationSection c) {
        Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
        if (icon == null) icon = Material.CHEST;
        String head = c.getString("head");
        if (head != null && !head.isBlank()) icon = Material.PLAYER_HEAD;

        String name = plugin.lang().pick(c, "name");
        if (name == null) name = cid;

        List<ShopItem> list = new ArrayList<>();
        ConfigurationSection is = c.getConfigurationSection("items");
        if (is != null) {
            for (String iid : is.getKeys(false)) {
                ConfigurationSection s = is.getConfigurationSection(iid);
                if (s == null) continue;
                Material m = Material.matchMaterial(s.getString("material", iid));
                if (m == null || !m.isItem()) {
                    plugin.getLogger().warning("shops.yml: unknown material in " + cid + "." + iid + ", skipped.");
                    continue;
                }
                ShopItem it = new ShopItem(
                        cid + "/" + iid, cid, m,
                        s.getDouble("buy", -1), s.getDouble("sell", -1),
                        plugin.lang().pick(s, "name"),
                        plugin.lang().pickList(s, "lore"),
                        s.getStringList("commands"),
                        s.getString("head"));
                list.add(it);
                items.put(it.key(), it);
                materialIndex.putIfAbsent(m, it);
                if (it.sellable()) sellIndex.putIfAbsent(m, it);
            }
        }
        categories.put(cid, new ShopCategory(cid, name, icon, head, c.getInt("slot", -1),
                plugin.lang().pickList(c, "lore"), list));
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        demand.clear();
        ConfigurationSection d = data.getConfigurationSection("demand");
        if (d != null) {
            for (String k : d.getKeys(false)) demand.put(k, d.getDouble(k, 1.0));
        }
    }

    public void saveData() {
        if (dataFile == null) return;
        data.set("demand", null);
        for (Map.Entry<String, Double> e : demand.entrySet()) {
            data.set("demand." + e.getKey(), e.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    public void startTasks() {
        FileConfiguration c = plugin.getConfig();
        long minutes = Math.max(1, c.getLong("dynamic-pricing.recovery-interval-minutes", 10));
        long ticks = minutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::recover, ticks, ticks);
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveData, 6000L, 6000L);
    }

    // ---------------------------------------------------------------- access

    public YamlConfiguration data() {
        return data;
    }

    public Collection<ShopCategory> categories() {
        return categories.values();
    }

    public ShopCategory category(String id) {
        if (id == null) return null;
        ShopCategory c = categories.get(id);
        if (c != null) return c;
        for (ShopCategory cat : categories.values()) {
            if (cat.id().equalsIgnoreCase(id)) return cat;
        }
        return null;
    }

    public ShopItem item(String key) {
        return items.get(key);
    }

    public int itemCount() {
        return items.size();
    }

    /** Item that can be sold for the given material (null if none). */
    public ShopItem sellItem(Material m) {
        return sellIndex.get(m);
    }

    /** Any shop entry for the material (used by /worth). */
    public ShopItem itemByMaterial(Material m) {
        return materialIndex.get(m);
    }

    // ---------------------------------------------------------------- prices

    private boolean dynamic() {
        return plugin.getConfig().getBoolean("dynamic-pricing.enabled", false);
    }

    public boolean dynamicEnabled() {
        return dynamic();
    }

    public double multiplier(ShopItem it) {
        return dynamic() ? demand.getOrDefault(it.key(), 1.0) : 1.0;
    }

    public double buyPrice(ShopItem it) {
        return roundPrice(it.buy() * multiplier(it));
    }

    public double sellPrice(ShopItem it) {
        return roundPrice(it.sell() * multiplier(it));
    }

    private static double roundPrice(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public void recordBuy(ShopItem it, int amount) {
        adjust(it, plugin.getConfig().getDouble("dynamic-pricing.step", 0.0015) * amount);
    }

    public void recordSell(ShopItem it, int amount) {
        adjust(it, -plugin.getConfig().getDouble("dynamic-pricing.step", 0.0015) * amount);
    }

    private void adjust(ShopItem it, double delta) {
        if (!dynamic()) return;
        FileConfiguration c = plugin.getConfig();
        double min = c.getDouble("dynamic-pricing.min", 0.5);
        double max = c.getDouble("dynamic-pricing.max", 2.0);
        double v = demand.getOrDefault(it.key(), 1.0) + delta;
        demand.put(it.key(), Math.max(min, Math.min(max, v)));
    }

    private void recover() {
        double rate = plugin.getConfig().getDouble("dynamic-pricing.recovery-rate", 0.1);
        demand.replaceAll((k, v) -> v + (1.0 - v) * rate);
        demand.values().removeIf(v -> Math.abs(v - 1.0) < 0.005);
    }

    public void resetDemand() {
        demand.clear();
        saveData();
    }

    /** Capitalizes category names when gui.uppercase-names is on. */
    public String displayName(ShopCategory c) {
        String n = c.name();
        return plugin.getConfig().getBoolean("gui.uppercase-names", true) ? n.toUpperCase(Locale.ROOT) : n;
    }
}

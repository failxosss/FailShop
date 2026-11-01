package cz.failshop.shop;

import cz.failshop.FailShop;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Rotating discounted items ("daily deals"). */
public final class DealManager {

    public record Deal(ShopItem item, double discount) {
    }

    private final FailShop plugin;
    private final ShopManager shop;
    private final List<Deal> deals = new ArrayList<>();
    private long expires;

    public DealManager(FailShop plugin, ShopManager shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("daily-deals.enabled", false);
    }

    public List<Deal> list() {
        return Collections.unmodifiableList(deals);
    }

    public long timeLeft() {
        return expires - System.currentTimeMillis();
    }

    public void load() {
        deals.clear();
        expires = shop.data().getLong("deals.expires", 0L);
        for (String s : shop.data().getStringList("deals.items")) {
            int i = s.lastIndexOf(':');
            if (i <= 0) continue;
            ShopItem it = shop.item(s.substring(0, i));
            if (it == null) continue;
            try {
                deals.add(new Deal(it, Double.parseDouble(s.substring(i + 1))));
            } catch (NumberFormatException ignored) {
                // skip broken entry
            }
        }
        if (enabled() && (deals.isEmpty() || System.currentTimeMillis() >= expires)) roll();
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (enabled() && System.currentTimeMillis() >= expires) roll();
        }, 1200L, 1200L);
    }

    public void roll() {
        FileConfiguration c = plugin.getConfig();
        List<ShopItem> pool = new ArrayList<>();
        for (ShopCategory cat : shop.categories()) {
            for (ShopItem it : cat.items()) {
                if (it.buyable() && !it.commandItem()) pool.add(it);
            }
        }
        Collections.shuffle(pool);

        int count = Math.min(Math.min(c.getInt("daily-deals.count", 5), pool.size()), 45);
        int min = Math.max(1, c.getInt("daily-deals.min-discount", 15));
        int max = Math.max(min, c.getInt("daily-deals.max-discount", 45));

        deals.clear();
        for (int i = 0; i < count; i++) {
            int percent = ThreadLocalRandom.current().nextInt(min, max + 1);
            deals.add(new Deal(pool.get(i), percent / 100.0));
        }
        double hours = Math.max(0.1, c.getDouble("daily-deals.duration-hours", 24));
        expires = System.currentTimeMillis() + (long) (hours * 3_600_000L);

        List<String> raw = new ArrayList<>();
        for (Deal d : deals) raw.add(d.item().key() + ":" + d.discount());
        shop.data().set("deals.expires", expires);
        shop.data().set("deals.items", raw);
        shop.saveData();
    }
}

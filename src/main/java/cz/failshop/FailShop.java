package cz.failshop;

import cz.failshop.command.ShopAdminCommand;
import cz.failshop.command.ShopCommand;
import cz.failshop.command.SellCommand;
import cz.failshop.command.WorthCommand;
import cz.failshop.config.Lang;
import cz.failshop.economy.EconomyHook;
import cz.failshop.gui.MenuBuilder;
import cz.failshop.gui.MenuListener;
import cz.failshop.shop.DealManager;
import cz.failshop.shop.ShopManager;
import cz.failshop.shop.TradeService;
import cz.failshop.shop.TransactionLog;
import cz.failshop.update.UpdateChecker;
import cz.failshop.util.Text;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FailShop extends JavaPlugin {

    private Lang lang;
    private EconomyHook economy;
    private ShopManager shop;
    private DealManager deals;
    private TradeService trade;
    private MenuBuilder menus;
    private TransactionLog txLog;
    private UpdateChecker updater;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lang = new Lang(this);
        lang.load();

        economy = new EconomyHook();
        shop = new ShopManager(this);
        deals = new DealManager(this, shop);
        trade = new TradeService(this);
        menus = new MenuBuilder(this);
        txLog = new TransactionLog(this);
        updater = new UpdateChecker(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        register("shop", new ShopCommand(this));
        register("sell", new SellCommand(this));
        register("worth", new WorthCommand(this));
        register("shopadmin", new ShopAdminCommand(this));

        // Economy providers register while the server starts, so hook in one tick later.
        getServer().getScheduler().runTask(this, () -> {
            if (!economy.setup()) {
                getLogger().severe("No Vault economy provider found (install EssentialsX or a similar plugin). Disabling FailShop.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            shop.load();
            deals.load();
            shop.startTasks();
            deals.startTask();
            updater.start();
            getLogger().info("Loaded " + shop.categories().size() + " categories with " + shop.itemCount()
                    + " items (language: " + lang.code() + ").");
        });
    }

    @Override
    public void onDisable() {
        if (shop != null) shop.saveData();
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) return;
        cmd.setExecutor(executor);
        if (executor instanceof TabCompleter tab) cmd.setTabCompleter(tab);
    }

    /** Reloads config, language, shops.yml and the daily deals. */
    public void reloadAll() {
        reloadConfig();
        lang.load();
        shop.saveData();
        shop.load();
        deals.load();
    }

    /** The plugin's own jar file (used by the auto-updater). */
    public File getPluginFile() {
        return getFile();
    }

    // ---------------------------------------------------------------- texts

    public String raw(String key, String... pairs) {
        String s = lang.get(key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return Text.color(s);
    }

    public List<String> rawList(String key, String... pairs) {
        List<String> out = new ArrayList<>();
        for (String line : lang.list(key)) {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                line = line.replace("{" + pairs[i] + "}", pairs[i + 1]);
            }
            out.add(Text.color(line));
        }
        return out;
    }

    public String msg(String key, String... pairs) {
        return raw("prefix") + raw(key, pairs);
    }

    public void send(CommandSender to, String key, String... pairs) {
        to.sendMessage(msg(key, pairs));
    }

    /** A price with the currency symbol, still containing &-codes (colored by raw()/rawList()). */
    public String price(double value) {
        return "&f" + Text.money(value) + " " + currency();
    }

    public String currency() {
        return getConfig().getString("currency-symbol", "&6$");
    }

    public void sound(Player p, boolean success) {
        String key = getConfig().getString(success ? "sounds.success" : "sounds.error", "");
        if (key != null && !key.isEmpty()) p.playSound(p.getLocation(), key, 1f, 1f);
    }

    // ---------------------------------------------------------------- accessors

    public Lang lang() {
        return lang;
    }

    public EconomyHook economy() {
        return economy;
    }

    public ShopManager shop() {
        return shop;
    }

    public DealManager deals() {
        return deals;
    }

    public TradeService trade() {
        return trade;
    }

    public MenuBuilder menus() {
        return menus;
    }

    public TransactionLog txLog() {
        return txLog;
    }
}

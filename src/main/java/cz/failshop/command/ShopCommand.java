package cz.failshop.command;

import cz.failshop.FailShop;
import cz.failshop.shop.ShopCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopCommand implements CommandExecutor, TabCompleter {

    private final FailShop plugin;

    public ShopCommand(FailShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "player-only");
            return true;
        }
        if (args.length == 0) {
            plugin.menus().openMain(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("deals")) {
            if (!plugin.deals().enabled()) {
                plugin.send(p, "deals-disabled");
            } else {
                plugin.menus().openDeals(p);
            }
            return true;
        }
        ShopCategory c = plugin.shop().category(args[0]);
        if (c == null) {
            plugin.send(p, "unknown-category", "category", args[0]);
            return true;
        }
        plugin.menus().openCategory(p, c.id(), 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length != 1) return out;
        String prefix = args[0].toLowerCase(Locale.ROOT);
        for (ShopCategory c : plugin.shop().categories()) {
            if (c.id().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(c.id());
        }
        if (plugin.deals().enabled() && "deals".startsWith(prefix)) out.add("deals");
        return out;
    }
}

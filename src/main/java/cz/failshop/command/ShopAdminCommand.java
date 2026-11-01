package cz.failshop.command;

import cz.failshop.FailShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB = List.of("reload", "refresh-deals", "reset-prices");

    private final FailShop plugin;

    public ShopAdminCommand(FailShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            plugin.send(sender, "admin-usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.send(sender, "reloaded", "language", plugin.lang().code());
            }
            case "refresh-deals" -> {
                if (!plugin.deals().enabled()) {
                    plugin.send(sender, "deals-disabled");
                } else {
                    plugin.deals().roll();
                    plugin.send(sender, "deals-refreshed");
                }
            }
            case "reset-prices" -> {
                plugin.shop().resetDemand();
                plugin.send(sender, "prices-reset");
            }
            default -> plugin.send(sender, "admin-usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUB) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        }
        return out;
    }
}

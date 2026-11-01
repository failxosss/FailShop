package cz.failshop.command;

import cz.failshop.FailShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SellCommand implements CommandExecutor, TabCompleter {

    private final FailShop plugin;

    public SellCommand(FailShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "player-only");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("hand")) {
            plugin.trade().sellHand(p);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
            plugin.trade().sellAll(p);
        } else {
            plugin.send(p, "sell-usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if ("hand".startsWith(args[0].toLowerCase())) out.add("hand");
            if ("all".startsWith(args[0].toLowerCase())) out.add("all");
        }
        return out;
    }
}

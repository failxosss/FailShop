package cz.failshop.command;

import cz.failshop.FailShop;
import cz.failshop.shop.ShopItem;
import cz.failshop.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class WorthCommand implements CommandExecutor {

    private final FailShop plugin;

    public WorthCommand(FailShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "player-only");
            return true;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            plugin.send(p, "no-item-in-hand");
            return true;
        }
        ShopItem it = plugin.shop().itemByMaterial(hand.getType());
        if (it == null) {
            plugin.send(p, "worth-none");
            return true;
        }
        String none = plugin.raw("not-available");
        String buy = it.buyable() ? plugin.price(plugin.shop().buyPrice(it)) : none;
        String sell = it.sellable() ? plugin.price(plugin.shop().sellPrice(it)) : none;
        String name = it.name() != null ? Text.color(it.name()) : Text.pretty(it.material());
        plugin.send(p, "worth", "item", name, "buy", buy, "sell", sell);
        return true;
    }
}

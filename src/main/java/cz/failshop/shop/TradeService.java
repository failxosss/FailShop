package cz.failshop.shop;

import cz.failshop.FailShop;
import cz.failshop.economy.EconomyHook;
import cz.failshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Buying and selling logic shared by the GUI and the commands. */
public final class TradeService {

    private static final String MULT_PERM = "failshop.sellmultiplier.";

    private final FailShop plugin;

    public TradeService(FailShop plugin) {
        this.plugin = plugin;
    }

    private ShopManager shop() {
        return plugin.shop();
    }

    private EconomyHook eco() {
        return plugin.economy();
    }

    private String name(ShopItem it) {
        return it.name() != null ? Text.color(it.name()) : Text.pretty(it.material());
    }

    // ---------------------------------------------------------------- buying

    public void buy(Player p, ShopItem it, int qty, double discount) {
        if (!it.buyable()) {
            fail(p, "cannot-buy");
            return;
        }
        final int amount = it.commandItem() ? 1 : Math.max(1, qty);

        if (!it.commandItem() && !fits(p, it.material(), amount)) {
            fail(p, "inventory-full");
            return;
        }

        double unit = shop().buyPrice(it) * (1.0 - discount);
        double total = Text.round(unit * amount);

        if (!eco().has(p, total)) {
            plugin.send(p, "not-enough-money", "price", plugin.price(total));
            plugin.sound(p, false);
            return;
        }
        if (!eco().withdraw(p, total)) {
            fail(p, "transaction-failed");
            return;
        }

        if (it.commandItem()) {
            for (String cmd : it.commands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
            }
        } else {
            Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(it.material(), amount));
            for (ItemStack rest : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
        }

        shop().recordBuy(it, amount);
        plugin.txLog().log("BUY", p.getName(), it.key(), amount, total);
        plugin.send(p, "bought", "amount", String.valueOf(amount), "item", name(it), "price", plugin.price(total));
        plugin.sound(p, true);
    }

    private boolean fits(Player p, Material m, int qty) {
        int max = m.getMaxStackSize();
        int space = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType().isAir()) {
                space += max;
            } else if (s.getType() == m && !s.hasItemMeta()) {
                space += Math.max(0, max - s.getAmount());
            }
            if (space >= qty) return true;
        }
        return space >= qty;
    }

    // ---------------------------------------------------------------- selling

    /** Sells up to {@code qty} plain items of this shop entry from the inventory. */
    public void sell(Player p, ShopItem it, int qty) {
        if (!it.sellable()) {
            fail(p, "cannot-sell");
            return;
        }
        int have = countPlain(p, it.material());
        if (have <= 0) {
            fail(p, "nothing-to-sell");
            return;
        }
        int amount = Math.min(have, Math.max(1, qty));
        double mult = sellMultiplier(p);
        double total = Text.round(shop().sellPrice(it) * amount * mult);

        if (!eco().deposit(p, total)) {
            fail(p, "transaction-failed");
            return;
        }
        removePlain(p, it.material(), amount);
        shop().recordSell(it, amount);
        plugin.txLog().log("SELL", p.getName(), it.key(), amount, total);
        plugin.send(p, "sold", "amount", String.valueOf(amount), "item", name(it),
                "price", plugin.price(total), "bonus", bonus(mult));
        plugin.sound(p, true);
    }

    /** /sell hand - sells the stack in the main hand. */
    public void sellHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            fail(p, "no-item-in-hand");
            return;
        }
        ShopItem it = hand.hasItemMeta() ? null : shop().sellItem(hand.getType());
        if (it == null) {
            fail(p, "cannot-sell");
            return;
        }
        int amount = hand.getAmount();
        double mult = sellMultiplier(p);
        double total = Text.round(shop().sellPrice(it) * amount * mult);

        if (!eco().deposit(p, total)) {
            fail(p, "transaction-failed");
            return;
        }
        p.getInventory().setItemInMainHand(null);
        shop().recordSell(it, amount);
        plugin.txLog().log("SELL", p.getName(), it.key(), amount, total);
        plugin.send(p, "sold", "amount", String.valueOf(amount), "item", name(it),
                "price", plugin.price(total), "bonus", bonus(mult));
        plugin.sound(p, true);
    }

    /** Sells every sellable plain item in the inventory. */
    public void sellAll(Player p) {
        PlayerInventory inv = p.getInventory();
        Map<ShopItem, Integer> totals = new LinkedHashMap<>();
        for (ItemStack s : inv.getStorageContents()) {
            if (s == null || s.getType().isAir() || s.hasItemMeta()) continue;
            ShopItem it = shop().sellItem(s.getType());
            if (it != null) totals.merge(it, s.getAmount(), Integer::sum);
        }
        if (totals.isEmpty()) {
            fail(p, "nothing-to-sell");
            return;
        }

        double mult = sellMultiplier(p);
        double sum = 0;
        int count = 0;
        Set<Material> materials = new HashSet<>();
        for (Map.Entry<ShopItem, Integer> e : totals.entrySet()) {
            sum += shop().sellPrice(e.getKey()) * e.getValue();
            count += e.getValue();
            materials.add(e.getKey().material());
        }
        double total = Text.round(sum * mult);

        if (!eco().deposit(p, total)) {
            fail(p, "transaction-failed");
            return;
        }

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack s = contents[i];
            if (s != null && !s.hasItemMeta() && materials.contains(s.getType())) contents[i] = null;
        }
        inv.setStorageContents(contents);

        for (Map.Entry<ShopItem, Integer> e : totals.entrySet()) {
            shop().recordSell(e.getKey(), e.getValue());
            plugin.txLog().log("SELL", p.getName(), e.getKey().key(), e.getValue(), 0);
        }
        plugin.send(p, "sold-all", "amount", String.valueOf(count),
                "price", plugin.price(total), "bonus", bonus(mult));
        plugin.sound(p, true);
    }

    // ---------------------------------------------------------------- helpers

    private int countPlain(Player p, Material m) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s != null && s.getType() == m && !s.hasItemMeta()) n += s.getAmount();
        }
        return n;
    }

    private void removePlain(Player p, Material m, int amount) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getStorageContents();
        int left = amount;
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() != m || s.hasItemMeta()) continue;
            if (s.getAmount() <= left) {
                left -= s.getAmount();
                contents[i] = null;
            } else {
                s.setAmount(s.getAmount() - left);
                left = 0;
            }
        }
        inv.setStorageContents(contents);
    }

    public double sellMultiplier(Player p) {
        double best = 1.0;
        for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission().toLowerCase();
            if (!perm.startsWith(MULT_PERM)) continue;
            try {
                best = Math.max(best, Double.parseDouble(perm.substring(MULT_PERM.length())));
            } catch (NumberFormatException ignored) {
                // wildcard or invalid node
            }
        }
        return best;
    }

    private String bonus(double mult) {
        if (mult <= 1.0) return "";
        long percent = Math.round((mult - 1.0) * 100.0);
        return plugin.raw("sell-bonus", "percent", String.valueOf(percent));
    }

    private void fail(Player p, String key) {
        plugin.send(p, key);
        plugin.sound(p, false);
    }
}

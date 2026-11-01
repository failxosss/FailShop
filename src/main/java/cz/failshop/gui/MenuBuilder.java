package cz.failshop.gui;

import cz.failshop.FailShop;
import cz.failshop.shop.DealManager;
import cz.failshop.shop.ShopCategory;
import cz.failshop.shop.ShopItem;
import cz.failshop.shop.ShopManager;
import cz.failshop.util.Heads;
import cz.failshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds the 6-row shop menus (main menu, category pages, daily deals). */
public final class MenuBuilder {

    public static final int SIZE = 54;

    // bottom row
    public static final int BALANCE_SLOT = 45;
    public static final int PREV_SLOT = 47;
    public static final int DEALS_SLOT = 48;
    public static final int BACK_SLOT = 49;
    public static final int SELLALL_SLOT = 50;
    public static final int NEXT_SLOT = 51;
    public static final int CLOSE_SLOT = 53;

    private static final int PER_PAGE = 45;

    private final FailShop plugin;

    public MenuBuilder(FailShop plugin) {
        this.plugin = plugin;
    }

    private ShopManager shop() {
        return plugin.shop();
    }

    // ---------------------------------------------------------------- opening

    public void openMain(Player p) {
        ShopMenu menu = new ShopMenu(ShopMenu.Type.MAIN, null, 0);
        Inventory inv = Bukkit.createInventory(menu, SIZE, up(plugin.raw("gui.main-title")));
        menu.setInventory(inv);
        render(menu, p);
        p.openInventory(inv);
    }

    public void openCategory(Player p, String categoryId, int page) {
        ShopCategory c = shop().category(categoryId);
        if (c == null) return;
        ShopMenu menu = new ShopMenu(ShopMenu.Type.CATEGORY, c.id(), Math.max(0, page));
        Inventory inv = Bukkit.createInventory(menu, SIZE,
                up(plugin.raw("gui.category-title", "category", shop().displayName(c))));
        menu.setInventory(inv);
        render(menu, p);
        p.openInventory(inv);
    }

    public void openDeals(Player p) {
        ShopMenu menu = new ShopMenu(ShopMenu.Type.DEALS, null, 0);
        Inventory inv = Bukkit.createInventory(menu, SIZE, up(plugin.raw("gui.deals-title")));
        menu.setInventory(inv);
        render(menu, p);
        p.openInventory(inv);
    }

    // ---------------------------------------------------------------- rendering

    public void render(ShopMenu menu, Player p) {
        Inventory inv = menu.getInventory();
        inv.clear();
        menu.clearMaps();
        fill(inv);

        switch (menu.type()) {
            case MAIN -> renderMain(menu, inv);
            case CATEGORY -> renderCategory(menu, inv);
            case DEALS -> renderDeals(menu, inv);
        }
        renderBottom(menu, inv, p);
    }

    private void renderMain(ShopMenu menu, Inventory inv) {
        Set<Integer> used = new HashSet<>();
        List<ShopCategory> auto = new ArrayList<>();

        for (ShopCategory c : shop().categories()) {
            int slot = c.slot();
            if (slot >= 0 && slot < 45 && used.add(slot)) {
                place(menu, inv, c, slot);
            } else {
                auto.add(c);
            }
        }

        int next = 10;
        for (ShopCategory c : auto) {
            while (next < 45 && (used.contains(next) || next % 9 == 0 || next % 9 == 8)) next++;
            if (next >= 45) break;
            used.add(next);
            place(menu, inv, c, next);
        }
    }

    private void place(ShopMenu menu, Inventory inv, ShopCategory c, int slot) {
        inv.setItem(slot, categoryIcon(c));
        menu.categorySlots().put(slot, c.id());
    }

    private void renderCategory(ShopMenu menu, Inventory inv) {
        ShopCategory c = shop().category(menu.categoryId());
        if (c == null) return;

        List<ShopItem> list = c.items();
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) PER_PAGE));
        int page = Math.max(0, Math.min(menu.page(), pages - 1));
        int start = page * PER_PAGE;

        for (int i = start; i < Math.min(list.size(), start + PER_PAGE); i++) {
            int slot = i - start;
            ShopItem it = list.get(i);
            inv.setItem(slot, display(it, 0));
            menu.itemSlots().put(slot, it);
        }
        menu.setPaging(page > 0, page < pages - 1);
    }

    private void renderDeals(ShopMenu menu, Inventory inv) {
        int slot = 0;
        for (DealManager.Deal d : plugin.deals().list()) {
            if (slot >= PER_PAGE) break;
            inv.setItem(slot, display(d.item(), d.discount()));
            menu.itemSlots().put(slot, d.item());
            menu.discounts().put(slot, d.discount());
            slot++;
        }
    }

    private void renderBottom(ShopMenu menu, Inventory inv, Player p) {
        String balance = plugin.price(plugin.economy().balance(p));
        inv.setItem(BALANCE_SLOT, button(Material.GOLD_INGOT, plugin.raw("gui.balance-name"),
                plugin.rawList("gui.balance-lore", "balance", balance)));
        inv.setItem(CLOSE_SLOT, button(Material.BARRIER, plugin.raw("gui.close-name"), null));

        if (menu.type() == ShopMenu.Type.MAIN) {
            if (plugin.deals().enabled()) {
                inv.setItem(DEALS_SLOT, button(Material.NETHER_STAR, plugin.raw("gui.deals-name"),
                        plugin.rawList("gui.deals-lore", "time", Text.duration(plugin.deals().timeLeft()))));
            }
            if (plugin.getConfig().getBoolean("gui.sell-all-button", false)) {
                inv.setItem(SELLALL_SLOT, button(Material.EMERALD, plugin.raw("gui.sellall-name"),
                        plugin.rawList("gui.sellall-lore")));
            }
        } else {
            inv.setItem(BACK_SLOT, button(Material.ARROW, plugin.raw("gui.back-name"), null));
            if (menu.hasPrev()) {
                inv.setItem(PREV_SLOT, button(Material.SPECTRAL_ARROW, plugin.raw("gui.prev-name"), null));
            }
            if (menu.hasNext()) {
                inv.setItem(NEXT_SLOT, button(Material.SPECTRAL_ARROW, plugin.raw("gui.next-name"), null));
            }
        }
    }

    // ---------------------------------------------------------------- items

    private ItemStack categoryIcon(ShopCategory c) {
        ItemStack stack = base(c.icon(), categoryHead(c));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(ChatColor.RESET + up(plugin.raw("gui.category-name", "name", shop().displayName(c))));

        List<String> lore = new ArrayList<>();
        for (String l : c.lore()) lore.add(line(Text.color(l)));
        for (String l : plugin.rawList("gui.category-lore")) lore.add(line(l));
        if (!lore.isEmpty()) meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    /** The item as shown in a category: vanilla name + price lines + click hints. */
    private ItemStack display(ShopItem it, double discount) {
        ItemStack stack = base(it.material(), itemHead(it));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String name = it.name() != null
                ? Text.color(it.name())
                : Text.color(plugin.getConfig().getString("gui.item-name-color", "&f")) + Text.pretty(it.material());
        meta.setDisplayName(ChatColor.RESET + up(name));

        List<String> lore = new ArrayList<>();
        for (String l : it.lore()) lore.add(line(Text.color(l)));
        lore.add("");

        if (it.buyable()) {
            double buy = shop().buyPrice(it);
            lore.add(line(plugin.raw("lore.buy-title")));
            if (discount > 0) {
                lore.add(line(plugin.raw("lore.deal-price",
                        "old", Text.money(buy),
                        "price", plugin.price(buy * (1.0 - discount)),
                        "discount", String.valueOf(Math.round(discount * 100)))));
            } else {
                lore.add(line(plugin.raw("lore.price-line", "price", plugin.price(buy))));
            }
            lore.add("");
        }
        if (it.sellable()) {
            lore.add(line(plugin.raw("lore.sell-title")));
            lore.add(line(plugin.raw("lore.price-line", "price", plugin.price(shop().sellPrice(it)))));
            lore.add("");
        }
        if (shop().dynamicEnabled() && (it.buyable() || it.sellable())) {
            long percent = Math.round(shop().multiplier(it) * 100.0);
            lore.add(line(plugin.raw("lore.market", "percent", String.valueOf(percent))));
            lore.add("");
        }

        if (it.buyable()) lore.add(line(plugin.raw("lore.controls-buy")));
        if (it.sellable()) {
            lore.add(line(plugin.raw("lore.controls-sell-all")));
            lore.add(line(plugin.raw("lore.controls-sell")));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private String line(String s) {
        return ChatColor.RESET + up(s);
    }

    /** Own head of the category (price.yml) or the default nick from config (gui.category-head). */
    private String categoryHead(ShopCategory c) {
        if (c.head() != null && !c.head().isBlank()) return c.head();
        return plugin.getConfig().getString("gui.category-head", "");
    }

    /** Own head of the item (price.yml) or the default nick from config (gui.item-head). */
    private String itemHead(ShopItem it) {
        if (it.head() != null && !it.head().isBlank()) return it.head();
        return plugin.getConfig().getString("gui.item-head", "");
    }

    /** Applies the text style of gui.text-style (capital letters by default) to a menu text. */
    private String up(String s) {
        return Text.style(s, plugin.getConfig().getString("gui.text-style", "uppercase"));
    }

    private ItemStack base(Material material, String head) {
        if (head != null && !head.isBlank()) return Heads.create(head);
        return new ItemStack(material);
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(ChatColor.RESET + up(name));
        if (lore != null && !lore.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (String l : lore) out.add(line(l));
            meta.setLore(out);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private void fill(Inventory inv) {
        Material m = Material.matchMaterial(plugin.getConfig().getString("gui.filler", "GRAY_STAINED_GLASS_PANE"));
        if (m == null) m = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(m);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }
}

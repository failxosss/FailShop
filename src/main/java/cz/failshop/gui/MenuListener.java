package cz.failshop.gui;

import cz.failshop.FailShop;
import cz.failshop.shop.ShopItem;
import cz.failshop.shop.TradeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    private final FailShop plugin;

    public MenuListener(FailShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ShopMenu) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopMenu menu)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getInventory())) return;

        int slot = e.getSlot();
        if (slot == MenuBuilder.CLOSE_SLOT) {
            p.closeInventory();
            return;
        }

        if (menu.type() == ShopMenu.Type.MAIN) {
            handleMain(p, menu, slot);
        } else {
            handleItems(p, menu, slot, e.getClick());
        }
    }

    private void handleMain(Player p, ShopMenu menu, int slot) {
        String category = menu.categorySlots().get(slot);
        if (category != null) {
            plugin.menus().openCategory(p, category, 0);
            return;
        }
        if (slot == MenuBuilder.DEALS_SLOT && plugin.deals().enabled()) {
            plugin.menus().openDeals(p);
            return;
        }
        if (slot == MenuBuilder.SELLALL_SLOT && plugin.getConfig().getBoolean("gui.sell-all-button", false)) {
            plugin.trade().sellAll(p);
            plugin.menus().render(menu, p);
        }
    }

    private void handleItems(Player p, ShopMenu menu, int slot, ClickType click) {
        MenuBuilder menus = plugin.menus();

        if (slot == MenuBuilder.BACK_SLOT) {
            menus.openMain(p);
            return;
        }
        if (menu.type() == ShopMenu.Type.CATEGORY) {
            if (slot == MenuBuilder.PREV_SLOT && menu.hasPrev()) {
                menus.openCategory(p, menu.categoryId(), menu.page() - 1);
                return;
            }
            if (slot == MenuBuilder.NEXT_SLOT && menu.hasNext()) {
                menus.openCategory(p, menu.categoryId(), menu.page() + 1);
                return;
            }
        }

        ShopItem it = menu.itemSlots().get(slot);
        if (it == null) return;
        double discount = menu.discounts().getOrDefault(slot, 0.0);
        TradeService trade = plugin.trade();

        switch (click) {
            case LEFT -> trade.buy(p, it, 1, discount);
            case SHIFT_LEFT -> trade.buy(p, it, it.commandItem() ? 1 : it.material().getMaxStackSize(), discount);
            case RIGHT -> trade.sell(p, it, 1);
            case SHIFT_RIGHT -> trade.sell(p, it, Integer.MAX_VALUE);
            default -> {
                return;
            }
        }
        menus.render(menu, p);
    }
}

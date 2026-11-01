package cz.failshop.gui;

import cz.failshop.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/** Holder that identifies a FailShop inventory and remembers what each slot does. */
public final class ShopMenu implements InventoryHolder {

    public enum Type { MAIN, CATEGORY, DEALS }

    private final Type type;
    private final String categoryId;
    private final int page;

    private Inventory inventory;
    private boolean hasPrev;
    private boolean hasNext;

    private final Map<Integer, ShopItem> itemSlots = new HashMap<>();
    private final Map<Integer, Double> discounts = new HashMap<>();
    private final Map<Integer, String> categorySlots = new HashMap<>();

    public ShopMenu(Type type, String categoryId, int page) {
        this.type = type;
        this.categoryId = categoryId;
        this.page = page;
    }

    public Type type() {
        return type;
    }

    public String categoryId() {
        return categoryId;
    }

    public int page() {
        return page;
    }

    public boolean hasPrev() {
        return hasPrev;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public void setPaging(boolean prev, boolean next) {
        this.hasPrev = prev;
        this.hasNext = next;
    }

    public Map<Integer, ShopItem> itemSlots() {
        return itemSlots;
    }

    public Map<Integer, Double> discounts() {
        return discounts;
    }

    public Map<Integer, String> categorySlots() {
        return categorySlots;
    }

    public void clearMaps() {
        itemSlots.clear();
        discounts.clear();
        categorySlots.clear();
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

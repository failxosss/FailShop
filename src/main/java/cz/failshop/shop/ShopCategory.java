package cz.failshop.shop;

import org.bukkit.Material;

import java.util.List;

public record ShopCategory(String id, String name, Material icon, String head, int slot,
                           List<String> lore, List<ShopItem> items, String color) {
}

package cz.failshop.shop;

import org.bukkit.Material;

import java.util.List;

public record ShopItem(String key, String categoryId, Material material, double buy, double sell,
                       String name, List<String> lore, List<String> commands, String head) {

    public boolean buyable() {
        return buy >= 0;
    }

    public boolean commandItem() {
        return !commands.isEmpty();
    }

    public boolean sellable() {
        return sell >= 0 && commands.isEmpty();
    }
}

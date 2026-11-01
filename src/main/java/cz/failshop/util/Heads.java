package cz.failshop.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creates player heads with a custom texture (base64 value, texture URL or texture hash). */
public final class Heads {

    private static final Pattern URL_IN_JSON = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private Heads() {
    }

    public static ItemStack create(String texture) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        apply(stack, texture);
        return stack;
    }

    public static void apply(ItemStack stack, String texture) {
        if (texture == null || texture.isBlank()) return;
        String url = resolveUrl(texture.trim());
        if (url == null) return;
        if (!(stack.getItemMeta() instanceof SkullMeta meta)) return;
        try {
            UUID id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createPlayerProfile(id, "FailShop");
            profile.getTextures().setSkin(new URL(url));
            meta.setOwnerProfile(profile);
            stack.setItemMeta(meta);
        } catch (Exception ignored) {
            // invalid texture - the plain head is shown instead
        }
    }

    private static String resolveUrl(String t) {
        if (t.startsWith("http://") || t.startsWith("https://")) return t;
        if (t.matches("[0-9a-fA-F]{40,70}")) return "http://textures.minecraft.net/texture/" + t;
        try {
            String json = new String(Base64.getDecoder().decode(t), StandardCharsets.UTF_8);
            Matcher m = URL_IN_JSON.matcher(json);
            if (m.find()) return m.group(1);
        } catch (IllegalArgumentException ignored) {
            // not base64
        }
        return null;
    }
}

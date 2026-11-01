package cz.failshop.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates player heads from a player nickname (e.g. "Notch") or a custom texture
 * (base64 value, texture URL or texture hash).
 * Skins of nicknames are fetched asynchronously once and cached.
 */
public final class Heads {

    private static final Pattern URL_IN_JSON = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern NICK = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final long RETRY_MILLIS = 5 * 60 * 1000L;

    private static final Map<String, PlayerProfile> PROFILES = new ConcurrentHashMap<>();
    private static final Map<String, Long> RETRY_AFTER = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();

    private Heads() {
    }

    public static boolean isNick(String value) {
        return value != null && NICK.matcher(value.trim()).matches();
    }

    /** Starts loading the skin of a nickname in the background (no-op for textures / already loaded). */
    public static void prefetch(String value) {
        if (!isNick(value)) return;
        String nick = value.trim();
        String key = nick.toLowerCase(Locale.ROOT);
        if (PROFILES.containsKey(key)) return;
        if (System.currentTimeMillis() < RETRY_AFTER.getOrDefault(key, 0L)) return;
        if (!PENDING.add(key)) return;
        try {
            Bukkit.createPlayerProfile(nick).update().whenComplete((profile, error) -> {
                if (profile != null && error == null) {
                    PROFILES.put(key, profile);
                } else {
                    RETRY_AFTER.put(key, System.currentTimeMillis() + RETRY_MILLIS);
                }
                PENDING.remove(key);
            });
        } catch (Exception e) {
            RETRY_AFTER.put(key, System.currentTimeMillis() + RETRY_MILLIS);
            PENDING.remove(key);
        }
    }

    public static ItemStack create(String texture) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        apply(stack, texture);
        return stack;
    }

    public static void apply(ItemStack stack, String texture) {
        if (texture == null || texture.isBlank()) return;
        if (!(stack.getItemMeta() instanceof SkullMeta meta)) return;

        if (isNick(texture)) {
            String nick = texture.trim();
            PlayerProfile profile = PROFILES.get(nick.toLowerCase(Locale.ROOT));
            if (profile == null) {
                prefetch(nick);
                profile = Bukkit.createPlayerProfile(nick);
            }
            meta.setOwnerProfile(profile);
            stack.setItemMeta(meta);
            return;
        }

        String url = resolveUrl(texture.trim());
        if (url == null) return;
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

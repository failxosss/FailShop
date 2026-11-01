package cz.failshop.util;

import org.bukkit.Material;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Text() {
    }

    /** Translates &-codes and &#RRGGBB hex colors. */
    public static String color(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String hex = net.md_5.bungee.api.ChatColor.of("#" + m.group(1)).toString();
            m.appendReplacement(sb, Matcher.quoteReplacement(hex));
        }
        m.appendTail(sb);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static List<String> color(List<String> in) {
        List<String> out = new ArrayList<>(in.size());
        for (String s : in) out.add(color(s));
        return out;
    }

    /** Upper-cases the visible text but leaves color codes (&a, §a, §x§r§r§g§g§b§b) untouched. */
    public static String upper(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < s.length()) {
                sb.append(c).append(s.charAt(i + 1));
                i++;
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    private static final String SC_FROM = "abcdefghijklmnopqrstuvwxyz";
    private static final String SC_TO = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    /**
     * Converts the visible text to small capitals (ᴛɪᴍᴛᴏ). Color codes are left untouched.
     * Accents are removed first (the small-caps alphabet has no accented letters).
     * Letters outside a-z (e.g. Cyrillic) are upper-cased instead.
     */
    public static String smallCaps(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < s.length()) {
                sb.append(c).append(s.charAt(i + 1));
                i++;
                continue;
            }
            char base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).charAt(0);
            int idx = SC_FROM.indexOf(Character.toLowerCase(base));
            sb.append(idx >= 0 ? SC_TO.charAt(idx) : Character.toUpperCase(base));
        }
        return sb.toString();
    }

    /** Applies gui.text-style: "small-caps", "uppercase" or anything else = unchanged. */
    public static String style(String s, String mode) {
        if (mode == null) return s;
        return switch (mode.toLowerCase(Locale.ROOT).trim()) {
            case "small-caps", "smallcaps" -> smallCaps(s);
            case "uppercase", "upper" -> upper(s);
            default -> s;
        };
    }

    public static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String duration(long millis) {
        long minutes = Math.max(0, millis) / 60000L;
        long h = minutes / 60;
        long m = minutes % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }
}

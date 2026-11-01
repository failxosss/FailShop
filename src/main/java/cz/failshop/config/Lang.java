package cz.failshop.config;

import cz.failshop.FailShop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Loads texts from lang/&lt;code&gt;.yml. Missing keys are filled in automatically
 * (from the bundled file of that language, or English) so updates never break a language file.
 */
public final class Lang {

    private final FailShop plugin;
    private YamlConfiguration lang = new YamlConfiguration();
    private YamlConfiguration fallback = new YamlConfiguration();
    private String code = "en";

    public Lang(FailShop plugin) {
        this.plugin = plugin;
    }

    public String code() {
        return code;
    }

    public void load() {
        for (String bundled : new String[]{"en", "cs"}) {
            File f = new File(plugin.getDataFolder(), "lang/" + bundled + ".yml");
            if (!f.exists()) plugin.saveResource("lang/" + bundled + ".yml", false);
        }

        YamlConfiguration en = readBundled("en");
        fallback = en != null ? en : new YamlConfiguration();

        code = plugin.getConfig().getString("language", "en").toLowerCase(Locale.ROOT).trim();
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file lang/" + code + ".yml was not found, using English.");
            code = "en";
            file = new File(plugin.getDataFolder(), "lang/en.yml");
        }
        lang = YamlConfiguration.loadConfiguration(file);
        mergeMissing(file);
    }

    private void mergeMissing(File file) {
        YamlConfiguration source = readBundled(code);
        if (source == null) source = fallback;
        boolean changed = false;
        for (String key : source.getKeys(true)) {
            if (source.isConfigurationSection(key)) continue;
            if (!lang.contains(key)) {
                lang.set(key, source.get(key));
                changed = true;
            }
        }
        if (changed) {
            try {
                lang.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not update " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private YamlConfiguration readBundled(String lc) {
        InputStream in = plugin.getResource("lang/" + lc + ".yml");
        if (in == null) return null;
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            return null;
        }
    }

    public String get(String key) {
        String s = lang.getString(key);
        if (s == null) s = fallback.getString(key);
        return s != null ? s : "&cMissing text: " + key;
    }

    public List<String> list(String key) {
        List<String> l = lang.getStringList(key);
        if (l.isEmpty()) l = fallback.getStringList(key);
        return l;
    }

    /** Reads a value that is either a plain string or a map {cs: "...", en: "..."} (current language wins). */
    public String pick(ConfigurationSection section, String key) {
        if (section == null) return null;
        Object o = section.get(key);
        if (o instanceof String str) return str;
        if (o instanceof ConfigurationSection sec) {
            String v = sec.getString(code);
            if (v == null) v = sec.getString("en");
            if (v == null) {
                for (String k : sec.getKeys(false)) {
                    v = sec.getString(k);
                    if (v != null) break;
                }
            }
            return v;
        }
        return null;
    }

    /** Reads a list that is either a plain list or a map {cs: [...], en: [...]}. */
    public List<String> pickList(ConfigurationSection section, String key) {
        if (section == null) return Collections.emptyList();
        Object o = section.get(key);
        if (o instanceof List<?>) return section.getStringList(key);
        if (o instanceof ConfigurationSection sec) {
            List<String> l = sec.getStringList(code);
            if (l.isEmpty()) l = sec.getStringList("en");
            return l;
        }
        return Collections.emptyList();
    }
}

package cz.failshop.shop;

import cz.failshop.FailShop;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TransactionLog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FailShop plugin;
    private final Path file;

    public TransactionLog(FailShop plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("transactions.log");
    }

    public void log(String type, String player, String item, int amount, double total) {
        if (!plugin.getConfig().getBoolean("log-transactions", true)) return;
        String line = "[" + FMT.format(LocalDateTime.now()) + "] " + type + " " + player + " " + item
                + " x" + amount + " = " + total + System.lineSeparator();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try {
                    Files.writeString(file, line, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException ignored) {
                    // logging must never break a trade
                }
            }
        });
    }
}

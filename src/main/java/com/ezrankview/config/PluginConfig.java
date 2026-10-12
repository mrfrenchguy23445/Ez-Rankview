package com.ezrankview.config;

import com.ezrankview.EzRankView;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed access to config.yml plus a small message helper.
 */
public final class PluginConfig {

    private final EzRankView plugin;

    private boolean applyPermissions;
    private boolean applyChatMeta;
    private int priorityOffset;
    private long expireAfterSeconds;
    private boolean restoreOnQuit;
    private String messagePrefix;

    public PluginConfig(EzRankView plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.applyPermissions = config.getBoolean("preview.apply-permissions", true);
        this.applyChatMeta = config.getBoolean("preview.apply-chat-meta", true);
        this.priorityOffset = config.getInt("preview.priority-offset", 1000);
        this.expireAfterSeconds = config.getLong("preview.expire-after-seconds", 3600L);
        this.restoreOnQuit = config.getBoolean("preview.restore-on-quit", true);
        this.messagePrefix = config.getString("messages.prefix", "&8[&6Ez-RankView&8] &r");
    }

    /**
     * Fetch a (prefixed, colour translated) message, applying %key% replacements.
     *
     * @param path         message path under "messages"
     * @param replacements alternating placeholder/value pairs, e.g. "%group%", "vip"
     */
    public String message(String path, String... replacements) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', messagePrefix + raw);
    }

    public boolean applyPermissions() {
        return applyPermissions;
    }

    public boolean applyChatMeta() {
        return applyChatMeta;
    }

    public int priorityOffset() {
        return priorityOffset;
    }

    public long expireAfterSeconds() {
        return expireAfterSeconds;
    }

    public boolean restoreOnQuit() {
        return restoreOnQuit;
    }
}

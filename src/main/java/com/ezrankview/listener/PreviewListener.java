package com.ezrankview.listener;

import com.ezrankview.config.PluginConfig;
import com.ezrankview.preview.PreviewManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears a player's preview when they disconnect so no temporary nodes are left behind.
 */
public final class PreviewListener implements Listener {

    private final PluginConfig config;
    private final PreviewManager previews;

    public PreviewListener(PluginConfig config, PreviewManager previews) {
        this.config = config;
        this.previews = previews;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!config.restoreOnQuit()) {
            return;
        }
        previews.restore(event.getPlayer().getUniqueId());
    }
}

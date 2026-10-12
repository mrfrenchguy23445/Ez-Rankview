package com.ezrankview;

import com.ezrankview.command.RankViewCommand;
import com.ezrankview.config.PluginConfig;
import com.ezrankview.listener.PreviewListener;
import com.ezrankview.luckperms.LuckPermsService;
import com.ezrankview.preview.PreviewManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ez-RankView - preview a LuckPerms rank without permanently changing anything.
 */
public final class EzRankView extends JavaPlugin {

    private PluginConfig pluginConfig;
    private LuckPermsService luckPermsService;
    private PreviewManager previewManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(this);

        RegisteredServiceProvider<LuckPerms> registration =
                getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) {
            getLogger().severe("LuckPerms was not found. Ez-RankView requires LuckPerms to run.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.luckPermsService = new LuckPermsService(registration.getProvider());
        this.previewManager = new PreviewManager(luckPermsService, pluginConfig);

        // Make sure every group is available for validation and tab-completion.
        this.luckPermsService.api().getGroupManager().loadAllGroups();

        RankViewCommand command = new RankViewCommand(this, luckPermsService, previewManager, pluginConfig);
        PluginCommand pluginCommand = getCommand("rankview");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new PreviewListener(pluginConfig, previewManager), this);

        String version = registration.getProvider().getPluginMetadata().getVersion();
        getLogger().info("Hooked into LuckPerms v" + version + ".");
    }

    @Override
    public void onDisable() {
        if (previewManager != null) {
            previewManager.shutdown();
        }
    }

    public PluginConfig config() {
        return pluginConfig;
    }

    public LuckPermsService luckPerms() {
        return luckPermsService;
    }

    public PreviewManager previews() {
        return previewManager;
    }
}

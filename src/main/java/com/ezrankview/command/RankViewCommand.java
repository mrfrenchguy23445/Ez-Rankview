package com.ezrankview.command;

import com.ezrankview.EzRankView;
import com.ezrankview.config.PluginConfig;
import com.ezrankview.luckperms.LuckPermsService;
import com.ezrankview.preview.PreviewManager;
import com.ezrankview.preview.PreviewSession;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * /rankview &lt;group&gt; [player] - preview a rank
 * /rankview stop [player]        - stop previewing
 * /rankview list                 - list groups and active previews
 * /rankview reload               - reload config.yml
 */
public final class RankViewCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "ezrankview.use";
    private static final String PERM_OTHERS = "ezrankview.others";
    private static final String PERM_RELOAD = "ezrankview.reload";

    private final EzRankView plugin;
    private final LuckPermsService luckPerms;
    private final PreviewManager previews;
    private final PluginConfig config;

    public RankViewCommand(EzRankView plugin, LuckPermsService luckPerms,
                           PreviewManager previews, PluginConfig config) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.previews = previews;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(config.message("usage"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                return reload(sender);
            case "list":
                return list(sender);
            case "stop":
                return stop(sender, args);
            default:
                return preview(sender, args);
        }
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(PERM_RELOAD)) {
            return noPermission(sender);
        }
        config.reload();
        sender.sendMessage(config.message("reloaded"));
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!sender.hasPermission(PERM_USE)) {
            return noPermission(sender);
        }

        sender.sendMessage(config.message("list-header"));
        List<String> groups = luckPerms.groupNames();
        if (groups.isEmpty()) {
            sender.sendMessage(config.message("list-empty"));
        } else {
            for (String group : groups) {
                sender.sendMessage(config.message("list-entry", "%group%", group));
            }
        }

        List<PreviewSession> active = new ArrayList<>(previews.sessions());
        if (!active.isEmpty()) {
            sender.sendMessage(config.message("sessions-header"));
            for (PreviewSession session : active) {
                sender.sendMessage(config.message("session-entry",
                        "%player%", nameOf(session.uuid()),
                        "%group%", session.groupName()));
            }
        }
        return true;
    }

    private boolean stop(CommandSender sender, String[] args) {
        boolean targetingOther = args.length >= 2;

        final Player target;
        if (targetingOther) {
            if (!sender.hasPermission(PERM_OTHERS)) {
                return noPermission(sender);
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(config.message("player-not-found", "%player%", args[1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(config.message("players-only"));
                return true;
            }
            if (!sender.hasPermission(PERM_USE)) {
                return noPermission(sender);
            }
            target = player;
        }

        PreviewSession session = previews.session(target.getUniqueId());
        if (session == null) {
            sender.sendMessage(config.message("not-previewing", "%player%", target.getName()));
            return true;
        }

        String groupName = session.groupName();
        boolean notifyTarget = !target.getUniqueId().equals(senderId(sender));
        previews.restore(target.getUniqueId()).thenRun(() -> main(() -> {
            sender.sendMessage(config.message("stopped", "%group%", groupName));
            if (notifyTarget) {
                target.sendMessage(config.message("stopped-target"));
            }
        }));
        return true;
    }

    private boolean preview(CommandSender sender, String[] args) {
        String groupName = args[0];
        boolean targetingOther = args.length >= 2;

        final Player target;
        if (targetingOther) {
            if (!sender.hasPermission(PERM_OTHERS)) {
                return noPermission(sender);
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(config.message("player-not-found", "%player%", args[1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(config.message("players-only"));
                return true;
            }
            if (!sender.hasPermission(PERM_USE)) {
                return noPermission(sender);
            }
            target = player;
        }

        boolean self = target.getUniqueId().equals(senderId(sender));

        luckPerms.loadGroup(groupName).thenAccept(optional -> main(() -> {
            if (optional.isEmpty()) {
                sender.sendMessage(config.message("group-not-found", "%group%", groupName));
                return;
            }
            startPreview(sender, target, optional.get(), self);
        }));
        return true;
    }

    /**
     * Runs on the main thread. Toggles off if the same group is already previewed,
     * switches if a different group is active, otherwise starts a fresh preview.
     */
    private void startPreview(CommandSender sender, Player target, Group group, boolean self) {
        PreviewSession existing = previews.session(target.getUniqueId());

        if (existing != null && existing.groupName().equalsIgnoreCase(group.getName())) {
            previews.restore(target.getUniqueId()).thenRun(() ->
                    main(() -> sender.sendMessage(config.message("stopped", "%group%", group.getName()))));
            return;
        }

        boolean switching = existing != null;
        CompletableFuture<Void> before = switching
                ? previews.restore(target.getUniqueId())
                : CompletableFuture.completedFuture(null);

        before.thenCompose(ignored -> previews.apply(target.getUniqueId(), group))
                .thenAccept(session -> main(() -> {
                    String prefix = colored(prefixOf(group));
                    sender.sendMessage(config.message(
                            switching ? "switched" : "started",
                            "%group%", group.getName(),
                            "%prefix%", prefix));
                    if (!self) {
                        target.sendMessage(config.message("started-target", "%group%", group.getName()));
                    }
                }))
                .exceptionally(error -> {
                    main(() -> sender.sendMessage(config.message("error", "%error%", String.valueOf(error.getMessage()))));
                    return null;
                });
    }

    private String prefixOf(Group group) {
        return Optional.ofNullable(group.getCachedData().getMetaData().getPrefix()).orElse("");
    }

    private String colored(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString();
    }

    private UUID senderId(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(config.message("no-permission"));
        return true;
    }

    private void main(Runnable task) {
        // Use Bukkit scheduler (works on Bukkit/Spigot/Paper). For Folia users, most commands
        // run from command sender context; command execution is fine. The scheduler here is
        // only used to jump back to the main thread after async LuckPerms calls.
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("stop", "list", "reload"));
            options.addAll(luckPerms.groupNames());
            return filter(options, args[0]);
        }

        if (args.length == 2) {
            boolean knownGroup = luckPerms.groupNames().stream().anyMatch(g -> g.equalsIgnoreCase(args[0]));
            if (args[0].equalsIgnoreCase("stop") || knownGroup) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return filter(players, args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}

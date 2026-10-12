package com.ezrankview.preview;

import com.ezrankview.config.PluginConfig;
import com.ezrankview.luckperms.LuckPermsService;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Applies and removes rank previews.
 *
 * <p>A preview is purely additive: we copy the resolved prefix/suffix of the target
 * group onto the player (at a higher priority so it wins) and, optionally, add a
 * temporary inheritance node so the group's permissions are previewed too. The
 * exact nodes we add are recorded and removed again on stop, so the player's own
 * LuckPerms data is never modified.</p>
 */
public final class PreviewManager {

    private final LuckPermsService luckPerms;
    private final PluginConfig config;
    private final Map<UUID, PreviewSession> sessions = new ConcurrentHashMap<>();

    public PreviewManager(LuckPermsService luckPerms, PluginConfig config) {
        this.luckPerms = luckPerms;
        this.config = config;
    }

    public boolean isPreviewing(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public PreviewSession session(UUID uuid) {
        return sessions.get(uuid);
    }

    public Collection<PreviewSession> sessions() {
        return List.copyOf(sessions.values());
    }

    /**
     * Start (or replace) a preview for the given player.
     */
    public CompletableFuture<PreviewSession> apply(UUID uuid, Group group) {
        long expireSeconds = config.expireAfterSeconds();
        AtomicReference<List<Node>> applied = new AtomicReference<>(List.of());

        return luckPerms.users().modifyUser(uuid, user -> {
            List<Node> added = new ArrayList<>();
            for (Node node : buildNodes(group, expireSeconds)) {
                if (user.data().add(node).wasSuccessful()) {
                    added.add(node);
                }
            }
            user.getCachedData().invalidate();
            applied.set(added);
        }).thenApply(ignored -> {
            PreviewSession session = new PreviewSession(uuid, group.getName(), applied.get(), Instant.now());
            sessions.put(uuid, session);
            return session;
        });
    }

    /**
     * Remove a preview and restore the player's real data.
     */
    public CompletableFuture<Void> restore(UUID uuid) {
        PreviewSession session = sessions.remove(uuid);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        return luckPerms.users().modifyUser(uuid, user -> {
            for (Node node : session.nodes()) {
                user.data().remove(node);
            }
            user.getCachedData().invalidate();
        });
    }

    /**
     * Best-effort synchronous restore of every active preview, used on plugin disable.
     * Because each node also carries an expiry (unless disabled), anything that slips
     * through here cleans itself up later.
     */
    public void shutdown() {
        for (PreviewSession session : sessions()) {
            sessions.remove(session.uuid());
            User user = luckPerms.users().getUser(session.uuid());
            if (user == null) {
                continue;
            }
            for (Node node : session.nodes()) {
                user.data().remove(node);
            }
            user.getCachedData().invalidate();
            luckPerms.users().saveUser(user);
        }
    }

    /**
     * Build every node a preview needs. Nodes carry an expiry when configured so a
     * crashed server still cleans up after itself.
     */
    private List<Node> buildNodes(Group group, long expireSeconds) {
        List<Node> nodes = new ArrayList<>();
        int offset = config.priorityOffset();

        if (config.applyChatMeta()) {
            CachedMetaData meta = group.getCachedData().getMetaData();

            for (Map.Entry<Integer, String> entry : meta.getPrefixes().entrySet()) {
                if (isBlank(entry.getValue())) {
                    continue;
                }
                PrefixNode.Builder builder = PrefixNode.builder(entry.getValue(), entry.getKey() + offset);
                withExpiry(builder, expireSeconds);
                nodes.add(builder.build());
            }

            for (Map.Entry<Integer, String> entry : meta.getSuffixes().entrySet()) {
                if (isBlank(entry.getValue())) {
                    continue;
                }
                SuffixNode.Builder builder = SuffixNode.builder(entry.getValue(), entry.getKey() + offset);
                withExpiry(builder, expireSeconds);
                nodes.add(builder.build());
            }
        }

        if (config.applyPermissions()) {
            InheritanceNode.Builder builder = InheritanceNode.builder(group.getName().toLowerCase(Locale.ROOT));
            withExpiry(builder, expireSeconds);
            nodes.add(builder.build());
        }

        return nodes;
    }

    private void withExpiry(PrefixNode.Builder builder, long expireSeconds) {
        if (expireSeconds > 0) {
            builder.expiry(expireSeconds, TimeUnit.SECONDS);
        }
    }

    private void withExpiry(SuffixNode.Builder builder, long expireSeconds) {
        if (expireSeconds > 0) {
            builder.expiry(expireSeconds, TimeUnit.SECONDS);
        }
    }

    private void withExpiry(InheritanceNode.Builder builder, long expireSeconds) {
        if (expireSeconds > 0) {
            builder.expiry(expireSeconds, TimeUnit.SECONDS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}

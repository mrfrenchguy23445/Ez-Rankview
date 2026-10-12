package com.ezrankview.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.UserManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Thin wrapper around the LuckPerms API so the rest of the plugin stays readable.
 */
public final class LuckPermsService {

    private final LuckPerms luckPerms;

    public LuckPermsService(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public LuckPerms api() {
        return luckPerms;
    }

    public UserManager users() {
        return luckPerms.getUserManager();
    }

    /**
     * Resolve a group by name, loading it on demand if it is not cached yet.
     */
    public CompletableFuture<Optional<Group>> loadGroup(String name) {
        Group cached = luckPerms.getGroupManager().getGroup(name);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return luckPerms.getGroupManager().loadGroup(name);
    }

    /**
     * @return the names of every group LuckPerms currently has loaded, sorted.
     */
    public List<String> groupNames() {
        return luckPerms.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}

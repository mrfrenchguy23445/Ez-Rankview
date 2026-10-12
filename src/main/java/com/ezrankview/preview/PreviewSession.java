package com.ezrankview.preview;

import net.luckperms.api.node.Node;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A single active preview. Stores the exact nodes that were added so they can be
 * removed again without touching the player's real data.
 */
public final class PreviewSession {

    private final UUID uuid;
    private final String groupName;
    private final List<Node> nodes;
    private final Instant startedAt;

    public PreviewSession(UUID uuid, String groupName, List<Node> nodes, Instant startedAt) {
        this.uuid = uuid;
        this.groupName = groupName;
        this.nodes = List.copyOf(nodes);
        this.startedAt = startedAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String groupName() {
        return groupName;
    }

    public List<Node> nodes() {
        return nodes;
    }

    public Instant startedAt() {
        return startedAt;
    }
}

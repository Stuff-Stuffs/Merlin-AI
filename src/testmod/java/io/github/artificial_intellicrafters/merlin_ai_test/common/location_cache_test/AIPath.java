package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class AIPath {
    private final AIPathNode[] nodes;
    private int index = 0;

    public AIPath(final AIPathNode[] nodes) {
        this.nodes = nodes;
    }

    public boolean isFinished() {
        return index >= nodes.length;
    }

    public void next() {
        index++;
        while (!isFinished() && nodes[index] == null) {
            index++;
        }
    }

    public AIPathNode getCurrent() {
        if (!isFinished()) {
            return nodes[index];
        }
        return nodes[nodes.length - 1];
    }

    @Environment(EnvType.CLIENT)
    public AIPathNode[] getNodes() {
        return nodes;
    }
}

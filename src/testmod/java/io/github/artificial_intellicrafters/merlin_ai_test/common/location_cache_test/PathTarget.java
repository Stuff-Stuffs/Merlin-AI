package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class PathTarget {
    private final double radius;

    protected PathTarget(final double radius) {
        this.radius = radius;
    }

    public double heuristic(final Vec3d pos) {
        return heuristic(pos.x, pos.y, pos.z);
    }

    public abstract double heuristic(double x, double y, double z);

    public double getRadius() {
        return radius;
    }

    public static PathTarget createEntityTarget(final double radius, final Entity entity) {
        return new PathTarget(radius) {
            @Override
            public double heuristic(final double x, final double y, final double z) {
                final Vec3d pos = entity.getPos();
                return Math.abs(pos.x - x) + Math.abs(pos.y - y) + Math.abs(pos.z - z);
            }
        };
    }

    public static PathTarget createBlockTarget(final double radius, final BlockPos pos) {
        return new PathTarget(radius) {
            @Override
            public double heuristic(final double x, final double y, final double z) {
                return Math.abs(vec.x - x) + Math.abs(vec.y - y) + Math.abs(vec.z - z);
            }

            private final Vec3d vec = Vec3d.ofBottomCenter(pos);
        };
    }
}

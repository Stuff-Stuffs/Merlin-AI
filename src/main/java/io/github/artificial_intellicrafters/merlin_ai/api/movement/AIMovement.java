package io.github.artificial_intellicrafters.merlin_ai.api.movement;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.util.math.BlockPos;

public interface AIMovement {
	boolean move(MovementInfo ret, BlockPos start, ShapeCache cache);

	final class MovementInfo {
		public double cost;
		public int x;
		public int y;
		public int z;
		public boolean symmetric;
	}
}

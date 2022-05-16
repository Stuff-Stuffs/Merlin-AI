package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import it.unimi.dsi.fastutil.shorts.*;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.List;
import java.util.function.Consumer;

public class BasicRegionClassifier implements ChunkSectionRegionClassifier {
	private final List<SymmetricMovement> symmetricMovements;

	public BasicRegionClassifier(final List<SymmetricMovement> symmetricMovements) {
		this.symmetricMovements = symmetricMovements;
	}

	@Override
	public void classify(final ShapeCache cache, final ChunkSectionPos pos, final Consumer<ShortSet> regionAdder, final ShortPredicate occupied) {
		final int baseX = pos.getMinX();
		final int baseY = pos.getMinY();
		final int baseZ = pos.getMinZ();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					if (!occupied.test(ChunkSectionRegionClassifier.packLocal(x, y, z))) {
						boolean valid = false;
						for (final SymmetricMovement movement : symmetricMovements) {
							if (movement.validStart(x + baseX, y + baseY, z + baseZ, cache)) {
								valid = true;
								break;
							}
						}
						if (valid) {
							regionAdder.accept(search(x, y, z, cache, pos, occupied));
						}
					}
				}
			}
		}
	}

	protected ShortSet search(final int inX, final int inY, final int inZ, final ShapeCache cache, final ChunkSectionPos pos, final ShortPredicate occupied) {
		final int baseX = pos.getMinX();
		final int baseY = pos.getMinY();
		final int baseZ = pos.getMinZ();
		final ShortSet region = new ShortOpenHashSet();
		final ShortPriorityQueue queue = new ShortArrayFIFOQueue();
		short packed = ChunkSectionRegionClassifier.packLocal(inX, inY, inZ);
		region.add(packed);
		queue.enqueue(packed);
		while (!queue.isEmpty()) {
			packed = queue.dequeueShort();
			final int x = ChunkSectionRegionClassifier.unpackLocalX(packed);
			final int y = ChunkSectionRegionClassifier.unpackLocalY(packed);
			final int z = ChunkSectionRegionClassifier.unpackLocalZ(packed);
			for (final SymmetricMovement movement : symmetricMovements) {
				int offsetX = x + movement.xOff();
				int offsetY = y + movement.yOff();
				int offsetZ = z + movement.zOff();
				if (movement.validStart(x + baseX, y + baseY, z + baseZ, cache)) {
					if (ChunkSectionRegionClassifier.isLocal(offsetX, offsetY, offsetZ) && movement.check(x + baseX, y + baseY, z + baseZ, cache)) {
						packed = ChunkSectionRegionClassifier.packLocal(offsetX, offsetY, offsetZ);
						if (region.add(packed)) {
							if (occupied.test(packed)) {
								throw new RuntimeException("Asymmetric movement found in region classifier");
							}
							queue.enqueue(packed);
						}
					}
					offsetX = x - movement.xOff();
					offsetY = y - movement.yOff();
					offsetZ = z - movement.zOff();
					if (ChunkSectionRegionClassifier.isLocal(offsetX, offsetY, offsetZ) && movement.checkReverse(x + baseX, y + baseY, z + baseZ, cache)) {
						packed = ChunkSectionRegionClassifier.packLocal(offsetX, offsetY, offsetZ);
						if (region.add(packed)) {
							if (occupied.test(packed)) {
								throw new RuntimeException("Asymmetric movement found in region classifier");
							}
							queue.enqueue(packed);
						}
					}
				}
			}
		}
		return region;
	}

	public interface SymmetricMovement {
		int xOff();

		int yOff();

		int zOff();

		boolean check(int x, int y, int z, ShapeCache cache);

		boolean checkReverse(int x, int y, int z, ShapeCache cache);

		boolean validStart(int x, int y, int z, ShapeCache cache);
	}
}

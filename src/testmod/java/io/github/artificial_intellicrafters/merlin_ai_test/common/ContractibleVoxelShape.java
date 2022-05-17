package io.github.artificial_intellicrafters.merlin_ai_test.common;

import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.VoxelSet;

public class ContractibleVoxelShape extends ArrayVoxelShape {
	private static final double[] OFFSETS = new double[17];
	public ContractibleVoxelShape(VoxelSet voxelSet) {
		super(voxelSet, OFFSETS, OFFSETS, OFFSETS);
	}

	static {
		for (int i = 0; i < 17; i++) {
			OFFSETS[i] = i;
		}
	}
}

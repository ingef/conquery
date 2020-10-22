package com.bakdata.conquery.util.dict;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Weigher {

	public long combineWeights(long left, long middle, long right) {
		return left + middle + right;
	}

	public long getLeftWeight(ABytesNode node) {
		return calculateWeight(node.getLeft());
	}

	public long getRightWeight(ABytesNode node) {
		return calculateWeight(node.getRight());
	}

	public long getMiddleAndHereWeight(ABytesNode node) {
		return node.key().length + calculateWeight(node.getMiddle());
	}

	public long combine(long leftWeight, long middleWeight, long rightWeight) {
		long mean = (leftWeight + middleWeight + rightWeight) / 3;

		return -((leftWeight - mean) * (leftWeight - mean) + (middleWeight - mean) * (middleWeight - mean)
				+ (rightWeight - mean) * (rightWeight - mean));
	}

	private long calculateWeight(ABytesNode node) {
		if (node == null) {
			return 0;
		}
		return node.key().length + calculateWeight(node.getLeft()) + calculateWeight(node.getRight())
				+ calculateWeight(node.getMiddle());
	}
}

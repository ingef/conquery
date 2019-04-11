package com.bakdata.conquery.util.dict;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TernaryTreeTestUtil {

	public static <T> Comparator<T> shuffle(Random random) {
		final Map<Object, UUID> uniqueIds = new IdentityHashMap<>();

		return Comparator.comparing(e -> uniqueIds.computeIfAbsent(e, ignored -> {
			byte[] randomBytes = new byte[16];
			random.nextBytes(randomBytes);

			return UUID.nameUUIDFromBytes(randomBytes);
		}));
	}

	public static boolean isBalanced(ABytesNode node) {
		if(node == null) {
			return true;
		}

		TernaryTreeBalancer.With balancer = new TernaryTreeBalancer.With(node, null);
		final boolean balanced = balancer.getClockwiseScore() <= balancer.getCurrentScore() && balancer.getCounterClockwiseScore() <= balancer.getCurrentScore();

		return  balanced
				&& isBalanced(node.getLeft())
				&& isBalanced(node.getMiddle())
				&& isBalanced(node.getRight());
	}

}

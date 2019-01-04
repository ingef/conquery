package com.bakdata.conquery.util.dict;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TernaryTreeBalancerTest {

	public static long[] getSeeds() {
		return new long[] {1530522325377L, 9874719284L};
	}

	@ParameterizedTest(name="seed: {0}")
	@MethodSource("getSeeds")
	public void valid(long seed) {
		final BytesTTMap bytesTTMap = new BytesTTMap();
		final Map<String, Integer> reference = new HashMap<>();

		AtomicInteger count = new AtomicInteger(0);

		Random random = new Random(seed);

		IntStream
			.range(0, 8192)
			.boxed()
			.sorted(TernaryTreeTestUtil.shuffle(random))
			.forEach( rep -> {
				final String prefix = Integer.toString(rep, 26);
	
				reference.put(prefix, count.get());
				bytesTTMap.put(prefix.getBytes(), count.get());
	
				count.incrementAndGet();
			});

		assertThat((Predicate<ABytesNode>) TernaryTreeTestUtil::isBalanced)
				.rejects(bytesTTMap.getRoot());

		bytesTTMap.balance();

		assertThat((Predicate<ABytesNode>) TernaryTreeTestUtil::isBalanced)
				.accepts(bytesTTMap.getRoot());

		for (Map.Entry<String, Integer> entry : reference.entrySet()) {
			assertThat(entry.getValue()).isEqualTo(bytesTTMap.get(entry.getKey().getBytes()));
		}
	}
}
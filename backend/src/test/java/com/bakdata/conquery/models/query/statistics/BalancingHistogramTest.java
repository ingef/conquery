package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class BalancingHistogramTest {

	public static final int SEED = 0xD00F;
	private final int max = 250;

	@Test
	void plain() {
		BalancingHistogram histogram = BalancingHistogram.create(0, max, 15);

		final Random random = new Random(SEED);

		for (int val = 0; val < 1000; val++) {
			histogram.add(random.nextDouble(0, max));
		}

		for (int val = 0; val < 100; val++) {
			histogram.add(random.nextDouble(0, max / 5d));
		}

		for (int val = 0; val < 100; val++) {
			histogram.add(random.nextDouble(max / 2d, max / 2d + max / 5d));
		}

		histogram.add(max);


		List<BalancingHistogram.Node> balanced = histogram.nodes();
		log.info("{}", balanced);
	}



}
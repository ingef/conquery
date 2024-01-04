package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class BalancingHistogramTest {

	@Test
	void add() {
		BalancingHistogram histogram = BalancingHistogram.create(0, 10, 15, 0.8d);

		final Random random = new Random();

		for (int val = 0; val < 1000; val++) {
			histogram.add(random.nextDouble(0, 10));
		}

		for (int val = 0; val < 100; val++) {
			histogram.add(random.nextDouble(0, 2));
		}

		histogram.add(0);
		histogram.add(10);


		List<BalancingHistogram.Node> balanced = histogram.balanced();
		log.info("{}", balanced);
	}
}
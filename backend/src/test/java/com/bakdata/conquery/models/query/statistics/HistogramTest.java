package com.bakdata.conquery.models.query.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

@Slf4j
class HistogramTest {

	public static final int SEED = 0xD00F;

	@Test
	void plain() {

		final Histogram histogram = Histogram.longTailed(0, 10, 10);

		final Random random = new Random(SEED);

		for (int it = 0; it < 10; it++) {
			histogram.add(0);
		}

		for (int it = 0; it < 100; it++) {
			histogram.add(random.nextDouble(-2, -1));
		}


		for (int it = 0; it < 100; it++) {
			histogram.add(random.nextDouble(0, 5));
		}

		for (int it = 0; it < 100; it++) {
			histogram.add(random.nextDouble(7, 10));
		}

		for (int it = 0; it < 100; it++) {
			histogram.add(random.nextDouble(10, 15));
		}

		final List<Histogram.Node> balanced = histogram.nodes();

		log.info("{}", balanced);

		assertThat(balanced).hasSize(11); // gap between 7-8

		final Histogram.Node first = balanced.get(0);

		assertThat(first.getMin()).isLessThanOrEqualTo(-1);

		final Histogram.Node last = balanced.get(balanced.size() - 1);

		assertThat(last.getMin()).isCloseTo(10, Offset.offset(0.2d));
		assertThat(last.getMax()).isGreaterThanOrEqualTo(11);

	}


}
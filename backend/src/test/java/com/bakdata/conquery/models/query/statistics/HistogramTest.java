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

		final Histogram histogram = Histogram.zeroCentered(0, 10, 10, -2);

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

		final List<Histogram.Node> nodes = histogram.nodes();

		log.info("{}", nodes);

		assertThat(nodes).hasSize(13);

		final Histogram.Node first = nodes.get(0);

		assertThat(first.getMin()).isLessThanOrEqualTo(-1);

		assertThat(nodes.get(7).getCount()).isZero();
		assertThat(nodes.get(8).getCount()).isZero();

		final Histogram.Node last = nodes.get(nodes.size() - 1);

		assertThat(last.getMin()).isCloseTo(10, Offset.offset(0.2d));
		assertThat(last.getMax()).isGreaterThanOrEqualTo(11);

		for (int i = 0; i < nodes.size(); i++) {
			final Histogram.Node node = nodes.get(i);

			assertThat(node.getLower())
					.as("[%d]=%s lower < upper", i, node)
					.isLessThanOrEqualTo(node.getUpper());

			if (node.getMin() == Double.POSITIVE_INFINITY) {
				// has no values
				continue;
			}

			assertThat(node.getMin()).isGreaterThanOrEqualTo(node.getLower());
			assertThat(node.getMax()).isLessThanOrEqualTo(node.getUpper());
			assertThat(node.getMin()).isLessThanOrEqualTo(node.getMax());
		}

	}


}
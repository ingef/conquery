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
	void recon() {

		final Histogram histogram = Histogram.zeroAligned(0.0021445979668993976d, 0.9999775467040908d, 0.0021445979668993976, 0.9999775467040908d, 10, false);

		histogram.add(0.9999775467040908d);

		log.info("{}", histogram);

		List<Histogram.Node> nodes = histogram.nodes();

		assertThat(nodes.get(9).getHits()).isEqualTo(1);

		assertThat(nodes.stream().filter(node -> node.getType().equals(Histogram.Node.Type.OVERFLOW)).findFirst()).isEmpty();

	}

	@Test
	void plain() {

		final Histogram histogram = Histogram.zeroAligned(0, 10, -2, 15, 10, false);

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

		//		assertThat(nodes.get(7).getCount()).isZero();
		//		assertThat(nodes.get(8).getCount()).isZero();

		final Histogram.Node last = nodes.get(nodes.size() - 1);

		assertThat(last.getMin()).isCloseTo(10, Offset.offset(0.2d));
		assertThat(last.getMax()).isGreaterThanOrEqualTo(11);

		for (int i = 0; i < nodes.size(); i++) {
			final Histogram.Node node = nodes.get(i);

			assertThat(node.getMin())
					.as("[%d]=%s lower < upper", i, node)
					.isLessThanOrEqualTo(node.getMax());

			if (node.getMin() == Double.POSITIVE_INFINITY) {
				// has no values
				continue;
			}

			assertThat(node.getMin()).isGreaterThanOrEqualTo(node.getMin());
			assertThat(node.getMax()).isLessThanOrEqualTo(node.getMax());
			assertThat(node.getMin()).isLessThanOrEqualTo(node.getMax());
		}

	}

	@Test
	void weird() {

		final Histogram histogram = Histogram.zeroAligned(-35, 27, -40, 28, 12, false);

		final Random random = new Random(SEED);

		for (int it = 0; it < 10; it++) {
			histogram.add(0);
		}

		for (int it = 0; it < 100; it++) {
			histogram.add(random.nextDouble(-40, 38));
		}


		final List<Histogram.Node> nodes = histogram.nodes();

		log.info("{}", nodes);

		assertThat(nodes).hasSize(15 /*12 + lower, zero, upper*/);

		final Histogram.Node first = nodes.get(0);

		assertThat(first.getMin()).isLessThanOrEqualTo(-1);

		assertThat(nodes.get(7).getCount()).isEqualTo(5);
		assertThat(nodes.get(8).getCount()).isEqualTo(10);

		final Histogram.Node last = nodes.get(nodes.size() - 1);

		assertThat(last.getMin()).isCloseTo(26, Offset.offset(0.2d));
		assertThat(last.getMax()).isGreaterThanOrEqualTo(28);

		for (int i = 0; i < nodes.size(); i++) {
			final Histogram.Node node = nodes.get(i);

			assertThat(node.getMin())
					.as("[%d]=%s lower < upper", i, node)
					.isLessThanOrEqualTo(node.getMax());

			if (node.getMin() == Double.POSITIVE_INFINITY) {
				// has no values
				continue;
			}

			assertThat(node.getMin()).isGreaterThanOrEqualTo(node.getMin());
			assertThat(node.getMax()).isLessThanOrEqualTo(node.getMax());
			assertThat(node.getMin()).isLessThanOrEqualTo(node.getMax());
		}

	}

	@Test
	public void zeroWidth() {
		Histogram histogram = Histogram.zeroAligned(0.0, 0.0, -1.0, 1.0, 10, true);
		// Should be only zero-bin, under and overflow bins
		histogram.add(0);
		histogram.add(-0.5);
		histogram.add(0.5);

		assertThat(histogram.nodes()).hasSize(3);
	}


}
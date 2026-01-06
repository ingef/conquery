package com.bakdata.conquery.models.query.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

@Slf4j
class HistogramTest {

	public static final int SEED = 0xD00F;

	@Test
	void recon() {

		final Histogram histogram = Histogram.zeroAligned(0.0021445979668993976d, 0.9999775467040908d, 0.0021445979668993976, 0.9999775467040908d, 10, false,
														  false
		);

		histogram.add(0.9999775467040908d);

		log.info("{}", histogram);

		List<Map.Entry<Range<Double>, Histogram.Counter>> nodes = histogram.nodes();

		assertThat(nodes.get(9).getValue().getHits()).isEqualTo(1);

		assertThat(nodes.stream().allMatch(node -> node.getKey().hasUpperBound())).isTrue();

	}

	@Test
	void plain() {

		final Histogram histogram = Histogram.zeroAligned(0, 10, -2, 15, 10, false, true);

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

		final List<Map.Entry<Range<Double>, Histogram.Counter>> nodes = histogram.nodes();

		log.info("{}", nodes);

		assertThat(nodes).hasSize(13);

		final Map.Entry<Range<Double>, Histogram.Counter> first = nodes.get(0);

		assertThat(first.getKey().contains(-1d)).isTrue();

		final Map.Entry<Range<Double>, Histogram.Counter> last = nodes.get(nodes.size() - 1);

		assertThat(last.getKey().lowerEndpoint()).isCloseTo(10, Offset.offset(0.2d));
		assertThat(last.getKey().contains(11d)).isTrue();
	}

	@Test
	void weird() {

		final Histogram histogram = Histogram.zeroAligned(-35, 27, -40, 28, 12, false, false);

		final Random random = new Random(SEED);

		for (int it = 0; it < 10; it++) {
			histogram.add(0);
		}

		for (int it = 0; it < 100; it++) {
			histogram.add(random.nextDouble(-40, 38));
		}


		final List<Map.Entry<Range<Double>, Histogram.Counter>> nodes = histogram.nodes();

		log.info("{}", nodes);

		assertThat(nodes).hasSize(15 /*12 + lower, zero, upper*/);

		final Map.Entry<Range<Double>, Histogram.Counter> first = nodes.get(0);

		assertThat(first.getKey().upperEndpoint()).isLessThan(-35);

		assertThat(nodes.get(7).getValue().getCount()).isEqualTo(5);
		assertThat(nodes.get(8).getValue().getCount()).isEqualTo(10);

		final Map.Entry<Range<Double>, Histogram.Counter> last = nodes.get(nodes.size() - 1);

		assertThat(last.getKey().lowerEndpoint()).isCloseTo(26, Offset.offset(0.2d));
		assertThat(last.getKey().contains(28d)).isTrue();
	}

	@Test
	public void zeroWidth() {
		Histogram histogram = Histogram.zeroAligned(0.0, 0.0, -1.0, 1.0, 10, true, false);
		// Should be only zero-bin, under and overflow bins
		histogram.add(0);
		histogram.add(-0.5);
		histogram.add(0.5);

		assertThat(histogram.nodes()).hasSize(3);
	}


}
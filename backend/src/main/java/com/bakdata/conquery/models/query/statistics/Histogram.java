package com.bakdata.conquery.models.query.statistics;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.Data;
import lombok.ToString;

/**
 * Simple implementation of a histogram.
 * First and last bin serve as potential overflow bins.
 * <p>
 * Bin labels are of real values and not partitions, this can make entries potentially non-contiguous, but ensures readable values.
 */
@Data
public class Histogram {

	private final RangeMap<Double, Integer> value2Index;

	private final Node[] nodes;

	private int total;

	public static Histogram longTailed(double min, double max, int expectedBins) {
		final double width = (max - min) / expectedBins;

		final RangeSet<Double> ranges = TreeRangeSet.create();

		ranges.add(Range.closed(0d, 0d));


		for (double start = 0 + Math.ulp(0); start < max; start += width) {
			ranges.add(Range.open(start, start + width));
		}

		for (double end = 0 - Math.ulp(0); end > min; end -= width) {
			ranges.add(Range.open(end - width, end));
		}

		final Range<Double> span = ranges.span();


		final TreeRangeMap<Double, Integer> value2Index = TreeRangeMap.create();

		final AtomicInteger index = new AtomicInteger(1);

		ranges.asRanges().stream()
			  .sorted(Comparator.comparingDouble(Range::lowerEndpoint))
			  .forEach(range -> value2Index.put(range, index.getAndIncrement()));

		value2Index.put(Range.lessThan(span.lowerEndpoint()), 0);
		value2Index.put(Range.greaterThan(span.upperEndpoint()), index.getAndIncrement());

		return new Histogram(value2Index, new Node[value2Index.asMapOfRanges().size()]);
	}

	public void add(double value) {
		total++;


		final int index = value2Index.get(value);

		if (nodes[index] == null) {
			nodes[index] = new Node(new DoubleArrayList());
		}

		nodes[index].add(value);
	}

	public List<Node> nodes() {
		return Arrays.stream(nodes).filter(Objects::nonNull).toList();
	}

	@Data
	public static final class Node {
		@ToString.Exclude
		private final DoubleList entries;
		private double min = Double.MAX_VALUE;
		private double max = Double.MIN_VALUE;

		@ToString.Include
		public int getCount() {
			return entries.size();
		}

		public void add(double value) {
			if (value < min) {
				min = value;
			}
			if (value > max) {
				max = value;
			}

			entries.add(value);
		}

	}

}

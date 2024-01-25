package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Functions;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple implementation of a histogram.
 * First and last bin serve as potential overflow bins.
 * <p>
 * Bin labels are of real values and not partitions, this can make entries potentially non-contiguous, but ensures readable values.
 */
@Data
@Slf4j
public class Histogram {


	private final Node[] belowZero, aboveZero;

	private final Node underflowNode;
	private final Node zeroNode;
	private final Node overflowNode;


	private final double lower, upper;

	private final double width;

	private int total;


	public static Histogram zeroCentered(double min, double max, int expectedBins) {

		final double width = (max - min) / expectedBins;

		final int nBelowZero = (int) Math.ceil(Math.abs(min) / width);

		final Node[] negative = IntStream.range(0, nBelowZero).mapToObj(index -> new Node(-(index + 1) * width, -index * width, false)).toArray(Node[]::new);

		final int nAboveZero = (int) Math.ceil(max / width);

		final Node[] positive = IntStream.range(0, nAboveZero).mapToObj(index -> new Node(index * width, (index + 1) * width, false)).toArray(Node[]::new);


		final double lowerBound = -(nBelowZero * width);
		final double upperBound = nAboveZero * width;

		return new Histogram(negative, positive,
							 new Node(Double.NEGATIVE_INFINITY, lowerBound, true),
							 new Node(0, 0, true),
							 new Node(upperBound, Double.POSITIVE_INFINITY, true),
							 lowerBound,
							 upperBound,
							 width
		);
	}

	public void add(double value) {
		total++;

		if (value == 0d) {
			zeroNode.add(value);
		}
		else if (value <= lower) {
			underflowNode.add(value);
		}
		else if (value >= upper) {
			overflowNode.add(value);
		}
		else if (value > 0) {
			final int index = (int) Math.floor(value / width);
			aboveZero[index].add(value);
		}
		else if (value < 0) {
			final int index = (int) Math.floor(Math.abs(value) / width);
			belowZero[index].add(value);
		}
		else {
			log.warn("Don't know how to handle value {} with {}", value, this);
		}

	}

	public List<Node> nodes() {
		return Stream.of(
							 Stream.of(underflowNode),
							 Stream.of(belowZero),
							 Stream.of(zeroNode),
							 Stream.of(aboveZero),
							 Stream.of(overflowNode)
					 )
					 .flatMap(Functions.identity()) // This is suggested concat of multiple streams
					 .filter(Objects::nonNull)
					 .filter(node -> node.getCount() > 0 || !node.isSpecial())
					 .collect(Collectors.toList());

	}

	@Data
	public static final class Node {
		@ToString.Exclude
		private final DoubleList entries = new DoubleArrayList();

		private final double lower, upper;
		private final boolean isSpecial;


		private double min = Double.POSITIVE_INFINITY;
		private double max = Double.NEGATIVE_INFINITY;

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


		String getLabel(Double2ObjectFunction<String> printer) {
			final String lower = printer.apply(Double.isFinite(getMin()) ? getMin() : getLower());
			final String upper = printer.apply(Double.isFinite(getMax()) ? getMax() : getUpper());

			final String binLabel = lower.equals(upper) ? lower : String.format("%s - %s", lower, upper);
			return binLabel;
		}
	}

}

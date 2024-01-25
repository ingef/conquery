package com.bakdata.conquery.models.query.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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


	private final Node[] nodes;
	private final Node zeroNode, underflowNode, overflowNode;
	private final double lower, upper;
	private final double width;

	private int total;

	public static Histogram zeroCentered(double lower, double upper, int expectedBins, boolean expectNegativeValues) {
		final double width = (upper - lower) / expectedBins;

		//TODO are the branches functionally the same?
		if (!expectNegativeValues) {
			final Node[] nodes = IntStream.range(0, expectedBins + 1)
										  .mapToObj(index -> new Node(lower + index * width, lower + (index + 1) * width))
										  .toArray(Node[]::new);

			return new Histogram(nodes, new Node(0, 0), new Node(0, lower), new Node(upper, Double.POSITIVE_INFINITY), lower, upper, width);
		}


		final double newLower = lower == 0 ? 0 : Math.signum(lower) * width * Math.ceil(Math.abs(lower) / width);
		// We adjust slightly downward so that we have even sized bins, that meet exactly at zero (which is tracked separately)
		final Node[] nodes = IntStream.range(0, expectedBins + 1)
									  .mapToObj(index -> new Node(newLower + width * index, newLower + width * (index + 1)))
									  .toArray(Node[]::new);

		return new Histogram(nodes, new Node(0, 0), new Node(Double.NEGATIVE_INFINITY, newLower), new Node(upper, Double.POSITIVE_INFINITY), newLower, upper, width);

	}

	public void add(double value) {
		total++;

		if (value == 0d) {
			zeroNode.add(value);
			return;
		}

		if (value <= lower) {
			underflowNode.add(value);
			return;
		}

		if (value >= upper) {
			overflowNode.add(value);
			return;
		}

		final int index = (int) Math.floor((value - lower) / width);
		nodes[index].add(value);
	}

	public List<Node> nodes() {
		return Stream.of(
							 Stream.of(underflowNode),
							 Stream.ofNullable(zeroNode),
							 Stream.of(nodes),
							 Stream.of(overflowNode)
					 )
					 .flatMap(Function.identity())
					 .sorted(Comparator.comparingDouble(Node::getLower))
					 .toList();
	}

	@Data
	public static final class Node {
		@ToString.Exclude
		private final DoubleList entries = new DoubleArrayList();

		private final double lower, upper;


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

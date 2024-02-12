package com.bakdata.conquery.models.query.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
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
	private final Node zeroNode;
	private final Node underflowNode;
	private final Node overflowNode;
	private final double lower;
	private final double upper;
	private final double width;

	private int total;

	public static Histogram zeroCentered(double lower, double upper, double absMin, double absMax, int expectedBins,  boolean round) {

		lower = Math.max(Math.ceil(absMin), Math.floor(lower));
		upper = Math.min(Math.ceil(absMax), Math.ceil(upper));

		final double width = round ? Math.ceil((upper - lower) / expectedBins) : (upper - lower) / expectedBins;

		final double newLower;

		if (lower == 0) {
			newLower = 0;
		}
		else if (absMin <= 0) {
			// We adjust slightly downward so that we have even sized bins, that meet exactly at zero (which is tracked separately)
			newLower = Math.signum(lower) * width * Math.ceil(Math.abs(lower) / width);
		}
		else {
			newLower = lower;
		}

		final double newUpper = newLower + width * expectedBins;

		final Node[] nodes = IntStream.range(0, expectedBins)
									  .mapToObj(index -> new Node(newLower + width * index, newLower + width * (index + 1)))
									  .toArray(Node[]::new);


		return new Histogram(nodes,
							 new Node(0, 0),
							 new Node(Math.min(absMin, newLower), newLower),
							 new Node(newUpper, Math.max(absMax, newUpper)),
							 newLower, newUpper,
							 width);

	}

	public void add(double value) {
		total++;

		if (value == 0d) {
			zeroNode.add();
			return;
		}

		if (value <= lower) {
			underflowNode.add();
			return;
		}

		if (value >= upper) {
			overflowNode.add();
			return;
		}

		final int index = (int) Math.floor((value - lower) / width);
		nodes[index].add();
	}

	public List<Node> nodes() {
		return Stream.of(
							 Stream.of(underflowNode, overflowNode, zeroNode).filter(node -> node.getCount() > 0),
							 Stream.of(nodes)
					 )
					 .flatMap(Function.identity())
					 // We compare by Max as well to fix zeroNode and underflowNode sorting when absMin > 0
					 .sorted(Comparator.comparingDouble(Node::getMin).thenComparing(Comparator.comparingDouble(Node::getMax)))
					 .toList();
	}

	@Data
	public static final class Node {
		@ToString.Include
		private int hits = 0;

		private final double min, max;

		public int getCount() {
			return hits;
		}

		public void add() {
			hits++;
		}


		String getLabel(Double2ObjectFunction<String> printer) {
			final String lower = printer.apply(getMin());
			final String upper = printer.apply(getMax());

			final String binLabel = lower.equals(upper) ? lower : String.format("%s - %s", lower, upper);
			return binLabel;
		}

	}

}

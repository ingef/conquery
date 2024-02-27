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
 * Basic implementation of a histogram.
 *
 * Of note:
 * * We have over and underflow bins, if values exceed our assumed value-range.
 * * The bins are always aligned such that zero is its own separate bin, as we assume it is a special value.
 * 		- This means, bin limits will be slightly adjusted left to have equal-spaced bins, that have zero not intersect them.
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

	public static Histogram zeroCentered(double lower, double upper, double absMin, double absMax, int expectedBins,  boolean roundWidth) {
		//TODO if we have integer bins, we can go further with bins and make Node.upper==Node.lower, which is very nice to read.
		//TODO split rounded and unrounded code paths

		// adjust lower/upper to start on rounded edges.
		final double adjLower = Math.max(Math.floor(absMin), Math.floor(lower));
		final double adjUpper = Math.min(Math.ceil(absMax), Math.ceil(upper));

		if (adjLower == adjUpper) {
			// Short circuit for degenerate cases
			return new Histogram(new Node[0],
								 new Node(0, 0),
								 new Node(absMin, adjLower),
								 new Node(adjUpper, absMax),
								 adjLower, adjUpper,
								 0
			);
		}

		final double width = roundWidth ? Math.max(1, Math.round((adjUpper - adjLower) / expectedBins)) : (adjUpper - adjLower) / expectedBins;

		final double newLower;

		if (adjLower == 0d) {
			newLower = 0;
		}
		else if (absMin <= 0) {
			// We adjust slightly downward so that we have even sized bins, that meet exactly at zero (which is tracked separately)
			newLower = Math.signum(adjLower) * width * Math.ceil(Math.abs(adjLower) / width);
		}
		else {
			newLower = adjLower;
		}

		final double newUpper = newLower + width * expectedBins;

		final Node[] nodes = IntStream.range(0, expectedBins)
									  // Note, using multiplication is important to avoid floating-point imprecision when wanting to arrive exactly around 0 etc.
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

		if (value < lower) {
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
					 // We compare by Max as well to fix zeroNode and underflowNode sorting when absMin >= 0
					 .sorted(Comparator.comparingDouble(Node::getMin).thenComparing(Comparator.comparingDouble(Node::getMax)))
					 .toList();
	}

	@Data
	public static final class Node {
		@ToString.Include
		private int hits;

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


			if(lower.equals(upper)){
				return lower;
			}

			final String binLabel = String.format("%s - %s", lower, upper);

			return binLabel;
		}

	}

}

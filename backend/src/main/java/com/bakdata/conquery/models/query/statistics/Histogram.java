package com.bakdata.conquery.models.query.statistics;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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

	private final TreeRangeMap<Double, Node> nodes;
	private final double absMin, absMax;

	private int total;

	public Histogram(TreeRangeMap<Double, Node> nodes, double absMin, double absMax) {
		Range<Double> span = nodes.span();
		nodes.put(Range.atMost(span.lowerEndpoint()), new Node(absMin, span.lowerEndpoint(), Node.Type.UNDERFLOW));
		nodes.put(Range.greaterThan(span.upperEndpoint()), new Node(span.upperEndpoint(), absMax, Node.Type.OVERFLOW));

		if (span.contains(0d)) {
			nodes.putCoalescing(Range.singleton(0d), new Node(0, 0, Node.Type.ZERO));
		}

		this.nodes = nodes;
		this.absMin = absMin;
		this.absMax = absMax;
	}


	/**
	 * Create a histogram that is segmented to always have 0 singled out.
	 */
	public static Histogram zeroAligned(double lower, double upper, double absMin, double absMax, int expectedBins, boolean roundWidth) {
		if (lower == upper) {
			TreeRangeMap<Double, Node> nodes = TreeRangeMap.create();
			nodes.put(Range.singleton(lower), new Node(lower, upper, Node.Type.NORMAL));

			// Short circuit for degenerate cases
			return new Histogram(nodes, absMin, absMax);
		}

		if (roundWidth) {
			return rounded(lower, upper, absMin, absMax, expectedBins);
		}
		else {
			return unrounded(lower, upper, absMin, absMax, expectedBins);
		}
	}

	private static Histogram rounded(double lower, double upper, double absMin, double absMax, int expectedBins) {
		// adjust lower/upper to start on rounded edges.
		double adjLower = Math.max(Math.floor(absMin), Math.floor(lower));
		final double adjUpper = Math.min(Math.ceil(absMax), Math.ceil(upper));

		final double binWidth = Math.max(1d, Math.round((adjUpper - adjLower) / (double) expectedBins));

		return createHistogram(lower, absMin, absMax, expectedBins, adjLower, binWidth);
	}

	private static Histogram unrounded(double lower, double upper, double absMin, double absMax, int expectedBins) {
		double binWidth = (upper - lower) / expectedBins;

		return createHistogram(lower, absMin, absMax, expectedBins, lower, binWidth);
	}

	@NotNull
	private static Histogram createHistogram(double lower, double absMin, double absMax, int expectedBins, double adjLower, double binWidth) {
		adjLower = ensureZeroCrossing(lower, absMax, adjLower, binWidth);

		final TreeRangeMap<Double, Node> nodes = TreeRangeMap.create();

		// Note, using multiplication is important to avoid floating-point imprecision when wanting to arrive exactly around 0 etc.
		for (int index = 0; index < expectedBins; index++) {
			Node node = new Node(adjLower + binWidth * index, adjLower + binWidth * (index + 1));
			nodes.put(Range.closedOpen(node.getMin(), node.getMax()), node);
		}

		return new Histogram(nodes, absMin, absMax);
	}



	private static double ensureZeroCrossing(double lower, double absMax, double adjLower, double binWidth) {
		// We have to adjust left if we have a zero-crossing, to ensure partitioning out the zero-bin
		if (lower < 0 && absMax > 0) {
			adjLower = -Math.ceil(Math.abs(lower) / binWidth) * binWidth;
		}
		return adjLower;
	}

	public void add(double value) {
		total++;
		Node node = nodes.get(value);

		if (node == null) {
			log.warn("Missing node for value `{}` in with ranges {}", value, nodes);
			return;
		}

		node.add();
	}

	public List<Node> nodes() {
		return nodes.asMapOfRanges().values().stream()
					.filter(node -> node.getType() == Node.Type.NORMAL || node.getCount() > 0)
					// We compare by Max as well to fix zeroNode and underflowNode sorting when absMin >= 0
					.sorted(Comparator.comparingDouble(Node::getMin).thenComparingDouble(Node::getMax))
					.toList();
	}

	@Data
	public static final class Node {
		/**
		 * This is an em-dash.
		 */
		private static final String FROM_TO = " – ";
		private final double min, max;
		private int hits;

		private Type type;

		public Node(double min, double max) {
			this(min, max, Type.NORMAL);
		}

		public Node(double min, double max, Type type) {
			this.min = min;
			this.max = max;
			this.type = type;
		}

		static String createLabel(Node node, Double2ObjectFunction<String> printer, boolean isInteger) {
			final String lower = printer.apply(node.getMin());

			if (node.getMin() == node.getMax()) {
				return lower;
			}

			if (isInteger) {

				if (node.getMax() - node.getMin() <= 1) {
					return lower;
				}

				// Integers allow us to forfeit the brace notation by closing the range (unless we are the overflow bin which tracks real values)
				final String upper = printer.apply(node.getMax() - (node.isOverflow() ? 0 : 1));

				return lower + FROM_TO + upper;
			}

			final String upper = printer.apply(node.getMax());

			final String startBrackets = node.getMin() == 0 ? "(" : "[";
			final String endBrackets = ")";

			return startBrackets + lower + FROM_TO + upper + endBrackets;
		}

		private boolean isOverflow() {
			return Type.OVERFLOW.equals(type);
		}

		public int getCount() {
			return hits;
		}

		public void add() {
			hits++;
		}


		public enum Type {
			UNDERFLOW, ZERO, OVERFLOW, NORMAL
		}

	}

}

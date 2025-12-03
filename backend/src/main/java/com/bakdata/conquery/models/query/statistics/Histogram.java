package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMaps;
import lombok.Data;
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

	/**
	 * This is an em-dash.
	 */
	private static final String FROM_TO = " – ";
	private final TreeRangeMap<Double, Counter> nodes;
	private final double absMin, absMax;
	private final boolean integral;

	private int total;

	public Histogram(TreeRangeMap<Double, Counter> nodes, double absMin, double absMax, boolean integral) {
		this.integral = integral;
		Range<Double> span = nodes.span();
		if (!span.contains(absMin)) {
			nodes.put(Range.lessThan(span.lowerEndpoint()), new Counter());
		}
		if (!span.contains(absMax)) {
			nodes.put(Range.atLeast(span.upperEndpoint()), new Counter());
		}

		if (span.contains(0d)) {
			nodes.put(Range.singleton(0d), new Counter());
		}

		this.nodes = nodes;
		this.absMin = absMin;
		this.absMax = absMax;
	}


	/**
	 * Create a histogram that is segmented to always have 0 singled out.
	 */
	public static Histogram zeroAligned(double lower, double upper, double absMin, double absMax, int expectedBins, boolean roundWidth, boolean integral) {
		if (lower == upper) {
			TreeRangeMap<Double, Counter> nodes = TreeRangeMap.create();
			nodes.put(Range.singleton(lower), new Counter());

			// Short circuit for degenerate cases
			return new Histogram(nodes, absMin, absMax, true);
		}

		if (roundWidth) {
			return rounded(lower, upper, absMin, absMax, expectedBins, integral);
		}
		else {
			return unrounded(lower, upper, absMin, absMax, expectedBins, integral);
		}
	}

	private static Histogram rounded(double lower, double upper, double absMin, double absMax, int expectedBins, boolean integral) {
		// adjust lower/upper to start on rounded edges.
		double adjLower = Math.max(Math.floor(absMin), Math.floor(lower));
		final double adjUpper = Math.min(Math.ceil(absMax), Math.ceil(upper));

		final double binWidth = Math.max(1d, Math.round((adjUpper - adjLower) / (double) expectedBins));

		return createHistogram(lower, absMin, absMax, expectedBins, adjLower, binWidth, integral);
	}

	private static Histogram unrounded(double lower, double upper, double absMin, double absMax, int expectedBins, boolean integral) {
		double binWidth = (upper - lower) / expectedBins;

		return createHistogram(lower, absMin, absMax, expectedBins, lower, binWidth, integral);
	}

	@NotNull
	private static Histogram createHistogram(double lower, double absMin, double absMax, int expectedBins, double adjLower, double binWidth, boolean integral) {
		adjLower = ensureZeroCrossing(lower, absMax, adjLower, binWidth);

		final TreeRangeMap<Double, Counter> nodes = TreeRangeMap.create();

		// Note, using multiplication is important to avoid floating-point imprecision when wanting to arrive exactly around 0 etc.
		for (int index = 0; index < expectedBins; index++) {
			Counter node = new Counter();
			nodes.put(Range.closedOpen(
					adjLower + binWidth * index,
					adjLower + binWidth * (index + 1)), node);
		}

		return new Histogram(nodes, absMin, absMax, integral);
	}


	private static double ensureZeroCrossing(double lower, double absMax, double adjLower, double binWidth) {
		// We have to adjust left if we have a zero-crossing, to ensure partitioning out the zero-bin
		if (lower < 0 && absMax > 0) {
			adjLower = -Math.ceil(Math.abs(lower) / binWidth) * binWidth;
		}
		return adjLower;
	}

	public String createLabel(Range<Double> node, Double2ObjectFunction<String> printer) {
		double min = node.hasLowerBound() ? node.lowerEndpoint() : absMin;
		double max = node.hasUpperBound() ? node.upperEndpoint() : absMax;

		final String lower = printer.apply(min);

		if (integral) {
			// Integers allow us to forfeit the brace notation by closing the range (unless we are the overflow bin which tracks real values)

			if (max - min <= 1) {
				return lower;
			}

			final String upper = node.hasUpperBound()
								 ? printer.apply(max)
								 : printer.apply(max - 1);

			return lower + FROM_TO + upper;
		}

		if (min == max) {
			return lower;
		}

		final String upper = printer.apply(max);

		final String startBrackets = min == 0 ? "(" : "[";
		final String endBrackets = ")";

		return startBrackets + lower + FROM_TO + upper + endBrackets;
	}

	public void add(double value) {
		total++;
		Counter node = nodes.get(value);

		if (node == null) {
			log.error("Missing node for value `{}` in with ranges {}", value, nodes);
			return;
		}

		node.add();
	}

	public List<Map.Entry<Range<Double>, Counter>> nodes() {
		return nodes.asMapOfRanges().entrySet().stream()
					.filter(entry -> {
						Range<Double> span = entry.getKey();
						if (span.hasLowerBound() && span.hasUpperBound()) {
							return true;
						}
						else {
							return entry.getValue().getCount() > 0;
						}
					})
					.toList();
	}

	@Data
	public static final class Counter {
		private int hits;

		public int getCount() {
			return hits;
		}

		public void add() {
			hits++;
		}
	}

}

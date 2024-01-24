package com.bakdata.conquery.models.query.statistics;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private final Node overflowNode;
	private final Node underflowNode;

	private final Node zeroNode;

	private final double lower, upper;

	private final double width;

	private int total;


	public static Histogram zeroCentered(double min, double max, int expectedBins) {

		final double width = (max - min) / expectedBins;

		final int nBelowZero = (int) Math.ceil(Math.abs(min) / width);
		final int nAboveZero = (int) Math.ceil(max / width);

		return new Histogram(new Node[nBelowZero], new Node[nAboveZero],
							 new Node(),
							 new Node(),
							 new Node(),
							 -(nBelowZero * width),
							 nAboveZero * width,
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

			if (aboveZero[index] == null) {
				aboveZero[index] = new Node();
			}

			aboveZero[index].add(value);
		}
		else if (value < 0) {
			final int index = (int) Math.floor(Math.abs(value) / width);

			if (belowZero[index] == null) {
				belowZero[index] = new Node();
			}

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
					 .flatMap(s -> s) // This is suggested concat of multiple nodes
					 .filter(Objects::nonNull)
					 .filter(node -> node.getCount() > 0)
					 .collect(Collectors.toList());

	}

	@Data
	public static final class Node {
		@ToString.Exclude
		private final DoubleList entries = new DoubleArrayList();


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

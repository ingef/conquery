package com.bakdata.conquery.models.query.statistics;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.Data;
import lombok.ToString;

@Data
public class Histogram {
	private final Node[] nodes;
	private final double min, max;
	private final double width;

	private final int expectedBins;


	private int total;

	public static Histogram create(double min, double max, int expectedBins) {
		final double width = (max - min) / expectedBins;

		return new Histogram(new Node[expectedBins], min, max, width, expectedBins);
	}

	public void add(double value) {
		total++;

		final int index;

		if (value >= max) {
			index = nodes.length - 1;
		}
		else if (value <= min) {
			index = 0;
		}
		else {
			index = (int) Math.floor((value - min) / width);
		}

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

		public Node merge(Node other) {
			final Node out = new Node(new DoubleArrayList(getCount() + other.getCount()));

			out.max = Math.max(max, other.getMax());
			out.min = Math.min(min, other.getMin());

			out.entries.addAll(other.getEntries());
			out.entries.addAll(getEntries());

			return out;
		}

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

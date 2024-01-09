package com.bakdata.conquery.models.query.statistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import groovy.lang.Tuple;
import groovy.lang.Tuple2;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.Data;
import lombok.ToString;

@Data
public class BalancingHistogram {
	private final Node[] nodes;
	private final double min, max;
	private final double width;

	private final int expectedBins;


	private int total;

	public static BalancingHistogram create(double min, double max, int expectedBins) {
		final double width = (max - min) / (expectedBins - 1);

		return new BalancingHistogram(new Node[expectedBins], min, max, width, expectedBins);
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

	public List<Node> balanced(double stiffness) {

		final List<Node> merged = mergeLeft(nodes, stiffness);

		final List<Node> split = splitRight(merged, stiffness);

		return split;

	}

	private List<Node> mergeLeft(Node[] nodes, double stiffness) {
		final List<Node> bins = new ArrayList<>();

		Node prior = null;

		for (int i = nodes.length - 1; i >= 0; i--) {
			final Node bin = nodes[i];
			// Not all bins are initialised.
			if (bin == null) {
				continue;
			}

			if (prior == null) {
				prior = bin;
				continue;
			}

			// If the bin is too small, we merge-left
			if (prior.getCount() < (total / expectedBins) * stiffness) {
				prior = prior.merge(bin);
				continue;
			}

			// emit prior, if we cannot merge left.
			bins.add(prior);
			prior = bin;
		}

		if (prior != null) {
			bins.add(prior);
		}

		// since we're merging from right, we need to reverse
		Collections.reverse(bins);

		return bins;
	}

	private List<Node> splitRight(List<Node> nodes, double stiffness) {
		final int expectedBinSize = total / expectedBins;

		final List<Node> bins = new ArrayList<>();

		final Deque<Node> frontier = new ArrayDeque<>(nodes);

		while (!frontier.isEmpty()) {
			final Node node = frontier.pop();
			if (node.getCount() <= (expectedBinSize * (1 + stiffness))) {
				bins.add(node);
				continue;
			}

			final List<Node> split = node.split();

			final Node lower = split.get(0);
			final Node higher = split.get(1);

			// node has a heavy bias
			if (Math.min(higher.getCount(), lower.getCount())
				<= expectedBinSize * 0.1d /* This is not the merge threshold, just a sufficiently small number */) {
				bins.add(node);
				continue;
			}

			frontier.addFirst(higher);
			frontier.addFirst(lower);
		}

		return bins;
	}

	public List<Node> snapped() {
		final Node first = nodes[0];

		double min;
		double max = Math.floor(first.min);


		for (int index = 0; index < nodes.length; index++) {
			final Node current = nodes[index];

			if (current == null) {
				continue;
			}

			min = max;
			max = Math.max(min, Math.round(current.max));

			boolean isLast = index == nodes.length - 1;

			if (isLast) {
				max = Math.ceil(current.max);
			}

			final Tuple2<DoubleList, DoubleList> spill = current.adjust(min, max);

			final DoubleList lower = spill.getV1();

			if (!lower.isEmpty()) {
				lower.forEach(nodes[index - 1]::add);
			}

			final DoubleList higher = spill.getV2();

			if (!higher.isEmpty()) {
				if (isLast) {
					higher.forEach(current::add);
				}
				else {
					higher.forEach(nodes[index + 1]::add);
				}
			}

		}

		return Arrays.stream(nodes).filter(Objects::nonNull).filter(node -> node.getCount() > 0).toList();
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

		public List<Node> split() {
			final double mean = entries.doubleStream().average().getAsDouble();
			final Node lower = new Node(new DoubleArrayList());
			final Node higher = new Node(new DoubleArrayList());


			for (double entry : entries) {
				if (entry <= mean) {
					lower.add(entry);
				}
				else {
					higher.add(entry);
				}
			}

			return List.of(lower, higher);
		}

		public void add(double value) {
			max = Math.max(max, value);
			min = Math.min(min, value);
			entries.add(value);
		}

		public Tuple2<DoubleList, DoubleList> adjust(double min, double max) {
			final DoubleList lower = new DoubleArrayList();
			final DoubleList higher = new DoubleArrayList();
			final DoubleIterator iterator = entries.doubleIterator();

			this.min = min;
			this.max = max;

			while (iterator.hasNext()) {
				final double value = iterator.nextDouble();

				if (value < min) {
					lower.add(value);
					iterator.remove();
				}
				else if (value >= max) {
					higher.add(value);
					iterator.remove();
				}
			}

			return Tuple.tuple(lower, higher);
		}
	}

}

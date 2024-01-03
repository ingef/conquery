package com.bakdata.conquery.models.query.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.math.Stats;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.Data;

@Data
public class DynamicHistogram {
	private final Node[] nodes;
	private final double min;
	private final double width;

	public static DynamicHistogram create(double min, double max, int expectedBins) {
		return new DynamicHistogram(new Node[expectedBins], min, (max - min) / expectedBins);
	}

	public void add(double value) {
		final int index = (int) Math.floor((value - min) / width);

		if (nodes[index] == null) {
			nodes[index] = new Node(new DoubleArrayList());
		}

		nodes[index].add(value);
	}

	public List<Node> balanced(int expectedBins, int total) {

		final List<Node> merged = mergeLeft(total, nodes);

		final List<Node> split = splitRight(expectedBins, merged);

		return split;

	}

	private static List<Node> mergeLeft(int total, Node[] nodes) {
		final List<Node> bins = new ArrayList<>();

		Node prior = null;

		for (Node bin : nodes) {

			if (prior == null) {
				prior = bin;
				continue;
			}

			// If the bin is too small, we merge-left
			if ((double) prior.getCount() / total <= (1d / total)) {
				prior = prior.merge(bin);
				continue;
			}

			// Only emit bin, if we cannot merge left.
			bins.add(prior);
			prior = null;
		}

		if (prior != null) {
			bins.add(prior);
		}

		// since we're merging from right, we need to reverse
		Collections.reverse(bins);

		return bins;
	}

	private static List<Node> splitRight(int expectedBins, List<Node> nodes) {

		if ((double) nodes.size() / (double) expectedBins >= 0.7d) {
			return nodes;
		}

		final List<Node> bins = new ArrayList<>();

		final Stats stats = nodes.stream().mapToDouble(node -> (double) node.getCount()).boxed().collect(Stats.toStats());

		final double stdDev = stats.sampleStandardDeviation();
		final double mean = stats.mean();


		for (Node node : nodes) {
			if (node.getCount() < mean + stdDev) {
				bins.add(node);
				continue;
			}

			bins.addAll(node.split());
		}

		return bins;
	}

	@Data
	public static final class Node {
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
	}

}

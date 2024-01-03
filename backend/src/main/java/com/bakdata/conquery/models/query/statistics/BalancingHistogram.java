package com.bakdata.conquery.models.query.statistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.Data;
import lombok.ToString;

@Data
public class BalancingHistogram {
	private final Node[] nodes;
	private final double min;
	private final double width;

	private final int expectedBins;

	private final double stiffness;

	private int total;

	public static BalancingHistogram create(double min, double max, int expectedBins, double stiffness) {
		return new BalancingHistogram(new Node[expectedBins], min, (max - min) / (expectedBins - 1), expectedBins, stiffness);
	}

	public void add(double value) {
		total++;

		final int index = (int) Math.floor((value - min) / width);

		if (nodes[index] == null) {
			nodes[index] = new Node(new DoubleArrayList());
		}

		nodes[index].add(value);
	}

	public List<Node> balanced() {

		final List<Node> merged = mergeLeft(nodes);

		final List<Node> split = splitRight(merged);

		return split;

	}

	private List<Node> mergeLeft(Node[] nodes) {
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

	private List<Node> splitRight(List<Node> nodes) {
		final int expectedBinSize = total / expectedBins;

		final List<Node> bins = new ArrayList<>();

		final Deque<Node> frontier = new ArrayDeque<>(nodes);

		while(!frontier.isEmpty()) {
			final Node node = frontier.pop();
			if (node.getCount() <= (expectedBinSize * (1 + stiffness))) {
				bins.add(node);
				continue;
			}

			final List<Node> split = node.split();

			final Node lower = split.get(0);
			final Node higher = split.get(1);

			// node has a heavy bias
			if(Math.min(higher.getCount(), lower.getCount()) <= expectedBinSize * 0.1d /* This is not the merge threshold, just a sufficiently small number */){
				bins.add(node);
				continue;
			}

			frontier.addFirst(higher);
			frontier.addFirst(lower);
		}

		return bins;
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
	}

}

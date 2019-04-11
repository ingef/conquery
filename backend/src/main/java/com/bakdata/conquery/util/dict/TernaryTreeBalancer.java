package com.bakdata.conquery.util.dict;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.math.NumberUtils;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class TernaryTreeBalancer {

	public void balance(Supplier<ABytesNode> getter, Consumer<ABytesNode> setter) {
		boolean balance = true;
		int i = 0;
		while (balance) {
			ABytesNode n = getter.get();
			if (n == null) {
				balance = false;
			}
			else {
				balance = balance(n, setter);
			}
			i++;
		}
		if (i != 1) {
			log.trace("Balanced {} times", i);
		}
	}

	private boolean balance(ABytesNode node, Consumer<ABytesNode> setter) {
		return with(node, setter).balance();
	}

	private With with(ABytesNode node, Consumer<ABytesNode> setter) {
		return new With(node, setter);
	}

	@RequiredArgsConstructor
	public static class With {

		private final ABytesNode node;
		private final Consumer<ABytesNode> setter;

		private boolean balance() {
			balanceChildren();
			long currentScore = getCurrentScore();
			long cwScore = getClockwiseScore();
			long ccwScore = getCounterClockwiseScore();
			long cwCcwScore = getClockwiseCounterClockwiseScore();
			long ccwCwScore = getCounterClockwiseClockwiseScore();
			long maxRotateScore = NumberUtils.max(ccwScore, cwScore, cwCcwScore, ccwCwScore);
			boolean balance = maxRotateScore > currentScore;
			if (balance) {
				if (cwScore == maxRotateScore) {
					log.trace("Rotating {} clockwise", node);
					rotateClockwise();
				} else if (ccwScore == maxRotateScore) {
					log.trace("Rotating {} counter-clockwise", node);
					rotateCounterClockwise();
				} else if (cwCcwScore == maxRotateScore) {
					log.trace("Rotating {} clockwise counter-clockwise", node);
					if (node.getRight() != null) {
						withRight(node.getRight()).rotateClockwise();
					}
					rotateCounterClockwise();
				} else {
					log.trace("Rotating {} counter-clockwise clockwise", node);
					if (node.getLeft() != null) {
						withLeft(node.getLeft()).rotateCounterClockwise();
					}
					rotateClockwise();
				}
			}
			return balance;
		}

		private void balanceChildren() {
			balanceLeft();
			balanceMiddle();
			balanceRight();
		}

		private void balanceLeft() {
			TernaryTreeBalancer.balance(node::getLeft, this.node::setLeft);
		}

		private void balanceMiddle() {
			TernaryTreeBalancer.balance(node::getMiddle, this.node::setMiddle);
		}

		private void balanceRight() {
			TernaryTreeBalancer.balance(node::getRight, this.node::setRight);
		}

		private long getClockwiseCounterClockwiseScore() {
			long leftWeight = Weigher.combineWeights(Weigher.getLeftWeight(node), Weigher.getMiddleAndHereWeight(node),
					node.getRight() != null && node.getRight().getLeft() != null
							? Weigher.getLeftWeight(node.getRight().getLeft())
							: 0L);
			long middleWeight = node.getRight() != null && node.getRight().getLeft() != null
					? Weigher.getMiddleAndHereWeight(node.getRight().getLeft())
					: 0L;

			long rightWeight = Weigher.combineWeights(
					node.getRight() != null && node.getRight().getLeft() != null
							? Weigher.getRightWeight(node.getRight().getLeft())
							: 0L,
					node.getRight() != null ? Weigher.getMiddleAndHereWeight(node.getRight()) : 0L,
					node.getRight() != null ? Weigher.getRightWeight(node.getRight()) : 0L);
			return Weigher.combine(leftWeight, middleWeight, rightWeight);
		}

		public long getClockwiseScore() {
			long leftWeight = node.getLeft() != null ? Weigher.getLeftWeight(node.getLeft()) : 0L;
			long middleWeight = node.getLeft() != null ? Weigher.getMiddleAndHereWeight(node.getLeft()) : 0L;

			long rightWeight = Weigher.combineWeights(
					node.getLeft() != null ? Weigher.getRightWeight(node.getLeft()) : 0L,
					Weigher.getMiddleAndHereWeight(node), Weigher.getRightWeight(node));

			return Weigher.combine(leftWeight, middleWeight, rightWeight);
		}

		public long getCounterClockwiseClockwiseScore() {
			long rightWeight = Weigher.combineWeights(
					node.getLeft() != null && node.getLeft().getRight() != null
							? Weigher.getLeftWeight(node.getLeft().getRight())
							: 0L,
					node.getLeft() != null ? Weigher.getMiddleAndHereWeight(node.getLeft()) : 0L,
					node.getLeft() != null ? Weigher.getLeftWeight(node.getLeft()) : 0L);

			long middleWeight = node.getLeft() != null && node.getLeft().getRight() != null
					? Weigher.getMiddleAndHereWeight(node.getLeft().getRight())
					: 0L;

			long leftWeight = Weigher.combineWeights(Weigher.getRightWeight(node), Weigher.getMiddleAndHereWeight(node),
					node.getLeft() != null && node.getLeft().getRight() != null
							? Weigher.getRightWeight(node.getLeft().getRight())
							: 0L);

			return Weigher.combine(leftWeight, middleWeight, rightWeight);
		}

		public long getCounterClockwiseScore() {
			long leftWeight = Weigher.combineWeights(Weigher.getLeftWeight(node), Weigher.getMiddleAndHereWeight(node),
					node.getRight() != null ? Weigher.getLeftWeight(node.getRight()) : 0L);
			long middleWeight = node.getRight() != null ? Weigher.getMiddleAndHereWeight(node.getRight()) : 0L;
			long rightWeight = node.getRight() != null ? Weigher.getRightWeight(node.getRight()) : 0L;

			return Weigher.combine(leftWeight, middleWeight, rightWeight);
		}

		public long getCurrentScore() {
			return Weigher.combine(Weigher.getLeftWeight(node), Weigher.getMiddleAndHereWeight(node),
					Weigher.getRightWeight(node));
		}

		private void rotateClockwise() {
			if (node.getLeft() == null) {
				throw new IllegalStateException();
			}
			ABytesNode newRoot = node.getLeft();
			if (node.getLeft() != null) {
				node.getLeft().setParent(null);
			}
			node.setLeft(null);
			newRoot.setParent(null);
			node.setLeft(newRoot.getRight());
			newRoot.setRight(node);
			setter.accept(newRoot);
		}

		private void rotateCounterClockwise() {
			if (node.getRight() == null) {
				throw new IllegalStateException();
			}
			ABytesNode newRoot = node.getRight();
			if (node.getRight() != null) {
				node.getRight().setParent(null);
			}
			newRoot.setParent(null);
			node.setRight(newRoot.getLeft());
			newRoot.setLeft(node);
			setter.accept(newRoot);
		}

		private With withLeft(ABytesNode left) {
			return with(left, node::setLeft);
		}

		private With withRight(ABytesNode right) {
			return with(right, node::setRight);
		}
	}
}

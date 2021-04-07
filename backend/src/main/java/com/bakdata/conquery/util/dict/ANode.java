package com.bakdata.conquery.util.dict;

import lombok.Getter;
import lombok.Setter;

public abstract class ANode<N extends ANode<N>> extends NodeParent<N> {

	@Getter
	private N left;
	@Getter
	private N middle;
	@Getter
	private N right;
	@Getter @Setter
	private N parent;

	@Override
	protected void replace(N oldNode, TTDirection direction, N newNode) {
		switch(direction) {
			case LEFT: {
				if(left!=oldNode) {
					throw new IllegalStateException();
				}
				this.setLeft(newNode);
				return;
			}
			case MIDDLE: {
				if(middle!=oldNode) {
					throw new IllegalStateException();
				}
				this.setMiddle(newNode);
				return;
			}
			case RIGHT: {
				if(right!=oldNode) {
					throw new IllegalStateException();
				}
				this.setRight(newNode);
				return;
			}
			default:
				throw new IllegalStateException();
		}
	}

	protected void copyChildrenFrom(N node) {
		this.setLeft(node.getLeft());
		this.setMiddle(node.getMiddle());
		this.setRight(node.getRight());
	}
	
	@Override
	public abstract String toString();
	
	public void setLeft(N left) {
		this.left = left;
		if(left != null) {
			left.setParent((N) this);
		}
	}
	
	public void setRight(N right) {
		this.right = right;
		if(right != null) {
			right.setParent((N) this);
		}
	}
	
	public void setMiddle(N middle) {
		this.middle = middle;
		if(middle != null) {
			middle.setParent((N) this);
		}
	}
}
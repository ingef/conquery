package com.bakdata.conquery.util.dict;

import static com.bakdata.conquery.util.dict.TTDirection.MIDDLE;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jackson.serializer.BytesTTMapDeserializer;
import com.bakdata.conquery.io.jackson.serializer.BytesTTMapSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonSerialize(using = BytesTTMapSerializer.class)
@JsonDeserialize(using = BytesTTMapDeserializer.class)
public class BytesTTMap extends NodeParent<ABytesNode> {

	private int size;

	@Getter
	private ABytesNode root;
	@Getter
	private List<ValueNode> entries = new ArrayList<>();

	public BytesTTMap(ABytesNode root) {
		this.root = root;
		entries = collectValueNodes();
		size = entries.size();
		entries.sort(Comparator.comparing(ValueNode::getValue));
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean containsKey(byte[] key) {
		return get(key) != -1;
	}

	public int get(byte[] key) {
		if (key == null) {
			throw new IllegalArgumentException("Illegal key. Must not be null.");
		}
		if (key.length == 0) {
			throw new IllegalArgumentException("Illegal key. Must not be of length 0.");
		}
		if (root == null) {
			return -1;
		}

		return root.get(key, 0);
	}

	public ValueNode getNode(byte[] key) {
		if (key == null) {
			throw new IllegalArgumentException("Illegal key. Must not be null.");
		}
		if (key.length == 0) {
			throw new IllegalArgumentException("Illegal key. Must not be of length 0.");
		}
		if (root == null) {
			return null;
		}

		return root.getNode(key, 0);
	}

	public ValueNode getNearestNode(byte[] key) {
		if (key == null) {
			throw new IllegalArgumentException("Illegal key. Must not be null.");
		}
		if (key.length == 0) {
			throw new IllegalArgumentException("Illegal key. Must not be of length 0.");
		}
		if (root == null) {
			return null;
		}

		return root.getNearestNode(key, 0, null);
	}

	public int put(byte[] key, int value) {
		if (key == null) {
			throw new IllegalArgumentException("Illegal key. Must not be null.");
		}
		if (value < 0) {
			throw new IllegalArgumentException("Illegal value '" + value + "'. Must not be null.");
		}
		if (key.length == 0) {
			throw new IllegalArgumentException("Illegal key. Must not be of length 0.");
		}

		if (root == null) {
			root = TTHelper.createBytesValueNode(this, key, value);
			size = 1;
			return -1;
		}
		int res = root.put(this, this, MIDDLE, key, 0, value);
		if (res == -1) {
			size++;
		}
		return res;
	}

	@Override
	protected void replace(ABytesNode oldNode, TTDirection direction, ABytesNode newNode) {
		switch (direction) {
			case MIDDLE: {
				if (root != oldNode) {
					throw new IllegalStateException();
				}
				root = newNode;
				return;
			}
			default:
				throw new IllegalStateException();
		}
	}

	private List<ValueNode> collectValueNodes() {
		List<ValueNode> valueNodes = new ArrayList<>();
		ArrayDeque<ABytesNode> openList = new ArrayDeque<>();
		openList.add(root);
		while (!openList.isEmpty()) {
			ABytesNode n = openList.removeFirst();
			if (n instanceof ValueNode) {
				valueNodes.add((ValueNode) n);
			}

			if (n.getRight() != null) {
				openList.addFirst(n.getRight());
			}
			if (n.getMiddle() != null) {
				openList.addFirst(n.getMiddle());
			}
			if (n.getLeft() != null) {
				openList.addFirst(n.getLeft());
			}
		}
		return valueNodes;
	}

	public void setEntry(ValueNode rep, int value) {
		if (value < entries.size()) {
			entries.set(value, rep);
		}
		else if (value == entries.size()) {
			entries.add(rep);
		}
		else {
			throw new IllegalStateException("Value " + value + "  beyond size" + entries.size());
		}

	}

	public void balance() {
		TernaryTreeBalancer.balance(() -> root, r -> root = r);
	}


	public List<String> getValues() {
		return getEntries().stream().map(ValueNode::toValue).collect(Collectors.toList());
	}

}
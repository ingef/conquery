package com.bakdata.conquery.util.dict;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.bakdata.conquery.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.buffer.IoBuffer;

@CPSType(id="SUCCINCT_TRIE", base=Dictionary.class)
@Getter
public class SuccinctTrie extends Dictionary {

	@Getter
	private int nodeCount;
	@Getter
	private int entryCount;
	@Getter
	private long totalBytesStored;
	// Reverse lookup can be performed with an int array, because the values are
	// consecutive increasing
	private int[] reverseLookup;
	private int[] parentIndex;
	private int[] lookup;

	// keyPartArray[x] contains the byte stored in node x
	private byte[] keyPartArray;

	@JsonIgnore
	private HelpNode root;

	// caches the the access on select0
	private int[] selectZeroCache;

	// indicates whether compress() has been performed and if the trie is ready to
	// query
	@Getter
	@JsonIgnore
	private boolean compressed;

	public SuccinctTrie(Dataset dataset, String name) {
		super(dataset, name);
		this.root = new HelpNode(null, (byte) 0);
		this.root.setPositionInArray(0);
		this.nodeCount = 2;
		entryCount = 0;
	}

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public SuccinctTrie(String name,
						Dataset dataset,
						int nodeCount,
						int entryCount,
						int[] reverseLookup,
						int[] parentIndex,
						int[] lookup,
						byte[] keyPartArray,
						int[] selectZeroCache,
						long totalBytesStored) {
		super(dataset, name);
		this.nodeCount = nodeCount;
		this.entryCount = entryCount;
		this.reverseLookup = reverseLookup;
		this.parentIndex = parentIndex;
		this.lookup = lookup;
		this.keyPartArray = keyPartArray;
		this.selectZeroCache = selectZeroCache;
		this.totalBytesStored = totalBytesStored;

		this.root = null;
		this.compressed = true;
	}

	@Override
	public int add(byte[] bytes) {
		return put(bytes,entryCount,true);
	}

	@Override
	public int put(byte[] key) {
		return put(key, entryCount, false);
	}
	
	public void checkCompressed(String errorMessage) {
		if (!isCompressed()) {
			throw new IllegalStateException(errorMessage);
		}
	}
	
	public void checkUncompressed(String errorMessage) {
		if (isCompressed()) {
			throw new IllegalStateException(errorMessage);
		}
	}

	private int put(byte[] key, int value, boolean failOnDuplicate) {
		checkUncompressed("No put allowed after compression");

		// insert help nodes
		int nodeIndex = 0;
		HelpNode current = root;
		while (nodeIndex < key.length) {
			// check if a prefix node exists
			HelpNode next = current.children.get(key[nodeIndex]);
			if (next == null) {
				// no prefix node could be found, we add a new one
				next = new HelpNode(current, key[nodeIndex]);
				next.setParent(current);
				current.addChild(next);
				nodeCount++;

				if (nodeCount > Integer.MAX_VALUE - 10) {
					throw new IllegalStateException("This dictionary is too large " + nodeCount);
				}
			}
			current = next;
			nodeIndex++;
		}

		// end of key, write the value into current
		if (current.getValue() == -1) {
			current.setValue(value);
			totalBytesStored += key.length;
			return entryCount++;
		}
		else if (failOnDuplicate){
			throw new IllegalStateException(String.format("the key `%s` was already part of this trie", new String(key, StandardCharsets.UTF_8)));
		}
		else {
			return current.getValue();
		}
	}

	public void compress() {
		if(compressed){
			return;
		}

		// get the nodes in left right, top down order (level order)
		List<HelpNode> nodesInOrder = createNodesInOrder();

		// write the bits
		selectZeroCache = new int[nodeCount + 1];
		int position = 2;
		int zeroesWritten = 1;
		selectZeroCache[1] = 1;

		for (HelpNode node : nodesInOrder) {
			position+=node.children.size();
			zeroesWritten++;
			selectZeroCache[zeroesWritten] = position;
			position++;
		}

		// free the helpTrie for GC
		root = null;
		compressed = true;
	}

	private List<HelpNode> createNodesInOrder() {
		ArrayList<HelpNode> nodesInOrder = new ArrayList<HelpNode>(nodeCount-1);

		// initialize arrays for rebuilding the data later on
		reverseLookup = new int[entryCount];
		parentIndex = new int[nodeCount];
		Arrays.fill(parentIndex, -1);

		lookup = new int[nodeCount];
		Arrays.fill(lookup, -1);


		keyPartArray = new byte[nodeCount];

		nodesInOrder.add(root);
		for (int index=0; index < nodeCount-1; index++) {
			HelpNode node = nodesInOrder.get(index);
			node.setPositionInArray(index);
			if (node != root) {
				keyPartArray[index] = node.getPartialKey();
			}

			if (node.getParent() != null) {
				parentIndex[index] = node.getParent().getPositionInArray();
			}

			node.getChildren().values().forEach(nodesInOrder::add);

			if (node.getValue() != -1) {
				reverseLookup[node.getValue()] = index;
				lookup[index] = node.getValue();
			}
		}
		return nodesInOrder;
	}

	private int select0(int positionForZero) {
		return selectZeroCache[positionForZero];
	}

	@Override
	@JsonIgnore
	public int getId(byte[] value) {
		if (!compressed) {
			HelpNode node = root;
			for (byte val : value) {
				node = findChildWithKey(node, val);
				if (node == null) {
					return -1;
				}
			}

			return node.value;
		}

		int node = 0;
		for (byte val : value) {
			// check for fitting child

			int firstChildNode = select0(node + 1) - node;
			// get the first child of the next node
			int lastChild = select0(node + 1 + 1) - (node + 1);

			node = childIdWithKey(firstChildNode, lastChild, val);

			if (node == -1) {
				// no fitting child found
				return -1;
			}
		}
		// node has a value
		return lookup[node];
	}

	private HelpNode findChildWithKey(HelpNode node, byte val) {
		return node.children.get(val);
	}

	private int childIdWithKey(int firstChildNode, int lastChildNode, byte val) {
		for (int i = firstChildNode; i < lastChildNode; i++) {
			if (keyPartArray[i] == val) {
				return i;
			}
		}
		// no fitting child found
		return -1;
	}


	public void getReverse(int intValue, IoBuffer buffer) {
		checkCompressed("use compress before performing getReverse on the trie");

		if (intValue >= reverseLookup.length) {
			throw new IllegalArgumentException(String.format("intValue %d too high, no such key in the trie (Have only %d values)", intValue, reverseLookup.length));
		}
		int nodeIndex = reverseLookup[intValue];
		while (parentIndex[nodeIndex] != -1) {
			// resolve nodeIndex and append byteValue
			buffer.put(keyPartArray[nodeIndex]);
			nodeIndex = parentIndex[nodeIndex];
		}
		buffer.flip();

		//reverse bytes
		byte tmp;
		int length = buffer.limit();
		for (int i = 0; i < length / 2; i++) {
			tmp = buffer.get(i);
			buffer.put(i, buffer.get(length - i - 1));
			buffer.put(length - i - 1, tmp);
		}
	}

	@Override
	public int size() {
		return entryCount;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return entryCount == 0;
	}

	@Data @RequiredArgsConstructor
	public static class Entry {
		private final int key;
		private final String value;
	}

	@Override
	public Iterator<DictionaryEntry> iterator() {
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		return new AbstractIterator<DictionaryEntry>() {

			private int index = 0;

			@Override
			protected DictionaryEntry computeNext() {
				if (index == entryCount) {
					buffer.free();
					return endOfData();
				}
				getReverse(index++, buffer);
				byte[] result = BufferUtil.toBytes(buffer);
				buffer.clear();
				return new DictionaryEntry(index, result);
			}
		};
	}

	@Data
	private class HelpNode {

		private final Byte2ObjectMap<HelpNode> children = new Byte2ObjectArrayMap<>();
		private final byte partialKey;
		private HelpNode parent;
		private int value = -1;
		private int positionInArray = -1;

		public HelpNode(HelpNode parent, byte key) {
			this.parent = parent;
			this.partialKey = key;
		}

		public void addChild(HelpNode child) {
			this.children.put(child.partialKey, child);
		}

	}

	@Override
	public byte[] getElement(int id) {
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		getReverse(id, buffer);
		byte[] out = new byte[buffer.limit()-buffer.position()];
		buffer.get(out);
		buffer.free();
		return out;
	}

	@Override
	public long estimateMemoryConsumption() {
		return 13L*getNodeCount() + 4L*size();
	}
}

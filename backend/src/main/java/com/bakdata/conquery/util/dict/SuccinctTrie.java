package com.bakdata.conquery.util.dict;

import java.nio.charset.StandardCharsets;
import java.util.*;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.bytes.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of a succinct trie that maps stored strings (byte arrays) to an id (https://en.wikipedia.org/wiki/Succinct_data_structure). The id is the node index of the
 * starting byte in the trie. To get all bytes of a string, all bytes towards the root must be collected. This means
 * that every node in the trie can be the beginning of a string, and that the nodes closest to the root are the endings
 * of the string.
 *
 * Inserting the strings this way (reversed) into the trie allows lookups in either direction with little computational
 * overhead.
 */
@CPSType(id="SUCCINCT_TRIE", base=Dictionary.class)
@Getter
public class SuccinctTrie extends Dictionary {

	@Getter
	private int nodeCount;
	@Getter
	private int depth = 0;
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

	private int put(byte[] key, int entryCount, boolean failOnDuplicate) {
		checkUncompressed("No put allowed after compression");

		// start at the end of the byte sequence and insert it reversed
		int keyIndex = key.length-1;
		HelpNode current = root;
		while (keyIndex >= 0) {
			// check if a prefix node exists
			HelpNode next = current.children.get(key[keyIndex]);
			if (next == null) {
				// no prefix node could be found, we add a new one
				next = new HelpNode(current, key[keyIndex]);
				next.setParent(current);
				current.addChild(next);
				nodeCount++;

				if(next.depth > depth) {
					depth = next.depth;
				}

				if (nodeCount > Integer.MAX_VALUE - 10) {
					throw new IllegalStateException("This dictionary is too large " + nodeCount);
				}
			}
			current = next;
			keyIndex--;
		}

		// end of key, write the value into current
		if (current.getValue() == -1) {
			current.setValue(entryCount);
			totalBytesStored += key.length;
			this.entryCount++;
		}
		else if (failOnDuplicate){
			throw new IllegalStateException(String.format("the key `%s` was already part of this trie", new String(key, StandardCharsets.UTF_8)));
		}

		return current.getValue();
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
			position += node.children.size();
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

	@Override
	@JsonIgnore
	public int getId(byte[] value) {
		if (!compressed) {
			HelpNode node = root;
			for (int i = value.length-1; i >= 0; i--) {
				node = findChildWithKey(node, value[i]);
				if (node == null) {
					return -1;
				}
			}

			return node.value;
		}

		int node = 0;
		// Traverse the tree along the byte[], exiting when we don't find a match
		for (int i = value.length-1; i >= 0; i--) {

			node = childIdWithKey(node, value[i]);

			if (node == -1) {
				// no fitting child found
				return -1;
			}
		}
		// node has a value
		return lookup[node];
	}

	public int findStart(int node) {
		return selectZeroCache[node + 1] - node;
	}

	private HelpNode findChildWithKey(HelpNode node, byte val) {
		return node.children.get(val);
	}

	private int childIdWithKey(int node, byte val) {
		int firstChildNode = findStart(node);
		// get the first child of the next node
		int lastChildNode = findStart(node +  1);

		for (int i = firstChildNode; i < lastChildNode; i++) {
			if (keyPartArray[i] == val) {
				return i;
			}
		}
		// no fitting child found
		return -1;
	}


	/**
	 * The provided id for the string is the index of the trie node that holds the first byte of the sequence.
	 * From there on, the bytes of the parents until the root are collected to build byte sequence in forward order.
	 * @param id the id that references the search byte sequence
	 * @param buf the buffer into which the bytes are inserted
	 */
	public void get(int id, ByteArrayList buf) {
		checkCompressed("use compress before performing getReverse on the trie");

		if (id >= reverseLookup.length) {
			throw new IllegalArgumentException(String.format("intValue %d too high, no such key in the trie (Have only %d values)", id, reverseLookup.length));
		}

		int nodeIndex = reverseLookup[id];
		int parentIndex = -1;
		while ((parentIndex = this.parentIndex[nodeIndex]) != -1) {
			buf.add(keyPartArray[nodeIndex]);
			nodeIndex = parentIndex;
		};
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

		return new AbstractIterator<DictionaryEntry>() {

			private ByteArrayList buf = new ByteArrayList(depth);
			private int index = 0;

			@Override
			protected DictionaryEntry computeNext() {
				if (index == entryCount) {
					return endOfData();
				}
				buf.clear();
				get(index++, buf);
				return new DictionaryEntry(index, buf.toByteArray());
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
		private int depth = 0;

		public HelpNode(HelpNode parent, byte key) {
			this.parent = parent;
			this.partialKey = key;
		}

		public void addChild(HelpNode child) {
			child.setDepth(this.depth+1);
			this.children.put(child.partialKey, child);
		}

	}

	@Override
	public byte[] getElement(int id) {
		ByteArrayList buf = new ByteArrayList(depth);
		get(id, buf);
		return buf.toByteArray();
	}

	@Override
	public long estimateMemoryConsumption() {
		return 13L*getNodeCount() + 4L*size();
	}
}

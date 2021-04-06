package com.bakdata.conquery.util.dict;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.buffer.IoBuffer;

@CPSType(id="SUCCINCT_TRIE", base=Dictionary.class)
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

	private HelpNode root;

	// caches the the access on select0
	private int[] selectZeroCache;

	// indicates whether compress() has been performed and if the trie is ready to
	// query
	@Getter
	private boolean compressed;

	public SuccinctTrie(DatasetId dataset, String name) {
		super(dataset, name);
		this.root = new HelpNode(null, (byte) 0);
		this.root.setPositionInArray(0);
		this.nodeCount = 2;
		entryCount = 0;
	}

	@JsonCreator
	public static SuccinctTrie fromSerialized(SerializedSuccinctTrie serialized) {
		SuccinctTrie trie = new SuccinctTrie(serialized.getDataset(), serialized.getName());
		trie.nodeCount = serialized.getNodeCount();
		trie.entryCount = serialized.getEntryCount();
		trie.reverseLookup = serialized.getReverseLookup();
		trie.parentIndex = serialized.getParentIndex();
		trie.lookup = serialized.getLookup();
		trie.keyPartArray = serialized.getKeyPartArray();
		trie.selectZeroCache = serialized.getSelectZeroCache();
		trie.totalBytesStored = serialized.getTotalBytesStored();

		trie.root = null;
		trie.compressed = true;

		return trie;
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
		checkUncompressed("no put allowed after compression");
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
				if (nodeCount > Integer.MAX_VALUE - 10)
					throw new IllegalStateException("This dictionary is to large " + nodeCount);
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
			throw new IllegalStateException(String.format("the key {} was already part of this trie", new String(key, StandardCharsets.UTF_8)));
		}
		else {
			return current.getValue();
		}
	}



	public void compress() {
		checkUncompressed("compress is only allowed once");

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

	public boolean isEmpty() {
		return entryCount == 0;
	}

	public List<byte[]> getValuesBytes() {
		List<byte[]> valuesBytes = new ArrayList<>();
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		for (int i = 0; i < entryCount; i++) {
			getReverse(i, buffer);
			byte[] bytes = new byte[buffer.limit() - buffer.position()];
			buffer.get(bytes);
			valuesBytes.add(bytes);
			buffer.clear();
		}
		buffer.free();
		return valuesBytes;
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

	@JsonValue
	public SerializedSuccinctTrie toSerialized() {
		checkCompressed("no serialisation allowed before compressing the trie");
		return new SerializedSuccinctTrie(getName(), getDataset(), nodeCount, entryCount, reverseLookup, parentIndex, lookup, keyPartArray, selectZeroCache, totalBytesStored);
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

package com.bakdata.conquery.util.dict;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;

import com.bakdata.conquery.util.BufferUtil;
import com.esotericsoftware.kryo.util.IntMap.Entry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.AbstractIterator;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import lombok.Data;
import lombok.Getter;

public class SuccinctTrie implements Iterable<String> {

	private int nodeCount;
	@Getter
	private int entryCount;
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
	private boolean compressed;

	public SuccinctTrie() {
		this.root = new HelpNode(null, (byte)0);
		this.root.setPositionInArray(0);
		this.nodeCount = 2;
		entryCount = 0;
	}
	
	public static SuccinctTrie createUncompressed(SuccinctTrie compressedTrie) {
		compressedTrie.checkCompressed("Constructor only works for compressed tries");
		
		SuccinctTrie trie = new SuccinctTrie();
		for(byte [] value: compressedTrie.getValuesBytes()) {
			trie.put(value);
		}
		return trie;
	}

	public int put(byte[] key) {
		return put(key, entryCount);
	}

	private int put(byte[] key, int value) {
		checkUncompressed("no put allowed after compression");
		// insert help nodes
		int nodeIndex = 0;
		HelpNode current = root;
		while (nodeIndex < key.length) {
			HelpNode next = null;
			// check if a prefix node exists
			next = current.children.get(key[nodeIndex]);
			if (next == null) {
				// no prefix node could be found, we add a new one
				next = new HelpNode(current, key[nodeIndex]);
				next.setParent(current);
				current.addChild(next);
				nodeCount++;
				if(nodeCount > Integer.MAX_VALUE - 10)
					throw new IllegalStateException("This dictionary is to large "+nodeCount);
			}
			current = next;
			nodeIndex++;
		}
		
		// end of key, write the value into current
		if(current.getValue() == -1) {
			current.setValue(value);
			entryCount++;
			return entryCount-1;
		}
		else {
			throw new IllegalStateException(String.format("the key {} was already part of this trie", new String(key, StandardCharsets.UTF_8)));
		}
	}

	public void tryCompress() {
		if(!compressed)
			compress();
	}
	
	public void compress() {
		checkUncompressed("compress is only allowed once");
		
		// get the nodes in left right, top down order (level order)
		ArrayList<HelpNode> nodesInOrder = new ArrayList<HelpNode>();
		ArrayList<HelpNode> nodesInDepth = new ArrayList<HelpNode>();
		ArrayList<HelpNode> nodesInNextDepth = new ArrayList<HelpNode>();
		ArrayList<HelpNode> tmp;

		// initialize arrays for rebuilding the data later on
		reverseLookup = new int[entryCount];
		parentIndex = new int[nodeCount];
		Arrays.fill(parentIndex, -1);

		lookup = new int[nodeCount];
		Arrays.fill(lookup, -1);

		int nodeIndex = 0;

		keyPartArray = new byte[nodeCount];

		nodesInDepth.add(root);
		nodesInOrder.add(root);
		while (!nodesInDepth.isEmpty()) {
			for (HelpNode node : nodesInDepth) {
				node.setPositionInArray(nodeIndex);
				if (node != root) {
					keyPartArray[nodeIndex] = node.partialKey;
				}

				if (node.parent != null) {
					parentIndex[nodeIndex] = node.parent.getPositionInArray();
				}

				Collection<HelpNode> children = node.children.values();
				nodesInNextDepth.addAll(children);
				nodesInOrder.addAll(children);

				if (node.value != -1) {
					reverseLookup[node.value] = nodeIndex;
					lookup[nodeIndex] = node.value;
				}

				nodeIndex++;
			}
			
			tmp = nodesInDepth;
			tmp.clear();
			nodesInDepth = nodesInNextDepth;
			nodesInNextDepth = tmp;
		}

		// write the bits
		selectZeroCache = new int[nodeCount + 1];
		int position = 2;
		int zeroesWritten = 1;
		selectZeroCache[1] = 1;
		
		for (HelpNode node : nodesInOrder) {
			for (nodeIndex = 0; nodeIndex < node.children.size(); nodeIndex++) {
				position++;
			}
			zeroesWritten++;
			selectZeroCache[zeroesWritten]=position;
			position++;
		}

		// free the helpTrie for GC
		root = null;
		compressed = true;
	}

	/*
	 * select0(n) - returns the position of the nth 0 in the bit store.
	 */

	private int select0(int positionForZero) {
		return selectZeroCache[positionForZero];
	}
	
	private void checkCompressed(String errorMessage) {
		if (!compressed) {
			throw new IllegalStateException(errorMessage);
		}
	}
	
	private void checkUncompressed(String errorMessage) {
		if (compressed) {
			throw new IllegalStateException(errorMessage);
		}
	}

	
	@JsonIgnore
	public int get(byte[] value) {
		if (!compressed) {
			HelpNode node = root;
			for (byte val : value) {
				node = findChildWithKey(node, val);
				if(node == null) {
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
	
	public boolean containsReverse(int intValue) {
		checkCompressed("use compress before performing containsReverse on the trie");
		return intValue < reverseLookup.length;
	}

	public void getReverse(int intValue, IoBuffer buffer) {
		checkCompressed("use compress before performing containsReverse on the trie");
		
		if(intValue >= reverseLookup.length) {
			throw new IllegalArgumentException("intValue "+intValue+" to high, no such key in the trie");
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
		for(int i = 0; i<length/2;i++ ) {
			tmp = buffer.get(i);
			buffer.put(i, buffer.get(length-i-1));
			buffer.put(length-i-1, tmp);
		}
	}

	public int size() {
		return entryCount;
	}

	public boolean isEmpty() {
		return entryCount == 0;
	}

	public List<String> getValues() {
		List<String> values = new ArrayList<>();
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		for(int i=0; i < entryCount; i++) {
			getReverse(i, buffer);
			values.add(BufferUtil.toUtf8String(buffer));
			buffer.clear();
		}
		buffer.free();
		return values;
	}
	
	public List<byte[]> getValuesBytes() {
		List<byte[]> valuesBytes = new ArrayList<>();
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		for(int i=0; i < entryCount; i++) {
			getReverse(i, buffer);
			byte[] bytes = new byte[buffer.limit()-buffer.position()];
			buffer.get(bytes);
			valuesBytes.add(bytes);
			buffer.clear();
		}
		buffer.free();
		return valuesBytes;
	}
	
	public Collection<Entry<String>> getEntries() {
		int i = 0;
		Collection<Entry<String>> entries = new ArrayList<Entry<String>>();
		
		for(String val: getValues()) {
			Entry<String> entry = new Entry<>();
			entry.key = i;
			entry.value = val;
			entries.add(entry);
			i++;
		}
		return entries;
	}
	
	@Override
	public Iterator<String> iterator() {
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		return new AbstractIterator<String>() {
			private int index = 0;
			
			@Override
			protected String computeNext() {
				if(index==entryCount) {
					buffer.free();
					return endOfData();
				}
				getReverse(index++, buffer);
				String result = BufferUtil.toUtf8String(buffer);
				buffer.clear();
				return result;
			}
		};
	}

	@JsonValue
	public SerializedSuccinctTrie toSerialized() {
		checkCompressed("no serialisation allowed before compressing the trie");
		return new SerializedSuccinctTrie(nodeCount, entryCount, reverseLookup, parentIndex, lookup, keyPartArray, selectZeroCache);
	}
	
	@JsonCreator
	public static SuccinctTrie fromSerialized(SerializedSuccinctTrie serialized) {
		SuccinctTrie trie = new SuccinctTrie();
		trie.nodeCount = serialized.getNodeCount();
		trie.entryCount = serialized.getEntryCount();
		trie.reverseLookup = serialized.getReverseLookup();
		trie.parentIndex = serialized.getParentIndex();
		trie.lookup = serialized.getLookup();
		trie.keyPartArray = serialized.getKeyPartArray();
		trie.selectZeroCache = serialized.getSelectZeroCache();
		
		trie.root = null;
		trie.compressed = true;
		
		return trie;
	}
	
	@Data
	private class HelpNode {
		private HelpNode parent;
		private final Byte2ObjectMap<HelpNode> children = new Byte2ObjectOpenHashMap<>();
		private final byte partialKey;
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
}


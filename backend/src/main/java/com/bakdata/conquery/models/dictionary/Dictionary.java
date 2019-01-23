package com.bakdata.conquery.models.dictionary;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;

import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.util.BufferUtil;
import com.bakdata.conquery.util.dict.SuccinctTrie;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class Dictionary extends NamedImpl<DictionaryId> implements Iterable<String> {

	@Getter @Setter
	private DatasetId dataset;
	@Getter @Setter
	private SuccinctTrie trie = new SuccinctTrie();

	public Dictionary(DictionaryId dictionaryId) {
		this.setName(dictionaryId.getDictionary());
		this.dataset = dictionaryId.getDataset();
	}

	public static Dictionary copyUncompressed(Dictionary compressedDictionary) {
		Dictionary dict = new Dictionary(compressedDictionary.getId());
		dict.trie = SuccinctTrie.createUncompressed(compressedDictionary.trie);
		return dict;
	}
	
	@Override
	public DictionaryId createId() {
		return new DictionaryId(dataset, getName());
	}

	public synchronized int add(String element) {
		byte[] bytes = element.getBytes();
		return add(bytes);
	}

	public int add(byte[] bytes) {
		int c = trie.get(bytes);
		if (c == -1) {
			return trie.put(bytes);
		} else {
			return c;
		}
	}

	@Override
	public String toString() {
		return "StringDictionary[size=" + size() + "]";
	}

	/**
	 * @return the id of element or -1 if element is not part of the dictionary
	 */
	public synchronized int getId(String element) {
		return trie.get(element.getBytes(StandardCharsets.UTF_8));
	}
	
	public synchronized int getId(byte[] element) {
		return trie.get(element);
	}

	public synchronized String getElement(int id) {
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		trie.getReverse(id, buffer);
		String str = BufferUtil.toUtf8String(buffer);
		buffer.free();
		return str;
	}

	public synchronized byte[] getElementBytes(int id) {
		IoBuffer buffer = IoBuffer.allocate(512);
		buffer.setAutoExpand(true);
		trie.getReverse(id, buffer);
		byte[] out = new byte[buffer.limit()-buffer.position()];
		buffer.get(out);
		buffer.free();
		return out;
	}

	@Override
	public Iterator<String> iterator() {
		return trie.iterator();
	}

	public int size() {
		return trie.size();
	}

	@Getter
	@AllArgsConstructor
	@Setter
	@NoArgsConstructor
	public static class DictionaryEntry {
		private String value;
		private int id;

	}

	public void compress() {
		trie.compress();
	}
	
	public void tryCompress() {
		trie.tryCompress();
	}

	public List<String> values() {
		return trie.getValues();
	}

	
}
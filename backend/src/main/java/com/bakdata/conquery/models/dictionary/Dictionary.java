package com.bakdata.conquery.models.dictionary;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import com.jakewharton.byteunits.BinaryByteUnit;
import com.jakewharton.byteunits.ByteUnit;

import it.unimi.dsi.fastutil.Hash;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor @Slf4j
public class Dictionary extends NamedImpl<DictionaryId> implements Iterable<String> {

	@Getter @Setter
	private DatasetId dataset;
	@Getter @Setter
	private StringMap trie = new SuccinctTrie();

	public Dictionary(DictionaryId dictionaryId) {
		this.setName(dictionaryId.getDictionary());
		this.dataset = dictionaryId.getDataset();
	}

	public static Dictionary copyUncompressed(Dictionary compressedDictionary) {
		Dictionary dict = new Dictionary(compressedDictionary.getId());
		dict.trie = compressedDictionary.getTrie().uncompress();
		return dict;
	}
	
	@Override
	public DictionaryId createId() {
		return new DictionaryId(dataset, getName());
	}

	public synchronized int add(String element) {
		return trie.add(element);
	}

	public int add(byte[] bytes) {
		return trie.add(bytes);
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
		return trie.getElement(id);
	}

	public synchronized byte[] getElementBytes(int id) {
		return trie.getElementBytes(id);
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
		//check if another type of StringMap would be smaller
		if(trie instanceof SuccinctTrie) {
			float trieSize = 13*((SuccinctTrie) trie).getNodeCount() + 4*trie.size();
			//size of two collections and string object overhead
			float mapSize = size()*(48f+8f/Hash.DEFAULT_LOAD_FACTOR)
				//number of string bytes
				+ ((SuccinctTrie) trie).getTotalBytesStored();

			if(mapSize < trieSize) {
				log.debug(
					"Using MapDictionary(est. {}) instead of Trie(est. {}) for {}",
					BinaryByteUnit.format((long)mapSize),
					BinaryByteUnit.format((long)trieSize),
					getId()
				);
				trie.compress();
				MapDictionary map = new MapDictionary();
				for(String v : trie) {
					map.add(v);
				}
				trie = map;
			}
		}
		trie.compress();
	}
	
	public void tryCompress() {
		if(!trie.isCompressed()) {
			this.compress();
		}
	}

	public List<String> values() {
		return trie.getValues();
	}
}
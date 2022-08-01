package com.bakdata.conquery.models.index;

import com.bakdata.conquery.util.search.TrieSearch;

public class TrieIndex<V extends Comparable<V>> extends TrieSearch<V> implements Index<TrieIndexKey<V>,V>{

	public TrieIndex(int suffixCutoff, String split) {
		super(suffixCutoff, split);
	}

	@Override
	public Object put(String key, Object value) {
		super.addItem(value, key);
		return value;
	}

	@Override
	public int size() {
		return 0;
	}
}

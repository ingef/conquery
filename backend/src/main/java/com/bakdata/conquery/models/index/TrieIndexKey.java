package com.bakdata.conquery.models.index;

import java.net.URL;

public class TrieIndexKey<V extends Comparable<V>> extends AbstractIndexKey<TrieIndex<V>, V> {


	private final int suffixCutoff;

	private final String splitPattern;

	public TrieIndexKey(URL csv, String internalColumn, String externalTemplate, int suffixCutoff, String splitPattern) {
		super(csv, internalColumn, externalTemplate);
		this.suffixCutoff = suffixCutoff;
		this.splitPattern = splitPattern;
	}

	@Override
	public TrieIndex<V> createIndex() {
		return new TrieIndex<>(suffixCutoff, splitPattern);
	}
}

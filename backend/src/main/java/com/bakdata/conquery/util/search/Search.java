package com.bakdata.conquery.util.search;

import java.util.Iterator;
import java.util.List;

public abstract class Search<K> {

	public abstract void finalizeSearch();

	public abstract long calculateSize();

	public abstract List<K> findExact(String searchTerm, int maxValue);

	public abstract Iterator<K> iterator();

	public abstract void addItem(K feValue, List<String> strings);
}

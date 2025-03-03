package com.bakdata.conquery.util.search;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class Search<K> {
	public abstract void addItem(K document, List<String> strings);

	public abstract void finalizeSearch();

	public abstract long calculateSize();

	public abstract List<K> findExact(Collection<String> searchTerm, int maxValue);

	public abstract Iterator<K> iterator();
}

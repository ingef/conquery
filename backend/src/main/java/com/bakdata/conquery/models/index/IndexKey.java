package com.bakdata.conquery.models.index;

import java.net.URL;
import java.util.List;

/**
 * Interface which is used by {@link IndexService} to create and cache a new {@link Index}.
 * For concrete implementations please use {@link AbstractIndexKey} and look out for correct
 * {@link Object#equals(Object)} and {@link Object#hashCode()} functions.
 *
 * @param <I> The type of Index that is indexed by this key
 */
public interface IndexKey<I extends Index<? extends IndexKey<I>>> {
	URL getCsv();

	String getInternalColumn();

	List<String> getExternalTemplates();

	I createIndex();
}

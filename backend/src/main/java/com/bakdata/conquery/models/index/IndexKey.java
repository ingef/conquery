package com.bakdata.conquery.models.index;

import java.net.URI;
import java.util.List;

/**
 * Interface which is used by {@link IndexService} to create and cache a new {@link Index}.
 * For concrete implementations please use {@link AbstractIndexKey} and look out for correct
 * {@link Object#equals(Object)} and {@link Object#hashCode()} functions.
 *
 * @param <I> The type of Index that is indexed by this key
 */
public interface IndexKey<I extends Index<? extends IndexKey<I>>> {

	/**
	 * An url, or a part of it, that points to the referenced csv file.
	 *
	 * @implNote This is an url but implemented as an uri in this data object, because url can have undesired
	 * side effects: <a href="https://www.baeldung.com/java-url-vs-uri#3-opening-a-remote-connection">URL equals() and hashcode()</a>
	 */
	URI getCsv();

	String getInternalColumn();

	List<String> getExternalTemplates();

	I createIndex(String defaultEmptyLabel);

}

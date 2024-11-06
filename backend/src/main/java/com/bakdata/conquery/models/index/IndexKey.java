package com.bakdata.conquery.models.index;

import java.net.URI;
import java.util.List;

/**
 * Interface which is used by {@link IndexService} to create and cache a new {@link Index}.
 * {@link Object#equals(Object)} and {@link Object#hashCode()} functions.
 *
 */
public interface IndexKey {

	/**
	 * An url, or a part of it, that points to the referenced csv file.
	 *
	 * @implNote This is an url but implemented as an uri in this data object, because url can have undesired
	 * side effects: <a href="https://www.baeldung.com/java-url-vs-uri#3-opening-a-remote-connection">URL equals() and hashcode()</a>
	 */
	URI getCsv();

	String getInternalColumn();

	List<String> getExternalTemplates();

	Index<?> createIndex(String defaultEmptyLabel);

}

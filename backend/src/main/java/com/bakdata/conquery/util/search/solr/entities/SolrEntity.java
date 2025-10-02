package com.bakdata.conquery.util.search.solr.entities;

import org.apache.solr.client.solrj.beans.Field;

public interface SolrEntity {

	/**
	 * The id field prevents duplicates.
	 */
	@Field
	String getId();
}

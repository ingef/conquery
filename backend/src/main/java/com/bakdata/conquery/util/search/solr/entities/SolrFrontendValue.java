package com.bakdata.conquery.util.search.solr.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.solr.client.solrj.beans.Field;

@NoArgsConstructor
@AllArgsConstructor
public class SolrFrontendValue {

	@Field
	public String searchable;

	@Field
	public String value;

	@Field
	public String label;

	@Field
	public String optionValue;
}

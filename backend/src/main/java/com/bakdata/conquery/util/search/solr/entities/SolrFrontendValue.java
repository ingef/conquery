package com.bakdata.conquery.util.search.solr.entities;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.apache.solr.client.solrj.beans.Field;

/**
 * Java bind to solr's query engine
 * We use solr's
 */
@NoArgsConstructor
@FieldNameConstants
public class SolrFrontendValue implements SolrEntity {

	@Field
	@Getter
	public String id;

	@Field
	public String searchable_s;

	/**
	 * @implNote The value field is handled as a single lowercased token.
	 */
	@Field
	public String value_s_lower;

	/**
	 * @implNote The label field is indexed as a whitespace-splitting text field resulting in possibly multiple token.
	 */
	@Field
	public String label_ws;

	/**
	 * We can remove this, maybe.
	 * @implNote We have currently no best fit type defined for this field: `_s` is indexed and stored, but we actually only need stored.
	 */
	@Field
	public String optionValue_s;

	public SolrFrontendValue(Searchable<?> searchable, FrontendValue feValue) {
		this(searchable, feValue.getValue(), feValue.getLabel(), feValue.getOptionValue());

	}

	public SolrFrontendValue(Searchable<?> searchable, String value, String label, String optionValue) {
		this.id = buildId(searchable, value);
		this.searchable_s = searchable.getId().toString();
		this.value_s_lower = value;
		this.label_ws = label;
		this.optionValue_s = optionValue;
	}

	public FrontendValue toFrontendValue() {
		return new FrontendValue(value_s_lower, label_ws, optionValue_s);
	}

	private static String buildId(Searchable<?> searchable, String value) {
		return searchable.getId().toString() + " " + value;
	}
}

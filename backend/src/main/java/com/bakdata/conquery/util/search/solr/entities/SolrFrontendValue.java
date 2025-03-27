package com.bakdata.conquery.util.search.solr.entities;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
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
	 * @implNote The value field is a custom field in the schema that allows to use collapsing of results.
	 * We use collapsing to filter out "duplicates" originating from an index and a column
	 */
	@Field
	public String value;

	/**
	 * @implNote The label field is indexed as a general text field resulting in possibly multiple tokens.
	 */
	@Field
	public String label_t;

	/**
	 * We can remove this, maybe.
	 * @implNote We have currently no best fit type defined for this field: `_s` is indexed and stored, but we actually only need stored.
	 */
	@Field
	public String optionValue_s;

	/**
	 * Non-retrievable field for full text search (indexed="true" stored="false")
	 */
	@Field
	public List<String> _text_;

	public SolrFrontendValue(Searchable<?> searchable, FrontendValue feValue) {
		this(searchable, feValue.getValue(), feValue.getLabel(), feValue.getOptionValue());

	}

	public SolrFrontendValue(Searchable<?> searchable, @NonNull String value, String label, String optionValue) {
		this.id = buildId(searchable, value);
		this.searchable_s = searchable.getId().toString();
		this.value = value;
		this.label_t = label;
		this.optionValue_s = optionValue;
		this._text_ = List.of(value, label);
	}

	public FrontendValue toFrontendValue() {
		return new FrontendValue(Objects.requireNonNullElse(value, ""), label_t, optionValue_s);
	}

	private static String buildId(Searchable<?> searchable, String value) {
		return searchable.getId().toString() + " " + value;
	}
}

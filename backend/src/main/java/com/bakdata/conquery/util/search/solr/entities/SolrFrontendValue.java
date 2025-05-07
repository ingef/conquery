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
 * Solr DAO for {@link FrontendValue}.
 * We use solr's object mapping and managed-schema to define a suitable document type
 */
@NoArgsConstructor
@FieldNameConstants
public class SolrFrontendValue implements SolrEntity {

	@Field
	@Getter
	public String id;

	/**
	 * Indicates, from which {@link Searchable} the {@link SolrFrontendValue#value_s}-{@link SolrFrontendValue#label_t}-{@link SolrFrontendValue#optionValue_s}-combo originates from.
	 */
	@Field
	public String searchable_s;

	/**
	 * Determines the priority in the search ranking by its source type ({@link Searchable}).
	 * @implNote lower value -> higher priority.
	 */
	@Field
	public int sourcePriority_i;


	/**
	 * Field for the actual value in our data.
	 * We use collapsing to filter out "duplicates" originating from an index and a column
	 */
	@Field
	public String value_s;

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

	public SolrFrontendValue(String searchable, int sourcePriority, FrontendValue feValue) {
		this(searchable, sourcePriority, feValue.getValue(), feValue.getLabel(), feValue.getOptionValue());

	}

	public SolrFrontendValue(String searchable, int sourcePriority, @NonNull String value, String label, String optionValue) {
		this.id = buildId(searchable, value);
		this.searchable_s = searchable;
		this.sourcePriority_i = sourcePriority;
		this.value_s = value;
		this.label_t = label;
		this.optionValue_s = optionValue;
		this._text_ = label == null ? List.of(value) : List.of(value, label);
	}

	public FrontendValue toFrontendValue() {
		return new FrontendValue(Objects.requireNonNullElse(value_s, ""), Objects.requireNonNullElse(label_t, value_s), optionValue_s);
	}

	private static String buildId(String searchable, String value) {
		return searchable + " " + value;
	}
}

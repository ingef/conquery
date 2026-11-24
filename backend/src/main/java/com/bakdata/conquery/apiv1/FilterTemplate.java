package com.bakdata.conquery.apiv1;

import java.net.URI;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"columns"})
@ToString(callSuper = true)
@Slf4j
@CPSType(id = "CSV_TEMPLATE", base = SearchIndex.class)
public class FilterTemplate extends SearchIndex implements Searchable {

	/**
	 * Path to CSV File.
	 */
	@NotNull
	@EqualsAndHashCode.Include
	private final URI filePath;

	/**
	 * Value to be sent for filtering.
	 */
	@NotEmpty
	@EqualsAndHashCode.Include
	private final String columnValue;

	/**
	 * Value displayed in Select list. Usually concise display.
	 */
	@NotEmpty
	@EqualsAndHashCode.Include
	private final String value;

	/**
	 * More detailed value. Displayed when value is selected.
	 */
	@NotEmpty
	@EqualsAndHashCode.Include
	private final String optionValue;

	@EqualsAndHashCode.Include
	private int minSuffixLength = 3;
	@EqualsAndHashCode.Include
	private boolean generateSuffixes = true;

	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@ToString.Exclude
	private IndexService indexService;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@ToString.Exclude
	private ConqueryConfig config;

	/**
	 * Does not make sense to distinguish at Filter level since it's only referenced when a template is also set.
	 */
	@Override
	@JsonIgnore
	public boolean isSearchDisabled() {
		return false;
	}

	@JsonIgnore
	public URI getResolvedUri() {
		return FileUtil.getResolvedUri(config.getIndex().getBaseUrl(), filePath);
	}

	@JsonIgnore
	@Override
	public String getSearchHandle() {
		return "filter_template_%s".formatted(getId());
	}
}

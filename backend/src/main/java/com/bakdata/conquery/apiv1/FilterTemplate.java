package com.bakdata.conquery.apiv1;

import java.net.URI;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.index.FrontendValueIndex;
import com.bakdata.conquery.models.index.FrontendValueIndexKey;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.util.io.FileUtil;
import com.bakdata.conquery.util.search.TrieSearch;
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
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"columns"})
@ToString
@Slf4j
@CPSType(id = "CSV_TEMPLATE", base = SearchIndex.class)
public class FilterTemplate extends SearchIndex implements Searchable {

	/**
	 * Path to CSV File.
	 */
	@NotNull
	private final URI filePath;

	/**
	 * Value to be sent for filtering.
	 */
	@NotEmpty
	private final String columnValue;
	/**
	 * Value displayed in Select list. Usually concise display.
	 */
	@NotEmpty
	private final String value;
	/**
	 * More detailed value. Displayed when value is selected.
	 */
	@NotEmpty
	private final String optionValue;

	private int minSuffixLength = 3;
	private boolean generateSuffixes = true;

	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private IndexService indexService;

	/**
	 * Does not make sense to distinguish at Filter level since it's only referenced when a template is also set.
	 */
	@Override
	@JsonIgnore
	public boolean isSearchDisabled() {
		return false;
	}

	public TrieSearch<FrontendValue> createTrieSearch(IndexConfig config) throws IndexCreationException {

		final URI resolvedURI = FileUtil.getResolvedUri(config.getBaseUrl(), getFilePath());
		log.trace("Resolved filter template reference url for search '{}': {}", getId(), resolvedURI);

		final FrontendValueIndex search = indexService.getIndex(new FrontendValueIndexKey(
				resolvedURI,
				columnValue,
				value,
				optionValue,
				isGenerateSuffixes() ? getMinSuffixLength() : Integer.MAX_VALUE,
				config.getSearchSplitChars()
		));

		return search.getDelegate();
	}


}

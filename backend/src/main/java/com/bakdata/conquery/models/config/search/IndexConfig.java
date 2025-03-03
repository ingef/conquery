package com.bakdata.conquery.models.config.search;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.index.FrontendValueIndex;
import com.bakdata.conquery.models.index.FrontendValueIndexKey;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.models.index.IndexKey;
import com.bakdata.conquery.models.query.InternalFilterSearch;
import com.bakdata.conquery.util.io.FileUtil;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.internal.TrieSearch;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@CPSType(id = "INTERNAL", base = SearchConfig.class)
public class IndexConfig implements SearchConfig {

	/**
	 * Base url under which reference files are access if the url in
	 * {@link FilterTemplate} only have a path element (no schema or authority).
	 * Pay attentions that the base url end with a slash <code>/</code>.
	 *
	 * @implNote See {@link IndexKey#getCsv()} for URI<->URL details.
	 */
	@Nullable
	private URI baseUrl;
	@JsonAlias("searchSuffixLength")
	@Min(0)
	private int ngramLength = 3;
	@Nullable
	private String searchSplitChars = "(),;.:\"'/";

	@NotNull
	private String emptyLabel = "No Value";

	@JsonIgnore
	@ValidationMethod(message = "Specified baseUrl is not valid")
	public boolean isValidUrl() {
		if (baseUrl == null) {
			// It is okay if no base is specified. Every template needs to specify it's full path then.
			return true;
		}
		try {
			// We just try to convert it to an url and discard the return value
			URL _ignore = baseUrl.toURL();
			return true;
		}
		catch (MalformedURLException e) {
			log.error("URL validation error.", e);
			return false;
		}

	}

	@Override
	public <K extends Comparable<K>> Search<K> createSearch(Searchable<K> searchable) {
		if (searchable instanceof FilterTemplate temp) {

			final URI resolvedURI = FileUtil.getResolvedUri(getBaseUrl(), temp.getFilePath());
			log.trace("Resolved filter template reference url for search '{}': {}", temp.getId(), resolvedURI);

			final FrontendValueIndex search;
			try {
				search = temp.getIndexService().getIndex(new FrontendValueIndexKey(
						resolvedURI,
						temp.getColumnValue(),
						temp.getValue(),
						temp.getOptionValue(),
						temp.isGenerateSuffixes() ? temp.getMinSuffixLength() : Integer.MAX_VALUE,
						getSearchSplitChars()
				));
			}
			catch (IndexCreationException e) {
				throw new RuntimeException(e);
			}

			return (Search<K>) search.getDelegate();
		}

		return new TrieSearch<>(searchable.isGenerateSuffixes() ? getNgramLength() : Integer.MAX_VALUE, getSearchSplitChars());

	}

	@Override
	public SearchProcessor createSearchProcessor() {
		return new InternalFilterSearch(this);
	}
}

package com.bakdata.conquery.apiv1;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.util.search.QuickSearch;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor
public class FilterSearch {

	/**
	 * Enum to specify a scorer function in {@link QuickSearch}. Used for resolvers in {@link AbstractSelectFilter}.
	 */
	@AllArgsConstructor
	@Getter
	public enum FilterSearchType {
		/**
		 * String must start with search-String.
		 */
		PREFIX {
			@Override
			public double score(String candidate, String keyword) {
				/* Sort ascending by length of match */
				if (keyword.startsWith(candidate)) {
					return 1d / candidate.length();
				}
				return -1d;
			}
		},
		/**
		 * Search is contained somewhere in string.
		 */
		CONTAINS {
			@Override
			public double score(String candidate, String keyword) {
				/* 0...1 depending on the length ratio */
				double matchScore = (double) candidate.length() / (double) keyword.length();

				/* boost by 1 if matches start of keyword */
				if (keyword.startsWith(candidate)) {
					return matchScore + 1.0;
				}

				return matchScore;
			}
		},
		/**
		 * Values must be exactly the same.
		 */
		EXACT {
			@Override
			public double score(String candidate, String keyword) {
				/* Only allow exact matches through (returning < 0.0 means skip this candidate) */
				return candidate.equals(keyword) ? 1.0 : -1.0;
			}
		};

		/**
		 * Search function. See {@link QuickSearch}.
		 *
		 * @param candidate potential match.
		 * @param match     search String.
		 * @return Value > 0 increases relevance of string (also used for ordering results). < 0 removes string.
		 */
		public abstract double score(String candidate, String match);
	}

	private final Map<String, QuickSearch<FilterSearchItem>> searchCache = new HashMap<>();

	public QuickSearch<FilterSearchItem> getSearchFor(String reference) {
		return searchCache.computeIfAbsent(reference, (ignored) -> new QuickSearch<>());
	}

	public boolean hasSearchFor(String reference) {
		return searchCache.containsKey(reference);
	}

	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch(NamespacedStorage storage, JobManager jobManager, CSVConfig parser) {
		searchCache.clear();

		storage.getAllConcepts().stream()
			   .flatMap(c -> c.getConnectors().stream())
			   .flatMap(co -> co.collectAllFilters().stream())
			   .filter(f -> f instanceof AbstractSelectFilter)
			   .map(AbstractSelectFilter.class::cast)
			   .map(f -> new SimpleJob(String.format("SourceSearch[%s]", f.getId()), () -> f.initializeSourceSearch(parser, storage, this)))
			   .forEach(jobManager::addSlowJob);
	}
}

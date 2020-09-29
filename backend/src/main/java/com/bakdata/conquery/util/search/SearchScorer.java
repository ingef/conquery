package com.bakdata.conquery.util.search;

@FunctionalInterface
public interface SearchScorer {
	public double score(String keywordMatch, String keyword);
}

/*
 *                                     //
 * Copyright 2016 Karlis Zigurs (http://zigurs.com)
 *                                   //
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bakdata.conquery.util.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.util.search.graph.QSGraph;
import com.bakdata.conquery.util.search.model.QuickSearchStats;
import com.bakdata.conquery.util.search.model.Result;
import com.bakdata.conquery.util.search.model.ResultItem;
import com.zigurs.karlis.utils.sort.MagicSort;


/**
 * Simple and lightweight in-memory com.bakdata.conquery.util.search library.
 * <p>
 * Fit for speedy querying of small to medium sized data sets (up to 10-20GB of JVM heap) for
 * (as an example) providing real-time incremental com.bakdata.conquery.util.search to users as they type the com.bakdata.conquery.util.search string
 * or fairly efficient faceted filtering of fairly large collections.
 * <p>
 * Internally it works by breaking down supplied item keywords into all possible fragments
 * (e.g. four into f,fo,fou,o,ou,our,ur,r...) and building an internal lookup graph where a fragment
 * contains links to all longer fragments it is part of and items associated with them
 * (e.g. fo &raquo; fou &raquo; four &raquo; Number(4)).
 * <p>
 * <small><em>(Technically it's incremental merge of unique leafs across a set of arbitrary entry points
 * in an acyclic multi-root digraph, but that's a bunch of complex words I'm not sure of correct use
 * of in half the cases anyway. However if you are a github stalking recruiter or want to give me some fungible
 * currency in exchange of overcomplicated solutions to trivial problems - try to hire me anyway!)</em></small>
 * <p>
 * Further documentation and benchmarks can be found at https://github.com/karliszigurs/QuickSearch.
 * <p>
 * Example uses include:
 * <ul>
 * <li>Searching a contacts list</li>
 * <li>Searching for a city based on landmark keywords (e.g. "tower bridge")</li>
 * <li>Searching for a category or item in an online shop based on keywords</li>
 * <li>Implement a sprawling multi-facet filter over large data sets (e.g. all your cloud instances)</li>
 * <li>Use in background to highlight UI elements corresponding to query (a-la OSX preferences com.bakdata.conquery.util.search)</li>
 * <li><em>and so on...</em></li>
 * </ul>
 * <p>
 * Each entry is associated with a set of keywords the com.bakdata.conquery.util.search will be performed against, therefore it is possible
 * to add aliases and facet descriptions. Can also include alternate spellings and so on.
 * <pre>{@code
 *  // Quick Start Example
 *  QuickSearch<String> qs = new QuickSearch<>(); // create instance
 *
 *  qs.addItem("Cat", "cat domestic mammal"); // add a few items
 *  qs.addItem("Dog", "dog domestic mammal");
 *  qs.addItem("Lobster", "lobster crustacean");
 *  qs.addItem("Shrimp", "shrimp crustacean");
 *  qs.addItem("Pigeon", "pigeon urban menace");
 *
 *  List<String> mammals = qs.findItems("mammal", 10); // find top 10 items matching mammal
 *  Optional<String> shrimp = qs.findItem("shrimp"); // find a top result for shrimp, if such exists
 * }</pre>
 * <p>
 * A few use examples are available in {@code QuickSearchUseCasesTest.java} class under tests.
 * <p>
 * A number of configuration options are available and documented via {@link QuickSearchBuilder}.
 * <p>
 * This class is thread safe. You'll get a cookie if you manage to break it. <small>I don't expect to part with any
 * cookies.</small>
 *
 * @author Karlis Zigurs, 2016
 */
public class QuickSearch<T extends Comparable<T>> {

    /**
     * Matching policy to apply to unmatched com.bakdata.conquery.util.search keywords.
     * <p>
     * In case of {@link UnmatchedPolicy#BACKTRACKING} a keyword that is not matched
     * will be incrementally truncated (e.g. 'banana' &raquo; 'banan' &raquo; 'bana' &raquo; 'ban')
     * until a matching fragment is found (in example 'banana' will be incrementally truncated to 'ban'
     * which will happen to match if 'band' had been previously added to index).
     * <p>
     * In case of {@link UnmatchedPolicy#IGNORE} any keywords not matched directly will be considered
     * empty. Note that if used in conjunction with {@link MergePolicy#INTERSECTION} this means
     * that whole result set will be automatically empty.
     */
    public enum UnmatchedPolicy {
        IGNORE, BACKTRACKING
    }

    /**
     * Select results set accumulation policy if query contains multiple keywords.
     * <p>
     * {@link MergePolicy#UNION} merges result sets for each keyword summing
     * individual scores as supplied by keywords scorer function with the final result
     * containing all items encountered during com.bakdata.conquery.util.search.
     * <p>
     * {@link MergePolicy#INTERSECTION} merges result sets retaining only items that
     * are present in results from each of the keywords.
     * <p>
     * As a note, this means that if any single keyword results in no found elements during
     * com.bakdata.conquery.util.search while using {@link MergePolicy#INTERSECTION} the final results will be empty.
     */
    public enum MergePolicy {
        UNION, INTERSECTION
    }

    /**
     * Default keywords extractor function that splits the user supplied item keywords or
     * com.bakdata.conquery.util.search string by word character and white space boundaries.
     * <p>
     * As an example both "one two,three-four" and "one$two%three^four" input strings will
     * produce a set of 4 extracted keywords to use during com.bakdata.conquery.util.search - [one,two,three,four].
     * <p>
     * Further details at {@link QuickSearchBuilder#withKeywordsExtractor(Function)}.
     */
    private static final Function<String, Set<String>> DEFAULT_KEYWORDS_EXTRACTOR =
            s -> Arrays.stream(s.replaceAll("[^\\w]+", " ").split("[\\s]+")).collect(Collectors.toSet());

    /**
     * Default keywords 'normalizer' function that ensures that all input keywords are in consistent format.
     * <p>
     * Further details at {@link QuickSearchBuilder#withKeywordNormalizer(Function)}.
     */
    private static final Function<String, String> DEFAULT_KEYWORD_NORMALIZER = s -> s.trim().toLowerCase();

    /**
     * Default keyword matches scoring function assigning a match a score between 0 and 1
     * and boosting score if match is encountered at the start of the keyword.
     * <p>
     * Provided for general use.
     * <p>
     * Further details at {@link QuickSearchBuilder#withKeywordMatchScorer(SearchScorer)}.
     */
    public static final SearchScorer DEFAULT_MATCH_SCORER = (keywordMatch, keyword) -> {
        /* 0...1 depending on the length ratio */
        double matchScore = (double) keywordMatch.length() / (double) keyword.length();

        /* boost by 1 if matches start of keyword */
        if (keyword.startsWith(keywordMatch))
            matchScore += 1.0;

        return matchScore;
    };

    /**
     * Alternative keyword matches scoring function filtering only exact matches to result set.
     * <p>
     * Provided for use cases where exact match between supplied com.bakdata.conquery.util.search string and keyword is required
     * (e.g. faceted filtering).
     * <p>
     * Further details at {@link QuickSearchBuilder#withKeywordMatchScorer(SearchScorer)}.
     */
    public static final SearchScorer EXACT_MATCH_SCORER = (candidate, keyword) -> {
        /* Only allow exact matches through (returning < 0.0 means skip this candidate) */
        return candidate.length() == keyword.length() ? 1.0 : -1.0;
    };

    /*
     * Configuration properties
     */

    private final BinaryOperator<Map<T, Double>> mergeFunction;
    private final UnmatchedPolicy unmatchedPolicy;

    private final SearchScorer keywordMatchScorer;
    private final Function<String, String> keywordNormalizer;
    private final Function<String, Set<String>> keywordsExtractor;

    private final boolean enableParallel;
    private final boolean enableKeywordsInterning;

    /*
     * Actual data is stored in {@link QSGraph} instance.
     */

    private final QSGraph<T> graph;

    /**
     * Create a QuickSearch instance with default configuration parameters.
     * <p>
     * This is suited for general use case of matching free-form user
     * input against all possible matches.
     */
    public QuickSearch() {
        this(new QuickSearchBuilder()); // with defaults from builder
    }

    private QuickSearch(final QuickSearchBuilder builder) {
        keywordsExtractor = builder.keywordsExtractor;
        keywordNormalizer = builder.keywordNormalizer;
        keywordMatchScorer = builder.keywordMatchScorer;
        unmatchedPolicy = builder.unmatchedPolicy;
        enableParallel = builder.enableForkJoin;
        enableKeywordsInterning = builder.enableKeywordsInterning;

        if (builder.mergePolicy == MergePolicy.UNION) {
            mergeFunction = QuickSearch::unionMaps;
        } else {
            mergeFunction = QuickSearch::intersectMaps;
        }

        graph = new QSGraph<>();
    }

    /*
     * Public interface
     */

    /**
     * Add an item with corresponding keywords string, e.g. an online store item Shoe with
     * keywords string "Shoe Red 10 Converse cheap free".
     * <p>
     * Can also be used to expand the keywords of item previously added. If this method is
     * invoked with an item that is already part of the index any keywords not previously known
     * will be added to mappings for this item (in addition to existing ones).
     *
     * @param item     Item to return in com.bakdata.conquery.util.search results
     * @param keywords keywords string. See {@link QuickSearchBuilder#withKeywordsExtractor(Function)}
     *
     * @return true if the item was added, false if validations before adding failed
     */
    public boolean addItem(final T item, final String keywords) {
        if (item == null || keywords == null || keywords.isEmpty())
            return false;

        ImmutableSet<String> keywordsSet = prepareKeywords(keywords, enableKeywordsInterning);

        if (keywordsSet.isEmpty())
            return false;

        addItemImpl(item, keywordsSet);
        return true;
    }

    /**
     * Remove an item, if it exists. Calling this method ensures that
     * specified item and any keywords it was associated with is gone.
     * <p>
     * No effect or side-effects if supplied item is not known.
     *
     * @param item Item to remove
     */
    public void removeItem(final T item) {
        if (item == null)
            return;

        removeItemImpl(item);
    }

	/**
	 * Retrieve (find) top matching item for specified com.bakdata.conquery.util.search string.
	 * <p>
	 * Note that the call to this method is not guaranteed to return the same item
	 * if multiple items in the instance have identical score for the supplied com.bakdata.conquery.util.search
	 * string and the instance is modified between invocations (adding or removing
	 * seemingly unrelated items) or com.bakdata.conquery.util.search instance is configured to use parallel
	 * processing.
	 *
	 * @param searchString raw com.bakdata.conquery.util.search string
	 *
	 * @return {@link Optional} wrapping (or not) the top scoring item found in instance
	 */
	public Optional<T> findItem(final String searchString) {
		return findItem(searchString, keywordMatchScorer);
	}

	/**
     * Retrieve (find) top matching item for specified com.bakdata.conquery.util.search string.
     * <p>
     * Note that the call to this method is not guaranteed to return the same item
     * if multiple items in the instance have identical score for the supplied com.bakdata.conquery.util.search
     * string and the instance is modified between invocations (adding or removing
     * seemingly unrelated items) or com.bakdata.conquery.util.search instance is configured to use parallel
     * processing.
     *
     * @param searchString raw com.bakdata.conquery.util.search string
     *
     * @param keywordMatchScorer separate scoring function to be used.
	 * @return {@link Optional} wrapping (or not) the top scoring item found in instance
     */
    public Optional<T> findItem(final String searchString, SearchScorer keywordMatchScorer) {
        return findItems(searchString,1, keywordMatchScorer).stream().findFirst();
    }

	/**
	 * Retrieve (find) top n items matching the supplied com.bakdata.conquery.util.search string.
	 *
	 * @param searchString     raw com.bakdata.conquery.util.search string, e.g. "new york pizza"
	 * @param numberOfTopItems number of items the returned result should be limited to (1 to Integer.MAX_VALUE)
	 *
	 * @return list (possibly empty) containing up to n top com.bakdata.conquery.util.search results
	 */
	public List<T> findItems(final String searchString, final int numberOfTopItems) {
		return findItems(searchString, numberOfTopItems, keywordMatchScorer);
	}

	public List<T> listItems() {
		return graph.listItems();
	}

	/**
     * Retrieve (find) top n items matching the supplied com.bakdata.conquery.util.search string.
     *
     * @param searchString     raw com.bakdata.conquery.util.search string, e.g. "new york pizza"
     * @param numberOfTopItems number of items the returned result should be limited to (1 to Integer.MAX_VALUE)
     *
     * @param keywordMatchScorer separate scoring function to be used.
	 * @return list (possibly empty) containing up to n top com.bakdata.conquery.util.search results
     */
    public List<T> findItems(final String searchString, final int numberOfTopItems, SearchScorer keywordMatchScorer) {
        if (isInvalidRequest(searchString, numberOfTopItems))
            return Collections.emptyList();

        ImmutableSet<String> searchKeywords = prepareKeywords(searchString);

        if (searchKeywords.isEmpty())
            return Collections.emptyList();

        List<SearchResult<T>> results = doSearch(searchKeywords, numberOfTopItems, keywordMatchScorer);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }
		return results.stream()
		        .map(SearchResult::unwrap)
		        .collect(Collectors.toList());
    }

	/**
	 * Retrieve (find) top matching item for the supplied com.bakdata.conquery.util.search string and return it wrapped in the
	 * {@link ResultItem} response object containing known keywords and assigned com.bakdata.conquery.util.search score.
	 *
	 * @param searchString raw com.bakdata.conquery.util.search string
	 *
	 * @return {@link Optional} wrapping (or not) the top scoring item, keywords and score found in instance
	 */
	public Optional<ResultItem<T>> findItemWithDetail(final String searchString) {
		return findItemWithDetail(searchString, keywordMatchScorer);
	}

	/**
     * Retrieve (find) top matching item for the supplied com.bakdata.conquery.util.search string and return it wrapped in the
     * {@link ResultItem} response object containing known keywords and assigned com.bakdata.conquery.util.search score.
     *
     * @param searchString raw com.bakdata.conquery.util.search string
     *
     * @param keywordMatchScorer separate scoring function to be used.
	 * @return {@link Optional} wrapping (or not) the top scoring item, keywords and score found in instance
     */
    public Optional<ResultItem<T>> findItemWithDetail(final String searchString, SearchScorer keywordMatchScorer) {
        return findItemsWithDetail(searchString, 1, keywordMatchScorer).getResponseResultItems().stream().findFirst();
    }

	/**
	 * Retrieve (find) an augmented com.bakdata.conquery.util.search result containing the com.bakdata.conquery.util.search string and
	 * wrapped found items with their scores and keywords.
	 *
	 * @param searchString     raw com.bakdata.conquery.util.search string, e.g. "new york pizza"
	 * @param numberOfTopItems number of items the returned result should be limited to (1 to Integer.MAX_VALUE)
	 *
	 * @return wrapper containing zero to n top scoring items and com.bakdata.conquery.util.search string
	 */
	public Result<T> findItemsWithDetail(final String searchString, final int numberOfTopItems) {
		return findItemsWithDetail(searchString, numberOfTopItems, keywordMatchScorer);
	}

	/**
     * Retrieve (find) an augmented com.bakdata.conquery.util.search result containing the com.bakdata.conquery.util.search string and
     * wrapped found items with their scores and keywords.
     *
     * @param searchString     raw com.bakdata.conquery.util.search string, e.g. "new york pizza"
     * @param numberOfTopItems number of items the returned result should be limited to (1 to Integer.MAX_VALUE)
     *
     * @param keywordMatchScorer separate scoring function to be used.
	 * @return wrapper containing zero to n top scoring items and com.bakdata.conquery.util.search string
     */
    public Result<T> findItemsWithDetail(final String searchString, final int numberOfTopItems, SearchScorer keywordMatchScorer) {
        if (isInvalidRequest(searchString, numberOfTopItems))
            return new Result<>(searchString, Collections.emptyList(), numberOfTopItems);

        ImmutableSet<String> searchKeywords = prepareKeywords(searchString);

        if (searchKeywords.isEmpty())
            return new Result<>(searchString, Collections.emptyList(), numberOfTopItems);

        List<SearchResult<T>> results = doSearch(searchKeywords, numberOfTopItems, keywordMatchScorer);

        if (results.isEmpty()) {
            return new Result<>(searchString, Collections.emptyList(), numberOfTopItems);
        }
		return new Result<>(
		        searchString,
		        results.stream().map(i -> new ResultItem<>(
		                i.unwrap(),
		                graph.getItemKeywords(i.unwrap()),
		                i.getScore())
		        ).collect(Collectors.toList()),
		        numberOfTopItems
		);
    }

    /**
     * Clear the com.bakdata.conquery.util.search index.
     */
    public void clear() {
        graph.clear();
    }

    /**
     * Returns an overview of contained graph size. To be fair this is not
     * really useful as anything beyond as quick sanity check.
     *
     * @return stats listing number of items and keyword fragments known
     */
    public QuickSearchStats getStats() {
        return graph.getStats();
    }

    /*
     * Implementation methods
     */

    private boolean isInvalidRequest(final String searchString, final int numItems) {
        return searchString == null || searchString.isEmpty() || numItems < 1;
    }

    private List<SearchResult<T>> doSearch(final ImmutableSet<String> searchKeywords,
										   final int maxItemsToList, final SearchScorer keywordMatchScorer) {

        final Map<T, Double> results = StreamSupport.stream(searchKeywords.spliterator(), enableParallel)
                .map(keyword -> walkGraphAndScore(keyword, keywordMatchScorer))
                .reduce(mergeFunction)
                .orElseGet(HashMap::new);

        return MagicSort.sortAndLimit(
                results.entrySet(),
                maxItemsToList,
                (e1, e2) -> e2.getValue().compareTo(e1.getValue())
        ).stream()
                .map(e -> new SearchResult<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /*
     * Interfacing with the graph
     */

    private void addItemImpl(final T item, final Set<String> suppliedKeywords) {
        graph.registerItem(item, suppliedKeywords);
    }

    private void removeItemImpl(final T item) {
        graph.unregisterItem(item);
    }

    private Map<T, Double> walkGraphAndScore(final String keyword, SearchScorer keywordMatchScorer) {
        Map<T, Double> result = graph.walkAndScore(keyword, keywordMatchScorer);

        /* Check if we need to back off */
        if (unmatchedPolicy == UnmatchedPolicy.BACKTRACKING
                && result.isEmpty()
                && keyword.length() > 1)
            return walkGraphAndScore(keyword.substring(0, keyword.length() - 1), keywordMatchScorer);

        return result;
    }

    private ImmutableSet<String> prepareKeywords(final String keywordsString) {
        return prepareKeywords(keywordsString, false);
    }

    private ImmutableSet<String> prepareKeywords(final String keywordsString,
                                                 final boolean internKeywords) {
        return ImmutableSet.fromCollection(
                keywordsExtractor.apply(keywordsString).stream()
                        .filter(Objects::nonNull)        /* Guarantee a non-null, */
                        .map(String::trim)               /* trimmed, */
                        .filter(s -> !s.isEmpty())       /* and non-empty string */
                        .map(keywordNormalizer)          /* to normalizer. */
                        .filter(Objects::nonNull)        /* And the same to final keywords set. */
                        .map(String::trim)               /* Just can't trust any user supplied functions these days... */
                        .filter(s -> !s.isEmpty())       /* I wonder what changed. Why it came to be so? Was it us? */
                        .map(s -> internKeywords ? s.intern() : s) /* do magic */
                        .collect(Collectors.toSet())     /* All keywords now distinct. */
        );
    }

    /*
     * Helpers
     */

    /**
     * Returns intersection of two provided {@link Map}{@code <T, Double} maps
     * (or empty map if no keys overlap) summing the values.
     * <p>
     * For performance reasons this function <em>modifies the maps supplied</em> and
     * possibly returns an instance of one of the supplied (by then modified) maps.
     *
     * @param left  map to intersect
     * @param right map to intersect
     * @param <T>   type of keys
     *
     * @return intersection with values summed
     */
    private static <T> Map<T, Double> intersectMaps(final Map<T, Double> left,
                                                   final Map<T, Double> right) {
        Map<T, Double> smaller = left.size() < right.size() ? left : right;
        Map<T, Double> bigger = smaller == left ? right : left;

        smaller.keySet().retainAll(bigger.keySet());
        smaller.entrySet().forEach(e -> e.setValue(e.getValue() + bigger.get(e.getKey())));
        return smaller;
    }

    /**
     * For performance reasons this function <em>modifies the maps supplied</em> and
     * possibly returns an instance of one of the supplied (by then modified) maps.
     *
     * @param left  map to intersect
     * @param right map to intersect
     * @param <T>   type of keys
     *
     * @return intersection with values summed
     */
    private static <T> Map<T, Double> unionMaps(final Map<T, Double> left,
                                                final Map<T, Double> right) {
        final Map<T, Double> smaller = (left.size() < right.size()) ? left : right;
        final Map<T, Double> bigger = (smaller == left) ? right : left;

        smaller.forEach((k, v) -> bigger.merge(k, v, Double::sum));
        return bigger;
    }

    /*
     * Builder.
     */

    /**
     * Shortcut to access a new {@link QuickSearchBuilder} instance
     * and start configuring a new {@link QuickSearch} instance.
     *
     * @return new {@link QuickSearchBuilder} instance
     */
    public static QuickSearchBuilder builder() {
        return new QuickSearchBuilder();
    }

    /**
     * {@link QuickSearch} configuration builder class.
     * <p>
     * Access and use: {@code QuickSearch.builder().build();}
     */
    public static class QuickSearchBuilder {

        /*
         * Defaults
         */
        private SearchScorer keywordMatchScorer = DEFAULT_MATCH_SCORER;
        private Function<String, String> keywordNormalizer = DEFAULT_KEYWORD_NORMALIZER;
        private Function<String, Set<String>> keywordsExtractor = DEFAULT_KEYWORDS_EXTRACTOR;
        private UnmatchedPolicy unmatchedPolicy = UnmatchedPolicy.BACKTRACKING;
        private MergePolicy mergePolicy = MergePolicy.UNION;
        private boolean enableForkJoin = false;
        private boolean enableKeywordsInterning = false;

        /**
         * Specify a keywords match scorer function.
         * <p>
         * This function is invoked for each visited graph node that has leafs (items) that will be added
         * to generated result set (for a single keyword - the scores from all encounters of a particular
         * item are summed during merge phase). In example it may be invoked to score a match
         * between "ban" and "banana" or "banana" and "banana".
         * <p>
         * The first parameter is the user supplied keyword, second is the existing keyword of the node being visited.
         * <p>
         * {@link QuickSearch} guarantees that the user supplied keyword will always be a substring (or a match
         * for) of the node keyword.
         * <p>
         * Function should return a Double assigning arbitrary score as required.
         * <p>
         * Function can return negative value to skip adding the items for this particular match.
         * <p>
         * Two default functions are available: {@link #DEFAULT_MATCH_SCORER} for user facing
         * implementations assigning a match score between 0 and 1 depending on the match length
         * and {@link #EXACT_MATCH_SCORER} for use cases where exact matching is desired.
         * <p>
         * This should be a true <strong>function in a mathematical sense</strong> as it will be called
         * frequently, from multiple threads and in undefined order during the com.bakdata.conquery.util.search graph traversal phase.
         * If you try to inject IO blocking, database lookups, remote service requests or functionality
         * like calculation of running averages you are going to have a bad time and things will break badly.
         * <p>
         * <small>If you have a use case where such functionality would actually
         * make sense I'd be very interested to hear about it.</small>
         *
         * @param scorerFunction function to use for keywords match scoring
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withKeywordMatchScorer(SearchScorer scorerFunction) {
            Objects.requireNonNull(scorerFunction);

            keywordMatchScorer = scorerFunction;
            return this;
        }

        /**
         * Specify a keywords 'extractor' function the created {@link QuickSearch} instance should apply to
         * user (and item) supplied keyword strings.
         * <p>
         * Function should accept a raw input string as an input and return a {@link Set} of extracted keywords
         * that should be assigned to the item (when called from {@link #addItem(Comparable, String)} or should
         * be treated as user supplied keywords.
         * <p>
         * In default and most basic implementation (available as {@link #DEFAULT_KEYWORDS_EXTRACTOR} the
         * function can simply extract all alpha-numerical sequences and ensure they are consistently lower
         * or upper case (which should be sufficient for almost all possible use cases), but special cases
         * may require overriding it (e.g. if directly parsing machine generated formats).
         * <p>
         * Note that the keywords extracted by this function will be additionally processed by keywords
         * normaliser function - {@link #withKeywordNormalizer(Function)}.
         * <p>
         * This should be a true <strong>function in a mathematical sense</strong> as it will be called
         * frequently, from multiple threads and in undefined order when adding items or initiating
         * searches. If you try to inject IO blocking, database lookups, remote service requests or functionality
         * like calculation of running averages you are going to have a bad time and things will break badly.
         * <p>
         * <small>If you have a use case where such functionality would actually
         * make sense I'd be very interested to hear about it.</small>
         *
         * @param extractorFunction function to extract keywords from raw input strings
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withKeywordsExtractor(Function<String, Set<String>> extractorFunction) {
            Objects.requireNonNull(extractorFunction);

            keywordsExtractor = extractorFunction;
            return this;
        }

        /**
         * Specify a keywords normalizer function.
         * <p>
         * This function should ensure that all semantically
         * equivalent strings (e.g. "London" and "lonDon") have consistent internal representation
         * both for item keywords and keywords used in com.bakdata.conquery.util.search.
         * <p>
         * Applied to keywords extracted by keywords extractor function for both supplied item keywords
         * and user supplied raw com.bakdata.conquery.util.search string. Can be extended to (in example) replace special
         * characters with common romanizations, skip or introduce common prefixes/suffixes, etc.
         * <p>
         * The normalized representation has no specific requirements.  Simply returning the supplied
         * string will mean that the com.bakdata.conquery.util.search results contain only exact (and case sensitive) matches.
         * It is also possible to return empty or null strings here, in which case the original
         * keyword will be ignored.
         * <p>
         * Example transformations:
         * <table summary="">
         * <tr><th>original</th><th>transformed to</th><th>possible rationale</th></tr>
         * <tr><td><code>"New York"</code></td><td><code>"new york"</code></td><td>all lower case internally</td></tr>
         * <tr><td><code>"Pythøn"</code></td><td><code>"python"</code></td><td>replace special characters</td></tr>
         * <tr><td><code>"HERMSGERVØRDENBRØTBØRDA"</code></td><td><code>"hermsgervordenbrotborda"</code></td><td>møøse
         * trained by...</td></tr>
         * <tr><td><code>"Россия"</code></td><td><code>"rossiya"</code></td><td>transliterate cyrilic alphabet to
         * latin</td></tr>
         * </table>
         * <p>
         * Default implementation available at {@link #DEFAULT_KEYWORD_NORMALIZER} simply ensures that the keyword
         * is trimmed and all lower-case.
         * <p>
         * This should be a true <strong>function in a mathematical sense</strong> as it will be called
         * frequently, from multiple threads and in undefined order when adding items or initiating
         * searches. If you try to inject IO blocking, database lookups, remote service requests or functionality
         * like calculation of running averages you are going to have a bad time and things will break badly.
         * <p>
         * <small>If you have a use case where such functionality would actually
         * make sense I'd be very interested to hear about it.</small>
         *
         * @param normalizerFunction function as described above
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withKeywordNormalizer(Function<String, String> normalizerFunction) {
            Objects.requireNonNull(normalizerFunction);

            keywordNormalizer = normalizerFunction;
            return this;
        }

        /**
         * Set {@link UnmatchedPolicy} for created {@link QuickSearch} instance.
         *
         * @param unmatchedPolicy policy to use
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withUnmatchedPolicy(UnmatchedPolicy unmatchedPolicy) {
            Objects.requireNonNull(unmatchedPolicy);

            this.unmatchedPolicy = unmatchedPolicy;
            return this;
        }

        /**
         * Set {@link MergePolicy} for created {@link QuickSearch} instance.
         *
         * @param mergePolicy policy to use
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withMergePolicy(MergePolicy mergePolicy) {
            Objects.requireNonNull(mergePolicy);

            this.mergePolicy = mergePolicy;
            return this;
        }

        /**
         * Enable parallel (fork-join) processing in the created {@link QuickSearch} instance.
         * <p>
         * Provides noticeable processing speedups in case if working with lots of com.bakdata.conquery.util.search
         * keywords (faceting) across large data sets.
         * <p>
         * Only enable if used in m2m use cases and you can benchmark it proving that it
         * improves your use case. This can be considered an experimental feature and may
         * be removed in the future (ether made default or removed as not worth the
         * maintenance complexity).
         *
         * @param enable enable parallel processing
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withParallelProcessing(boolean enable) {
            enableForkJoin = enable;
            return this;
        }

        /**
         * See {@link #withParallelProcessing(boolean)}.
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withParallelProcessing() {
            return withParallelProcessing(true);
        }

        /**
         * Enable String interning ({@link String#intern()}) of stored internal index keywords.
         * <p>
         * Can drastically reduce memory footprint of the com.bakdata.conquery.util.search index, but requires explicit
         * JVM tuning for large data sets to avoid equally drastic loss of performance on adding items.
         * <p>
         * Should be generally left alone unless you have a strong opinion about fiddling
         * with undocumented JVM options, jdk7u40 changes, hash collisions, most interesting
         * 5 digit prime numbers (you'll need a few of those for tuning anyway) and
         * noisy neighbours on AWS and GCE.
         *
         * @param enable enable keywords interning
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withKeywordsInterning(boolean enable) {
            enableKeywordsInterning = enable;
            return this;
        }

        /**
         * See {@link #withKeywordsInterning(boolean)}.
         *
         * @return current {@link QuickSearchBuilder} instance for configuration chaining
         */
        public QuickSearchBuilder withKeywordsInterning() {
            return withKeywordsInterning(true);
        }

        /**
         * One shiny {@link QuickSearch} instance with specified configuration parameters coming up.
         *
         * @param <T> required instance type
         *
         * @return new {@link QuickSearch} instance with this {@link QuickSearchBuilder}s configuration
         */
        public <T extends Comparable<T>> QuickSearch<T> build() {
            return new QuickSearch<>(this);
        }
    }

    /**
     * Internal wrapper of item and score for results list.
     */
    private static final class SearchResult<T> {

        private final T item;
        private final double score;

        private SearchResult(final T item, final double score) {
            this.item = item;
            this.score = score;
        }

        private T unwrap() {
            return item;
        }

        private Double getScore() {
            return score;
        }
    }
}

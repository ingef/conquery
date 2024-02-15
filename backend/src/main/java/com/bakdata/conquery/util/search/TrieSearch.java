package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;

/**
 * Trie based keyword search for autocompletion and resolving.
 * <p>
 * We store whole words and their ngrams to enable a sort of fuzzy and partial search with longer and compound search terms.
 * <p>
 * The output result should be in the following order:
 * 1) Original words (in order of difference)
 * 2) ngrams (in order of difference)
 * <p>
 *
 * @implNote If ngramLength is Integer.MAX_VALUE, the ngram trie is empty and TrieSearch::findItems only performs a prefix search using the query keywords.
 * If TrieSearch is instantiated with ngramLength = 0, ngramLength is set to Integer.MAX_VALUE.
 */
@Slf4j
@ToString(of = {"ngramLength", "splitPattern"})
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * Weight of a search hit for query and keyword of different lengths.
	 * Should be greater than one because the weights of hits in the whole worlds trie are squared.
	 */
	private static final long BASE_WEIGHT = 2;

	/**
	 * Weight of a search hit for query and keyword of the same length.
	 * Should be greater than one because the weights of hits in the whole worlds trie are squared.
	 */
	private static final long EXACT_MATCH_WEIGHT = 10;

	/**
	 * For 0 and Integer.MAX_VALUE TrieSearch has the same behaviour and the ngram trie is empty.
	 */
	private final int ngramLength;

	private final Pattern splitPattern;

	// We store whole words and ngrams separately to avoid additional work,
	// such as checking and skipping ngrams when iterating through all whole words
	private final PatriciaTrie<List<T>> ngrams = new PatriciaTrie<>();
	private final PatriciaTrie<List<T>> whole = new PatriciaTrie<>();
	private boolean shrunk = false;
	private long size = -1;

	public TrieSearch(int ngramLength, String split) {
		if (ngramLength < 0) {
			throw new IllegalArgumentException("Negative ngram Length is not allowed.");
		}

		// We want TrieSearch to behave the same for ngramLength = 0 and ngramLength = Integer.MAX_VALUE.
		if (ngramLength == 0) {
			this.ngramLength = Integer.MAX_VALUE;
		}
		else {
			this.ngramLength = ngramLength;
		}

		splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split, ""))));
	}

	public List<T> findItems(Collection<String> queries, int limit) {
		final Object2LongMap<T> itemWeights = new Object2LongAVLTreeMap<>();

		// We are not guaranteed to have split queries incoming, so we normalize them for searching
		queries = queries.stream().flatMap(this::split).collect(Collectors.toSet());

		for (final String query : queries) {
			// Query trie for all items associated with extensions of queries
			final SortedMap<String, List<T>> prefixHits = whole.prefixMap(query);

			// Slightly favor whole words starting with query
			updateWeights(query, prefixHits, itemWeights, true);

			// If ngramLength is Integer.MAX_VALUE the ngram trie is empty.
			final int queryLength = query.length();
			if (queryLength == 0 || ngramLength == Integer.MAX_VALUE) {
				continue;
			}

			if (queryLength < ngramLength) {
				updateWeights(query, ngrams.prefixMap(query), itemWeights, false);
				continue;
			}

			// Collectors::toMap throws IllegalStateException if there are duplicate keys
			final Map<String, List<T>> ngramHits = ngramSplit(query)
					.distinct()
					.collect(Collectors.toMap(
							Function.identity(),
							ng -> ngrams.getOrDefault(ng, Collections.emptyList())
					));

			updateWeights(query, ngramHits, itemWeights, false);
		}

		// Sort items according to their weight, then limit.
		// Note that sorting is in descending order, meaning higher-scores are better.
		return itemWeights.object2LongEntrySet()
						  .stream()
						  .sorted(Comparator.comparing(Object2LongMap.Entry::getLongValue, Comparator.reverseOrder()))
						  .limit(limit)
						  .map(Map.Entry::getKey)
						  .collect(Collectors.toList());
	}

	/**
	 * Normalize keyword into a stream of non-empty strings without whitespaces via the splitPattern
	 * <p>
	 * '@implNote This does not split keyword into ngrams
	 */
	private Stream<String> split(String keyword) {
		if (Strings.isNullOrEmpty(keyword)) {
			return Stream.empty();
		}

		return splitPattern.splitAsStream(keyword.trim())
						   .map(String::trim)
						   .filter(StringUtils::isNotBlank)
						   .map(String::toLowerCase);
	}

	/**
	 * Calculate and update weights for all queried items
	 */
	private void updateWeights(String query, final Map<String, List<T>> items, Object2LongMap<T> itemWeights, boolean original) {
		if (items == null) {
			return;
		}

		for (Map.Entry<String, List<T>> entry : items.entrySet()) {
			final List<T> hits = entry.getValue();

			if (hits.isEmpty()) {
				continue;
			}

			long weight = weightWord(query, entry.getKey(), original);

			// We combine hits additively to favor items with multiple hits
			for (T item : hits) {
				itemWeights.put(item, itemWeights.getOrDefault(item, 1) + weight);
			}
		}
	}

	/**
	 * Returns an empty stream if word is shorter than ngramLength or ngramLength is Integer.MAX_VALUE
	 */
	public Stream<String> ngramSplit(String word) {

		// Any String  is its own ngram when ngramLength is Integer.MAX_VALUE.
		if (word.length() < ngramLength || ngramLength == Integer.MAX_VALUE) {
			return Stream.empty();
		}

		return IntStream.range(0, word.length() - ngramLength + 1)
						.mapToObj(start -> word.substring(start, start + ngramLength));
	}

	/**
	 * A higher weight implies more relevant words.
	 */
	private long weightWord(String query, String itemWord, boolean original) {
		// The weight function needs to be fast, as it is called frequently.
		final long weight;

		// We prefer same length words.
		if (query.length() == itemWord.length()) {
			weight = EXACT_MATCH_WEIGHT;
		}
		else {
			weight = BASE_WEIGHT;
		}

		// We prefer original words
		if (original) {
			return weight * weight;
		}

		return weight;
	}

	public List<T> findExact(Collection<String> keywords, int limit) {
		return keywords.stream()
					   .flatMap(this::split)
					   .map(whole::get)
					   .filter(Objects::nonNull)
					   .flatMap(List::stream)
					   .distinct()
					   .limit(limit)
					   .collect(Collectors.toList());
	}

	public void addItem(T item, List<String> keywords) {
		ensureWriteable();

		// This barrier avoids work when shrinking and therefore unnecessary calls to ngrams::computeIfAbsent
		final Set<String> barrier = new HashSet<>();

		// Associate item with all extracted keywords
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				.forEach(word -> doPut(word, item, barrier));
	}

	private void ensureWriteable() {
		if (isWriteable()) {
			return;
		}
		throw new IllegalStateException("Cannot alter a shrunk search.");
	}

	private void doPut(String word, T item, Set<String> barrier) {
		whole.computeIfAbsent(word, (ignored) -> new ArrayList<>()).add(item);

		ngramSplit(word)
				.filter(barrier::add)
				.forEach(key -> ngrams.computeIfAbsent(key, (ignored) -> new ArrayList<>()).add(item));
	}

	public boolean isWriteable() {
		return !shrunk;
	}

	/**
	 * Since growth of ArrayList might be excessive, we can shrink the internal lists to only required size instead.
	 *
	 * @implSpec the TrieSearch is still mutable after this. Shrinking might result in different search results.
	 */
	public void shrinkToFit() {
		if (shrunk) {
			return;
		}

		// items can contain duplicates if an item is inserted multiple times with the same keyword or with keywords that share ngrams
		ngrams.replaceAll((key, items) -> items.stream().distinct().collect(Collectors.toList()));
		whole.replaceAll((key, items) -> items.stream().distinct().collect(Collectors.toList()));

		size = calculateSize();
		shrunk = true;
	}

	/**
	 * This is the number of (not necessarily distinct) items associated with whole words.
	 * <p>
	 * Ngrams have their own lists of items and are not considered for the size.
	 */
	public long calculateSize() {
		if (size != -1) {
			return size;
		}

		return whole.values().stream().mapToLong(List::size).sum();
	}

	public Stream<T> stream() {
		return whole.values().stream()
					.flatMap(Collection::stream)
					.distinct();
	}

	public Iterator<T> iterator() {
		// This is a very ugly workaround to not get eager evaluation (which happens when using flatMap and distinct on streams)
		final Set<T> seen = new HashSet<>();

		return Iterators.filter(
				Iterators.concat(Iterators.transform(whole.values().iterator(), Collection::iterator)),
				seen::add
		);
	}

}

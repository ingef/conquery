package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
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
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;

/**
 * Trie based keyword search for autocompletion and resolving.
 * <p>
 * We store not only whole words but ngrams up to length of ngramLength to enable a sort of fuzzy and partial search with longer and compound search terms.
 * <p>
 * The output result should be in the following order:
 * 1) Original words (in order of difference)
 * 2) ngrams (in order of difference)
 */
@Slf4j
@ToString(of = {"ngramLength", "splitPattern"})
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * We saturate matches to avoid favoring very short keywords, when multiple keywords are used.
	 */
	private static final double EXACT_MATCH_WEIGHT = 0.1d;

	private final int ngramLength;

	private final Pattern splitPattern;
	private final PatriciaTrie<List<T>> entries = new PatriciaTrie<>();
	private final PatriciaTrie<List<T>> whole = new PatriciaTrie<>();
	private boolean shrunk = false;
	private long size = -1;

	public TrieSearch(int ngramLength, String split) {
		if (ngramLength < 0) {
			throw new IllegalArgumentException("Negative ngram Length is not allowed.");
		}
		this.ngramLength = ngramLength;

		splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split, ""))));
	}

	public List<T> findItems(Collection<String> queries, int limit) {
		final Object2DoubleMap<T> itemWeights = new Object2DoubleAVLTreeMap<>();

		// We are not guaranteed to have split queries incoming, so we normalize them for searching
		queries = queries.stream().flatMap(this::split).collect(Collectors.toSet());

		for (final String query : queries) {
			// Query trie for all items associated with extensions of queries
			final SortedMap<String, List<T>> prefixHits = whole.prefixMap(query);

			// Slightly favor whole words starting with query
			updateWeights(query, prefixHits, itemWeights);

			if (query.length() < ngramLength) {
				updateWeights(query, entries.prefixMap(query), itemWeights);
			}
			else {
				final Map<String, List<T>> ngramHits = ngrams(query).collect(Collectors.toMap(Function.identity(), entries::get));
				updateWeights(query, ngramHits, itemWeights);
			}
		}

		// Sort items according to their weight, then limit.
		// Note that sorting is in ascending order, meaning lower-scores are better.
		return itemWeights.object2DoubleEntrySet()
						  .stream()
						  .sorted(Comparator.comparingDouble(Object2DoubleMap.Entry::getDoubleValue))
						  .limit(limit)
						  .map(Map.Entry::getKey)
						  .collect(Collectors.toList());
	}

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
	 * calculate and update weights for all queried items
	 */
	private void updateWeights(String query, final Map<String, List<T>> items, Object2DoubleMap<T> itemWeights) {
		if (items == null) {
			return;
		}


		for (Map.Entry<String, List<T>> entry : items.entrySet()) {
			final String key = entry.getKey();
			final List<T> hits = entry.getValue();

			final double weight = weightWord(query, key);

			// We combine hits multiplicative to favor items with multiple hits
			for (T item : hits) {
				final double currentWeight = itemWeights.getOrDefault(item, 1);
				itemWeights.put(item, currentWeight * weight);
			}
		}
	}

	public Stream<String> ngrams(String word) {

		if (word.length() < ngramLength) {
			return Stream.empty();
		}

		return IntStream.range(0, word.length() - ngramLength + 1)
						.mapToObj(start -> word.substring(start, start + ngramLength));
	}

	/**
	 * A lower weight implies more relevant words.
	 */
	private double weightWord(String query, String itemWord) {
		final double itemLength = itemWord.length();
		final double queryLength = query.length();

		final double weight;

		// We saturate the weight to avoid favoring extremely short matches.
		if (queryLength == itemLength) {
			weight = EXACT_MATCH_WEIGHT;
		}
		// We assume that less difference implies more relevant words
		else if (queryLength < itemLength) {
			weight = (itemLength - queryLength) / itemLength;
		}
		else {
			weight = (queryLength - itemLength) / queryLength;
		}

		// Soft grouping based on string length
		return Math.pow(weight, 2 / (itemLength + queryLength));
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

		// Associate item with all extracted keywords

		final Set<String> barrier = new HashSet<>();

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

		ngrams(word)
				.filter(barrier::add)
				.forEach(key -> entries.computeIfAbsent(key, (ignored) -> new ArrayList<>()).add(item));
	}

	public boolean isWriteable() {
		return !shrunk;
	}

	/**
	 * Since growth of ArrayList might be excessive, we can shrink the internal lists to only required size instead.
	 *
	 * @implSpec the TrieSearch is still mutable after this.
	 */
	public void shrinkToFit() {
		if (shrunk) {
			return;
		}
		entries.replaceAll((key, values) -> values.stream().distinct().collect(Collectors.toList()));
		whole.replaceAll((key, values) -> values.stream().distinct().collect(Collectors.toList()));

		size = calculateSize();
		shrunk = true;
	}

	public long calculateSize() {
		if (size != -1) {
			return size;
		}

		long totalSize = 0;
		for (List<T> entry : whole.values()) {
			totalSize += entry.size();
		}
		return totalSize;
	}

	public Stream<T> stream() {
		return whole.values().stream()
					.flatMap(Collection::stream)
					.distinct();
	}

	public Iterator<T> iterator() {
		// This is a very ugly workaround to not get eager evaluation (which happens when using flatMap and distinct on streams)
		final Set<T> seen = new HashSet<>();

		final Iterator<Iterator<T>> enIter = Iterators.transform(whole.values().iterator(), List::listIterator);
		return Iterators.filter(Iterators.concat(enIter), seen::add);
	}

}

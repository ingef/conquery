package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
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
 * We store not only whole words but suffixes up to length of SUFFIX_CUTOFF to enable a sort of fuzzy and partial search with longer and compound search terms.
 * <p>
 * The output result should be in the following order:
 * 1) Original words (in order of difference)
 * 2) Suffixes (in order of difference)
 */
@Slf4j
@ToString(of = {"suffixCutoff", "splitPattern"})
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * We saturate matches to avoid favoring very short keywords, when multiple keywords are used.
	 */
	private static final double EXACT_MATCH_WEIGHT = 0.1d;
	/**
	 * We prefer original words in our results.
	 *
	 * @implNote they are marked by appending WHOLE_WORD_MARKER to them in the trie.
	 */
	private static final double ORIGINAL_WORD_WEIGHT_FACTOR = EXACT_MATCH_WEIGHT;
	private static final String WHOLE_WORD_MARKER = "!";

	private final int suffixCutoff;

	private final Pattern splitPattern;

	private boolean shrunk = false;
	private long size = -1;

	public TrieSearch(int suffixCutoff, String split) {
		if (suffixCutoff < 0) {
			throw new IllegalArgumentException("Negative Suffix Length is not allowed.");
		}
		this.suffixCutoff = suffixCutoff;

		splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split, "") + WHOLE_WORD_MARKER)));
	}

	/**
	 * Maps from keywords to associated items.
	 */
	private final PatriciaTrie<List<T>> trie = new PatriciaTrie<>();

	Stream<String> suffixes(String word) {
		return Stream.concat(
				// We append a special character here marking original words as we want to favor them in weighing.
				Stream.of(word + WHOLE_WORD_MARKER),
				IntStream.range(1, Math.max(1, word.length() - suffixCutoff))
						 .mapToObj(word::substring)
		);
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
	 * A lower weight implies more relevant words.
	 */
	private double weightWord(String keyword, String itemWord) {

		// Test if the word is an original word and not a suffix.
		final boolean isOriginal = isOriginal(itemWord);

		final double keywordLength = keyword.length();
		final double itemLength = itemWord.length() - (isOriginal ? 1 : 0);

		// keyword is prefix of itemWord
		assert itemLength >= keywordLength;

		final double weight;

		// We saturate the weight to avoid favoring extremely short matches.
		if (keywordLength == itemLength) {
			weight = EXACT_MATCH_WEIGHT;
		}
		else {
			// We assume that less difference implies more relevant words
			weight = (itemLength - keywordLength) / keywordLength;
		}

		// If itemWord ends with WHOLE_WORD_MARKER, we are matching an original input from the beginning which are favorable (but less than exact matches).
		if (isOriginal) {
			return ORIGINAL_WORD_WEIGHT_FACTOR * weight;
		}

		return weight;
	}

	private boolean isOriginal(String itemWord) {
		return itemWord.endsWith(WHOLE_WORD_MARKER);
	}

	public List<T> findItems(Collection<String> keywords, int limit) {
		final Object2DoubleMap<T> itemWeights = new Object2DoubleAVLTreeMap<>();

		// We are not guaranteed to have split keywords incoming, so we normalize them for searching
		keywords = keywords.stream().flatMap(this::split).collect(Collectors.toSet());

		for (String keyword : keywords) {
			// Query trie for all items associated with extensions of keywords
			final SortedMap<String, List<T>> hits = trie.prefixMap(keyword);

			for (Map.Entry<String, List<T>> entry : hits.entrySet()) {

				// calculate and update weights for all queried items
				final String itemWord = entry.getKey();

				final double weight = weightWord(keyword, itemWord);

				entry.getValue()
					 .forEach(item ->
							  {
								  // We combine hits multiplicative to favor items with multiple hits
								  final double currentWeight = itemWeights.getOrDefault(item, 1);
								  itemWeights.put(item, currentWeight * weight);
							  });
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

	public List<T> findExact(Collection<String> keywords, int limit) {
		return keywords.stream()
					   .flatMap(this::split)
					   .map(kw -> kw + WHOLE_WORD_MARKER)
					   .flatMap(this::doGet)
					   .distinct()
					   .limit(limit)
					   .collect(Collectors.toList());
	}

	private Stream<T> doGet(String kw) {
		return trie.getOrDefault(kw, Collections.emptyList()).stream();
	}


	private void doPut(String kw, T item) {
		ensureWriteable();

		trie.computeIfAbsent(kw, (ignored) -> new ArrayList<>())
			.add(item);
	}

	private void ensureWriteable() {
		if (!shrunk) {
			return;
		}
		throw new IllegalStateException("Cannot alter a shrunk search.");
	}

	public void addItem(T item, List<String> keywords) {
		// Associate item with all extracted keywords
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				.flatMap(this::suffixes)
				.distinct()
				.forEach(kw -> doPut(kw, item));
	}


	public Collection<T> listItems() {
		//TODO this a pretty dangerous operation, I'd rather see a session based iterator instead
		return trie.values().stream()
				   .flatMap(Collection::stream)
				   .distinct()
				   .sorted()
				   .collect(Collectors.toList());
	}

	public long calculateSize() {
		if (size != -1) {
			return size;
		}

		return trie.values().stream().distinct().count();
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

		trie.replaceAll((key, values) -> values.stream().distinct().collect(Collectors.toList()));

		size = calculateSize();
		shrunk = true;
	}

	public void logStats() {
		final IntSummaryStatistics statistics =
				trie.values()
					.stream()
					.mapToInt(List::size)
					.summaryStatistics();

		final long singletons =
				trie.values().stream()
					.mapToInt(List::size)
					.filter(length -> length == 1)
					.count();

		log.info("Stats=`{}`, with {} singletons.", statistics, singletons);
	}

	public Stream<T> stream() {
		return trie.values().stream()
				   .flatMap(Collection::stream)
				   .distinct();
	}

	public Iterator<T> iterator() {
		// This is a very ugly workaround to not get eager evaluation (which happens when using flatMap and distinct on streams)
		final Set<T> seen = new HashSet<>();

		return Iterators.filter(
				Iterators.<T>concat(trie.values().stream()
										.map(Collection::iterator)
										.toArray(Iterator[]::new)),
				seen::add
		);
	}
}

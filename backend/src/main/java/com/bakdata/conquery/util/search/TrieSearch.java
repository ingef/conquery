package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;

/**
 * Trie based keyword search for autocompletion and resolving.
 * <p>
 * We store not only whole words but suffixes up to length of SUFFIX_CUTOFF to enable a sort of fuzzy and partial search with longer and compound search terms.
 */
@Slf4j
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * We saturate matches to avoid favoring very short keywords, when multiple keywords are used.
	 */
	private static final double EXACT_MATCH_WEIGHT = 1d / 10d;
	/**
	 * If we find original words in the search, we prefer them a bit.
	 */
	private static final double ORIGINAL_WORD_WEIGHT_FACTOR = 0.5d;
	private static final String WHOLE_WORD_MARKER = "!";

	private final int suffixCutoff;

	private final Pattern splitPattern; //TODO FK: Investigate better split patterns

	public TrieSearch(int suffixCutoff, String split) {
		this.suffixCutoff = suffixCutoff;

		splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split + WHOLE_WORD_MARKER, ""))));
	}

	/**
	 * Maps from keywords to associated items.
	 */
	private final PatriciaTrie<List<T>> trie = new PatriciaTrie<>();

	public void clear() {
		trie.clear();
	}

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
		final boolean isOriginal = itemWord.endsWith(WHOLE_WORD_MARKER);

		final double keywordLength = keyword.length();
		final double itemLength = itemWord.length() - (isOriginal ? 1 : 0);

		// keyword is prefix of itemWord
		assert itemLength >= keywordLength;


		// We saturate the weight to avoid favoring extremely short matches.
		if (keywordLength == itemLength) {
			return EXACT_MATCH_WEIGHT;
		}

		// We assume that less difference implies more relevant words
		final double weight = (itemLength - keywordLength) / keywordLength;

		// If itemWord ends with WHOLE_WORD_MARKER, we are matching an original input from the beginning which are favorable (but less than exact matches).
		if (isOriginal) {
			return ORIGINAL_WORD_WEIGHT_FACTOR * weight;
		}

		return weight;
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
		trie.computeIfAbsent(kw, (ignored) -> new ArrayList<>())
			.add(item);
	}

	public void addItem(T item, List<String> keywords) {
		// Associate item with all extracted keywords
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				.flatMap(this::suffixes)
				.forEach(kw -> doPut(kw, item));
	}

	public Iterator<T> iterator() {
		return trie.keySet().stream().flatMap(this::doGet).distinct().iterator();
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
		return trie.values().stream().distinct().count();
	}

	/**
	 * Since growth of ArrayList might be excessive, we can shrink the internal lists to only required size instead.
	 *
	 * @implSpec the TrieSearch is still mutable after this.
	 */
	public void shrinkToFit() {
		trie.replaceAll((key, values) -> values.stream().distinct().collect(Collectors.toList()));
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

}

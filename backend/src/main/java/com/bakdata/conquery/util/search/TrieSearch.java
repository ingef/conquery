package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * We saturate matches here to avoid favoring very short keywords, when multiple keywords are used.
	 */
	private static final double MATCH_THRESHOLD = 1d / 20d;
	private static final int SUFFIX_CUTOFF = 1;

	private static final Pattern SPLIT = Pattern.compile("[\\s(),:\"']+"); //TODO FK: Investigate better split patterns
	/**
	 * @implSpec the underlying type of the trie is a union of T or List<T>, please never access it directly.
	 */
	private final PatriciaTrie<Object> trie = new PatriciaTrie<>();

	public void clear() {
		trie.clear();
	}

	private Stream<String> suffixes(String word) {
		return IntStream.range(0, Math.max(0, word.length() - SUFFIX_CUTOFF))
						.mapToObj(word::substring);
	}

	private Stream<String> split(String keyword) {
		if (Strings.isNullOrEmpty(keyword)) {
			return Stream.empty();
		}

		return SPLIT.splitAsStream(keyword.trim())
					.map(String::trim)
					.filter(StringUtils::isNotBlank)
					.map(String::toLowerCase);
	}

	/**
	 * A lower weight implies more relevant words.
	 */
	private double weightWord(String keyword, String itemWord) {
		final double keywordLength = keyword.length();
		final double itemLength = itemWord.length();

		// keyword is prefix of itemWord
		assert itemLength >= keywordLength;

		// We saturate the weight to avoid favoring short matches.
		if (keywordLength == itemLength) {
			return MATCH_THRESHOLD;
		}

		// We assume that less difference implies more relevant words
		return (itemLength - keywordLength) / keywordLength;
	}

	public List<T> findItems(Collection<String> keywords, int limit) {
		final Object2DoubleMap<T> itemWeights = new Object2DoubleAVLTreeMap<>();
		// We are not guaranteed to have split keywords incoming, so we normalize them for searching
		keywords = keywords.stream().flatMap(this::split).collect(Collectors.toSet());

		for (String keyword : keywords) {
			// Query trie for all items associated with extensions of keywords
			final SortedMap<String, Object> hits = trie.prefixMap(keyword);

			for (Map.Entry<String, Object> entry : hits.entrySet()) {

				// calculate and update weights for all queried items
				final String itemWord = entry.getKey();
				final double weight = weightWord(keyword, itemWord);

				raise(entry.getValue())
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
		return
				keywords.stream().flatMap(this::split)
						.flatMap(this::doGet)
						.distinct()
						.limit(limit)
						.collect(Collectors.toList());
	}

	private Stream<T> doGet(String kw) {
		final Object maybeItems = trie.get(kw);

		return raise(maybeItems);
	}

	private Stream<T> raise(Object maybeItems) {
		if (maybeItems == null) {
			return Stream.empty();
		}

		if (maybeItems instanceof ArrayList) {
			return ((ArrayList<T>) maybeItems).stream();
		}

		return Stream.of(((T) maybeItems));
	}

	private void doPut(String kw, T item) {
		trie.compute(kw, (ignored, prior) -> {
			if (prior instanceof ArrayList) {

				((ArrayList) prior).add(item);

				return prior;
			}
			if (prior != null) {
				final List items = new ArrayList<>(2);
				items.add(prior);
				items.add(item);

				return items;
			}
			return item;
		});
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
		return trie.values().stream()
				   .flatMap(this::raise)
				   .distinct()
				   .sorted()
				   .collect(Collectors.toList());
	}

	public long calculateSize() {
		return trie.values().stream().distinct().count();
	}

}

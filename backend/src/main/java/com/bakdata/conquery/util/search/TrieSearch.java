package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.trie.PatriciaTrie;

@NoArgsConstructor
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * We saturate matches here to avoid favoring very short keywords, when multiple keywords are used.
	 */
	private static final double MATCH_THRESHOLD = 0.05d;

	private static final Pattern SPLIT = Pattern.compile("[\\s()_,:\"'-]+");
	private final PatriciaTrie<List<T>> trie = new PatriciaTrie<>();

	public void clear() {
		trie.clear();
	}

	private Stream<String> split(String keyword) {
		if (Strings.isNullOrEmpty(keyword)) {
			return Stream.empty();
		}

		return SPLIT.splitAsStream(keyword.trim())
					.map(String::trim)
					.filter(Predicate.not(Strings::isNullOrEmpty))
					.map(String::toLowerCase);
	}

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
			final SortedMap<String, List<T>> hits = trie.prefixMap(keyword);

			for (Map.Entry<String, List<T>> entry : hits.entrySet()) {
				// calculate and update weights for all queried items
				final String itemWord = entry.getKey();
				final double weight = weightWord(keyword, itemWord);

				for (T item : entry.getValue()) {
					// We combine hits multiplicative to favor items with multiple hits
					final double currentWeight = itemWeights.getOrDefault(item, 1);
					itemWeights.put(item, currentWeight * weight);
				}
			}
		}

		// Sort items according to their weight, then limit.
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
						.map(kw -> trie.getOrDefault(kw, Collections.emptyList()))
						.flatMap(List::stream)
						.distinct()
						.limit(limit)
						.collect(Collectors.toList());
	}

	public void addItem(T item, List<String> keywords) {
		// Associate item with all extracted keywords
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				//TODO FK: We could lessen the memory demand a bit by making this a union of List<T> and T, such that items are initially stored directly and only upgraded to lists when needed (although this would complicate ahdnling a tiny bit)
				.forEach(kw -> trie.computeIfAbsent(kw, (ignored) -> new ArrayList<>()).add(item));
	}

	public Iterator<T> iterator() {
		return trie.keySet().stream().map(trie::get).flatMap(List::stream).distinct().iterator();
	}

	public Collection<T> listItems() {
		return trie.values().stream()
				   .flatMap(List::stream)
				   .distinct()
				   .sorted()
				   .collect(Collectors.toList());
	}

	public long calculateSize() {
		return trie.values().stream().distinct().count();
	}

}

package com.bakdata.conquery.util.search;

import java.util.*;
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
 * 2) Suffixes (in order of difference)
 */
@Slf4j
@ToString(of = {"ngramLength", "splitPattern"})
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
	private static final String WHOLE_WORD_MARKER = "!";

	private final int ngramLength;

	private final Pattern splitPattern;

	private record KeywordItems<T>(String word, List<T> items) {
	}

	private final List<KeywordItems<T>> keywordItemsList = new ArrayList<>();

	private record NgramIndex(String ngram, int index) {
	}

	/**
	 * Maps from keywords to associated items.
	 */
	private final PatriciaTrie<List<Integer>> trie = new PatriciaTrie<>();

	private boolean shrunk = false;
	private long size = -1;

	public TrieSearch(int ngramLength, String split) {
		if (ngramLength < 0) {
			throw new IllegalArgumentException("Negative ngram Length is not allowed.");
		}
		this.ngramLength = ngramLength;

		splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split, "") + WHOLE_WORD_MARKER)));
	}

	public List<T> findItems(Collection<String> queries, int limit) {
		final Object2DoubleMap<T> itemWeights = new Object2DoubleAVLTreeMap<>();

		// We are not guaranteed to have split queries incoming, so we normalize them for searching
		queries = queries.stream().flatMap(this::split).collect(Collectors.toSet());

		for (final String query : queries) {
			// Query trie for all items associated with extensions of queries
			final int queryLength = query.length();
			if (queryLength < ngramLength) {
				// ToDo: A guard against queryLength < ngramLength would make this case obsolete, but the tests would have to be adjusted
				for (final List<Integer> hits : trie.prefixMap(query).values()) {
					updateWeights(query, hits, itemWeights);
				}
			}
			else {
				for (int start = 0; start <= queryLength - ngramLength; start++) {
					final String ngram = query.substring(start, start + ngramLength);
					updateWeights(query, trie.get(ngram), itemWeights);
				}
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

	/**
	 * calculate and update weights for all queried items
	 */
	private void updateWeights(String query, final List<Integer> hits, Object2DoubleMap<T> itemWeights) {
		if (hits == null) {
			return;
		}

		for (int index : hits) {

			final KeywordItems<T> entry = keywordItemsList.get(index);

			final String itemWord = entry.word;

			final double weight = weightWord(query, itemWord);

			entry.items.forEach(item ->
								{
									// We combine hits multiplicative to favor items with multiple hits
									final double currentWeight = itemWeights.getOrDefault(item, 1);
									itemWeights.put(item, currentWeight * weight);
								});
		}
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
	private double weightWord(String query, String itemWord) {
		final double itemLength = itemWord.length();
		final double queryLength = query.length();

		double weight;
		double root = (itemLength + queryLength) / 2;

		// We saturate the weight to avoid favoring extremely short matches.
		if (queryLength == itemLength) {
			weight = EXACT_MATCH_WEIGHT;
		}
		else if (queryLength < itemLength) {
			// We assume that less difference implies more relevant words
			weight = (itemLength - queryLength) / itemLength;
		}
		else{
			// We assume that less difference implies more relevant words
			weight = (queryLength - itemLength) / queryLength;
		}

		return Math.pow(weight, 1 / root);
	}

	private boolean isOriginal(String itemWord) {
		return itemWord.endsWith(WHOLE_WORD_MARKER);
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
		return trie.getOrDefault(kw, Collections.emptyList()).stream()
				   .flatMap(index -> keywordItemsList.get(index).items.stream());
	}

	public void addItem(T item, List<String> keywords) {
		// Associate item with all extracted keywords
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				.flatMap(this::ngramSplit)
				.forEach(ni -> doPut(ni, item));
	}

	public Stream<String> ngramSplitToStringStream(String word) {
		return ngramSplit(word).map(ni -> ni.ngram);
	}

	private Stream<NgramIndex> ngramSplit(String word) {
		// We append a special character here marking original words as we want to favor them in weighing.
		final String wholeWord = word + WHOLE_WORD_MARKER;
		List<Integer> entry = trie.get(wholeWord);

		final int index;
		if (entry != null) {
			index = entry.get(0);
		}
		else {
			keywordItemsList.add(new KeywordItems<>(word, new ArrayList<>()));
			index = keywordItemsList.size() - 1;
		}

		Stream<NgramIndex> wholeWordStream = Stream.of(new NgramIndex(wholeWord, index));
		if (word.length() < ngramLength) {
			return wholeWordStream;
		}

		return Stream.concat(
				wholeWordStream,
				IntStream.range(0, word.length() - ngramLength + 1)
						 .mapToObj(start -> new NgramIndex(word.substring(start, start + ngramLength), index))
		);
	}

	private void doPut(NgramIndex ni, T item) {
		// ToDo: wouldn't it suffice to check once in addItem()? Is concurrency the reason?
		ensureWriteable();
		trie.computeIfAbsent(ni.ngram, (ignored) -> new ArrayList<>())
			.add(ni.index);

		if (isOriginal(ni.ngram)) {
			keywordItemsList.get(ni.index).items.add(item);
		}
	}

	private void ensureWriteable() {
		if (isWriteable()) {
			return;
		}
		throw new IllegalStateException("Cannot alter a shrunk search.");
	}

	public Collection<T> listItems() {
		//TODO this a pretty dangerous operation, I'd rather see a session based iterator instead
		return stream().sorted().collect(Collectors.toList());
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

		trie.replaceAll((key, values) -> new ArrayList<>(values));
		((ArrayList<KeywordItems<T>>) keywordItemsList).trimToSize();
		keywordItemsList.replaceAll(ki -> new KeywordItems<>(
				ki.word,
				ki.items.stream().distinct().collect(Collectors.toList())
		));

		size = calculateSize();
		shrunk = true;
	}

	public long calculateSize() {
		if (size != -1) {
			return size;
		}

		return keywordItemsList.stream().flatMap(ki -> ki.items.stream()).distinct().count();
	}

	public void logStats() {
		// ToDo: meaning changed
		final IntSummaryStatistics statistics =
				trie.values()
					.stream()
					.mapToInt(List::size)
					.summaryStatistics();

		// ToDo: meaning changed
		final long singletons =
				trie.values().stream()
					.mapToInt(List::size)
					.filter(length -> length == 1)
					.count();

		log.info("Stats=`{}`, with {} singletons.", statistics, singletons);
	}

	public Stream<T> stream() {
		return keywordItemsList.stream()
							   .flatMap(ki -> ki.items.stream())
							   .distinct();
	}

	public Iterator<T> iterator() {
		// This is a very ugly workaround to not get eager evaluation (which happens when using flatMap and distinct on streams)
		final Set<T> seen = new HashSet<>();

		return Iterators.filter(
				Iterators.concat(Iterators.transform(keywordItemsList.iterator(), ki -> ki.items.iterator())),
				seen::add
		);
	}

	public boolean isWriteable() {
		return !shrunk;
	}
}

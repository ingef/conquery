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
     * @implNote they are identifiable by having a KeywordIndex with start equal to 0.
     */
    private static final double ORIGINAL_WORD_WEIGHT_FACTOR = EXACT_MATCH_WEIGHT;

    private final int ngramLength;

    private final Pattern splitPattern;

    private record KeywordItems<T>(String word, List<T> items) {
    }

    private final List<KeywordItems<T>> keywordItemsList = new ArrayList<>();

    private record NgramIndex(String ngram, int index, int start) {
    }

    private record KeywordIndex(int index, int start) {
        public KeywordIndex(NgramIndex info) {
            this(info.index, info.start);
        }

        public boolean isOriginal() {
            return start == 0;
        }
    }

    /**
     * Maps from keywords to associated items.
     */
    private final PatriciaTrie<List<KeywordIndex>> trie = new PatriciaTrie<>();

    private boolean shrunk = false;
    private long size = -1;

    public TrieSearch(int ngramLength, String split) {
        if (ngramLength < 0) {
            throw new IllegalArgumentException("Negative ngram Length is not allowed.");
        }
        this.ngramLength = ngramLength;

        splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split, ""))));
    }

    public List<T> findItems(Collection<String> keywords, int limit) {
        final Object2DoubleMap<T> itemWeights = new Object2DoubleAVLTreeMap<>();

        // We are not guaranteed to have split keywords incoming, so we normalize them for searching
        keywords = keywords.stream().flatMap(this::split).collect(Collectors.toSet());

        for (String keyword : keywords) {
            // Query trie for all items associated with extensions of keywords
            for (final List<KeywordIndex> hits : trie.prefixMap(keyword).values())
                updateWeights(keyword, hits, itemWeights);
            if (keyword.length() >= ngramLength) {
                // If there are items associated with the keyword, then there is an
                // ngram associated with those items that is a prefix of keyword
                updateWeights(keyword, trie.get(keyword.substring(0, ngramLength)), itemWeights);
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
    private void updateWeights(String keyword, final List<KeywordIndex> hits, Object2DoubleMap<T> itemWeights) {
        if (hits == null) return;

        for (KeywordIndex keywordIndex : hits) {

            final KeywordItems<T> entry = keywordItemsList.get(keywordIndex.index);
            if (!entry.word.startsWith(keyword, keywordIndex.start)) continue;

            boolean isOriginal = keywordIndex.isOriginal();
            final String itemWord = isOriginal ? entry.word : entry.word.substring(keywordIndex.start);

            final double weight = weightWord(keyword, itemWord, isOriginal);
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
    private double weightWord(String keyword, String itemWord, boolean isOriginal) {
        final double keywordLength = keyword.length();
        final double itemLength = itemWord.length();

        // keyword is prefix of itemWord
        assert itemLength >= keywordLength;

        final double weight;

        // We saturate the weight to avoid favoring extremely short matches.
        if (keywordLength == itemLength) {
            weight = EXACT_MATCH_WEIGHT;
        } else {
            // We assume that less difference implies more relevant words
            weight = (itemLength - keywordLength) / keywordLength;
        }

        // An original itemWord is favorable (but less than exact matches).
        if (isOriginal) {
            return ORIGINAL_WORD_WEIGHT_FACTOR * weight;
        }

        return weight;
    }

    public List<T> findExact(Collection<String> keywords, int limit) {
        return keywords.stream()
                .flatMap(this::split)
                .flatMap(this::doGetWholeWords)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Stream<T> doGetWholeWords(String kw) {
        return trie.getOrDefault(kw, Collections.emptyList()).stream().filter(KeywordIndex::isOriginal)
                .flatMap(ndx -> keywordItemsList.get(ndx.index).items.stream());
    }

    public void addItem(T item, List<String> keywords) {
        // Associate item with all extracted keywords
        keywords.stream()
                .filter(Predicate.not(Strings::isNullOrEmpty))
                .flatMap(this::split)
                .flatMap(word -> ngramSplit(word, item))
                .distinct()
                .forEach(this::doPut);
    }

    public Stream<String> ngramSplitStrings(String word, T item) {
        return ngramSplit(word, item).map(wi -> wi.ngram);
    }

    private Stream<NgramIndex> ngramSplit(String word, T item) {
        // We append a special character here marking original words as we want to favor them in weighing.
        int index;
        List<KeywordIndex> entry = trie.get(word);

        Optional<KeywordIndex> optionalIndex = Optional.empty();
        if (entry != null)
            optionalIndex = entry.stream().filter(KeywordIndex::isOriginal).findFirst();

        if (optionalIndex.isPresent())
            index = optionalIndex.get().index;
        else {
            keywordItemsList.add(new KeywordItems<>(word, new ArrayList<>()));
            index = keywordItemsList.size() - 1;
        }

        keywordItemsList.get(index).items.add(item);

        // start equal to 0 marks word as an original
        Stream<NgramIndex> wholeWordStream = Stream.of(new NgramIndex(word, index, 0));
        if (word.length() < ngramLength) return wholeWordStream;

        return Stream.concat(
                wholeWordStream,
                IntStream.range(1, word.length() - ngramLength + 1)
                        .mapToObj(start -> new NgramIndex(word.substring(start, start + ngramLength), index, start))
        );
    }

    private void doPut(NgramIndex ni) {
        // wouldn't it suffice to check once in addItem()?
        // is concurrency the reason?
        ensureWriteable();
        trie.computeIfAbsent(ni.ngram, (ignored) -> new ArrayList<>())
                .add(new KeywordIndex(ni));
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

        trie.replaceAll((key, values) -> values.stream().distinct().collect(Collectors.toList()));
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
                Iterators.concat(Iterators.transform(keywordItemsList.stream().map(ki -> ki.items).iterator(), Collection::iterator)),
                seen::add
        );
    }

    public boolean isWriteable() {
        return !shrunk;
    }
}

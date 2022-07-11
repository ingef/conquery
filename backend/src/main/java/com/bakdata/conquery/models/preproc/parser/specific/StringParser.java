package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeNumber;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypePrefixSuffix;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeSingleton;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import com.google.common.base.Strings;
import io.dropwizard.util.DataSize;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Analyze all strings for common suffix/prefix, or if they are singleton.
 * <p>
 * Values are stored DictionaryEncoded(Integer->String), Integers are stored using {@link IntegerParser}.
 */
@Slf4j
@Getter
@ToString(callSuper = true, of = {"encoding", "prefix", "suffix"})
public class StringParser extends Parser<Integer, StringStore> {

	private Object2IntMap<String> strings = new Object2IntOpenHashMap<>();

	//TODO FK: this field is not used at the moment, but we want to use it to prune unused values, this would mean cleaning up strings and allowing Dictionary to set a specific valuie, not just setting it.
	private IntSet registered = new IntOpenHashSet();

	private List<byte[]> decoded;
	private Encoding encoding;
	private String prefix;
	private String suffix;

	public StringParser(ConqueryConfig config) {
		super(config);
	}

	public static StringTypeNumber tryCreateNumberStringStore(StringParser stringParser, ConqueryConfig config) {
		//check if the remaining strings are all numbers
		Range<Integer> range = new Range.IntegerRange(0, 0);
		IntegerParser numberParser = new IntegerParser(config);

		if (stringParser.getStrings().keySet().parallelStream()
						.anyMatch(key -> key.startsWith("0") && !"0".equals(key))) {
			return null;
		}

		try {
			//check that there are no leading zeroes that we would destroy

			for (Map.Entry<String, Integer> e : stringParser.getStrings().entrySet()) {
				int intValue = Integer.parseInt(e.getKey());
				range = range.span(new Range.IntegerRange(intValue, intValue));

				numberParser.addLine((long) intValue);
			}
		}
		catch (NumberFormatException e) {
			return null;
		}

		numberParser.setLines(stringParser.getLines());

		/*
		Do not use a number type if the range is much larger than the number if distinct values
		e.g. if the column contains only 0 and 5M
		 */

		final int span = range.getMax() - range.getMin() + 1;

		if (span > stringParser.getStrings().size()) {
			return null;
		}

		IntegerStore decision = numberParser.findBestType();

		Int2ObjectMap<String> inverse = new Int2ObjectOpenHashMap<>(stringParser.getStrings().size());
		stringParser.getStrings().forEach((key, value) -> inverse.putIfAbsent((int) value, key));

		return new StringTypeNumber(range, decision, inverse);
	}

	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return strings.computeIfAbsent(value, this::processSingleValue);
	}

	@Override
	protected void registerValue(Integer v) {
		registered.add(v.intValue());
	}

	public int processSingleValue(String value) {
		//set longest common prefix and suffix
		prefix = Strings.commonPrefix(value, Objects.requireNonNullElse(prefix, value));
		suffix = Strings.commonSuffix(value, Objects.requireNonNullElse(suffix, value));

		//return next id
		return strings.size();
	}

	@Override
	protected StringStore decideType() {

		//check if a singleton type is enough
		if (strings.isEmpty()) {
			return EmptyStore.INSTANCE;
		}

		// Is this a singleton?
		if (strings.size() == 1) {
			StringTypeSingleton type = new StringTypeSingleton(strings.keySet().iterator().next(), BitSetStore.create(getLines()));

			return type;
		}

		//remove prefix and suffix
		if (!StringUtils.isEmpty(prefix) || !StringUtils.isEmpty(suffix)) {
			stripPrefixSuffix();
			log.debug("Reduced strings by the '{}' prefix and '{}' suffix", prefix, suffix);
		}

		decode();

		StringStore result = decideStorageType();

		//wrap in prefix suffix
		if (!Strings.isNullOrEmpty(prefix) || !Strings.isNullOrEmpty(suffix)) {
			result = new StringTypePrefixSuffix(result, Strings.nullToEmpty(prefix), Strings.nullToEmpty(suffix));
		}

		return result;
	}

	private StringStore decideStorageType() {

		StringTypeNumber numberType = tryCreateNumberStringStore(this, getConfig());

		if (numberType != null) {
			log.debug("Decided for {}", numberType);
			return numberType;
		}

		final String name = UUID.randomUUID().toString();

		SuccinctTrie trie = new SuccinctTrie(Dataset.PLACEHOLDER, name);

		getDecoded().forEach(trie::add);

		final long mapTypeEstimate = MapDictionary.estimateMemoryConsumption(getStrings().size(), getDecoded().stream().mapToLong(s -> s.length).sum());

		Dictionary dictionary;

		if (trie.estimateMemoryConsumption() < mapTypeEstimate) {
			trie.compress();
			dictionary = trie;
		}
		else {
			dictionary = Dictionary.copyUncompressed(trie);
		}

		final IntegerStore indexType = decideIndexType();

		log.debug(
				"Decided for {} and {} (est. {})",
				dictionary,
				indexType,
				DataSize.megabytes(indexType.estimateMemoryConsumptionBytes() + dictionary.estimateMemoryConsumption())
		);

		return new StringTypeEncoded(new StringTypeDictionary(indexType, dictionary), getEncoding());
	}

	private void stripPrefixSuffix() {
		Object2IntMap<String> oldStrings = strings;
		strings = new Object2IntOpenHashMap<>(oldStrings.size());
		int stripLeading = prefix.length();
		int stripTrailing = suffix.length();

		for (Object2IntMap.Entry<String> e : oldStrings.object2IntEntrySet()) {
			strings.put(
					e.getKey().substring(stripLeading, e.getKey().length() - stripTrailing),
					e.getIntValue()
			);

		}
	}

	/**
	 * Select the least memory intensive encoding and decode all values using it.
	 */
	private void decode() {
		encoding = findEncoding();
		log.debug("\tChosen encoding is {}", encoding);
		applyEncoding(encoding);
	}

	/**
	 * Test all available encodings and of the ones that can decode all values, use the one using the least memory.
	 */
	private Encoding findEncoding() {
		EnumSet<Encoding> bases = EnumSet.allOf(Encoding.class);
		for (String value : strings.keySet()) {

			bases.removeIf(encoding -> !encoding.canEncode(value));

			if (bases.size() == 1) {
				return bases.iterator().next();
			}

			if (bases.isEmpty()) {
				throw new IllegalStateException("No Encoding can encode the values.");
			}
		}

		return bases.stream()
					.min(Encoding::compareTo)
					.orElseThrow(() -> new IllegalStateException("No valid encoding."));

	}

	public void applyEncoding(Encoding encoding) {
		this.encoding = encoding;
		decoded = strings.object2IntEntrySet().stream()
						 .sorted(Comparator.comparing(Object2IntMap.Entry::getIntValue))
						 .map(entry -> encoding.encode(entry.getKey()))
						 .collect(Collectors.toList());
	}

	@Override
	public void setValue(StringStore store, int event, Integer value) {
		store.setString(event, value);
	}

	@SneakyThrows
	@Override
	public ColumnValues<Integer> createColumnValues() {
		return new IntegerColumnValues();
	}

	public IntegerStore decideIndexType() {
		final IntegerParser indexParser = new IntegerParser(getConfig());

		final IntSummaryStatistics indexStatistics = getStrings().values().intStream()
																 .summaryStatistics();

		indexParser.setMaxValue(indexStatistics.getMax());
		indexParser.setMinValue(indexStatistics.getMin());

		indexParser.setLines(getLines());
		indexParser.setNullLines(getNullLines());


		return indexParser.findBestType();
	}
}

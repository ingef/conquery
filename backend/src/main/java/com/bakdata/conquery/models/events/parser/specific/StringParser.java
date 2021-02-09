package com.bakdata.conquery.models.events.parser.specific;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.parser.specific.string.MapTypeGuesser;
import com.bakdata.conquery.models.events.parser.specific.string.NumberTypeGuesser;
import com.bakdata.conquery.models.events.parser.specific.string.StringTypeGuesser;
import com.bakdata.conquery.models.events.parser.specific.string.StringTypeGuesser.Guess;
import com.bakdata.conquery.models.events.parser.specific.string.TrieTypeGuesser;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypePrefixSuffix;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeSingleton;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jakewharton.byteunits.BinaryByteUnit;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Analyze all strings for common suffix/prefix, or if they are singleton.
 *
 * Values are stored DictionaryEncoded(Integer->String), Integers are stored using {@link IntegerParser}.
 */
@Slf4j
@Getter
@ToString(callSuper = true, of = {"encoding", "prefix","suffix"})
public class StringParser extends Parser<Integer, StringStore> {

	private BiMap<String, Integer> strings = HashBiMap.create();

	private List<byte[]> decoded;
	private Encoding encoding;
	private String prefix;
	private String suffix;

	public StringParser(ParserConfig config) {
		super(config);
	}


	public int processSingleValue(String value) {
		//set longest common prefix and suffix
		prefix = Strings.commonPrefix(value, Objects.requireNonNullElse(prefix, value));
		suffix = Strings.commonSuffix(value, Objects.requireNonNullElse(suffix, value));

		//return next id
		return strings.size();
	}

	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return strings.computeIfAbsent(value, this::processSingleValue);
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
			log.debug("Reduced strings by the '{}' prefix and '{}' suffix", prefix, suffix);
			Map<String, Integer> oldStrings = strings;
			strings = HashBiMap.create(oldStrings.size());
			for (Entry<String, Integer> e : oldStrings.entrySet()) {
				strings.put(
						e.getKey().substring(
								prefix.length(),
								e.getKey().length() - suffix.length()
						),
						e.getValue()
				);

			}
		}

		decode();

		// Try all guesses and select the least memory intensive one.
		Guess guess = Stream.of(
				new TrieTypeGuesser(this),
				new MapTypeGuesser(this),
				new NumberTypeGuesser(this, getConfig())
		)
							.map(StringTypeGuesser::createGuess)
							.filter(Objects::nonNull)
							.min(Comparator.naturalOrder())
							.get();

		log.info(
				"\tUsing {}(est. {})",
				guess.getGuesser().getClass().getSimpleName(),
				BinaryByteUnit.format(guess.estimate())
		);

		StringStore result = guess.getType();
		//wrap in prefix suffix
		if (!Strings.isNullOrEmpty(prefix) || !Strings.isNullOrEmpty(suffix)) {
			result = new StringTypePrefixSuffix(result, prefix, suffix);

		}

		return result;
	}

	@Override
	public void setValue(StringStore store, int event, Integer value) {
		store.setString(event, value);
	}

	@Override
	public List<Integer> createPrimitiveList() {
		return new IntArrayList();
	}

	@Override
	public Integer getNullValue() {
		return 0;
	}

	/**
	 * Select the least memory intensive encoding and decode all values using it.
	 */
	private void decode() {
		encoding = findEncoding();
		log.info("\tChosen encoding is {}", encoding);
		applyEncoding(encoding);
	}

	/**
	 * Test all available encodings and of the ones that can decode all values, use the one using the least memory.
	 */
	private Encoding findEncoding() {
		EnumSet<Encoding> bases = EnumSet.allOf(Encoding.class);
		for (String value : strings.keySet()) {
			bases.removeIf(encoding -> !encoding.canDecode(value));
			if (bases.size() == 1) {
				return bases.iterator().next();
			}

			if(bases.isEmpty()){
				throw new IllegalStateException("No Encoding can encode the values.");
			}
		}

		return bases.stream()
					.min(Encoding::compareTo)
					.orElseThrow(() -> new IllegalStateException("No valid encoding."));

	}

	public void applyEncoding(Encoding encoding) {
		this.encoding = encoding;
		decoded = strings
						  .keySet()
						  .stream()
						  .map(encoding::decode)
						  .collect(Collectors.toList());
	}

	public IntegerStore decideIndexType() {
		final IntegerParser indexParser = new IntegerParser(getConfig());

		final IntSummaryStatistics indexStatistics = getStrings().values().stream()
																 .mapToInt(Integer::intValue)
																 .summaryStatistics();

		indexParser.setMaxValue(indexStatistics.getMax());
		indexParser.setMinValue(indexStatistics.getMin());

		indexParser.setLines(getLines());
		indexParser.setNullLines(getNullLines());


		return indexParser.findBestType();
	}
}

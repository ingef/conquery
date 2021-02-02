package com.bakdata.conquery.models.events.parser.specific.string;

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
import com.bakdata.conquery.models.events.parser.specific.IntegerParser;
import com.bakdata.conquery.models.events.parser.specific.string.StringTypeGuesser.Guess;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypePrefixSuffix;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeSingleton;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jakewharton.byteunits.BinaryByteUnit;
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
public class StringParser extends Parser<Integer> {

	private BiMap<String, Integer> strings = HashBiMap.create();

	private List<byte[]> decoded;
	private Encoding encoding;
	private String prefix = null;
	private String suffix = null;

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


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected ColumnStore<Integer> decideType() {

		//check if a singleton type is enough
		if (strings.isEmpty()) {
			return new EmptyStore<>();
		}

		// Is this a singleton?
		if (strings.size() == 1) {
			StringTypeSingleton type = new StringTypeSingleton(strings.keySet().iterator().next(), BooleanStore.create(getLines()));
			copyLineCounts(type);
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

		StringType result = guess.getType();
		//wrap in prefix suffix
		if (!Strings.isNullOrEmpty(prefix) || !Strings.isNullOrEmpty(suffix)) {
			result = new StringTypePrefixSuffix(result, prefix, suffix);
			copyLineCounts(result);
		}

		return result;
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

	public ColumnStore<Long> decideIndexType() {
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

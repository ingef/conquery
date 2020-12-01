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
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.parser.specific.IntegerParser;
import com.bakdata.conquery.models.events.parser.specific.string.TypeGuesser.Guess;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Getter
public class StringParser extends Parser<Integer> {

	private final String name = "";

	private BiMap<String, Integer> strings = HashBiMap.create();

	private List<byte[]> decoded;
	private Encoding encoding;
	private String prefix = null;
	private String suffix = null;

	public StringParser(ParserConfig config) {

	}

	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return strings.computeIfAbsent(value, v -> {
			//new values

			//set longest common prefix and suffix
			prefix = Strings.commonPrefix(v, Objects.requireNonNullElse(prefix, v));
			suffix = Strings.commonSuffix(v, Objects.requireNonNullElse(suffix, v));

			//return next id
			return strings.size();
		});
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected ColumnStore<Integer> decideType() {

		//check if a singleton type is enough
		if (strings.size() <= 1) {
			StringTypeSingleton type;
			if (strings.isEmpty()) {
				// todo empty store ?
				type = new StringTypeSingleton(null, BooleanStore.create(getLines()));
			}
			else {
				type = new StringTypeSingleton(strings.keySet().iterator().next(), BooleanStore.create(getLines()));
			}
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

		Guess guess = Stream.of(
				new TrieTypeGuesser(this),
				new MapTypeGuesser(this),
				new NumberTypeGuesser(this)
		)
							.map(TypeGuesser::createGuess)
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

	private void decode() {
		encoding = findEncoding();
		log.info("\tChosen encoding is {}", encoding);
		setEncoding(encoding);
	}

	private Encoding findEncoding() {
		EnumSet<Encoding> bases = EnumSet.allOf(Encoding.class);
		for (String value : strings.keySet()) {
			bases.removeIf(encoding -> !encoding.canDecode(value));
			if (bases.size() == 1) {
				return bases.iterator().next();
			}
		}

		return bases.stream()
					.min(Encoding::compareTo)
					.orElseThrow(() -> new IllegalStateException("No valid encoding."));

	}

	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
		decoded = strings
						  .entrySet()
						  .stream()
						  .map(e -> encoding.decode(e.getKey()))
						  .collect(Collectors.toList());
	}

	public ColumnStore<Long> decideIndexType() {
		final IntegerParser indexParser = new IntegerParser();

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

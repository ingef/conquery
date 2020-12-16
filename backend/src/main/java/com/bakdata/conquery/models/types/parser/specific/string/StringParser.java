package com.bakdata.conquery.models.types.parser.specific.string;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.parser.specific.VarIntParser;
import com.bakdata.conquery.models.types.parser.specific.string.TypeGuesser.Guess;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.types.specific.StringTypePrefix;
import com.bakdata.conquery.models.types.specific.StringTypeSingleton;
import com.bakdata.conquery.models.types.specific.StringTypeSuffix;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.google.common.base.Strings;
import com.jakewharton.byteunits.BinaryByteUnit;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Getter
public class StringParser extends Parser<Integer> {

	private DictionaryId dictionaryId = new DictionaryId(new DatasetId("null"), UUID.randomUUID().toString());
	private VarIntParser indexType = new VarIntParser();
	private Map<String, Integer> strings = new LinkedHashMap<>();
	private List<byte[]> decoded;
	private Encoding encoding;
	private String prefix = null;
	private String suffix = null;

	public StringParser(ParserConfig config) {

	}

	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return strings.computeIfAbsent(value, this::processNewValue);
	}

	private int processNewValue(String value) {

		//set longest common prefix and suffix
		prefix = Strings.commonPrefix(value, Objects.requireNonNullElse(prefix, value));
		suffix = Strings.commonSuffix(value, Objects.requireNonNullElse(suffix, value));

		//return next id
		return strings.size();
	}

	@Override
	public Integer addLine(Integer v) {
		super.addLine(v);
		return indexType.addLine(v);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected Decision<Integer, ?, ? extends CType<Integer, ?>> decideType() {
		Decision<Integer, Number, VarIntType> subDecision = indexType.findBestType();

		//check if a singleton type is enough
		if (strings.size() <= 1) {
			StringTypeSingleton type;
			if (strings.isEmpty()) {
				type = new StringTypeSingleton(null);
			}
			else {
				type = new StringTypeSingleton(strings.keySet().iterator().next());
			}
			setLineCounts(type);
			return new Decision<Integer, Boolean, StringTypeSingleton>(
					new Transformer<Integer, Boolean>() {
						@Override
						public Boolean transform(@NonNull Integer value) {
							return Boolean.TRUE;
						}
					},
					type
			);
		}

		//remove prefix and suffix
		if (!StringUtils.isEmpty(prefix) || !StringUtils.isEmpty(suffix)) {
			log.debug("Reduce strings by the '{}' prefix and '{}' suffix", prefix, suffix);
			strings = truncatePrefixSuffix(strings);
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
				"\tUsing {}(est. {}) for {}",
				guess.getGuesser().getClass().getSimpleName(),
				BinaryByteUnit.format(guess.estimate()),
				dictionaryId
		);

		AStringType<Number> result = guess.getType();
		//wrap in prefix suffix
		if (!StringUtils.isEmpty(prefix)) {
			result = new StringTypePrefix(result, prefix);
			setLineCounts(result);
		}
		if (!StringUtils.isEmpty(suffix)) {
			result = new StringTypeSuffix(result, suffix);
			setLineCounts(result);
		}
		return new Decision(
				guess.getTransformer(),
				result
		);
	}

	/**
	 * Truncate suffix and prefix. If an overlap is detected, strip suffix, and restart.
	 */
	private Map<String, Integer> truncatePrefixSuffix(Map<String, Integer> strings) {
		do {
			Map<String, Integer> cutStrings = new LinkedHashMap<>(strings.size());

			for (Entry<String, Integer> e : this.strings.entrySet()) {
				// prefix and suffix overlap in this string, therefore it's not reconstructable. We truncate the suffix a bit, and sadly have to recompute the entire map.
				if (e.getKey().length() < prefix.length() + suffix.length()) {
					int overlap = (prefix.length() + suffix.length()) - e.getKey().length();
					log.debug("Prefix[{}] and Suffix[{}] have overlap of length = {} for String[{}]", prefix, suffix, overlap, e.getKey());

					suffix = suffix.substring(overlap);
					cutStrings = null;
					break;
				}

				final String substring = e.getKey()
										  .substring(prefix.length(), e.getKey().length() - suffix.length());

				cutStrings.put(substring, e.getValue());
			}

			if(cutStrings != null){
				return cutStrings;
			}
		} while (true);
	}

	private void decode() {
		encoding = findEncoding();
		log.info("\tChosen encoding is {} for {}", encoding, dictionaryId);
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
}

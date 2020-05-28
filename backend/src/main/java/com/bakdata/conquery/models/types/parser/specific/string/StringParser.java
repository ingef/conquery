package com.bakdata.conquery.models.types.parser.specific.string;

import java.util.Collections;
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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j @Getter
@ToString(callSuper = true)
public class StringParser extends Parser<Integer> {

	private DictionaryId dictionaryId = new DictionaryId(new DatasetId("null"), UUID.randomUUID().toString());
	private VarIntParser indexType = new VarIntParser(); 
	private Map<String, Integer> strings = new LinkedHashMap<>();
	private List<byte[]> decoded;
	private Encoding encoding;
	private String prefix = null;
	private String suffix = null;
	
	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return strings.computeIfAbsent(value, v-> {
			//new values

			//set longest common prefix and suffix
			prefix = Strings.commonPrefix(v, Objects.requireNonNullElse(prefix, v));
			suffix = Strings.commonSuffix(v, Objects.requireNonNullElse(suffix, v));

			//return next id
			return strings.size();
		});
	}
	
	@Override
	public Integer addLine(Integer v) {
		super.addLine(v);
		return indexType.addLine(v);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Decision<Integer, ?, ? extends CType<Integer, ?>> decideType() {
		Decision<Integer, Number, VarIntType> subDecision = indexType.findBestType();

		//check if a singleton type is enough
		if(strings.size() <= 1) {
			StringTypeSingleton type;
			if(strings.isEmpty()) {
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
		if(!StringUtils.isEmpty(prefix) || !StringUtils.isEmpty(suffix)) {
			log.debug("Reduced strings by the '{}' prefix and '{}' suffix", prefix, suffix);
			Map<String, Integer> oldStrings = strings;
			strings = Collections.synchronizedMap(new LinkedHashMap<>(oldStrings.size()));
			for(Entry<String, Integer> e : oldStrings.entrySet()) {
				strings.put(
					e.getKey().substring(
						prefix.length(),
						e.getKey().length()-suffix.length()
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
			"\tUsing {}(est. {}) for {}",
			guess.getGuesser().getClass().getSimpleName(),
			BinaryByteUnit.format(guess.estimate()),
			dictionaryId
		);
		
		AStringType<Number> result = guess.getType();
		//wrap in prefix suffix
		if(!StringUtils.isEmpty(prefix)) {
			result = new StringTypePrefix(result, prefix);
			setLineCounts(result);
		}
		if(!StringUtils.isEmpty(suffix)) {
			result = new StringTypeSuffix(result, suffix);
			setLineCounts(result);
		}
		return new Decision(
			guess.getTransformer(),
			result
		);
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
			if(bases.size()==1) {
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
			.map(e->encoding.decode(e.getKey()))
			.collect(Collectors.toList());
	}
}

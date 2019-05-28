package com.bakdata.conquery.models.types.parser.specific;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.models.types.specific.StringTypeDictionary;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.types.specific.StringTypePrefix;
import com.bakdata.conquery.models.types.specific.StringTypeSingleton;
import com.bakdata.conquery.models.types.specific.StringTypeSuffix;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import com.google.common.base.Strings;
import com.jakewharton.byteunits.BinaryByteUnit;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringParser extends Parser<Integer> {

	private DictionaryId dictionaryId = new DictionaryId(new DatasetId("null"), UUID.randomUUID().toString());
	private VarIntParser subType = new VarIntParser(); 
	private LinkedHashMap<String, Integer> strings = new LinkedHashMap<>();
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
		return subType.addLine(v);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Decision<Integer, ?, ? extends CType<Integer, ?>> decideType() {
		Decision<Integer, Number, VarIntType> subDecision = subType.findBestType();

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
			LinkedHashMap<String, Integer> oldStrings = strings;
			strings = new LinkedHashMap<>(oldStrings.size());
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
		

		//check if encoding
		Encoding encoding = findEncoding();
		log.debug("Chosen encoding is {}", encoding);
		
		//create base type
		AStringType<Number> result = createBaseType(encoding);
		if(!StringUtils.isEmpty(prefix)) {
			result = new StringTypePrefix<>(result, prefix);
			setLineCounts(result);
		}
		if(!StringUtils.isEmpty(suffix)) {
			result = new StringTypeSuffix<>(result, suffix);
			setLineCounts(result);
		}
		return new Decision<>(
			subDecision.getTransformer(),
			result
		);
	}

	public StringTypeEncoded createBaseType(Encoding encoding) {
		StringTypeDictionary type = new StringTypeDictionary(subType.decideType().getType());
		SuccinctTrie trie = new SuccinctTrie();
		for(String v: strings.keySet()) {
			trie.add(encoding.decode(v));
		}
		long trieSize = trie.estimateMemoryConsumption();
		long mapSize = MapDictionary.estimateMemoryConsumption(trie.size(), trie.getTotalBytesStored());

		if(trieSize < mapSize) {
			trie.compress();
			type.setDictionary(trie);
		}
		else {
			log.debug(
				"Using MapDictionary(est. {}) instead of Trie(est. {}) for {}",
				BinaryByteUnit.format((long)mapSize),
				BinaryByteUnit.format((long)trieSize),
				dictionaryId
			);
			MapDictionary map = new MapDictionary();
			for(String v : strings.keySet()) {
				map.add(encoding.decode(v));
			}
			type.setDictionary(map);
		}
		type.setDictionaryId(dictionaryId);
		setLineCounts(type);
		StringTypeEncoded result = new StringTypeEncoded(type, encoding);
		setLineCounts(result);
		return result;
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
}

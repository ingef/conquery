package com.bakdata.conquery.models.types.parser.specific;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.StringTypeVarInt;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.types.specific.VarIntType;

import lombok.NonNull;

public class StringParser extends Parser<Integer> {

	private DictionaryId dictionaryId = new DictionaryId(new DatasetId("null"), UUID.randomUUID().toString());
	private VarIntParser subType = new VarIntParser(); 
	private Dictionary dictionary = new Dictionary(dictionaryId);
	
	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return dictionary.add(value);
	}
	
	@Override
	public Integer addLine(Integer v) {
		super.addLine(v);
		return subType.addLine(v);
	}
	
	@Override
	protected Decision<Integer, Number, ? extends CType<Integer, Number>> decideType() {
		Decision<Integer, Number, VarIntType> subDecision = subType.findBestType();
		dictionary.tryCompress();
		if(dictionary.size() == 0) {
			return simpleDict(subDecision);
		}
		EnumSet<StringTypeEncoded.Encoding> bases = EnumSet.allOf(StringTypeEncoded.Encoding.class);

		for (String value : dictionary) {
			bases.removeIf(encoding -> !encoding.canDecode(value));
			if(bases.isEmpty())
				return simpleDict(subDecision);
		}

		if (!bases.isEmpty()) {
			Encoding encoding = bases.stream()
				.min(StringTypeEncoded.Encoding::compareTo)
				.orElseThrow(() -> new IllegalStateException("Bases not empty, but no valid minimum."));
			
			Dictionary newDictionary = new Dictionary(dictionaryId);
			StringTypeEncoded type = new StringTypeEncoded(
				subDecision.getType(),
				encoding
			);
			type.setDictionary(newDictionary);
			type.setDictionaryId(dictionaryId);
			
			return new Decision<>(
				new EncodedTransformer(dictionary, newDictionary, encoding, subDecision),
				type
			);
		}

		return simpleDict(subDecision);
	}
	
	public CType<Integer, Number> createSimpleType() {
		Decision<Integer, Number, VarIntType> subDecision = subType.findBestType();
		dictionary.tryCompress();
		return simpleDict(subDecision).getType();
	}

	private Decision<Integer, Number, ? extends CType<Integer, Number>> simpleDict(Decision<Integer, Number, VarIntType> subDecision) {
		StringTypeVarInt type = new StringTypeVarInt(subDecision.getType());
		type.setDictionary(dictionary);
		type.setDictionaryId(dictionaryId);
		return new Decision<>(subDecision.getTransformer(), type);
	}
	
	private static class EncodedTransformer extends Transformer<Integer, Number> {
		private final int[] cache;
		private final Dictionary dictionary;
		private final Encoding encoding;
		private final Decision<Integer, Number, VarIntType> subDecision;
		private Dictionary newDictionary;
		
		public EncodedTransformer(Dictionary dictionary, Dictionary newDictionary, Encoding encoding, Decision<Integer, Number, VarIntType> subDecision) {
			this.dictionary = dictionary;
			this.newDictionary = newDictionary;
			this.encoding = encoding;
			this.subDecision = subDecision;
			cache = new int[dictionary.size()];
			Arrays.fill(cache, -1);
		}

		@Override
		public Number transform(@NonNull Integer from) {
			int id = (Integer) from;
			int result = cache[id];
			if(result == -1) {
				String value = dictionary.getElement(id);
				result = newDictionary.add(encoding.decode(value));
				cache[id] = result;
			}
			return subDecision.getTransformer().transform(result);
		}
	}
}

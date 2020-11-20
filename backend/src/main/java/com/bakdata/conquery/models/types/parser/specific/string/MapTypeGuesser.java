package com.bakdata.conquery.models.types.parser.specific.string;

import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.types.specific.integer.VarIntType;
import com.bakdata.conquery.models.types.specific.integer.VarIntTypeInt;
import com.bakdata.conquery.models.types.specific.string.StringType;
import com.bakdata.conquery.models.types.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.types.specific.string.StringTypeEncoded;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MapTypeGuesser implements TypeGuesser {

	private final StringParser p;

	@Override
	public Guess createGuess() {
		VarIntType indexType = new VarIntTypeInt(0, Integer.MAX_VALUE, IntegerStore.create(p.getLines()));

		final MapDictionary dictionaryEntries = new MapDictionary(null, "");

		StringTypeDictionary type = new StringTypeDictionary(indexType, dictionaryEntries, dictionaryEntries.getName());
		long mapSize = MapDictionary.estimateMemoryConsumption(
				p.getStrings().size(),
				p.getDecoded().stream().mapToLong(s -> s.length).sum()
		);


		p.copyLineCounts(type);
		StringTypeEncoded result = new StringTypeEncoded(type, p.getEncoding());
		p.copyLineCounts(result);
		p.copyLineCounts(indexType);

		return new Guess(
				this,
				result,
				indexType.estimateMemoryConsumption(),
				mapSize
		) {
			@Override
			public StringType getType() {
				MapDictionary map = new MapDictionary(null, "");
				for (byte[] v : p.getDecoded()) {
					map.add(v);
				}
				type.setDictionary(map);
				return super.getType();
			}
		};
	}

}

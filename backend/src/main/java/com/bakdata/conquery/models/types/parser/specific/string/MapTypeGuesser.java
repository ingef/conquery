package com.bakdata.conquery.models.types.parser.specific.string;

import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.models.types.specific.StringTypeDictionary;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import com.bakdata.conquery.models.types.specific.VarIntType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MapTypeGuesser implements TypeGuesser {
	
	private final StringParser p;

	@Override
	public Guess createGuess() {
		Decision<Integer, Number, VarIntType> indexDecision = p.getIndexType().decideType();
		
		StringTypeDictionary type = new StringTypeDictionary(indexDecision.getType());
		long mapSize = MapDictionary.estimateMemoryConsumption(
			p.getStrings().size(),
			p.getDecoded().stream().mapToLong(s->s.length).sum()
		);

		type.setDictionaryId(p.getDictionaryId());
		p.setLineCounts(type);
		StringTypeEncoded result = new StringTypeEncoded(type, p.getEncoding());
		p.setLineCounts(result);
		p.setLineCounts(indexDecision.getType());
		
		return new Guess(
			this,
			result,
			indexDecision.getTransformer(),
			indexDecision.getType().estimateMemoryConsumption(),
			mapSize
		) {
			@Override
			public AStringType<Number> getType() {
				MapDictionary map = new MapDictionary();
				for(byte[] v : p.getDecoded()) {
					map.add(v);
				}
				type.setDictionary(map);
				return super.getType();
			}
		};
	}

}

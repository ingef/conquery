package com.bakdata.conquery.models.types.parser.specific.string;

import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.types.specific.string.StringType;
import com.bakdata.conquery.models.types.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.types.specific.string.StringTypeEncoded;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TrieTypeGuesser implements TypeGuesser {
	
	private final StringParser p;

	@Override
	public Guess createGuess() {
		Decision<VarIntType> indexDecision = p.getIndexType().decideType();
		
		StringTypeDictionary type = new StringTypeDictionary(indexDecision.getType());
		SuccinctTrie trie = new SuccinctTrie();
		for(byte[] v: p.getDecoded()) {
			trie.add(v);
		}
		long trieSize = trie.estimateMemoryConsumption();

		type.setDictionaryId(p.getDictionaryId());
		p.setLineCounts(type);
		StringTypeEncoded result = new StringTypeEncoded(type, p.getEncoding());
		p.setLineCounts(result);
		p.setLineCounts(indexDecision.getType());
		
		return new Guess(
			this,
			result,
			indexDecision.getType().estimateMemoryConsumption(),
			trieSize
		) {
			@Override
			public StringType getType() {
				trie.compress();
				type.setDictionary(trie);
				return super.getType();
			}
		};
	}

}

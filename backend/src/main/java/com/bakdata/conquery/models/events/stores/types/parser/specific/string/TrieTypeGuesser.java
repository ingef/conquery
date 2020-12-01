package com.bakdata.conquery.models.events.stores.types.parser.specific.string;

import com.bakdata.conquery.models.events.stores.types.specific.IntegerType;
import com.bakdata.conquery.models.events.stores.types.specific.string.StringType;
import com.bakdata.conquery.models.events.stores.types.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.events.stores.types.specific.string.StringTypeEncoded;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TrieTypeGuesser implements TypeGuesser {

	private final StringParser p;

	@Override
	public Guess createGuess() {
		// todo this is confusing and unnecessary

		IntegerType indexType = p.decideIndexType();

		SuccinctTrie trie = new SuccinctTrie(null, "");
		StringTypeDictionary type = new StringTypeDictionary(indexType, trie, trie.getName());

		for (byte[] v : p.getDecoded()) {
			trie.add(v);
		}


		p.copyLineCounts(type);
		StringTypeEncoded result = new StringTypeEncoded(type, p.getEncoding());
		p.copyLineCounts(result);
		p.copyLineCounts(indexType);

		return new Guess(
				this,
				result,
				indexType.estimateMemoryConsumption(),
				trie.estimateMemoryConsumption()
		) {
			@Override
			public StringType getType() {
				trie.compress();
				return super.getType();
			}
		};
	}

}

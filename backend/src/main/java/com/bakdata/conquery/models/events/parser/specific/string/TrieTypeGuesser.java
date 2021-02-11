package com.bakdata.conquery.models.events.parser.specific.string;

import com.bakdata.conquery.models.events.parser.specific.StringParser;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import lombok.RequiredArgsConstructor;

/**
 * Construct a {@link SuccinctTrie} and estimate it's memory usage. Return
 */
@RequiredArgsConstructor
public class TrieTypeGuesser extends StringTypeGuesser {

	private final StringParser p;

	@Override
	public Guess createGuess() {
		IntegerStore indexType = p.decideIndexType();

		SuccinctTrie trie = new SuccinctTrie(null, "");
		StringTypeDictionary type = new StringTypeDictionary(indexType, trie, trie.getName());

		for (byte[] v : p.getDecoded()) {
			trie.add(v);
		}


		StringTypeEncoded result = new StringTypeEncoded(type, p.getEncoding());

		return new Guess(
				result,
				indexType.estimateMemoryConsumptionBytes(),
				trie.estimateMemoryConsumption()
		) {
			@Override
			public StringStore getType() {
				trie.compress();
				return super.getType();
			}
		};
	}

}

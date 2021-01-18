package com.bakdata.conquery.models.events.parser.specific.string;

import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TrieTypeGuesser implements TypeGuesser {

	private final StringParser p;

	@Override
	public Guess createGuess() {
		ColumnStore<Long> indexType = p.decideIndexType();

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
				indexType.estimateMemoryConsumptionBytes(),
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

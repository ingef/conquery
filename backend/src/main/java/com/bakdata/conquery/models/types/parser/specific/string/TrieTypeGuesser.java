package com.bakdata.conquery.models.types.parser.specific.string;

import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.types.specific.VarIntTypeInt;
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
		// todo this is confusing and unnecessary
		VarIntType indexType = new VarIntTypeInt(0, Integer.MAX_VALUE, IntegerStore.create(p.getLines()));

		SuccinctTrie trie = new SuccinctTrie(null, p.getName());
		StringTypeDictionary type = new StringTypeDictionary(indexType, trie, null, trie.getName());

		for (byte[] v : p.getDecoded()) {
			trie.add(v);
		}


		p.setLineCounts(type);
		StringTypeEncoded result = new StringTypeEncoded(type, p.getEncoding());
		p.setLineCounts(result);
		p.setLineCounts(indexType);

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

package com.bakdata.conquery.util.dict;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor(onConstructor_ = @JsonCreator )
public class SerializedSuccinctTrie {
	private int nodeCount;
	private int entryCount;
	private int[] reverseLookup;
	private int[] parentIndex;
	private int[] lookup;
	private byte[] keyPartArray;
	private int[] selectZeroCache;
}

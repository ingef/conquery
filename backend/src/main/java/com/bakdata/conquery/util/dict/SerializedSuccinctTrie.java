package com.bakdata.conquery.util.dict;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class SerializedSuccinctTrie {
	private int nodeCount;
	private int entryCount;
	private int[] reverseLookup;
	private int[] parentIndex;
	private int[] lookup;
	private byte[] keyPartArray;
	private int[] selectZeroCache;
}

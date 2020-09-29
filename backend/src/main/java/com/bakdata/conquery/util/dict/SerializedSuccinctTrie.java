package com.bakdata.conquery.util.dict;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor(onConstructor_ = @JsonCreator )
public class SerializedSuccinctTrie {
	private String name;
	private DatasetId dataset;
	private int nodeCount;
	private int entryCount;
	private int[] reverseLookup;
	private int[] parentIndex;
	private int[] lookup;
	private byte[] keyPartArray;
	private int[] selectZeroCache;
	private long totalBytesStored;
}

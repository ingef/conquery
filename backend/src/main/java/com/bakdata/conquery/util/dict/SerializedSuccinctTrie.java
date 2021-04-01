package com.bakdata.conquery.util.dict;

import javax.inject.Inject;

import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor(onConstructor_ = @JsonCreator )
public class SerializedSuccinctTrie {
	private String name;
	@Inject
	private Dataset dataset;
	private int nodeCount;
	private int entryCount;
	private int[] reverseLookup;
	private int[] parentIndex;
	private int[] lookup;
	private byte[] keyPartArray;
	private int[] selectZeroCache;
	private long totalBytesStored;
}

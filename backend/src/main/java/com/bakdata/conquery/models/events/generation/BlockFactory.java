package com.bakdata.conquery.models.events.generation;

import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;

public abstract class BlockFactory {

	public abstract Bucket create(Import imp, List<Object[]> events);

	public abstract Bucket construct(int bucketNumber, Import imp, int numberOfEvents, int[] offsets);
}

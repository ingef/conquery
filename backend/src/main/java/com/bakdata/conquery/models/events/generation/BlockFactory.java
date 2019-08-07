package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.util.io.SmallIn;

import it.unimi.dsi.fastutil.ints.IntList;

public abstract class BlockFactory {

	public abstract Bucket create(Import imp, List<Object[]> events);

	public abstract Bucket construct(int bucketNumber, Import imp, int[] offsets);
	
	public Bucket readSingleValue(int bucketNumber, Import imp, InputStream inputStream) throws IOException {
		try (SmallIn input = new SmallIn(inputStream)){
			Bucket bucket = construct(bucketNumber, imp, new int[] {0});
			bucket.read(input);
			return bucket;
		}
	}
	
	public abstract Bucket adaptValuesFrom(int bucketNumber, Import outImport, Bucket value, PPHeader header);

	public abstract Bucket combine(IntList includedEntities, Bucket[] buckets);
}

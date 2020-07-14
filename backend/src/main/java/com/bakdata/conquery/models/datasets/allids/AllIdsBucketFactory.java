package com.bakdata.conquery.models.datasets.allids;

import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class AllIdsBucketFactory extends BlockFactory {

	@Override
	public AllIdsBucket create(Import imp, List<Object[]> events) {
		throw new IllegalStateException("Cannot directly create ALL_ID Buckets");
	}

	@Override
	public AllIdsBucket adaptValuesFrom(int bucketNumber, Import outImport, Bucket value, PreprocessedHeader header) {
		throw new IllegalStateException("Cannot directly create ALL_ID Buckets");
	}

	@Override
	public AllIdsBucket construct(int bucketNumber, Import imp, int[] offsets) {
		IntList entities = new IntArrayList(offsets.length);

		for (int index = 0; index < offsets.length; index++) {
			if(offsets[index] == -1) {
				continue;
			}

			entities.add(bucketNumber + index);
		}

		return new AllIdsBucket(imp, bucketNumber, entities);
	}



	@Override
	public AllIdsBucket combine(IntList includedEntities, Bucket[] buckets) {
		return new AllIdsBucket(buckets[0].getImp(), buckets[0].getBucket(), includedEntities);
	}

}

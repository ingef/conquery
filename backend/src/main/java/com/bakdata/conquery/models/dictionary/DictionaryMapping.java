package com.bakdata.conquery.models.dictionary;


import java.util.Arrays;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Namespace;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class DictionaryMapping {

	private final Dictionary sourceDictionary;
	private final Dictionary targetDictionary;

	private int[] source2TargetMap;
	private Range<Integer> newIds;
	private IntSet newBuckets = new IntOpenHashSet();
	private IntSet usedBuckets = new IntOpenHashSet();
	private int bucketSize = ConqueryConfig.getInstance().getCluster().getEntityBucketSize();

	public static DictionaryMapping create(Dictionary from, Dictionary to, Namespace namespace){
		DictionaryMapping mapping = new DictionaryMapping(from, to);

		mapping.mapValues(namespace);
		if(Arrays.stream(mapping.source2TargetMap).distinct().count() < mapping.source2TargetMap.length) {
			throw new IllegalStateException("Multiple source ids map to the same target");
		}

		return mapping;
	}

	private void mapValues(Namespace namespace) {
		source2TargetMap = new int[sourceDictionary.size()];

		for (int id = 0; id < sourceDictionary.size(); id++) {
			byte[] value = sourceDictionary.getElement(id);
			int targetId = targetDictionary.getId(value);
			//if id was unknown until now
			if (targetId == -1L) {
				targetId = targetDictionary.add(value);
				if (newIds == null) {
					newIds = Range.exactly(targetId);
				}
				else {
					newIds = newIds.span(Range.exactly(targetId));
				}
			}
			source2TargetMap[id] = targetId;
			
			int bucket = Entity.getBucket(targetId, bucketSize);
			usedBuckets.add(bucket);
			if (namespace.getResponsibleWorker(targetId) == null) {
				newBuckets.add(bucket);
			}
		}
	}

	public int source2Target(int sourceId) {
		return source2TargetMap[sourceId];
	}
}

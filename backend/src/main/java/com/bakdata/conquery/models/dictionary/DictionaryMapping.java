package com.bakdata.conquery.models.dictionary;


import java.util.Arrays;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.worker.Namespace;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Create a mapping from one {@link Dictionary} to the other (Map source to target). Adding all ids in target, not in source, to source. Additionally, gather all encountered Buckets (see {@link Namespace}) and all not yet used Buckets.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class DictionaryMapping {

	private final Dictionary sourceDictionary;
	private final Dictionary targetDictionary;

	private final int[] source2TargetMap;
	private final Range<Integer> newIds;
	private final IntSet usedBuckets;

	public static DictionaryMapping create(Dictionary from, Dictionary to, int entityBucketSize) {

		int[] source2TargetMap = new int[from.size()];
		Range<Integer> newIds = null;
		IntSet buckets = new IntOpenHashSet();

		for (int id = 0; id < from.size(); id++) {

			byte[] value = from.getElement(id);
			int targetId = to.getId(value);

			//if id was unknown until now
			if (targetId == -1L) {
				targetId = to.add(value);
				if (newIds == null) {
					newIds = Range.exactly(targetId);
				}
				else {
					newIds = newIds.span(Range.exactly(targetId));
				}
			}
			source2TargetMap[id] = targetId;

			int bucket = Entity.getBucket(targetId, entityBucketSize);
			buckets.add(bucket);
		}
		if (Arrays.stream(source2TargetMap).distinct().count() < source2TargetMap.length) {
			throw new IllegalStateException("Multiple source ids map to the same target");
		}

		return new DictionaryMapping(from, to, source2TargetMap, newIds, buckets);
	}

	public int source2Target(int sourceId) {
		return source2TargetMap[sourceId];
	}

	public int getNumberOfNewIds() {
		if (newIds == null) {
			return 0;
		}
		return newIds.getMax() - newIds.getMin() + 1;
	}
}

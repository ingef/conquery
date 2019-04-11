package com.bakdata.conquery.models.dictionary;


import java.util.Arrays;

import com.bakdata.conquery.models.common.Range;

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

	public static DictionaryMapping create(Dictionary from, Dictionary to){
		DictionaryMapping mapping = new DictionaryMapping(from, to);

		mapping.mapValues();
		if(Arrays.stream(mapping.source2TargetMap).distinct().count() < mapping.source2TargetMap.length) {
			throw new IllegalStateException("Multiple source ids map to the same target");
		}

		return mapping;
	}

	private void mapValues() {
		source2TargetMap = new int[sourceDictionary.size()];

		for (int id = 0; id < sourceDictionary.size(); id++) {
			byte[] value = sourceDictionary.getElementBytes(id);
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
		}
	}

	public int source2Target(int sourceId) {
		return source2TargetMap[sourceId];
	}
}

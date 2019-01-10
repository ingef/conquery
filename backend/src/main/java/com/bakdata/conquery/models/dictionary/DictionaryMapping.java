package com.bakdata.conquery.models.dictionary;

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

		return mapping;
	}

	private void mapValues() {
		source2TargetMap = new int[sourceDictionary.size()];

		for (int id = 0; id < sourceDictionary.size(); id++) {
			String value = sourceDictionary.getElement(id);
			int targetId = targetDictionary.getId(value);
			//if id was unknown until now
			if (targetId == -1L) {
				targetId = targetDictionary.add(value);
				if (newIds == null) {
					newIds = Range.exactly(id);
				}
				else {
					newIds = newIds.span(Range.exactly(id));
				}
			}
			source2TargetMap[id] = targetId;
		}
	}

	public int source2Target(int sourceId) {
		return source2TargetMap[sourceId];
	}
}

package com.bakdata.conquery.models.dictionary;


import java.util.Arrays;

import com.bakdata.conquery.models.events.stores.ColumnStore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Create a mapping from one {@link Dictionary} to the other (Map source to target). Adding all ids in target, not in source, to source.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
@ToString
public class DictionaryMapping {

	private final Dictionary sourceDictionary;
	private final Dictionary targetDictionary;

	@ToString.Exclude
	private final int[] source2TargetMap;

	private final int numberOfNewIds;

	public static DictionaryMapping create(Dictionary from, Dictionary to) {

		int[] source2TargetMap = new int[from.size()];
		int newIds = 0;

		for (int id = 0; id < from.size(); id++) {

			byte[] value = from.getElement(id);
			int targetId = to.getId(value);

			//if id was unknown until now
			if (targetId == -1L) {
				targetId = to.add(value);
				newIds++;
			}
			source2TargetMap[id] = targetId;

		}
		if (Arrays.stream(source2TargetMap).distinct().count() < source2TargetMap.length) {
			throw new IllegalStateException("Multiple source ids map to the same target");
		}

		return new DictionaryMapping(from, to, source2TargetMap, newIds);
	}

	public int source2Target(int sourceId) {
		return source2TargetMap[sourceId];
	}

	/**
	 * Mutably applies mapping to store.
	 */
	public void applyToStore(ColumnStore<Integer> from, ColumnStore<Long> to, long rows) {
		for (int row = 0; row < rows; row++) {
			if (!from.has(row)) {
				to.set(row, null);
				continue;
			}

			to.set(row, (long) source2Target(from.getString(row)));
		}
	}

}

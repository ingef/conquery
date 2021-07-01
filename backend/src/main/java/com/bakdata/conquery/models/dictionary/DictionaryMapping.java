package com.bakdata.conquery.models.dictionary;


import java.util.Arrays;

import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

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

	public static DictionaryMapping createAndImport(Dictionary from, Dictionary to) {

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

	public int target2Source(int targetId) {
		return ArrayUtils.indexOf(source2TargetMap, targetId);
	}

	/**
	 * Mutably applies mapping to store.
	 */
	public void applyToStore(StringStore from, IntegerStore to) {
		for (int event = 0; event < from.getLines(); event++) {
			if (!from.has(event)) {
				to.setNull(event);
				continue;
			}

			final int string = from.getString(event);

			to.setInteger(event, source2Target(string));
		}
	}

}

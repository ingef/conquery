package com.bakdata.conquery.models.dictionary;


import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
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
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class DictionaryMapping {

	@ToString.Include
	private final Dictionary sourceDictionary;

	@ToString.Include
	private final Dictionary targetDictionary;

	private final Int2IntMap source2Target;

	private final Int2IntMap target2Source;

	@ToString.Include
	private final int numberOfNewIds;

	public static DictionaryMapping createAndImport(Dictionary incoming, Dictionary into) {

		log.debug("Importing values from `{}` into `{}`", incoming, into);

		if (log.isTraceEnabled()) {
			incoming.iterator()
					.forEachRemaining(entry -> log.trace("{} {}=`{}`", incoming.getId(), entry.getId(), new String(entry.getValue())));
		}

		int newIds = 0;

		Int2IntMap source2Target = new Int2IntOpenHashMap(incoming.size());

		source2Target.defaultReturnValue(-1);

		Int2IntMap target2Source = new Int2IntOpenHashMap(incoming.size());

		target2Source.defaultReturnValue(-1);

		for (int id = 0; id < incoming.size(); id++) {

			byte[] value = incoming.getElement(id);
			int targetId = into.getId(value);

			//if id was unknown until now
			if (targetId == -1L) {
				targetId = into.add(value);
				newIds++;
			}

			if (log.isTraceEnabled()) {
				log.trace("Remapping(`{}` into `{}`): `{}` => `{}` ({} = {})", incoming, into, new String(value), new String(into.getElement(targetId)), id, targetId);
			}

			if (source2Target.put(id, targetId) != -1) {
				log.error("Multiple ids map to same target");
			}

			if (target2Source.put(targetId, id) != -1) {
				log.error("Multiple ids map to same target");
			}

		}

		return new DictionaryMapping(incoming, into, source2Target, target2Source, newIds);
	}

	public int source2Target(int sourceId) {
		return source2Target.get(sourceId);
	}

	public int target2Source(int targetId) {
		return target2Source.get(targetId);
	}

	public IntCollection source() {
		return source2Target.keySet();
	}

	public IntCollection target() {
		return source2Target.values();
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

			int value = source2Target(string);

			if (value == -1) {
				log.warn("Missing mapping for {}", string);
			}

			to.setInteger(event, value);
		}
	}

}

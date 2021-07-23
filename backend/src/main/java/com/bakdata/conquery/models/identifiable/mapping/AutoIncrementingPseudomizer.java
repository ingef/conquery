package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import lombok.RequiredArgsConstructor;

/**
 * Used to generate pseudonymized ids for every query if the user does
 * not have the rights to see the real data.
 */
@RequiredArgsConstructor
public class AutoIncrementingPseudomizer {
	private static final String ANONYMOUS_ID_PREFIX = "anon_"; // Abbreviation for anonymous

	private final Map<String, EntityPrintId> cachedIds = new ConcurrentHashMap<>();
	private final AtomicInteger pseudoIdPointer = new AtomicInteger(0);

	private final int size;
	private final int position;

	/**
	 * In the pseudo format the actual id columns are preserved but empty.
	 * Only the Pid Column is written with a new generated id.
	 */
	public EntityPrintId getPseudoId(String csvEntityId) {

		EntityPrintId pseudonym = cachedIds.computeIfAbsent(csvEntityId, this::createPseudonym);

		return pseudonym;
	}

	private EntityPrintId createPseudonym(String ignored) {
		final String name = ANONYMOUS_ID_PREFIX + pseudoIdPointer.getAndIncrement();
		final String[] parts = new String[size];

		parts[position] = name;

		return EntityPrintId.from(parts);
	}
}

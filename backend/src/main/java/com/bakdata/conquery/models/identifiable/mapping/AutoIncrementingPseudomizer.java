package com.bakdata.conquery.models.identifiable.mapping;

import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.query.results.EntityResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.RequiredArgsConstructor;

/**
 * Used to generate pseudonymized ids for every query if the user does
 * not have the rights to see the real data.
 */
@RequiredArgsConstructor
public class AutoIncrementingPseudomizer implements IdPrinter {
	private static final String ANONYMOUS_ID_PREFIX = "anon_"; // Abbreviation for anonymous

	private final Int2ObjectMap<EntityPrintId> cachedIds = new Int2ObjectAVLTreeMap<>();
	private final AtomicInteger pseudoIdPointer = new AtomicInteger(0);

	private final int size;
	private final int position;

	@Override
	public EntityPrintId createId(EntityResult entityResult) {
		return getPseudoId(entityResult.getEntityId());
	}

	/**
	 * In the pseudo format the actual id columns are preserved but empty.
	 * Only the Pid Column is written with a new generated id.
	 */
	public EntityPrintId getPseudoId(int csvEntityId) {
		return cachedIds.computeIfAbsent(csvEntityId, this::createPseudonym);
	}

	private EntityPrintId createPseudonym(int ignored) {
		final String name = ANONYMOUS_ID_PREFIX + pseudoIdPointer.getAndIncrement();
		final String[] parts = new String[size];

		parts[position] = name;

		return EntityPrintId.from(parts);
	}
}

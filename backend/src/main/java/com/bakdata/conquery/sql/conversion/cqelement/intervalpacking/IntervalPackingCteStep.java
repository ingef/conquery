package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IntervalPackingCteStep implements CteStep {

	PREVIOUS_END("previous_end", null),
	RANGE_INDEX("range_index", PREVIOUS_END),
	INTERVAL_COMPLETE("interval_complete", RANGE_INDEX);

	private static final Set<IntervalPackingCteStep> STEPS = Set.of(values());

	private final String suffix;
	private final IntervalPackingCteStep predecessor;

	public static SqlTables getTables(String label, String rootTable, NameGenerator nameGenerator) {
		return new SqlTables(label, STEPS, rootTable, nameGenerator);
	}

}

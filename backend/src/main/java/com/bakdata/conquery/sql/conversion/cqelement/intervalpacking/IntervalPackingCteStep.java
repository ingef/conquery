package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IntervalPackingCteStep implements CteStep {

	PREVIOUS_END("previous_end", null),
	RANGE_INDEX("range_index", PREVIOUS_END),
	INTERVAL_COMPLETE("interval_complete", RANGE_INDEX);

	private static final Set<IntervalPackingCteStep> STEPS = Set.of(values());

	private final String suffix;
	private final CteStep predecessor;

	/**
	 * Create {@link SqlTables} based on a preceding {@link QueryStep}, that must contain a validity date which shall be interval-packed.
	 */
	public static SqlTables createTables(QueryStep predecessor, Context context) {

		String rootTable = predecessor.getCteName();
		Set<CteStep> requiredSteps = context.getSqlDialect().supportsSingleColumnRanges()
									 ? Set.of(INTERVAL_COMPLETE)
									 : Set.of(values());

		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(requiredSteps, rootTable, context.getNameGenerator());
		Map<CteStep, CteStep> predecessorMap = CteStep.getDefaultPredecessorMap(requiredSteps);

		return new SqlTables(rootTable, cteNameMap, predecessorMap);
	}

	/**
	 * Create predecessor mappings for these interval packing {@link CteStep}s based on a preceding root step that must contain a validity date which
	 * shall be interval-packed.
	 */
	public static Map<CteStep, CteStep> getMappings(CteStep root, SqlDialect dialect) {
		if (dialect.supportsSingleColumnRanges()) {
			return Map.of(INTERVAL_COMPLETE, root);
		}
		Map<CteStep, CteStep> mappings = CteStep.getDefaultPredecessorMap(Set.of(values()));
		mappings.put(PREVIOUS_END, root);
		return mappings;
	}

}

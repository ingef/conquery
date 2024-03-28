package com.bakdata.conquery.sql.conversion.model;

import java.util.Map;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import lombok.Getter;

@Getter
public class ConceptConversionTables extends SqlTables {

	/**
	 * Stores the name of the predecessor of the last CTE these tables contain.
	 */
	private final String lastPredecessor;

	/**
	 * True if these tables contain interval packing CTEs {@link IntervalPackingCteStep}.
	 */
	private final boolean withIntervalPacking;

	public ConceptConversionTables(
			String rootTable,
			Map<CteStep, String> cteNameMap,
			Map<CteStep, CteStep> predecessorMap,
			String lastPredecessor,
			boolean containsIntervalPacking
	) {
		super(rootTable, cteNameMap, predecessorMap);
		this.lastPredecessor = lastPredecessor;
		this.withIntervalPacking = containsIntervalPacking;
	}

}

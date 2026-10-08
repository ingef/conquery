package com.bakdata.conquery.sql.compiler.ir;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Resolves CTE graph steps to their generated table names and predecessor tables. */
@RequiredArgsConstructor
public class SqlTables {

	@Getter
	private final String rootTable;
	private final Map<CteStep, String> cteNameMap;
	private final Map<CteStep, CteStep> predecessorMap;

	/** Allow backend adapters to attach framework-specific metadata without exposing the graph maps. */
	protected SqlTables(SqlTables tables) {
		this.rootTable = tables.rootTable;
		this.cteNameMap = tables.cteNameMap;
		this.predecessorMap = tables.predecessorMap;
	}

	/** Returns the generated CTE name for the supplied step. */
	public String cteName(CteStep cteStep) {
		return cteNameMap.get(cteStep);
	}

	/** Returns whether the supplied step participates in this CTE graph. */
	public boolean isRequiredStep(CteStep cteStep) {
		return cteNameMap.containsKey(cteStep);
	}

	/** Returns the predecessor CTE name, or the root table when the step has no mapped predecessor. */
	public String getPredecessor(CteStep cteStep) {
		CteStep predecessor = predecessorMap.get(cteStep);
		if (predecessor == null) {
			return rootTable;
		}
		return cteNameMap.get(predecessor);
	}
}

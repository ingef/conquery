package com.bakdata.conquery.sql.conversion.model;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SqlTables provide a mapping from {@link CteStep}s to their respective table names/cte names and from a {@link CteStep} to the respective preceding step
 * in the generated SQL query.
 */
@RequiredArgsConstructor
public class SqlTables {

	@Getter
	private final String rootTable;
	private final Map<CteStep, String> cteNameMap;
	private final Map<CteStep, CteStep> predecessorMap;

	/**
	 * @return The CTE name for a {@link CteStep}.
	 */
	public String cteName(CteStep cteStep) {
		return cteNameMap.get(cteStep);
	}

	/**
	 * @return True if the given {@link CteStep} is part of these {@link SqlTables}.
	 */
	public boolean isRequiredStep(CteStep cteStep) {
		return cteNameMap.containsKey(cteStep);
	}

	/**
	 * @return The name of the table the given {@link CteStep} will select from. If their exists no mapped preceding {@link CteStep} for the given
	 * {@link CteStep}, the root table is returned.
	 */
	public String getPredecessor(CteStep cteStep) {
		CteStep predecessor = predecessorMap.get(cteStep);
		if (predecessor == null) {
			return rootTable;
		}
		return cteNameMap.get(predecessor);
	}

}

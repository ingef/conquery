package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConceptTableNames {

	private final Map<CteStep, String> cteNames;
	private final String rootTable;

	public ConceptTableNames(String conceptLabel, String rootTable) {
		this.cteNames = Arrays.stream(CteStep.values()).collect(Collectors.toMap(
				Function.identity(),
				step -> "concept_%s%s".formatted(conceptLabel, step.suffix())
		));
		this.rootTable = rootTable;
	}

	/**
	 * @return The root table this concept refers to.
	 */
	public String rootTable() {
		return this.rootTable;
	}

	/**
	 * @return The table or CTE name for the given {@link CteStep}.
	 */
	public String tableNameFor(final CteStep suffix) {
		return cteNames.get(suffix);
	}

}

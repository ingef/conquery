package com.bakdata.conquery.sql.compiler.ir;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A logical step in a common-table-expression graph. */
public interface CteStep {

	String getSuffix();

	/**
	 * Maps every required step to its default predecessor. Values may be {@code null} for root steps.
	 */
	static Map<CteStep, CteStep> getDefaultPredecessorMap(Set<? extends CteStep> requiredSteps) {
		return requiredSteps.stream().collect(
				HashMap::new,
				(map, cteStep) -> map.put(cteStep, cteStep.getPredecessor()),
				Map::putAll
		);
	}

	/** Generates a unique CTE name for every required step using the supplied naming strategy. */
	static Map<CteStep, String> createCteNameMap(
			Set<? extends CteStep> requiredSteps,
			String label,
			BiFunction<CteStep, String, String> namingFunction
	) {
		return requiredSteps.stream().collect(
				Collectors.toMap(
						Function.identity(),
						step -> namingFunction.apply(step, label)
				)
		);
	}

	/** Returns this step's default predecessor, or {@code null} if it reads from the graph's root table. */
	default CteStep getPredecessor() {
		return null;
	}

	default String cteName(String nodeLabel) {
		return "%s-%s".formatted(nodeLabel, getSuffix());
	}
}

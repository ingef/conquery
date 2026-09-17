package com.bakdata.conquery.sql.conversion.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * A CteStep represents a common table expression.
 */
public interface CteStep {

	String getSuffix();

	/**
	 * Maps each given required step to its default predecessor (see {@link CteStep#getPredecessor()}. The map will contain all given required steps as keys,
	 * but values might be null.
	 */
	static Map<CteStep, CteStep> getDefaultPredecessorMap(Set<? extends CteStep> requiredSteps) {
		return requiredSteps.stream().collect(
				HashMap::new,
				(map, cteStep) -> map.put(cteStep, cteStep.getPredecessor()), // value might be null
				Map::putAll
		);
	}

	/**
	 * Generates a CTE name for each of the given required steps. Combines the given label with the CTE step suffix (@link CteStep#getSuffix()).
	 */
	static Map<CteStep, String> createCteNameMap(Set<? extends CteStep> requiredSteps, String label, NameGenerator nameGenerator) {
		return requiredSteps.stream().collect(
				Collectors.toMap(
						Function.identity(),
						step -> nameGenerator.cteStepName(step, label)
				));
	}

	@Nullable
	default CteStep getPredecessor() {
		return null;
	}

	default String cteName(String nodeLabel) {
		return "%s-%s".formatted(nodeLabel, getSuffix());
	}

}

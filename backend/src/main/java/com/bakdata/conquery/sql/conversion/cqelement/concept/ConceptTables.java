package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class ConceptTables {

	private final Map<CteStep, String> cteNames;
	private final String rootTable;

	public ConceptTables(String conceptLabel, Set<CteStep> requiredSteps, String rootTable) {
		this.cteNames = requiredSteps.stream()
									 .collect(Collectors.toMap(
											 Function.identity(),
											 step -> "concept_%s%s".formatted(conceptLabel, step.suffix())
									 ));
		this.rootTable = rootTable;
	}

	public boolean isRequiredStep(CteStep cteStep) {
		return this.cteNames.containsKey(cteStep);
	}

	/**
	 * @return The CTE name for this {@link CteStep}.
	 */
	public String cteName(CteStep cteStep) {
		return this.cteNames.get(cteStep);
	}

	/**
	 * @return The name of the table this {@link CteStep} will select from.
	 */
	public String getPredecessorTableName(CteStep cteStep) {
		return switch (cteStep) {
			case PREPROCESSING -> this.rootTable;
			case EVENT_FILTER -> this.cteNames.get(CteStep.PREPROCESSING);
			case AGGREGATION_SELECT -> this.cteNames.getOrDefault(CteStep.EVENT_FILTER, this.cteNames.get(CteStep.PREPROCESSING));
			case AGGREGATION_FILTER -> this.cteNames.get(CteStep.AGGREGATION_SELECT);
			case FINAL -> this.cteNames.getOrDefault(CteStep.AGGREGATION_FILTER, this.cteNames.get(CteStep.AGGREGATION_SELECT));
		};
	}

	/**
	 * Qualify a field for a {@link CteStep}.
	 * <p>
	 * For example, if you want to qualify a {@link Field} for the AGGREGATION_SELECT step,
	 * it's qualified on the EVENT_FILTER or PREPROCESSING_STEP depending on the presence of the respective step.
	 * See {@link ConceptTables#getPredecessorTableName(CteStep)}
	 *
	 * @param cteStep The {@link CteStep} you want to qualify the given field for.
	 * @param field   The field you want to qualify.
	 */
	@SuppressWarnings("unchecked")
	public <C> Field<C> qualifyOnPredecessorTableName(CteStep cteStep, Field<?> field) {
		return DSL.field(DSL.name(getPredecessorTableName(cteStep), field.getName()), (DataType<C>) field.getDataType());
	}

}

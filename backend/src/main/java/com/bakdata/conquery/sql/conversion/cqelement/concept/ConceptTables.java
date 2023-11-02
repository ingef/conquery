package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class ConceptTables {

	private final Map<ConceptCteStep, String> cteNames;
	private final String rootTable;

	public ConceptTables(String conceptLabel, Set<ConceptCteStep> requiredSteps, String rootTableName) {
		this.cteNames = requiredSteps.stream()
									 .collect(Collectors.toMap(
											 Function.identity(),
											 step -> step.cteName(conceptLabel)
									 ));
		this.rootTable = rootTableName;
	}

	public boolean isRequiredStep(ConceptCteStep conceptCteStep) {
		return this.cteNames.containsKey(conceptCteStep);
	}

	/**
	 * @return The CTE name for this {@link ConceptCteStep}.
	 */
	public String cteName(ConceptCteStep conceptCteStep) {
		return this.cteNames.get(conceptCteStep);
	}

	/**
	 * @return The name of the table this {@link ConceptCteStep} will select from.
	 */
	public String getPredecessorTableName(ConceptCteStep conceptCteStep) {
		return switch (conceptCteStep) {
			case PREPROCESSING -> this.rootTable;
			case EVENT_FILTER -> this.cteNames.get(ConceptCteStep.PREPROCESSING);
			case AGGREGATION_SELECT -> this.cteNames.getOrDefault(ConceptCteStep.EVENT_FILTER, this.cteNames.get(ConceptCteStep.PREPROCESSING));
			case AGGREGATION_FILTER -> this.cteNames.get(ConceptCteStep.AGGREGATION_SELECT);
			case FINAL -> this.cteNames.getOrDefault(ConceptCteStep.AGGREGATION_FILTER, this.cteNames.get(ConceptCteStep.AGGREGATION_SELECT));
		};
	}

	/**
	 * Qualify a field for a {@link ConceptCteStep}.
	 * <p>
	 * For example, if you want to qualify a {@link Field} for the AGGREGATION_SELECT step,
	 * it's qualified on the EVENT_FILTER or PREPROCESSING_STEP depending on the presence of the respective step.
	 * See {@link ConceptTables#getPredecessorTableName(ConceptCteStep)}
	 *
	 * @param conceptCteStep The {@link ConceptCteStep} you want to qualify the given field for.
	 * @param field   The field you want to qualify.
	 */
	@SuppressWarnings("unchecked")
	public <C> Field<C> qualifyOnPredecessorTableName(ConceptCteStep conceptCteStep, Field<?> field) {
		return DSL.field(DSL.name(getPredecessorTableName(conceptCteStep), field.getName()), (DataType<C>) field.getDataType());
	}

}

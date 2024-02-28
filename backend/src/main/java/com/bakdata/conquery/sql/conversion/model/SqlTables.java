package com.bakdata.conquery.sql.conversion.model;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import lombok.Getter;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * SqlTables provide a mapping from {@link CteStep}s to their respective table names/cte names in the generated SQL query.
 */
@Getter
public abstract class SqlTables<C extends CteStep> {

	private final Map<C, String> cteNames;
	private final String rootTable;

	public SqlTables(String nodeLabel, Set<C> requiredSteps, String rootTableName, NameGenerator nameGenerator) {
		this.cteNames = requiredSteps.stream()
									 .collect(Collectors.toMap(
											 Function.identity(),
											 step -> nameGenerator.cteStepName(step, nodeLabel)
									 ));
		this.rootTable = rootTableName;
	}

	/**
	 * @return The CTE name for a {@link CteStep}.
	 */
	public String cteName(CteStep cteStep) {
		return this.cteNames.get(cteStep);
	}

	/**
	 * @return True if the given {@link CteStep} is part of these {@link SqlTables}.
	 */
	public boolean isRequiredStep(CteStep cteStep) {
		return this.cteNames.containsKey(cteStep);
	}

	/**
	 * @return The name of the table the given {@link CteStep} will select from.
	 */
	public String getPredecessor(CteStep cteStep) {
		CteStep predecessor = cteStep.getPredecessor();
		while (!this.cteNames.containsKey(predecessor)) {
			if (predecessor == null) {
				return this.rootTable;
			}
			predecessor = predecessor.getPredecessor();
		}
		return this.cteNames.get(predecessor);
	}

	/**
	 * Qualify a field for a {@link CteStep}.
	 * <p>
	 * For example, if you want to qualify a {@link Field} for the AGGREGATION_SELECT step of {@link ConnectorCteStep},
	 * it's qualified on the EVENT_FILTER or PREPROCESSING_STEP depending on the presence of the respective step.
	 * See {@link SqlTables#getPredecessor(CteStep)}
	 *
	 * @param cteStep The {@link CteStep} you want to qualify the given field for.
	 * @param field   The field you want to qualify.
	 */
	@SuppressWarnings("unchecked")
	public <T> Field<T> qualifyOnPredecessor(CteStep cteStep, Field<?> field) {
		return DSL.field(DSL.name(getPredecessor(cteStep), field.getName()), (DataType<T>) field.getDataType());
	}
}

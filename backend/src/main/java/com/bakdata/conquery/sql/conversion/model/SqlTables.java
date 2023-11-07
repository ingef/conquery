package com.bakdata.conquery.sql.conversion.model;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import lombok.Getter;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Getter
public abstract class SqlTables<C extends CteStep> {

	protected final Map<C, String> cteNames;
	protected final String rootTable;

	public SqlTables(String nodeLabel, Set<C> requiredSteps, String rootTableName) {
		this.cteNames = requiredSteps.stream()
									 .collect(Collectors.toMap(
											 Function.identity(),
											 step -> step.cteName(nodeLabel)
									 ));
		this.rootTable = rootTableName;
	}

	public SqlTables(String rootTable, Map<C, String> cteNames) {
		this.rootTable = rootTable;
		this.cteNames = cteNames;
	}

	/**
	 * @return The CTE name for a {@link CteStep}.
	 */
	public String cteName(CteStep cteStep) {
		return this.cteNames.get(cteStep);
	}

	/**
	 * @return The name of the table this {@link CteStep} will select from.
	 */
	public String getPredecessorTableName(CteStep cteStep) {
		CteStep predecessor = cteStep.predecessor();
		while (!this.cteNames.containsKey(predecessor)) {
			if (predecessor == null) {
				return this.rootTable;
			}
			predecessor = predecessor.predecessor();
		}
		return this.cteNames.get(predecessor);
	}

	/**
	 * Qualify a field for a {@link CteStep}.
	 * <p>
	 * For example, if you want to qualify a {@link Field} for the AGGREGATION_SELECT step of {@link ConceptCteStep},
	 * it's qualified on the EVENT_FILTER or PREPROCESSING_STEP depending on the presence of the respective step.
	 * See {@link ConceptTables#getPredecessorTableName(CteStep)}
	 *
	 * @param cteStep The {@link CteStep} you want to qualify the given field for.
	 * @param field   The field you want to qualify.
	 */
	@SuppressWarnings("unchecked")
	public <T> Field<T> qualifyOnPredecessorTableName(CteStep cteStep, Field<?> field) {
		return DSL.field(DSL.name(getPredecessorTableName(cteStep), field.getName()), (DataType<T>) field.getDataType());
	}
}

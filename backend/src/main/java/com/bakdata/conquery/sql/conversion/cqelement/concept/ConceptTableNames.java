package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class ConceptTableNames {

	private final Map<CteStep, String> cteNames;
	private final String rootTable;

	public ConceptTableNames(String conceptLabel, String rootTable) {
		this.cteNames = Arrays.stream(CteStep.values())
							  .collect(Collectors.toMap(
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
	public String tableNameFor(CteStep cteStep) {
		return cteNames.get(cteStep);
	}

	/**
	 * Qualify any field on the table name of a {@link CteStep}.
	 *
	 * @param cteStep The {@link CteStep} you want to qualify the given field onto.
	 * @param field The field you want to qualify with the given cteStep table name.
	 */
	@SuppressWarnings("unchecked")
	public <C> Field<C> qualify(CteStep cteStep, Field<?> field) {
		if (field.getDataType().equals(SQLDataType.VARCHAR)) {
			// need to handle String separately because the generic approach leads to type inference issues
			return DSL.field(DSL.name(tableNameFor(cteStep), field.getName()), (Class<C>) String.class);
		} else {
			return DSL.field(DSL.name(tableNameFor(cteStep), field.getName()), (DataType<C>) field.getDataType());
		}
	}

}

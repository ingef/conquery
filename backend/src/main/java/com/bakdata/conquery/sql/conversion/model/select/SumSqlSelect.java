package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.NumberMapUtil;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
@EqualsAndHashCode
public class SumSqlSelect implements SqlSelect {

	Field<? extends Number> sumColumn;
	Field<? extends Number> subtractColumn;
	List<SqlSelect> preprocessingSelects;
	String alias;

	private SumSqlSelect(Column column, Column subtractColumn, String alias, ConceptTables conceptTables) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(column.getType());

		this.preprocessingSelects = new ArrayList<>();
		String predecessorTableName = conceptTables.getPredecessorTableName(ConceptCteStep.PREPROCESSING);

		// sum column
		SqlSelect sumColumnSelect = new ExtractingSqlSelect<>(predecessorTableName, column.getName(), numberClass);
		this.preprocessingSelects.add(sumColumnSelect);
		this.sumColumn = conceptTables.qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, sumColumnSelect.aliased());

		// optional diffsum column
		if (subtractColumn == null) {
			this.subtractColumn = null;
		}
		else {
			SqlSelect subtractColumnRootSelect = new ExtractingSqlSelect<>(predecessorTableName, subtractColumn.getName(), numberClass);
			this.preprocessingSelects.add(subtractColumnRootSelect);
			this.subtractColumn = conceptTables.qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, subtractColumnRootSelect.aliased());
		}

		this.alias = alias;
	}

	public static SumSqlSelect create(SumSelect sumSelect, String alias, ConceptTables conceptTables) {
		return new SumSqlSelect(sumSelect.getColumn(), sumSelect.getSubtractColumn(), alias, conceptTables);
	}

	public static SumSqlSelect create(SumFilter<?> sumFilter, String alias, ConceptTables conceptTables) {
		return new SumSqlSelect(sumFilter.getColumn(), sumFilter.getSubtractColumn(), alias, conceptTables);
	}

	@Override
	public Field<BigDecimal> select() {
		Field<? extends Number> columnToSum = this.sumColumn;
		if (subtractColumn != null) {
			columnToSum = this.sumColumn.minus(this.subtractColumn);
		}
		return DSL.sum(columnToSum)
				  .as(alias);
	}

	@Override
	public Field<BigDecimal> aliased() {
		return DSL.field(alias, BigDecimal.class);
	}

	@Override
	public List<String> columnNames() {
		if (subtractColumn == null) {
			return List.of(sumColumn.getName());
		}
		return List.of(sumColumn.getName(), subtractColumn.getName());
	}

}

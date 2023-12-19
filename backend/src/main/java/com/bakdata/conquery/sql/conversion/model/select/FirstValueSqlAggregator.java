package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import lombok.Value;
import org.jooq.Field;

@Value
public class FirstValueSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	Filters filters;

	private FirstValueSqlAggregator(
			Column column,
			String alias,
			Optional<ColumnDateRange> validityDate,
			SqlTables<ConceptCteStep> conceptTables,
			SqlFunctionProvider functionProvider
	) {
		String rootTableName = conceptTables.getPredecessor(ConceptCteStep.PREPROCESSING);
		String columnName = column.getName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		List<Field<?>> validityDateFields =
				validityDate.map(_validityDate -> _validityDate.qualify(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)))
							.map(ColumnDateRange::toFields)
							.orElse(Collections.emptyList());
		Field<?> qualifiedRootSelect = rootSelect.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<?> firstGroupBy = new FieldWrapper<>(functionProvider.first(qualifiedRootSelect, validityDateFields).as(alias), columnName);

		ExtractingSqlSelect<?> finalSelect = firstGroupBy.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.FINAL));

		this.sqlSelects = SqlSelects.builder()
									.preprocessingSelect(rootSelect)
									.aggregationSelect(firstGroupBy)
									.finalSelect(finalSelect)
									.build();

		this.filters = Filters.builder().build();
	}

	public static FirstValueSqlAggregator create(FirstValueSelect firstValueSelect, SelectContext selectContext) {
		return new FirstValueSqlAggregator(
				firstValueSelect.getColumn(),
				selectContext.getNameGenerator().selectName(firstValueSelect),
				selectContext.getValidityDate(),
				selectContext.getConceptTables(),
				selectContext.getParentContext().getSqlDialect().getFunctionProvider()
		);
	}

}

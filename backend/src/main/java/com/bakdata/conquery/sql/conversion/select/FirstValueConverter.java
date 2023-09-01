package com.bakdata.conquery.sql.conversion.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.FirstValueGroupBy;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class FirstValueConverter implements SelectConverter<FirstValueSelect> {

	@Override
	public SqlSelects convert(FirstValueSelect convert, SelectContext context) {

		ExtractingSelect<Object> rootSelect = new ExtractingSelect<>(
				context.getTables().rootTable(),
				convert.getColumn().getName(),
				Object.class
		);

		List<Field<Object>> validityDateFields = context.getValidityDate()
														.map(ColumnDateRange::toFields)
														.orElse(List.of(DSL.field("1")));

		FirstValueGroupBy firstValueGroupBy = new FirstValueGroupBy(
				rootSelect.alias(),
				validityDateFields,
				context.getParentContext().getSqlDialect().getFunction()
		);

		ExtractingSelect<Object> finalSelect = new ExtractingSelect<>(
				context.getTables().tableNameFor(CteStep.AGGREGATION_SELECT),
				firstValueGroupBy.alias().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(firstValueGroupBy))
						 .forAggregationFilterStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<FirstValueSelect> getConversionClass() {
		return FirstValueSelect.class;
	}

}

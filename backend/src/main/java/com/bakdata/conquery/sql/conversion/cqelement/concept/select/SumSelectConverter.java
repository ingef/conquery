package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.SumSqlSelect;
import org.jooq.Field;

public class SumSelectConverter implements SelectConverter<SumSelect> {

	@Override
	public SqlSelects convert(SumSelect sumSelect, SelectContext context) {

		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumSelect.getColumn().getType());

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				sumSelect.getColumn().getName(),
				numberClass
		);

		Field<? extends Number> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		SumSqlSelect sumGroupBy = new SumSqlSelect(qualifiedRootSelect, sumSelect.getName());

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				sumSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				sumGroupBy.aliased().getName(),
				numberClass
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(sumGroupBy))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends SumSelect> getConversionClass() {
		return SumSelect.class;
	}

}

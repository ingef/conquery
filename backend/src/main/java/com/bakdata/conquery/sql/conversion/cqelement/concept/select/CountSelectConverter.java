package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class CountSelectConverter implements SelectConverter<CountSelect> {

	@Override
	public SqlSelects convert(CountSelect countSelect, SelectContext context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				countSelect.getColumn().getName(),
				Object.class
		);

		CountSqlSelect.CountType countType = CountSqlSelect.CountType.fromBoolean(countSelect.isDistinct());
		Field<Object> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		String alias = context.getNameGenerator().selectName(countSelect);
		CountSqlSelect countSqlSelect = new CountSqlSelect(qualifiedRootSelect, alias, countType);

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				countSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				countSqlSelect.aliased().getName(),
				Integer.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(countSqlSelect))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends CountSelect> getConversionClass() {
		return CountSelect.class;
	}

}

package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.PrefixSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitExtractingSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.PrefixSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class PrefixSelectConverter implements SelectConverter<PrefixSelect> {

	@Override
	public SqlSelects convert(PrefixSelect prefixSelect, SelectContext context) {

		SqlSelect rootSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.PREPROCESSING),
				prefixSelect.getColumn().getName(),
				String.class
		);

		Field<Object> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		PrefixSqlSelect prefixGroupBy = new PrefixSqlSelect(
			qualifiedRootSelect, 
			prefixSelect.getPrefix(),
			context.getParentContext().getSqlDialect().getFunctionProvider(),
			context.getNameGenerator().selectName(prefixSelect)
		);

		ExplicitSelect finalSelect = ExplicitExtractingSelect.fromSelect(
				prefixSelect,
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				prefixGroupBy.aliased().getName(),
				String.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(prefixGroupBy))
						 .forFinalStep(List.of(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends PrefixSelect> getConversionClass() {
		return PrefixSelect.class;
	}

}

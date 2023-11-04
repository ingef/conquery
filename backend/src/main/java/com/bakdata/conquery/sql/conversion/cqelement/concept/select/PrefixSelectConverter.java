package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.PrefixSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
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

		Field<String> qualifiedRootSelect = context.getConceptTables().qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		PrefixSqlSelect prefixGroupBy = new PrefixSqlSelect(
			qualifiedRootSelect, 
			prefixSelect.getPrefix(),
			context.getParentContext().getSqlDialect().getFunctionProvider(),
			prefixSelect.getName()
		);

		ExtractingSqlSelect<String> finalSelect = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessorTableName(ConceptCteStep.FINAL),
				prefixGroupBy.aliased().getName(),
				String.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(rootSelect))
						 .forAggregationSelectStep(Collections.singletonList(prefixGroupBy))
						 .forFinalStep(Collections.singletonList(finalSelect))
						 .build();
	}

	@Override
	public Class<? extends PrefixSelect> getConversionClass() {
		return PrefixSelect.class;
	}

}

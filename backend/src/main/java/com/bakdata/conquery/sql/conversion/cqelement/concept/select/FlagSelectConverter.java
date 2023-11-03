package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.FlagSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptTables;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FlagSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Field;

public class FlagSelectConverter implements SelectConverter<FlagSelect> {

	@Override
	public SqlSelects convert(FlagSelect flagSelect, SelectContext context) {

		ConceptTables conceptTables = context.getConceptTables();

		String rootTable = conceptTables.getPredecessorTableName(ConceptCteStep.PREPROCESSING);
		Map<String, SqlSelect> rootSelects =
				flagSelect.getFlags()
						  .entrySet().stream()
						  .collect(Collectors.toMap(
								  Map.Entry::getKey,
								  entry -> new ExtractingSqlSelect<>(rootTable, entry.getValue().getName(), Boolean.class)
						  ));

		Map<String, Field<Boolean>> flagFieldsMap =
				rootSelects.entrySet().stream()
						   .collect(Collectors.toMap(
								   Map.Entry::getKey,
								   entry -> conceptTables.qualifyOnPredecessorTableName(ConceptCteStep.AGGREGATION_SELECT, entry.getValue().aliased())
						   ));
		String alias = flagSelect.getName();
		FlagSqlSelect flagSqlSelect = new FlagSqlSelect(flagFieldsMap, alias);

		String finalStepPredecessorTable = conceptTables.getPredecessorTableName(ConceptCteStep.FINAL);
		ExtractingSqlSelect<String> flagSelectReference = new ExtractingSqlSelect<>(
				finalStepPredecessorTable,
				flagSqlSelect.aliased().getName(),
				String.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(rootSelects.values().stream().toList())
						 .forAggregationSelectStep(List.of(flagSqlSelect))
						 .forFinalStep(List.of(flagSelectReference))
						 .build();
	}

	@Override
	public Class<? extends FlagSelect> getConversionClass() {
		return FlagSelect.class;
	}

}

package com.bakdata.conquery.sql.conversion.model.select;

import java.sql.Date;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.QuarterSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.google.common.base.Preconditions;
import org.jooq.Field;

public class QuarterSelectConverter implements SelectConverter<QuarterSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(QuarterSelect select, SelectContext<ConnectorSqlTables> selectContext) {

		FieldWrapper<String> quarterAggregation = createQuarterAggregation(select, selectContext);
		ExtractingSqlSelect<String> finalSelect = quarterAggregation.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .eventDateSelect(quarterAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(QuarterSelect select, SelectContext<ConceptSqlTables> selectContext) {

		FieldWrapper<String> quarterAggregation = createQuarterAggregation(select, selectContext);
		ExtractingSqlSelect<String> finalSelect = quarterAggregation.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));

		return ConceptSqlSelects.builder()
								.eventDateSelect(quarterAggregation)
								.finalSelect(finalSelect)
								.build();
	}

	private static FieldWrapper<String> createQuarterAggregation(QuarterSelect select, SelectContext<?> selectContext) {

		Preconditions.checkArgument(selectContext.getValidityDate().isPresent(), "Can't convert a QuarterSelect without a validity date being present");
		String predecessorCteName = selectContext.getTables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS);
		ColumnDateRange qualifiedValidityDate = selectContext.getValidityDate().get().qualify(predecessorCteName);

		Field<Date> sampledDate = selectContext.getDialectBundle()
											 .getStratificationFunctions()
											 .indexSelectorField(select.getSample(), qualifiedValidityDate);
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		String alias = selectContext.getNameGenerator().selectName(select);
		Field<String> quarter = functionProvider.yearQuarter(sampledDate).as(alias);

		return new FieldWrapper<>(quarter);
	}
}

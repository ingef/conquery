package com.bakdata.conquery.sql.conversion.dialect.pg;

import static org.jooq.impl.DSL.field;

import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SingleColumnSqlSelect;

public class PgDistinctSelectConverter implements SelectConverter<DistinctSelect> {


	@Override
	public ConnectorSqlSelects connectorSelect(DistinctSelect distinctSelect, SelectContext<ConnectorSqlTables> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(distinctSelect);

		ConnectorSqlTables tables = selectContext.getTables();
		SingleColumnSqlSelect preprocessingSelect =
				MappableSingleColumnSelect.getSubstringSelect(distinctSelect.getColumn().get(), distinctSelect.getSubstringRange(), selectContext, alias);

		String eventFilterTable = selectContext.getTables().cteName(ConceptCteStep.EVENT_FILTER);
		SingleColumnSqlSelect qualified = preprocessingSelect.qualify(eventFilterTable);

		FieldWrapper<?> grouped = new FieldWrapper<>(field("array_agg(DISTINCT {0})", Object.class, qualified.select()).as(alias), qualified.select().getName());

		SingleColumnSqlSelect finalSelect = grouped.qualify(tables.cteName(ConceptCteStep.AGGREGATION_SELECT));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(preprocessingSelect)
								  .aggregationSelect(grouped)
								  .finalSelect(finalSelect)
								  .build();
	}


}

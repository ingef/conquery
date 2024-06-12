package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;

public class ExistsSelectConverter implements SelectConverter<ExistsSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(ExistsSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {
		ExistsSqlSelect existsSqlSelect = createExistsSelect(select, selectContext);
		return ConnectorSqlSelects.builder()
								  .finalSelect(existsSqlSelect)
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(ExistsSelect select, SelectContext<TreeConcept, ConceptSqlTables> selectContext) {
		ExistsSqlSelect existsSqlSelect = createExistsSelect(select, selectContext);
		return ConceptSqlSelects.builder()
								.finalSelect(existsSqlSelect)
								.build();
	}

	private static ExistsSqlSelect createExistsSelect(ExistsSelect select, SelectContext<?, ?> selectContext) {
		String alias = selectContext.getNameGenerator().selectName(select);
		return new ExistsSqlSelect(alias);
	}
}

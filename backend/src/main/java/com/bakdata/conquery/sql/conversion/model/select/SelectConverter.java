package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;

public interface SelectConverter<S extends Select> {

	default ConnectorSqlSelects connectorSelect(S select, SelectContext<ConnectorSqlTables> selectContext) {
		throw new UnsupportedOperationException("Conversion of Select %s not implemented on Connector-level".formatted(select.getClass()));
	}

	default ConceptSqlSelects conceptSelect(S select, SelectContext<ConceptSqlTables> selectContext) {
		throw new UnsupportedOperationException("Conversion of Select %s not implemented or not possible on Concept-level".formatted(select.getClass()));
	}

}

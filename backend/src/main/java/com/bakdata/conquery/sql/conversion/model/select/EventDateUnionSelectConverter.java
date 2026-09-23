package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;

public class EventDateUnionSelectConverter implements SelectConverter<EventDateUnionSelect> {
	@Override
	public ConnectorSqlSelects connectorSelect(EventDateUnionSelect select, SelectContext<ConnectorSqlTables> context) {
		return ResolvedSelectAdapter.connectorSelect(new BuiltInSelects.EventDateUnion(select.getName()), select.getName(), context);
	}

	@Override
	public ConceptSqlSelects conceptSelect(EventDateUnionSelect select, SelectContext<ConceptSqlTables> context) {
		return ResolvedSelectAdapter.conceptSelect(new BuiltInSelects.EventDateUnion(select.getName()), select.getName(), context);
	}
}

package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;

public class EventDurationSumSelectConverter implements SelectConverter<EventDurationSumSelect> {
	@Override
	public ConnectorSqlSelects connectorSelect(EventDurationSumSelect select, SelectContext<ConnectorSqlTables> context) {
		return ResolvedSelectAdapter.connectorSelect(new BuiltInSelects.EventDurationSum(select.getName()), select.getName(), context);
	}

	@Override
	public ConceptSqlSelects conceptSelect(EventDurationSumSelect select, SelectContext<ConceptSqlTables> context) {
		return ResolvedSelectAdapter.conceptSelect(new BuiltInSelects.EventDurationSum(select.getName()), select.getName(), context);
	}
}

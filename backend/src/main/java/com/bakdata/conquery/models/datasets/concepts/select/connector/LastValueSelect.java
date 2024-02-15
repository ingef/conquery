package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.LastValueAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.aggregator.LastValueSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "LAST", base = Select.class)
public class LastValueSelect extends MappableSingleColumnSelect {

	@JsonCreator
	public LastValueSelect(@NsIdRef Column column,
						   @NsIdRef InternToExternMapper mapping) {
		super(column, mapping);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new LastValueAggregator<>(getColumn());
	}

	@Override
	public SqlSelects convertToSqlSelects(SelectContext selectContext) {
		return LastValueSqlAggregator.create(this, selectContext).getSqlSelects();
	}

}

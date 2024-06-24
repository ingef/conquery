package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.LastValueAggregator;
import com.bakdata.conquery.sql.conversion.model.select.LastValueSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.model.aggregator.LastValueSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "LAST", base = Select.class)
public class LastValueSelect extends MappableSingleColumnSelect {

	@JsonCreator
	public LastValueSelect(
			ColumnId column,
			InternToExternMapperId mapping
	) {
		super(column, mapping);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new LastValueAggregator<>(getColumn().resolve());
	}

	@Override
	public SelectConverter<LastValueSelect> createConverter() {
		return new LastValueSelectConverter();
	}
}

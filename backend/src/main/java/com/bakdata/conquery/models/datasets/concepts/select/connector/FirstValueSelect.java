package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.FirstValueAggregator;
import com.bakdata.conquery.sql.conversion.model.select.FirstValueSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "FIRST", base = Select.class)
public class FirstValueSelect extends MappableSingleColumnSelect {

	@JsonCreator
	public FirstValueSelect(
			ColumnId column,
			InternToExternMapperId mapping
	) {
		super(column, mapping);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new FirstValueAggregator<>(getColumn().resolve());
	}

	@Override
	public SelectConverter<FirstValueSelect> createConverter() {
		return new FirstValueSelectConverter();
	}
}

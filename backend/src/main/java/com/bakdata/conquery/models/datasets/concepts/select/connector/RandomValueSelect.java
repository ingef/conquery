package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.RandomValueAggregator;
import com.bakdata.conquery.sql.conversion.model.select.RandomValueSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "RANDOM", base = Select.class)
public class RandomValueSelect extends MappableSingleColumnSelect {
	@JsonCreator
	public RandomValueSelect(
			ColumnId column,
			InternToExternMapperId mapping, Range.IntegerRange substring
	) {
		super(column, mapping, substring);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new RandomValueAggregator<>(getColumn().resolve(), getSubstringRange());
	}

	@Override
	public SelectConverter<RandomValueSelect> createConverter() {
		return new RandomValueSelectConverter();
	}
}

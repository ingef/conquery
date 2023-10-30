package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.FlagsAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Implements a MultiSelect type filter, where an event can meet multiple criteria (as opposed to {@link MultiSelectFilter} which is restricted to one value per event).
 * This is achieved by using multiple {@link com.bakdata.conquery.models.types.ResultType.BooleanT} columns, each defining if one property is met or not.
 *
 * The selected flags are logically or-ed.
 */
@Getter
@CPSType(base = Select.class, id = "FLAGS")
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@ToString
public class FlagSelect extends Select {

	@NsIdRefCollection
	private final Map<String, Column> flags;


	@Override
	public List<Column> getRequiredColumns() {
		return new ArrayList<>(flags.values());
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new FlagsAggregator(flags);
	}


	@JsonIgnore
	@ValidationMethod(message = "Columns must be unique.")
	public boolean isAllColumnsOfSameTable() {
		return flags.values().stream().distinct().count() == flags.size();
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns must be BOOLEAN.")
	public boolean isAllColumnsBoolean() {
		return flags.values().stream().map(Column::getType).allMatch(MajorTypeId.BOOLEAN::equals);
	}
}

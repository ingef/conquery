package com.bakdata.conquery.models.datasets.concepts.select.concept.specific;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@CPSType(id = "EXISTS", base = Select.class)
public class ExistsSelect extends UniversalSelect {

	@JsonIgnore
	@Getter(lazy = true)
	private final Set<Table> requiredTables = collectRequiredTables();

	@Override
	public ExistsAggregator createAggregator() {
		return new ExistsAggregator(getRequiredTables());
	}

	@Override
	public SelectConverter<ExistsSelect> createConverter() {
		return new ExistsSelectConverter();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.Primitive.BOOLEAN;
	}

	private Set<Table> collectRequiredTables() {
		return this.getHolder().findConcept().getConnectors().stream().map(Connector::getResolvedTable).collect(Collectors.toSet());
	}
}

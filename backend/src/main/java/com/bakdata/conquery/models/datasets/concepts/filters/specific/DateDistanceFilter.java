package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.ColumnUtils;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.EventFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.event.DateDistanceFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.sql.conversion.model.aggregator.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@Slf4j
@CPSType(id = "DATE_DISTANCE", base = Filter.class)
public class DateDistanceFilter extends EventFilter<Range.LongRange> {

	@Valid
	@NotNull
	private ColumnId column;

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(getColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		return ColumnUtils.assertValidColumnTypes(getColumn(), Set.of(MajorTypeId.DATE));
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		MajorTypeId type = getColumn().resolve().getType();
		if (type != MajorTypeId.DATE) {
			throw new ConceptConfigurationException(getConnector(), "DATE_DISTANCE filter is incompatible with columns of type " + type);
		}

		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
	}

	@Override
	public EventFilterNode<?> createFilterNode(Range.LongRange value) {
		return new DateDistanceFilterNode(getColumn().resolve(), timeUnit, value);
	}

	@Override
	public FilterConverter<DateDistanceFilter, Range.LongRange> createConverter() {
		return new DateDistanceSqlAggregator();
	}
}

package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends Filter<Range.LongRange> {

	@Valid
	@NotNull
	@JsonAlias("column")
	private final ColumnId dateRangeColumn;

	@Valid
	@Nullable
	private final List<ColumnId> distinctBy;

	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE_RANGE);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		MajorTypeId type = getDateRangeColumn().resolve().getType();
		if (type != MajorTypeId.DATE_RANGE) {
			throw new ConceptConfigurationException(getConnector(), "DURATION_SUM filter is incompatible with columns of type "
																	+ type
			);
		}

		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(0);
	}

	@JsonIgnore
	private boolean hasDistinct() {
		return distinctBy != null && !distinctBy.isEmpty();
	}

	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		ColumnAggregator<?> aggregator = new DurationSumAggregator(getDateRangeColumn().resolve());

		if (hasDistinct()) {
			aggregator = new DistinctValuesWrapperAggregator<>(aggregator, distinctBy.stream().map(ColumnId::resolve).toList());
		}

		return new RangeFilterNode(value, aggregator);
	}

	@Override
	public List<ColumnId> getRequiredColumns() {
		List<ColumnId> required = new ArrayList<>();

		if (hasDistinct()) {
			required.addAll(distinctBy);
		}

		required.add(dateRangeColumn);
		return required;

	}

	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		final Column resolved = getDateRangeColumn().resolve();
		final boolean acceptable = getAcceptedColumnTypes().contains(resolved.getType());

		if (!acceptable) {
			log.error("Column[{}] is of Type[{}]. Not one of [{}]", resolved.getId(), resolved.getType(), getAcceptedColumnTypes());
		}

		return acceptable;
	}

}

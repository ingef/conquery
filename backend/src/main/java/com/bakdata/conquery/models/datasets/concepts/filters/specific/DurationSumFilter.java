package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.TwoColumnDurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.aggregator.DurationSumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends Filter<Range.LongRange> implements DaterangeSelectOrFilter {

	@Valid
	@Nullable
	private List<ColumnId> distinctBy;
	@JsonAlias("dateRangeColumn")
	@Nullable
	private ColumnId column;
	@Nullable
	private ColumnId startColumn;
	@Nullable
	private ColumnId endColumn;

	@JsonIgnore
	@Override
	public List<ColumnId> getRequiredColumns() {
		List<ColumnId> required = new ArrayList<>();

		if (hasDistinct()) {
			required.addAll(distinctBy);
		}
		if (column != null) {
			required.add(column);
		}
		else {
			required.add(startColumn);
			required.add(endColumn);
		}
		return required;

	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(0);
	}

	@JsonIgnore
	private boolean hasDistinct() {
		return distinctBy != null && !distinctBy.isEmpty();
	}

	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		ColumnAggregator<?> aggregator = getColumn() != null ? new DurationSumAggregator(getColumn().resolve())
										 : new TwoColumnDurationSumAggregator(startColumn.resolve(), endColumn.resolve());

		if (hasDistinct()) {
			aggregator = new DistinctValuesWrapperAggregator<>(aggregator, distinctBy.stream().map(ColumnId::resolve).toList());
		}

		return new RangeFilterNode(value, aggregator);
	}

	@Override
	public FilterConverter<DurationSumFilter, Range.LongRange> createConverter() {
		return new DurationSumSqlAggregator();
	}


	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		if (column != null) {
			final Column resolved = getColumn().resolve();

			if (!(resolved.getType().equals(MajorTypeId.DATE) || resolved.getType().equals(MajorTypeId.DATE_RANGE))) {
				log.error("Column {} of type {} is not date compatible", resolved.getId(), resolved.getType());
				return false;
			}
			return true;
		}

		if (!startColumn.resolve().getType().equals(MajorTypeId.DATE)) {
			log.error("startColumn {} is not of type DATE", startColumn);
			return false;
		}

		if (!endColumn.resolve().getType().equals(MajorTypeId.DATE)) {
			log.error("startColumn {} is not of type DATE", endColumn);
			return false;
		}


		return true;
	}

}

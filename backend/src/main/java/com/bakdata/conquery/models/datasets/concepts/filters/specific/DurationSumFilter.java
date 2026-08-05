package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.ColumnUtils;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.AggregationFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.TwoColumnDurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.AggregationResultFilterNode;
import com.bakdata.conquery.sql.conversion.model.aggregator.DurationSumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends AggregationFilter<Range.LongRange> implements DaterangeSelectOrFilter {

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
		} else {
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
	public AggregationResultFilterNode<?, ?> createFilterNode(Range.LongRange value) {
		ColumnAggregator<?> aggregator = isSingleColumnDaterange() ? new DurationSumAggregator(getColumn().resolve())
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
			return ColumnUtils.assertValidColumnTypes(this, column, Set.of(MajorTypeId.DATE, DATE_RANGE));
		}

		return ColumnUtils.assertValidColumnTypes(this, startColumn, Set.of(MajorTypeId.DATE)) &&
				ColumnUtils.assertValidColumnTypes(this, endColumn, Set.of(MajorTypeId.DATE));
	}

}

package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.aggregator.CountSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import lombok.Data;
import lombok.NoArgsConstructor;

@CPSType(id = "COUNT", base = Filter.class)
@NoArgsConstructor
@Data
public class CountFilter extends Filter<Range.LongRange> {

	private ColumnId column;

	@NotNull
	private List<ColumnId> distinctByColumn = Collections.emptyList();

	private boolean distinct;

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		if (!isDistinct()) {
			return new RangeFilterNode(value, new CountAggregator(getColumn().resolve()));
		}

		if (distinctByColumn != null && !getDistinctByColumn().isEmpty()) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(new CountAggregator(getColumn().resolve()), getDistinctByColumn().stream().map(ColumnId::resolve).toList()));
		}

		return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(new CountAggregator(), List.of(getColumn().resolve())));

	}

	@Override
	public List<ColumnId> getRequiredColumns() {
		final List<ColumnId> out = new ArrayList<>();
		out.add(getColumn());
		if (distinctByColumn != null) {
			out.addAll(getDistinctByColumn());
		}

		return out;
	}

	@Override
	public FilterConverter<CountFilter, Range.LongRange> createConverter() {
		return new CountSqlAggregator();
	}
}

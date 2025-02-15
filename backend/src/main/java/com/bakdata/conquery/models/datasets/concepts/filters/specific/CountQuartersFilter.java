package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDateRangeAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDatesAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.aggregator.CountQuartersSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@CPSType(id = "COUNT_QUARTERS", base = Filter.class)
public class CountQuartersFilter extends Filter<Range.LongRange> implements DaterangeSelectOrFilter {

	@Nullable
	private ColumnId column;
	@Nullable
	private ColumnId startColumn;
	@Nullable
	private ColumnId endColumn;

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
	}

	@Override
	public List<ColumnId> getRequiredColumns() {
		if (isSingleColumnDaterange()) {
			return List.of(column);
		}
		return List.of(startColumn, endColumn);
	}

	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		final Column column = getColumn().resolve();
		if (column.getType() == MajorTypeId.DATE_RANGE) {
			return new RangeFilterNode(value, new CountQuartersOfDateRangeAggregator(column));
		}
		return new RangeFilterNode(value, new CountQuartersOfDatesAggregator(column));
	}

	@Override
	public FilterConverter<CountQuartersFilter, Range.LongRange> createConverter() {
		return new CountQuartersSqlAggregator();
	}

}

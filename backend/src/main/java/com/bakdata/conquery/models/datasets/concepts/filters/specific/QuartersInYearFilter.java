package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.AggregationFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.QuartersInYearAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.AggregationResultFilterNode;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@CPSType(id="QUARTERS_IN_YEAR", base= Filter.class)
public class QuartersInYearFilter extends AggregationFilter<Range.LongRange> {

	private ColumnId column;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(column);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
		f.setMax(4);
	}


	@Override
	public AggregationResultFilterNode createFilterNode(Range.LongRange value) {
		return new RangeFilterNode(value, new QuartersInYearAggregator(getColumn().resolve()));
	}

}

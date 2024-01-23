package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;
import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDateRangeAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDatesAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.CountQuartersSqlAggregator;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@CPSType(id="COUNT_QUARTERS", base=Filter.class)
public class CountQuartersFilter extends SingleColumnFilter<Range.LongRange> {
	
	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
	}

	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		if (getColumn().getType() == MajorTypeId.DATE_RANGE) {
			return new RangeFilterNode(value, new CountQuartersOfDateRangeAggregator(getColumn()));
		}
		return new RangeFilterNode(value, new CountQuartersOfDatesAggregator(getColumn()));
	}

	@Override
	public SqlFilters convertToSqlFilter(FilterContext<Range.LongRange> filterContext) {
		if (getColumn().getType() == MajorTypeId.DATE_RANGE) {
			throw new UnsupportedOperationException("COUNT_QUARTERS conversion on columns of type DATE_RANGE not implemented yet.");
		}
		return CountQuartersSqlAggregator.create(this, filterContext).getSqlFilters();
	}

	@Override
	public Set<ConceptCteStep> getRequiredSqlSteps() {
		return ConceptCteStep.withOptionalSteps(ConceptCteStep.AGGREGATION_FILTER);
	}
}

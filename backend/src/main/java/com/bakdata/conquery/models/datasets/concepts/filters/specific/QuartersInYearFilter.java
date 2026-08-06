package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;
import java.util.List;

import com.bakdata.conquery.models.common.ColumnUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.AggregationFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.QuartersInYearAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.AggregationFilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Setter
@Getter
@Slf4j
@CPSType(id = "QUARTERS_IN_YEAR", base = Filter.class)
public class QuartersInYearFilter extends AggregationFilter<Range.LongRange> {

	@Valid
	@NotNull
	private ColumnId column;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(getColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "Column do not match required Type.")
	public boolean isValidColumnType() {
		return ColumnUtils.assertValidColumnTypes(this, getColumn(), EnumSet.of(MajorTypeId.DATE));
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
		f.setMin(1);
		f.setMax(4);
	}


	@Override
	public AggregationFilterNode<?, ?> createFilterNode(Range.LongRange value) {
		return new RangeFilterNode(value, new QuartersInYearAggregator(getColumn().resolve()));
	}

}

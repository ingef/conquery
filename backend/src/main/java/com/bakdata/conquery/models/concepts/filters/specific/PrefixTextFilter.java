package com.bakdata.conquery.models.concepts.filters.specific;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.ComplexFilter;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.validators.RequiresColumnType;
import com.bakdata.conquery.models.query.aggregators.filter.PrefixTextFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQStringFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="PREFIX_TEXT", base=Filter.class)
public class PrefixTextFilter extends ComplexFilter<CQStringFilter> {
	
	private static final long serialVersionUID = 1L;

	@Valid @NotNull @RequiresColumnType(MajorTypeId.STRING)
	@IdReference
	private Column column;
	
	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.STRING);
	}
	
	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getColumn()};
	}
	
	@Override
	public FilterNode createAggregator(CQStringFilter filterValue) {
		return new PrefixTextFilterNode(this, filterValue);
	}

}

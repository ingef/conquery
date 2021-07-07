package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.filter.event.PrefixTextFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "PREFIX_TEXT", base = Filter.class)
public class PrefixTextFilter extends SingleColumnFilter<String> {


	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.STRING);
	}
	
	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public FilterNode createFilterNode(String value) {
		return new PrefixTextFilterNode(getColumn(), value);
	}

}

package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;


@CPSType(id = "POSTAL_CODE", base = Filter.class)
public class PostalCodeFilter extends MultiSelectFilter {

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.POSTAL_CODE);
	}

}
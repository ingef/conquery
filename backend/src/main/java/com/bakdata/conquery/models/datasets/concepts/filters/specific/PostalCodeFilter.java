package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.postalcode.PostalCodeSearchEntity;
import com.bakdata.conquery.models.datasets.concepts.filters.postalcode.PostalCodesManager;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;


@CPSType(id = "POSTAL_CODE", base = Filter.class)
public class PostalCodeFilter extends SingleColumnFilter<PostalCodeSearchEntity> {
	static private final PostalCodesManager postalCodesManager = new PostalCodesManager();

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.POSTAL_CODE);
	}

	@Override
	public FilterNode<String[]> createFilterNode(PostalCodeSearchEntity postalCodeSearchEntity) {
		return new MultiSelectFilterNode(getColumn(), postalCodesManager.filterAllNeighbours(postalCodeSearchEntity));
	}

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

}
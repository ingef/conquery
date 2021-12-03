package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.io.IOException;
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
	static private PostalCodesManager
			postalCodesManager = null;

	static {
		try {
			postalCodesManager = PostalCodesManager.loadFrom("/com/bakdata/conquery/postalcodes.csv", false);
		}
		catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.POSTAL_CODE);
	}

	@Override
	public FilterNode<String[]> createFilterNode(PostalCodeSearchEntity postalCodeSearchEntity) {
		return new MultiSelectFilterNode(getColumn(), postalCodesManager.filterAllNeighbours(Integer.parseInt(postalCodeSearchEntity.getPlz()), postalCodeSearchEntity
				.getRadius()));
	}

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

}
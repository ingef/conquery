package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface Select<SELECT_ID extends SelectId<? extends Select<? extends SELECT_ID>>> extends Named<SELECT_ID> {

	String getDescription();

	AggregatorNode<?> createAggregator(int position);

	String getLabel();
	
	@Override
	SELECT_ID getId();
}

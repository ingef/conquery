package com.bakdata.eva.models.translation.query.oldmodel.time;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.eva.models.translation.query.oldmodel.OIQuery;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="TIME_QUERY", base= OIQuery.class)
public class TimeQuery extends OIQuery {
	private long version;
	private List<TQCondition> conditions;
	private UUID indexResult;//this has to be contained in one pair of the list.

	@Override
	public IQuery translate(DatasetId dataset) {
		final ConceptQuery out = new ConceptQuery();

		final CQAnd and = new CQAnd();

		out.setRoot(and);

		and.setChildren(conditions.stream().map(cond -> cond.translate(dataset)).collect(Collectors.toList()));

		return out;
	}
}

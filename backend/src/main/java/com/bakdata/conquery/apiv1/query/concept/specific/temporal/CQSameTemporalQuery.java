package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.PrecedenceMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.SameTemporalMatcher;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "SAME", base = CQElement.class)
public class CQSameTemporalQuery extends CQAbstractTemporalQuery {

	public CQSameTemporalQuery(CQSampled index, CQSampled preceding) {
		super(index, preceding);
	}

	@Override
	protected PrecedenceMatcher createMatcher() {
		return new SameTemporalMatcher();
	}
}

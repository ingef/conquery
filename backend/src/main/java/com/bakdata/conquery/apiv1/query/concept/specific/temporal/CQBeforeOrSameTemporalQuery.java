package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.BeforeOrSameTemporalMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.PrecedenceMatcher;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "BEFORE_OR_SAME", base = CQElement.class)
public class CQBeforeOrSameTemporalQuery extends CQAbstractTemporalQuery {

	public CQBeforeOrSameTemporalQuery(CQSampled index, CQSampled preceding) {
		super(index, preceding);
	}

	@Override
	protected PrecedenceMatcher createMatcher() {
		return new BeforeOrSameTemporalMatcher();
	}
}

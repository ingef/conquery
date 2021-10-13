package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.DaysBeforeOrNeverPrecedenceMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.PrecedenceMatcher;
import lombok.Getter;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.
 */
@CPSType(id = "DAYS_OR_NO_EVENT_BEFORE", base = CQElement.class)
public class CQDaysBeforeOrNeverTemporalQuery extends CQAbstractTemporalQuery {

	@Getter
	private int days;

	public CQDaysBeforeOrNeverTemporalQuery(CQSampled index, CQSampled preceding, int days) {
		super(index, preceding);
		this.days = days;
	}

	@Override
	protected PrecedenceMatcher createMatcher() {
		return new DaysBeforeOrNeverPrecedenceMatcher(getDays());
	}
}

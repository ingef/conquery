package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.DaysBeforePrecedenceMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.PrecedenceMatcher;
import lombok.Getter;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "DAYS_BEFORE", base = CQElement.class)
public class CQDaysBeforeTemporalQuery extends CQAbstractTemporalQuery {

	@Getter
	private Range.IntegerRange days;

	public CQDaysBeforeTemporalQuery(CQSampled index, CQSampled preceding, Range.IntegerRange days) {
		super(index, preceding);
		this.days = days;
	}

	@Override
	protected PrecedenceMatcher createMatcher() {
		return new DaysBeforePrecedenceMatcher(getDays());
	}
}

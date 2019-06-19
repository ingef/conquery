package com.bakdata.eva.models.translation.query.oldmodel.time;

import org.apache.commons.lang3.NotImplementedException;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQAbstractTemporalQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeTemporalQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeTemporalQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQSameTemporalQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQSampled;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TQCondition {

	private TimeAccessedResult result0;
	private TimeAccessedResult result1;
	private Operator operator;
	private Integer maxDays;
	private Integer minDays;

	public CQAbstractTemporalQuery translate(DatasetId dataset) {
		final CQSampled query0 = result0.translate(dataset);
		final CQSampled query1 = result1.translate(dataset);

		final CQAbstractTemporalQuery query;

		switch (operator) {
			case BEFORE:
				query = new CQBeforeTemporalQuery(query0, query1);
				break;
			case BEFORE_OR_SAME:
				query = new CQBeforeOrSameTemporalQuery(query0, query1);
				break;
			case SAME:
				query = new CQSameTemporalQuery(query0, query1);
				break;
			case DAYS_BEFORE:
				query = new CQDaysBeforeTemporalQuery(query0, query1, new Range.IntegerRange(minDays, maxDays));
				break;
			case DAYS_OR_NO_EVENT_BEFORE:
				query = new CQDaysBeforeTemporalQuery(query0, query1, new Range.IntegerRange(minDays, maxDays));
				break;
			default:
				throw new NotImplementedException("Invalid Case");
		}


		return query;
	}
}

package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Helper Aggregator, returning if it was used at least once.
 */
@RequiredArgsConstructor @ToString(of = {"parent"})
public class ExistsAggregator implements UniversalAggregator<Boolean> {

	@Setter
	private FiltersNode parent;

	@Override
	public void acceptEvent(Bucket bucket, int event) {  }

	@Override
	public boolean isAlwaysActive() {
		return false;
	}

	@Override
	public Boolean getAggregationResult() {
		return parent.isContained();
	}

	@Override
	public ExistsAggregator doClone(CloneContext ctx) {
		return new ExistsAggregator();
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.BOOLEAN;
	}
}

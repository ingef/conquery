package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RealFilterNode extends NumberFilterNode<Range.DoubleRange> {

	public RealFilterNode(Column column, Range.DoubleRange filterValue) {
		super(column, filterValue);
	}

	@Override
	public RealFilterNode doClone(CloneContext ctx) {
		return new RealFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean contains(Bucket bucket, int event) {
		final double real = bucket.getReal(event, getColumn());
		log.warn("For testing: {}", real);
		return getFilterValue().contains(real);
	}
}

package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class IntegerFilterNode extends NumberFilterNode<Range.LongRange> {

	public IntegerFilterNode(Column column, Range.LongRange filterValue) {
		super(column, filterValue);
	}

	@Override
	public IntegerFilterNode doClone(CloneContext ctx) {
		return new IntegerFilterNode(getColumn(), getFilterValue());
	}

	@Override
	public boolean contains(Bucket bucket, int event) {
		return getFilterValue().contains(bucket.getInteger(event, getColumn()));
	}
}

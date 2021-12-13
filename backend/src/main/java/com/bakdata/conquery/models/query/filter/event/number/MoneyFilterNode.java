package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyFilterNode extends NumberFilterNode<Range.LongRange> {

	public MoneyFilterNode(Column column, Range.LongRange filterValue) {
		super(column, filterValue);
	}

	@Override
	public boolean contains(Bucket bucket, int event) {
		return getFilterValue().contains(bucket.getMoney(event, getColumn()));
	}
}

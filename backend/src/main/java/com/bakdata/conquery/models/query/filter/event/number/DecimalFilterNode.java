package com.bakdata.conquery.models.query.filter.event.number;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import lombok.ToString;

@ToString(callSuper = true)
public class DecimalFilterNode extends NumberFilterNode<Range<BigDecimal>> {

	public DecimalFilterNode(Column column, Range<BigDecimal> filterValue) {
		super(column, filterValue);
	}

	@Override
	public boolean contains(Bucket bucket, int event) {
		return getFilterValue().contains(bucket.getDecimal(event, getColumn()));
	}
}

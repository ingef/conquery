package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;

/**
 * Entity is included when the number of values for a specified column are within a given range.
 */
public class PrefixTextFilterNode extends SingleColumnFilterNode<String> {

	private boolean hit;

	public PrefixTextFilterNode(Column column, String filterValue) {
		super(column, filterValue);
	}

	@Override
	public PrefixTextFilterNode doClone(CloneContext ctx) {
		return new PrefixTextFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}

		int stringToken = bucket.getString(event, getColumn());

		String value = (String) getColumn().getTypeFor(bucket).createScriptValue(stringToken);

		//if performance is a problem we could find the filterValue once in the dictionary and then only check the values
		return value.startsWith(filterValue);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		this.hit = true;
	}

	@Override
	public boolean isContained() {
		return hit;
	}
}

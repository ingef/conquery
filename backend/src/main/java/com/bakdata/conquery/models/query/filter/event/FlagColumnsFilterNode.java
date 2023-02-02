package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;

public class FlagColumnsFilterNode extends EventFilterNode<Column[]> {
	public FlagColumnsFilterNode(Column[] columns) {
		super(columns);
	}


	@Override
	public void init(Entity entity, QueryExecutionContext context) {
	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		for (Column column : getFilterValue()) {
			if (bucket.has(event, column) && bucket.getBoolean(event, column)) {
				return true;
			}
		}

		return false;
	}


}

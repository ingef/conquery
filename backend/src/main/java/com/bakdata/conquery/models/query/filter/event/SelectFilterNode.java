package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;
import com.bakdata.conquery.models.types.specific.AStringType;


/**
 * Single events are filtered, and included if they have a selected value. Entity is only included if it has any event with selected value.
 */
public class SelectFilterNode extends SingleColumnFilterNode<String> {

	private int selectedId = -1;
	private boolean hit = false;

	public SelectFilterNode(Column column, String filterValue) {
		super(column, filterValue);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		//you can then also skip the block if the id is -1
		selectedId = ((AStringType) getColumn().getTypeFor(bucket)).getId(filterValue);
	}

	@Override
	public SelectFilterNode doClone(CloneContext ctx) {
		return new SelectFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (selectedId == -1 || !bucket.has(event, getColumn())) {
			return false;
		}

		int value = bucket.getString(event, getColumn());

		return value == selectedId;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		this.hit = true;
	}

	@Override
	public boolean isContained() {
		return hit;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return super.isOfInterest(bucket) &&
			   ((AStringType) bucket.getImp().getColumns()[getColumn().getPosition()].getType()).getId(filterValue) != -1;
	}
}

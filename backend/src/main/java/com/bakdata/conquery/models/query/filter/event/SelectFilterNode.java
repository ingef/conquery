package com.bakdata.conquery.models.query.filter.event;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.types.specific.AStringType;
import lombok.Getter;
import lombok.Setter;


/**
 * Single events are filtered, and included if they have a selected value. Entity is only included if it has any event with selected value.
 */
public class SelectFilterNode extends EventFilterNode<String> {

	private int selectedId = -1;
	@NotNull
	@Getter
	@Setter
	private Column column;

	@Override
	public boolean isAlwaysActive() {
		return false;
	}

	public SelectFilterNode(Column column, String filterValue) {
		super(filterValue);
		this.column = column;
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
	public boolean isOfInterest(Bucket bucket) {
		return ((AStringType) bucket.getImp().getColumns()[getColumn().getPosition()].getType()).getId(filterValue) != -1;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(column.getTable().getId());
	}

}
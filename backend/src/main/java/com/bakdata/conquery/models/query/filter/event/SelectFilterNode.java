package com.bakdata.conquery.models.query.filter.event;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;


/**
 * Single events are filtered, and included if they have a selected value. Entity is only included if it has any event with selected value.
 */
@ToString(callSuper = true, of = "column")
public class SelectFilterNode extends EventFilterNode<String> {

	private final boolean empty;
	private int selectedId = -1;
	@NotNull
	@Getter
	@Setter
	private Column column;

	public SelectFilterNode(Column column, String filterValue) {
		super(filterValue);
		this.column = column;

		empty = Strings.isEmpty(filterValue);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		// You can skip the block if the id is -1
		selectedId = ((StringStore) bucket.getStore(getColumn())).getId(filterValue);
	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		final boolean has = bucket.has(event, getColumn());

		if(empty && !has){
			return true;
		}

		if (selectedId == -1 || !has) {
			return false;
		}

		final int value = bucket.getString(event, getColumn());

		return value == selectedId;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return empty || ((StringStore) bucket.getStores()[getColumn().getPosition()]).getId(filterValue) != -1;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(column.getTable());
	}

}
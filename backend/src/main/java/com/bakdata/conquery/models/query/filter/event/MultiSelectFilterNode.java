package com.bakdata.conquery.models.query.filter.event;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * Event is included when the value in column is one of many selected.
 */
public class MultiSelectFilterNode extends EventFilterNode<String[]> {

	private final int[] selectedValues;

	@NotNull
	@Getter
	@Setter
	private Column column;

	public MultiSelectFilterNode(Column column, String[] filterValue) {
		super(filterValue);
		this.column = column;
		this.selectedValues = new int[filterValue.length];
	}

	@Override
	public void nextBlock(Bucket bucket) {
		StringType type = (StringType) getColumn().getTypeFor(bucket);

		for (int index = 0; index < filterValue.length; index++) {
			String select = filterValue[index];
			int parsed = type.getId(select);
			selectedValues[index] = parsed;
		}
	}


	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}

		int stringToken = bucket.getString(event, getColumn());

		for (int selectedValue : selectedValues) {
			if (selectedValue == stringToken) {
				return true;
			}
		}

		return false;
	}

	@Override
	public MultiSelectFilterNode doClone(CloneContext ctx) {
		return new MultiSelectFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (String selected : getFilterValue()) {
			if(((StringType) bucket.getStores()[getColumn().getPosition()]).getId(selected) != -1) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(column.getTable().getId());
	}

}

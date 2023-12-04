package com.bakdata.conquery.models.query.filter.event;

import java.util.Arrays;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;

/**
 * Event is included when the value in column is one of many selected.
 */

@ToString(callSuper = true, of = "column")
public class MultiSelectFilterNode extends EventFilterNode<String[]> {

	@NotNull
	@Getter
	@Setter
	private Column column;

	private boolean empty;
	
	private Set<String> selectedValues;

	public MultiSelectFilterNode(Column column, String[] filterValue) {
		super(filterValue);
		this.column = column;
		setFilterValue(filterValue);
	}


	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
	}

	@Override
	public void setFilterValue(String[] strings) {
		super.setFilterValue(strings);
		selectedValues = Set.of(strings);
		empty = Arrays.stream(filterValue).anyMatch(Strings::isEmpty);
	}

	@Override
	public void nextBlock(Bucket bucket) {
	}




	@Override
	public boolean checkEvent(Bucket bucket, int event) {

		if (!bucket.has(event, getColumn())) {
			return empty;
		}

		final String stringToken = bucket.getString(event, getColumn());

		return selectedValues.contains(stringToken);
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		//TODO
//		for (String selected : getFilterValue()) {
//			StringStore stringStore = (StringStore) bucket.getStores()[getColumn().getPosition()];
//			if(stringStore.getId(selected) != -1) {
//				return true;
//			}
//		}

		return true;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(column.getTable());
	}
	}

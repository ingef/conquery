package com.bakdata.conquery.models.query.filter.event;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
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

	private final boolean empty;

	/**
	 * Shared between all executing Threads to maximize utilization.
	 */
	private ConcurrentMap<Import, int[]> selectedValuesCache;
	private int[] selectedValues;

	public MultiSelectFilterNode(Column column, String[] filterValue) {
		super(filterValue);
		this.column = column;
		selectedValuesCache = new ConcurrentHashMap<>();
		empty = Arrays.stream(filterValue).anyMatch(Strings::isEmpty);
	}


	@Override
	public void setFilterValue(String[] strings) {
		selectedValuesCache = new ConcurrentHashMap<>();
		selectedValues = null;
		super.setFilterValue(strings);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		selectedValues = selectedValuesCache.computeIfAbsent(bucket.getImp(),imp -> findIds(bucket, filterValue));
	}

	private int[] findIds(Bucket bucket, String[] values) {
		final int[] selectedValues = new int[values.length];

		final StringStore type = (StringStore) bucket.getStore(getColumn());

		for (int index = 0; index < values.length; index++) {
			final String select = values[index];
			final int parsed = type.getId(select);

			selectedValues[index] = parsed;
		}

		return selectedValues;
	}


	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if(selectedValues == null){
			throw new IllegalStateException("No selected values  were set.");
		}

		if (!bucket.has(event, getColumn())) {
			return empty;
		}

		final int stringToken = bucket.getString(event, getColumn());

		for (int selectedValue : selectedValues) {
			if (selectedValue == stringToken) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (String selected : getFilterValue()) {
			if(((StringStore) bucket.getStores()[getColumn().getPosition()]).getId(selected) != -1) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(column.getTable());
	}
	}

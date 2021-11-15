package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;

public class PostalCodeFilterNode extends EventFilterNode<List<String>> {

	@NotNull
	@Getter
	@Setter
	private Column column;

	private StringStore store;

	public PostalCodeFilterNode(Column column, List<String> filterValue) {
		super(filterValue);
		this.column = column;
	}

	@Override
	public void nextBlock(Bucket bucket) {
		store = (StringStore) bucket.getStore(getColumn());
	}


	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}
		final int id = store.getString(event);
		String value = store.getElement(id);

		return filterValue.contains(value);
	}
}

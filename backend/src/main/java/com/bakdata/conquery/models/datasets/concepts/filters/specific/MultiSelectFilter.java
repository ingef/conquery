package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.Arrays;
import java.util.Collections;

import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@CPSType(id = "SELECT", base = Filter.class)
public class MultiSelectFilter extends SelectFilter<String[]> {

	@JsonIgnore
	@Override
	public String getFilterType() {
		// If we have labels we don't need a big multi select.
		if (!getLabels().isEmpty()) {
			return FEFilterType.Fields.MULTI_SELECT;
		}

		return FEFilterType.Fields.BIG_MULTI_SELECT;

	}

	@Override
	public FilterNode<?> createFilterNode(String[] value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}


	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context, String[] selection) {
		final IntSet out = new IntOpenHashSet();


		for (CBlock cBlock : context.getBucketManager().getCBlocksForConnector(getConnector())) {

			final StringStore store = (StringStore) cBlock.getBucket().getStore(getColumn());

			final IntSet selectionIds = new IntOpenHashSet(selection.length);

			Arrays.stream(selection)
				  .mapToInt(store::getId)
				  .filter(value -> value != -1)
				  .forEach(selectionIds::add);


			final Int2ObjectMap<IntSet> entityValues = cBlock.getColumnIndex(getColumn());

			if(entityValues == null) {
				continue;
			}

			entityValues.int2ObjectEntrySet().stream()
							.filter(kv -> {
								final IntSet values = kv.getValue();

								return Collections.disjoint(values, selectionIds);
							})
							.mapToInt(Int2ObjectMap.Entry::getIntKey)
							.forEach(out::add);
		}

		return new RequiredEntities(out);
	}
}

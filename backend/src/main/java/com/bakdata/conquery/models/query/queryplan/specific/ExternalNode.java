package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ExternalNode<T> extends QPNode {

	private final Table table;
	private final CDateSet dateUnion = CDateSet.createEmpty();

	@NotEmpty
	@NonNull
	private final Map<Integer, CDateSet> includedEntities;

	private final Map<Integer, Map<String, T>> extraData;
	private final String[] extraColumns;
	private final Map<String, ConstantValueAggregator<T>> extraAggregators;
	private CDateSet contained;

	public Set<Integer> getEntities() {
		return includedEntities.keySet();
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		contained = includedEntities.get(entity.getId());
		dateUnion.clear();

		for (ConstantValueAggregator<T> extraAggregator : extraAggregators.values()) {
			// reset aggregators
			extraAggregator.setValue(null);
		}

		for (Map.Entry<String, ConstantValueAggregator<T>> colAndAgg : extraAggregators.entrySet()) {
			final String col = colAndAgg.getKey();
			final ConstantValueAggregator<T> agg = colAndAgg.getValue();

			// Clear if entity has no value for the column
			if (!extraData.getOrDefault(entity.getId(), Collections.emptyMap()).containsKey(col)) {
				continue;
			}

			agg.setValue(extraData.get(entity.getId()).get(col));
		}
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);

		if (table.equals(currentTable) && contained != null) {
			dateUnion.addAll(contained);
			dateUnion.retainAll(ctx.getDateRestriction());
		}
	}

	@Override
	public boolean acceptEvent(Bucket bucket, int event) {
		// Nothing to do
		return true;
	}

	@Override
	public boolean isContained() {
		if (contained == null) {
			// Entity was not in the selected set.
			return false;
		}

		/*
		If the intersection 'dateUnion' is not empty its contained.
		Unless the initial dateset 'contained' was also empty, which means that no date context was provided anyway.
		 */
		return !dateUnion.isEmpty() || contained.isEmpty();
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return Set.of(new ConstantValueAggregator<>(dateUnion, new ResultType.ListT(ResultType.DateRangeT.INSTANCE)));
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		super.collectRequiredTables(requiredTables);
		//add the allIdsTable
		requiredTables.add(table);
	}
}

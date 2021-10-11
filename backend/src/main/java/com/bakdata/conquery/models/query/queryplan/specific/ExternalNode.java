package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExternalNode extends QPNode {

	private final Table table;
	private SpecialDateUnion dateUnion = new SpecialDateUnion();

	@NotEmpty
	@NonNull
	private final Map<Integer, CDateSet> includedEntities;

	private final Map<Integer, Map<String, List<String>>> extraData;
	private final Map<String, ConstantValueAggregator> extraAggregators;

	private CDateSet contained;

	public ExternalNode(Table table, Map<Integer, CDateSet> includedEntities, Map<Integer, Map<String, List<String>>> extraData, Map<String, ConstantValueAggregator> extraAggregators) {
		this.includedEntities = includedEntities;
		this.table = table;
		this.extraData = extraData;
		this.extraAggregators = extraAggregators;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		contained = includedEntities.get(entity.getId());
		dateUnion.init(entity, context);

		log.debug("Entity {} has values ({})", entity.getId(), extraData.get(entity.getId()));

		for (Map.Entry<String, ConstantValueAggregator> colAndAgg : extraAggregators.entrySet()) {
			final String col = colAndAgg.getKey();
			final ConstantValueAggregator agg = colAndAgg.getValue();

			// Clear if entity has no value for the column
			if (!extraData.getOrDefault(entity.getId(), Collections.emptyMap()).containsKey(col)) {
				agg.setValue(null);
				continue;
			}

			agg.setValue(extraData.get(entity.getId()).get(col));
		}
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		if (contained != null) {
			CDateSet newSet = CDateSet.create(ctx.getDateRestriction());
			newSet.retainAll(contained);
			ctx = ctx.withDateRestriction(newSet);
		}

		super.nextTable(ctx, currentTable);
		dateUnion.nextTable(getContext(), currentTable);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (contained != null) {
			dateUnion.acceptEvent(bucket, event);
		}
	}

	@Override
	public boolean isContained() {
		return contained != null;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return Set.of(dateUnion);
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		super.collectRequiredTables(requiredTables);
		//add the allIdsTable
		requiredTables.add(table);
	}
}

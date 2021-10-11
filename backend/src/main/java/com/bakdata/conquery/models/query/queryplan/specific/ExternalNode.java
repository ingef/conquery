package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
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
import lombok.Getter;
import lombok.NonNull;

public class ExternalNode extends QPNode {

	private final Table table;
	private SpecialDateUnion dateUnion = new SpecialDateUnion();

	@Getter
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

		if (extraData.containsKey(entity.getId())) {
			for (Map.Entry<String, List<String>> colValues : extraData.get(entity.getId()).entrySet()) {
				extraAggregators.get(colValues.getKey()).setValue(colValues.getValue());
			}
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

package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.common.BitMapCDateSet;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;
import lombok.NonNull;

public class ExternalNode extends QPNode {

	private final TableId tableId;
	private SpecialDateUnion dateUnion;

	@Getter @NotEmpty @NonNull
	private final Map<Integer, BitMapCDateSet> includedEntities;

	private BitMapCDateSet contained;

	public ExternalNode(TableId tableId, Map<Integer, BitMapCDateSet> includedEntities, SpecialDateUnion dateUnion) {
		this.dateUnion = dateUnion;
		this.includedEntities = includedEntities;
		this.tableId = tableId;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		contained = includedEntities.get(entity.getId());
	}
	
	@Override
	public ExternalNode doClone(CloneContext ctx) {
		return new ExternalNode(tableId, includedEntities, ctx.clone(dateUnion));
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		if(contained != null) {
			BitMapCDateSet newSet = BitMapCDateSet.create(ctx.getDateRestriction());
			newSet.retainAll(contained);
			ctx = ctx.withDateRestriction(newSet);
		}

		super.nextTable(ctx, currentTable);
		dateUnion.nextTable(getContext(), currentTable);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if(contained != null) {
			dateUnion.acceptEvent(bucket, event);
		}
	}
	
	@Override
	public boolean isContained() {
		return contained != null && !contained.isEmpty();
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);
		//add the allIdsTable
		requiredTables.add(tableId);
	}
}

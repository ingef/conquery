package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.date.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

public class ExternalNode extends QPNode {

	private final TableId allIdsTable;
	@Getter
	@NotEmpty
	private final Map<Integer, CDateSet> includedEntities;
	private final SpecialDateUnion dateUnionAggregatorNode;

	private CDateSet contained;
	private CDateSet restricted;

	public ExternalNode(TableId allIdsTable, Map<Integer, CDateSet> includedEntities, SpecialDateUnion dateUnionAggregatorNode) {
		this.allIdsTable = allIdsTable;
		this.includedEntities = Objects.requireNonNull(includedEntities);
		this.dateUnionAggregatorNode = dateUnionAggregatorNode;
	}

	@Override
	public void init(Entity entity) {
		super.init(entity);
		contained = includedEntities.get(entity.getId());
	}

	@Override
	public ExternalNode doClone(CloneContext ctx) {
		return new ExternalNode(allIdsTable, includedEntities, ctx.clone(dateUnionAggregatorNode));
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		if (contained == null) {
			super.nextTable(ctx, currentTable);
			return;
		}

		restricted = CDateSet.create(ctx.getDateRestriction());
		restricted.retainAll(contained);
	}

	@Override
	public void nextEvent(Bucket bucket, int event) {
		if (restricted == null) {
			return;
		}
		dateUnionAggregatorNode.merge(restricted);
	}

	@Override
	public boolean isContained() {
		return restricted != null && !restricted.isEmpty();
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(allIdsTable);
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return includedEntities.containsKey(entity.getId());
	}
}

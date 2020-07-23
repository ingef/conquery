package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotEmpty;

public class ExternalNode extends QPNode {

	private SpecialDateUnion dateUnion;
	@Getter
	private final DatasetId dataset;
	@Getter @NotEmpty @NonNull
	private final Map<Integer, CDateSet> includedEntities;
	private CDateSet contained;
	
	public ExternalNode(SpecialDateUnion dateUnion, DatasetId dataset, @NonNull Map<Integer, CDateSet> includedEntities) {
		this.dateUnion = dateUnion;
		this.dataset = dataset;
		this.includedEntities = includedEntities;
	}

	@Override
	public void init(Entity entity) {
		super.init(entity);
		contained = includedEntities.get(entity.getId());
	}
	
	@Override
	public ExternalNode doClone(CloneContext ctx) {
		return new ExternalNode(ctx.clone(dateUnion), dataset, includedEntities);
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		if(contained != null) {
			CDateSet newSet = CDateSet.create(ctx.getDateRestriction());
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
		requiredTables.add(
			new TableId(
				dataset,
				ConqueryConstants.ALL_IDS_TABLE
			)
		);
	}
}

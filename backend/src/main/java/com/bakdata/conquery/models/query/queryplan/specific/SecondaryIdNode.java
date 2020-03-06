package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPSwitchNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor
@Getter @Setter
public class SecondaryIdNode extends QPSwitchNode<Object> {

	private String secondaryId;
	private Column secondaryIdColumn;
	
	public SecondaryIdNode(@NonNull String secondaryId, @NonNull QPNode child) {
		super(child);
		this.secondaryId = secondaryId;
	}

	@Override
	public SecondaryIdNode doClone(CloneContext ctx) {
		return new SecondaryIdNode(secondaryId, getChild().clone(ctx));
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		findSecondaryIdColumn(currentTable);
		super.nextTable(ctx, currentTable);
	}
	
	private void findSecondaryIdColumn(Table table) {
		for(var col:table.getColumns()) {
			if(secondaryId.equals(col.getSecondaryId())) {
				secondaryIdColumn = col;
				return;
			}
		}
		throw new IllegalStateException("Table "+table+" should not appear in a query about secondary id "+secondaryId);
	}

	@Override
	protected Object rowKey(Bucket bucket, int event) {
		return bucket.getAsObject(event, secondaryIdColumn);
	}

}

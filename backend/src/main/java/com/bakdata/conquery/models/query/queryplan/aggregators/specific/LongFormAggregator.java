package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.LongFormAggregator.Entry;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter
public class LongFormAggregator implements Aggregator<List<Entry>> {

	private final Table requiredTable;
	@Setter(AccessLevel.PROTECTED)
	private boolean triggered = false;
	private List<Entry> results = new ArrayList<>();
	private boolean prettyPrint;
	
	@Override
	public LongFormAggregator doClone(CloneContext ctx) {
		return new LongFormAggregator(requiredTable);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(requiredTable.getId());
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}

	@Override
	public List<Entry> getAggregationResult() {
		return results;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		prettyPrint = ctx.isPrettyPrint();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		for(int i=0;i<requiredTable.getColumns().length;i++) {
			if(bucket.has(event, i)) {
				CType type = requiredTable.getColumns()[i].getTypeFor(bucket);
				
				
				results.add(new Entry(
					requiredTable.getColumns()[i].getId().toStringWithoutDataset(),
					//depending on context use pretty printing or script value
					prettyPrint?type.createPrintValue(bucket.getRaw(event, i)):type.createScriptValue(bucket.getRaw(event, i))
				));
			}
		}
	}

	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}
	
	@Data
	public static class Entry {
		private final String columnId;
		private final Object value;
	}
}

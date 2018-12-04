package com.bakdata.conquery.models.query.queryplan;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;

public interface EventIterating {
	
	default void collectRequiredTables(Set<Table> requiredTables) {}
	
	default Set<Table> collectRequiredTables() {
		HashSet<Table> out = new HashSet<>();
		this.collectRequiredTables(out);
		return out;
	}
	
	default void nextTable(QueryContext ctx, Table currentTable) {}
	
	default void nextBlock(Block block) {}
}

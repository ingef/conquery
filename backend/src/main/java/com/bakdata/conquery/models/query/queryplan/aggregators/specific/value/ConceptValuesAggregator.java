package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;

@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ConceptValuesAggregator extends Aggregator<Set<Object>> {

	private final Set<Object> entries = new HashSet<>();
	private final TreeConcept concept;

	private Column column;

	private final Map<Table, Connector> tableConnectors;

	public ConceptValuesAggregator(TreeConcept concept) {
		super();
		this.concept = concept;
		tableConnectors = concept.getConnectors().stream()
								 .filter(conn -> conn.getColumn() != null)
								 .collect(Collectors.toMap(Connector::getTable, Functions.identity()));
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.addAll(tableConnectors.keySet());
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		Connector connector = tableConnectors.get(currentTable);

		if (connector == null) {
			column = null;
			return;
		}

		column = connector.getColumn();
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		entries.clear();
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, column)) {
			return;
		}

		entries.add(bucket.createScriptValue(event, column));
	}

	@Override
	public Set<Object> createAggregationResult() {
		return entries.isEmpty() ? null : ImmutableSet.copyOf(entries);
	}


	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.StringT.INSTANCE);
	}
}

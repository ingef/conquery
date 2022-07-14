package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;

@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ConceptValuesAggregator extends Aggregator<Set<Object>> {

	private final Set<Object> entries = new HashSet<>();
	private final TreeConcept concept;

	private Column column;

	public ConceptValuesAggregator(TreeConcept concept) {
		super();
		this.concept = concept;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		Optional<ConceptTreeConnector> maybeConnector =
				concept.getConnectors().stream()
					   .filter(conn -> conn.getTable().equals(currentTable))
					   .findAny();

		if(maybeConnector.isEmpty()){
			column = null;
			return;
		}


		final ConceptTreeConnector connector = maybeConnector.get();

		column = connector.getColumn();
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		entries.clear();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
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

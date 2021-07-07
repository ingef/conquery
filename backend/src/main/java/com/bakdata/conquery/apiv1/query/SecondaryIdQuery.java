package com.bakdata.conquery.apiv1.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(id = "SECONDARY_ID_QUERY", base = QueryDescription.class)
public class SecondaryIdQuery extends Query {

	@NotNull
	private CQElement root;

	@NsIdRef
	@NotNull
	private SecondaryIdDescription secondaryId;

	@NotNull
	protected DateAggregationMode dateAggregationMode = DateAggregationMode.MERGE;
	

	/**
	 * @apiNote not using {@link ConceptQuery} directly in the API-spec simplifies the API.
	 */
	@InternalOnly
	private ConceptQuery query;

	@InternalOnly
	@NsIdRefCollection
	private Set<Column> withSecondaryId;

	@InternalOnly
	@NsIdRefCollection
	private Set<Table> withoutSecondaryId;

	@Override
	public SecondaryIdQueryPlan createQueryPlan(QueryPlanContext context) {

		context = context.withSelectedSecondaryId(getSecondaryId());

		final ConceptQueryPlan queryPlan = query.createQueryPlan(context);

		return new SecondaryIdQueryPlan(queryPlan, secondaryId, withSecondaryId, withoutSecondaryId);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
		// Be aware, that this.query cannot be checked, as it does not exists at this point, however this.root exists
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {

		DateAggregationMode resolvedDateAggregationMode = dateAggregationMode;
		if(context.getDateAggregationMode() != null) {
			log.trace("Overriding date aggregation mode ({}) with mode from context ({})", dateAggregationMode, context.getDateAggregationMode());
			resolvedDateAggregationMode = context.getDateAggregationMode();
		}
		context = context.withDateAggregationMode(resolvedDateAggregationMode);

		this.query = new ConceptQuery(root);
		query.resolve(context);

		withSecondaryId = new HashSet<>();
		withoutSecondaryId = new HashSet<>();


		//TODO FK: can we refactor this into methods of CQConcept?

		// partition tables by their holding of the requested SecondaryId.
		// This assumes that from the root, only ConceptNodes hold TableIds we are interested in.
		query.visit(queryElement -> {
			if (!(queryElement instanceof CQConcept)) {
				return;
			}

			final CQConcept concept = (CQConcept) queryElement;

			for (CQTable connector : concept.getTables()) {
				final Table table = connector.getConnector().getTable();
				final Column secondaryIdColumn = findSecondaryIdColumn(table);

				if (secondaryIdColumn != null && !concept.isExcludeFromSecondaryIdQuery()) {
					withSecondaryId.add(secondaryIdColumn);
				}
				else {
					withoutSecondaryId.add(table);
				}
			}
		});

		// If there are no tables with the secondaryId, we fail as that is user error.
		if (withSecondaryId.isEmpty()) {
			throw new ConqueryError.NoSecondaryIdSelectedError();
		}
	}

	/**
	 * selects the right column for the given secondaryId from a table
	 */
	@CheckForNull
	private Column findSecondaryIdColumn(Table table) {

		for (Column col : table.getColumns()) {
			if (col.getSecondaryId() == null || !secondaryId.equals(col.getSecondaryId())) {
				continue;
			}

			return col;
		}

		return null;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add(new SimpleResultInfo(secondaryId.getName(), ResultType.IdT.INSTANCE));
		query.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		root.visit(visitor);
	}

	@Override
	public long countResults(Stream<EntityResult> results) {
		return results.map(EntityResult::listResultLines)
					  .mapToLong(List::size)
					  .sum();
	}

	@Override
	public CQElement getReusableComponents() {
		return getRoot();
	}
}
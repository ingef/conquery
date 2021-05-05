package com.bakdata.conquery.models.query.concept;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.QueryDescription;
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
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "SECONDARY_ID_QUERY", base = QueryDescription.class)
public class SecondaryIdQuery extends IQuery {

	@NotNull
	private CQElement root;

	@NsIdRef
	@NotNull
	private SecondaryIdDescription secondaryId;

	/**
	 * @apiNote not using {@link ConceptQuery} directly in the API-spec simplifies the API.
	 */
	@JsonIgnore
	private ConceptQuery query;

	@InternalOnly
	@NsIdRefCollection
	private Set<Column> withSecondaryId;

	@InternalOnly
	@NsIdRefCollection
	private Set<Table> withoutSecondaryId;

	@JsonProperty
	public void setRoot(@NotNull CQElement root) {
		this.root = root;
		this.query = new ConceptQuery(root);
	}


	@Override
	public SecondaryIdQueryPlan createQueryPlan(QueryPlanContext context) {

		context = context.withSelectedSecondaryId(getSecondaryId());

		final ConceptQueryPlan queryPlan = query.createQueryPlan(context);

		return new SecondaryIdQueryPlan(queryPlan, secondaryId, withSecondaryId, withoutSecondaryId);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
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
	public long countResults(List<EntityResult> results) {
		return results.stream()
					  .map(EntityResult::listResultLines)
					  .mapToLong(List::size)
					  .sum();
	}

	@Override
	public CQElement getReusableComponents() {
		return getRoot();
	}
}
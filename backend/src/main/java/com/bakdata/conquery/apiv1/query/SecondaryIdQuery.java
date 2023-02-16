package com.bakdata.conquery.apiv1.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(id = "SECONDARY_ID_QUERY", base = QueryDescription.class)
public class SecondaryIdQuery extends Query {

	@NotNull
	protected DateAggregationMode dateAggregationMode = DateAggregationMode.MERGE;
	@NotNull
	private CQElement root;
	@NsIdRef
	@NotNull
	private SecondaryIdDescription secondaryId;
	/**
	 * @apiNote not using {@link ConceptQuery} directly in the API-spec simplifies the API.
	 */
	@JsonView(View.InternalCommunication.class)
	private ConceptQuery query;

	@NsIdRefCollection
	@JsonView(View.InternalCommunication.class)
	private Set<Column> withSecondaryId;

	@NsIdRefCollection
	@JsonView(View.InternalCommunication.class)
	private Set<Table> withoutSecondaryId;

	@Override
	public SecondaryIdQueryPlan createQueryPlan(QueryPlanContext context) {

		return new SecondaryIdQueryPlan(query, context, secondaryId, withSecondaryId, withoutSecondaryId, query.createQueryPlan(context.withSelectedSecondaryId(secondaryId)));
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		// Be aware, that this.query cannot be checked, as it does not exists at this point, however this.root exists
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(final QueryResolveContext context) {

		DateAggregationMode resolvedDateAggregationMode = dateAggregationMode;
		if (context.getDateAggregationMode() != null) {
			log.trace("Overriding date aggregation mode ({}) with mode from context ({})", dateAggregationMode, context.getDateAggregationMode());
			resolvedDateAggregationMode = context.getDateAggregationMode();
		}
		final QueryResolveContext resolvedContext = context.withDateAggregationMode(resolvedDateAggregationMode);

		query = new ConceptQuery(root);
		query.resolve(resolvedContext);

		withSecondaryId = new HashSet<>();
		withoutSecondaryId = new HashSet<>();


		//TODO FK: can we refactor this into methods of CQConcept?

		// partition tables by their holding of the requested SecondaryId.
		// This assumes that from the root, only ConceptNodes hold TableIds we are interested in.
		query.visit(queryElement -> {
			// We cannot check for CQExternal here and add the ALL_IDS Table because it is not serializable at the moment

			if (!(queryElement instanceof CQConcept)) {
				return;
			}

			final CQConcept concept = (CQConcept) queryElement;

			for (CQTable connector : concept.getTables()) {
				final Table table = connector.getConnector().getTable();
				final Column secondaryIdColumn = findSecondaryIdColumn(table);

				if (secondaryIdColumn != null && !concept.isExcludeFromSecondaryId()) {
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
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();

		resultInfos.add(new SimpleResultInfo(secondaryId.getLabel(), ResultType.StringT.INSTANCE, secondaryId.getDescription(), Set.of(new SemanticType.SecondaryIdT(getSecondaryId()))));

		resultInfos.addAll(query.getResultInfos());

		return resultInfos;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		root.visit(visitor);
	}

	@Override
	public CQElement getReusableComponents() {
		return getRoot();
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return query.collectRequiredEntities(context);
	}
}
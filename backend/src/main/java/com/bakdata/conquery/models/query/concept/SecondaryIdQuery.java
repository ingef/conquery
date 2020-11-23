package com.bakdata.conquery.models.query.concept;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@Getter
@CPSType(id = "SECONDARY_ID_QUERY", base = QueryDescription.class)
public class SecondaryIdQuery extends IQuery {

	public static enum QueryPlanPhase {
		None,
		WithId,
		WithoutId
	}

	@Valid
	@NotNull
	private final CQElement root;

	/**
	 * @apiNote not using {@link ConceptQuery} directly in the API-spec simplifies the API.
	 */
	@JsonIgnore
	private final ConceptQuery query;

	@NotNull
	private final SecondaryId secondaryId;


	@JsonCreator
	public SecondaryIdQuery(@Valid @NotNull CQElement root, @NotNull SecondaryId secondaryId) {
		this.root = root;
		this.secondaryId = secondaryId;
		this.query = new ConceptQuery(root);
	}

	@Override
	public SecondaryIdQueryPlan createQueryPlan(QueryPlanContext context) {
		final ConceptQueryPlan queryPlan = query.createQueryPlan(context);

		final IdMap<TableId, Table> tables = context.getStorage().getDataset().getTables();

		Map<TableId, ColumnId> withSecondaryId = new HashMap<>();
		Set<TableId> withoutSecondaryId = new HashSet<>();

		// partition tables by their holding of the requested SecondaryId.
		for (TableId currentTable : queryPlan.collectRequiredTables()) {
			Column secondaryIdColumn = findSecondaryIdColumn(tables.getOrFail(currentTable));
			if (secondaryIdColumn != null) {
				withSecondaryId.put(currentTable, secondaryIdColumn.getId());
			}
			else {
				withoutSecondaryId.add(currentTable);
			}
		}

		// If there are no tables with the secondaryId, we fail as that is user error.
		if(withSecondaryId.isEmpty()){
			throw new ConqueryError.ExecutionCreationPlanError();
		}

		return new SecondaryIdQueryPlan(queryPlan, secondaryId, withSecondaryId, withoutSecondaryId);
	}

	/**
	 * selects the right column for the given secondaryId from a table
	 */
	private Column findSecondaryIdColumn(Table table) {

		for (Column col : table.getColumns()) {
			if (!secondaryId.equals(col.getSecondaryId())) {
				continue;
			}

			return col;
		}

		return null;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context);
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add(new SimpleResultInfo(secondaryId.getName(), ResultType.STRING));
		query.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		root.visit(visitor);
	}
}
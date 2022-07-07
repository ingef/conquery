package com.bakdata.conquery.apiv1.query;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefKeys;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * TableExportQuery can be used to export raw data from selected {@link Connector}s, for selected {@link com.bakdata.conquery.models.query.entity.Entity}s.
 * <p>
 * Output format is lightly structured:
 * 1: Contains the {@link com.bakdata.conquery.models.datasets.concepts.ValidityDate} if one is available for the event.
 * 2: Contains the source {@link com.bakdata.conquery.models.datasets.Table}s label.
 * 3 - X: Contain the SecondaryId columns de-duplicated.
 * Following: Columns of all tables, (except for SecondaryId Columns), grouped by tables. The order is not guaranteed.
 * <p>
 * Columns used in Connectors to build Concepts, are marked with {@link SemanticType.ConceptColumnT} in {@link FullExecutionStatus#getColumnDescriptions()}.
 */
@Slf4j
@Getter
@Setter
@CPSType(id = "TABLE_EXPORT", base = QueryDescription.class)
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class TableExportQuery extends Query {

	@Valid
	@NotNull
	@NonNull
	protected Query query;
	@NotNull
	private Range<LocalDate> dateRange = Range.all();

	@NotEmpty
	@Valid
	private List<CQConcept> tables;

	/**
	 * We collect the positions for each Column of the output in here.
	 * Multiple columns can map to the same output position:
	 * - ValidityDate-Columns are merged into a single Date-Column
	 * - SecondaryIds are collected into a Column per SecondaryId
	 * - The remaining columns are arbitrarily ordered, but usually grouped by their source table.
	 */
	@NsIdRefKeys
	@JsonView(View.InternalCommunication.class)
	private Map<Column, Integer> positions;

	@JsonIgnore
	private List<ResultInfo> resultInfos = Collections.emptyList();

	@Override
	public TableExportQueryPlan createQueryPlan(QueryPlanContext context) {

		final Map<CQTable, QPNode> filterQueryNodes = new HashMap<>(tables.size());

		for (CQConcept cqConcept : tables) {
			for (CQTable table : cqConcept.getTables()) {
				// We use this query just to properly Construct a QPNode, filtering is done manually inside TableQueryPlan
				final ConceptQuery tableFilterQuery = new ConceptQuery(cqConcept, DateAggregationMode.NONE);
				filterQueryNodes.put(table, tableFilterQuery.createQueryPlan(context).getChild());
			}
		}

		return new TableExportQueryPlan(
				query.createQueryPlan(context),
				CDateSet.create(CDateRange.of(dateRange)),
				filterQueryNodes,
				positions
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context);

		// First is dates, second is source id
		AtomicInteger currentPosition = new AtomicInteger(2);

		positions = new HashMap<>();

		Map<SecondaryIdDescription, Integer> secondaryIdPositions = new HashMap<>();

		// SecondaryIds are pulled to the front and grouped over all tables
		tables.stream()
			  .flatMap(con -> con.getTables().stream())
			  .map(cqUnfilteredTable -> cqUnfilteredTable.getConnector().getTable().getColumns())
			  .flatMap(Arrays::stream)
			  .map(Column::getSecondaryId)
			  .filter(Objects::nonNull)
			  .distinct()
			  .sorted(Comparator.comparing(SecondaryIdDescription::getLabel))
			  .forEach(secondaryId -> secondaryIdPositions.put(secondaryId, currentPosition.getAndIncrement()));


		for (CQConcept concept : tables) {
			for (CQTable table : concept.getTables()) {

				final Column validityDateColumn = table.findValidityDateColumn();

				if (validityDateColumn != null) {
					positions.putIfAbsent(validityDateColumn, 0);
				}

				// Set column positions, set SecondaryId positions to precomputed ones.
				for (Column column : table.getConnector().getTable().getColumns()) {
					if (positions.containsKey(column)) {
						continue;
					}

					if (column.getSecondaryId() != null) {
						positions.putIfAbsent(column, secondaryIdPositions.get(column.getSecondaryId()));
						continue;
					}

					positions.put(column, currentPosition.getAndIncrement());
				}
			}

		}

		resultInfos = createResultInfos(secondaryIdPositions, positions, getTables());
	}

	private static List<ResultInfo> createResultInfos(Map<SecondaryIdDescription, Integer> secondaryIdPositions, Map<Column, Integer> positions, @NotEmpty @Valid List<CQConcept> tables) {

		final int size = positions.values().stream().mapToInt(i -> i).max().getAsInt() + 1;

		ResultInfo[] infos = new ResultInfo[size];

		infos[0] = ConqueryConstants.DATES_INFO;
		infos[1] = ConqueryConstants.SOURCE_INFO;

		for (Map.Entry<SecondaryIdDescription, Integer> e : secondaryIdPositions.entrySet()) {
			SecondaryIdDescription desc = e.getKey();
			Integer pos = e.getValue();
			infos[pos] = new SimpleResultInfo(desc.getLabel(), ResultType.StringT.INSTANCE, Set.of(new SemanticType.SecondaryIdT(desc)));
		}

		final Map<Column, Concept<?>> conceptColumns =
				tables.stream()
					  .flatMap(con -> con.getTables().stream())
					  .filter(tbl -> tbl.getConnector().getColumn() != null)
					  .collect(Collectors.toMap(tbl -> tbl.getConnector().getColumn(), tbl -> tbl.getConnector().getConcept()));


		for (Map.Entry<Column, Integer> entry : positions.entrySet()) {

			// 0 Position is date, already covered
			final int position = entry.getValue();

			// SecondaryIds are pulled to the front, already covered.
			final Column column = entry.getKey();

			if (position == 0 || column.getSecondaryId() != null) {
				continue;
			}

			// Columns that are used to build concepts are marked as PrimaryColumn.
			final ResultType resultType = ResultType.resolveResultType(column.getType());

			infos[position] = new SimpleResultInfo(
					column.getTable().getLabel() + " " + column.getLabel(),
					resultType,
					conceptColumns.containsKey(column)
					? Set.of(new SemanticType.ConceptColumnT(conceptColumns.get(column)))
					: Collections.emptySet()
			);
		}

		return List.of(infos);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}
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
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefKeys;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Columns used in Connectors to build Concepts, are marked with {@link ResultType.ConceptColumnT} in {@link FullExecutionStatus#getColumnDescriptions()}.
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

	@InternalOnly
	@NsIdRefKeys
	private Map<Column, Integer> positions;

	@JsonIgnore
	private List<ResultInfo> resultInfos = Collections.emptyList();

	@Override
	public TableExportQueryPlan createQueryPlan(QueryPlanContext context) {

		final Map<CQTable, QPNode> resolved = new HashMap<>(tables.size());

		for (CQConcept cqConcept : tables) {
			for (CQTable table : cqConcept.getTables()) {
				// We use this query just to properly Construct a QPNode, filtering is done manually inside our QueryPlan
				final ConceptQuery tableFilterQuery = new ConceptQuery(cqConcept, DateAggregationMode.NONE);
				resolved.put(table, tableFilterQuery.createQueryPlan(context).getChild());
			}
		}

		return new TableExportQueryPlan(
				query.createQueryPlan(context),
				CDateSet.create(CDateRange.of(dateRange)),
				resolved,
				positions
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
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
			  // First position is dates, so
			  .forEach(secondaryId -> secondaryIdPositions.put(secondaryId, currentPosition.getAndIncrement()));


		for (CQConcept concept : tables) {
			for (CQTable table : concept.getTables()) {

				final Connector connector = table.getConnector();
				final Column validityDateColumn = TableExportQueryPlan.findValidityDateColumn(connector, table.getDateColumn());

				if (validityDateColumn != null) {
					positions.putIfAbsent(validityDateColumn, 0);
				}

				// Set column positions, set SecondaryId positions to precomputed ones.
				for (Column column : connector.getTable().getColumns()) {
					positions.computeIfAbsent(column, col -> col.getSecondaryId() != null
															 ? secondaryIdPositions.get(col.getSecondaryId())
															 : currentPosition.getAndIncrement());
				}
			}

		}

		resultInfos = createResultInfos(currentPosition.get(), secondaryIdPositions, positions);
	}

	private List<ResultInfo> createResultInfos(int size, Map<SecondaryIdDescription, Integer> secondaryIdPositions, Map<Column, Integer> positions) {

		ResultInfo[] infos = new ResultInfo[size];

		infos[0] = ConqueryConstants.DATES_INFO;
		infos[1] = ConqueryConstants.SOURCE_INFO;

		for (Map.Entry<SecondaryIdDescription, Integer> e : secondaryIdPositions.entrySet()) {
			SecondaryIdDescription desc = e.getKey();
			Integer pos = e.getValue();
			infos[pos] = new SimpleResultInfo(desc.getLabel(), ResultType.SecondaryIdT.INSTANCE);
		}

		final Map<Column, Concept<?>> conceptColumns = getTables().stream()
																  .flatMap(con -> con.getTables().stream())
																  .filter(tbl -> tbl.getConnector().getColumn() != null)
																  .collect(Collectors.toMap(tbl -> tbl.getConnector().getColumn(), tbl -> tbl.getConcept()
																																			 .getConcept()));

		for (Map.Entry<Column, Integer> entry : positions.entrySet()) {

			// 0 Position is date, already covered
			final int position = entry.getValue();

			// SecondaryIds are pulled to the front, already covered.
			final Column column = entry.getKey();

			if (position == 0 || column.getSecondaryId() != null) {
				continue;
			}

			// Columns that are used to build concepts are marked as PrimaryColumn.


			final ResultType resultType;

			if (conceptColumns.containsKey(column)) {
				resultType = ResultType.ConceptColumnT.INSTANCE;
			}
			else {
				resultType = ResultType.resolveResultType(column.getType());
			}

			infos[position] = new SimpleResultInfo(column.getTable().getLabel() + " " + column.getLabel(), resultType);
		}

		return List.of(infos);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}
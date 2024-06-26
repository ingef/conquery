package com.bakdata.conquery.apiv1.query;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
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
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ColumnResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class TableExportQuery extends Query {

	@Valid
	@NotNull
	@NonNull
	protected final Query query;
	@NotNull
	private Range<LocalDate> dateRange = Range.all();

	@NotEmpty
	@Valid
	private List<CQConcept> tables;

	/**
	 * @see TableExportQueryPlan#isRawConceptValues()
	 */
	private boolean rawConceptValues = true;

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
				positions,
				rawConceptValues
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
		final AtomicInteger currentPosition = new AtomicInteger(2);

		final Map<SecondaryIdDescription, Integer> secondaryIdPositions = calculateSecondaryIdPositions(currentPosition);

		final Set<ValidityDate> validityDates = tables.stream()
													  .map(CQConcept::getTables)
													  .flatMap(Collection::stream)
													  .map(CQTable::findValidityDate)
													  .filter(Objects::nonNull)
													  .collect(Collectors.toSet());

		// We need to know if a column is a concept column so we can prioritize it if it is also a SecondaryId
		final Set<Column> conceptColumns = tables.stream()
												 .map(CQConcept::getTables)
												 .flatMap(Collection::stream)
												 .map(CQTable::getConnector)
												 .map(Connector::getColumn)
												 .filter(Objects::nonNull)
												 .collect(Collectors.toSet());

		positions = calculateColumnPositions(currentPosition, tables, secondaryIdPositions, conceptColumns, validityDates);

		resultInfos = createResultInfos(secondaryIdPositions, conceptColumns);
	}

	private Map<SecondaryIdDescription, Integer> calculateSecondaryIdPositions(AtomicInteger currentPosition) {
		final Map<SecondaryIdDescription, Integer> secondaryIdPositions = new HashMap<>();

		// SecondaryIds are pulled to the front and grouped over all tables
		tables.stream()
			  .flatMap(con -> con.getTables().stream())
			  .flatMap(table -> Arrays.stream(table.getConnector().getTable().getColumns()))
			  .map(Column::getSecondaryId)
			  .filter(Objects::nonNull)
			  .distinct()
			  .sorted(Comparator.comparing(SecondaryIdDescription::getLabel))
			  // Using for each and not a collector allows us to guarantee sorted insertion.
			  .forEach(secondaryId -> secondaryIdPositions.put(secondaryId, currentPosition.getAndIncrement()));

		return secondaryIdPositions;
	}

	private static Map<Column, Integer> calculateColumnPositions(AtomicInteger currentPosition, List<CQConcept> tables, Map<SecondaryIdDescription, Integer> secondaryIdPositions, Set<Column> conceptColumns, Set<ValidityDate> validityDates) {
		final Map<Column, Integer> positions = new HashMap<>();


		for (CQConcept concept : tables) {
			for (CQTable table : concept.getTables()) {

				// Set column positions, set SecondaryId positions to precomputed ones.
				for (Column column : table.getConnector().getTable().getColumns()) {

					// ValidityDates are handled separately in column=0
					if (validityDates.stream().anyMatch(vd -> vd.containsColumn(column))) {
						continue;
					}

					if (positions.containsKey(column)) {
						continue;
					}

					// We want to have ConceptColumns separate here.
					if (column.getSecondaryId() != null && !conceptColumns.contains(column)) {
						positions.putIfAbsent(column, secondaryIdPositions.get(column.getSecondaryId()));
						continue;
					}

					positions.put(column, currentPosition.getAndIncrement());
				}
			}
		}

		return positions;
	}

	private List<ResultInfo> createResultInfos(Map<SecondaryIdDescription, Integer> secondaryIdPositions, Set<Column> conceptColumns) {

		final int size = positions.values().stream().mapToInt(i -> i).max().getAsInt() + 1;

		final ResultInfo[] infos = new ResultInfo[size];

		infos[0] = ConqueryConstants.DATES_INFO_HISTORY;
		infos[1] = ConqueryConstants.SOURCE_INFO;


		for (Map.Entry<SecondaryIdDescription, Integer> e : secondaryIdPositions.entrySet()) {
			final SecondaryIdDescription desc = e.getKey();
			final Integer pos = e.getValue();

			// If mapping is available, values are mapped
			final ResultType.StringT resultType =
					desc.getMapping() != null
					? new ResultType.StringT((internal, printSettings) -> {
						if (internal == null) {
							return null;
						}
						return desc.getMapping().external((String) internal);
					})
					: ResultType.StringT.INSTANCE;

			final Set<SemanticType> semantics = new HashSet<>();

			semantics.add(new SemanticType.SecondaryIdT(desc));

			infos[pos] = new SimpleResultInfo(desc.getLabel(), resultType, desc.getDescription(), semantics);
		}


		final Map<Column, Concept<?>> connectorColumns =
				tables.stream()
					  .flatMap(con -> con.getTables().stream())
					  .filter(tbl -> tbl.getConnector().getColumn() != null)
					  .collect(Collectors.toMap(tbl -> tbl.getConnector().getColumn(), tbl -> tbl.getConnector().getConcept()));


		for (Map.Entry<Column, Integer> entry : positions.entrySet()) {

			final int position = entry.getValue();

			final Column column = entry.getKey();

			if(position == 0) {
				continue;
			}

			// SecondaryIds and date columns are pulled to the front, thus already covered.
			if (column.getSecondaryId() != null && !conceptColumns.contains(column)) {
				infos[secondaryIdPositions.get(column.getSecondaryId())].getSemantics()
																		.add(new SemanticType.ColumnT(column));
				continue;
			}

			final Set<SemanticType> semantics = new HashSet<>();

			ResultType resultType = ResultType.resolveResultType(column.getType());

			if (connectorColumns.containsKey(column)) {
				// Additionally, Concept Columns are returned as ConceptElementId, when rawConceptColumns is not set.

				final Concept<?> concept = connectorColumns.get(column).getConcept();

				// Columns that are used to build concepts are marked as ConceptColumn.
				semantics.add(new SemanticType.ConceptColumnT(concept));

				if (!isRawConceptValues()) {
					resultType = new ResultType.StringT(concept::printConceptLocalId);
				}
			}
			else {
				// If it's not a connector column, we just link to the source column.
				semantics.add(new SemanticType.ColumnT(column));
			}

			infos[position] = new ColumnResultInfo(column, resultType, semantics);
		}

		return List.of(infos);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return query.collectRequiredEntities(context);
	}
}

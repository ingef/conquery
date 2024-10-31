package com.bakdata.conquery.mode.local;

import static org.jooq.impl.DSL.asterisk;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.table;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;

@Slf4j
public class SqlUpdateMatchingStatsJob extends Job {

	private static final Name CONNECTOR_COLUMN = name("connector_column");
	private static final Name EVENTS = name("events");
	private static final Name ENTITIES = name("entities");
	private static final Name DATES = name("dates");

	private final DatabaseConfig databaseConfig;
	private final SqlExecutionService executionService;
	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final Set<ConceptId> concepts;
	private final ExecutorService executors;

	public SqlUpdateMatchingStatsJob(
			DatabaseConfig databaseConfig,
			SqlExecutionService executionService,
			SqlFunctionProvider functionProvider,
			Set<ConceptId> concepts,
			ExecutorService executors
	) {
		this.databaseConfig = databaseConfig;
		this.executionService = executionService;
		this.dslContext = executionService.getDslContext();
		this.functionProvider = functionProvider;
		this.concepts = concepts;
		this.executors = executors;
	}

	@Override
	public String getLabel() {
		return "Calculating Matching Stats for %s.".formatted(executionService);
	}

	@Override
	public void execute() throws Exception {

		log.debug("BEGIN update Matching stats for {} Concepts.", concepts.size());

		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		final List<Future<?>> runningQueries = concepts.stream()
													   .map(ConceptId::resolve)
													   .filter(SqlUpdateMatchingStatsJob::isTreeConcept)
													   .map(TreeConcept.class::cast)
													   .map(treeConcept -> (Runnable) () -> calculateMatchingStats(treeConcept))
													   .map(executors::submit)
													   .collect(Collectors.toList());

		executors.shutdown();
		while (!executors.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for executors to set matching stats for all concepts...");
		}

		stopWatch.stop();
		log.debug("DONE collecting matching stats. Elapsed time: {} ms.", stopWatch.getTime());

		runningQueries.forEach(SqlUpdateMatchingStatsJob::checkForError);
	}

	@Override
	public void cancel() {
		super.cancel();
		executors.shutdownNow();
	}

	private static boolean isTreeConcept(final Concept<?> concept) {
		if (!(concept instanceof TreeConcept)) {
			log.error("Collecting MatchingStats is currently only supported for TreeConcepts.");
			return false;
		}
		return true;
	}

	private static void checkForError(final Future<?> future) {
		try {
			future.get();
		}
		catch (ExecutionException | InterruptedException e) {
			log.error("Unknown error while querying SQL matching stats. Cause: \n", e.getCause());
		}
	}

	public void calculateMatchingStats(final TreeConcept treeConcept) {

		final Map<Connector, Set<Field<?>>> relevantColumns = collectRelevantColumns(treeConcept);
		final Map<Connector, List<ColumnDateRange>> validityDateMap = createColumnDateRanges(treeConcept);

		// union of all connectors of the concept
		final Select<?> unioned = treeConcept.getConnectors().stream()
											 .map(connector -> this.createConnectorQuery(connector, relevantColumns, validityDateMap))
											 .reduce(Select::unionAll)
											 .orElseThrow(IllegalStateException::new);

		// select the minimum of the least start date and the maximum of the greatest end date of all validity dates of all connectors
		final List<ColumnDateRange> validityDates = validityDateMap.values().stream().flatMap(List::stream).map(functionProvider::toDualColumn).toList();
		final List<Field<Date>> allStarts = validityDates.stream().map(ColumnDateRange::getStart).toList();
		final List<Field<Date>> allEnds = validityDates.stream().map(ColumnDateRange::getEnd).toList();
		final ColumnDateRange minAndMax = ColumnDateRange.of(min(functionProvider.least(allStarts)), max(functionProvider.greatest((allEnds))));
		final Field<String> validityDateExpression = functionProvider.daterangeStringExpression(minAndMax).as(DATES);

		// all connectors need the same columns originating from the concept definition - they might have different names in the respective connector tables,
		// but as we aliased them already, we can just use the unified aliases in the final query
		final List<Field<?>> relevantColumnsAliased = relevantColumns.get(treeConcept.getConnectors().get(0)).stream()
																	 .map(field -> field(field.getUnqualifiedName()))
																	 .collect(Collectors.toList());

		final Select<? extends Record> query = dslContext.select(relevantColumnsAliased)
														 .select(
																 count(asterisk()).as(EVENTS),
																 countDistinct(field(ENTITIES)).as(ENTITIES),
																 validityDateExpression
														 )
														 .from(unioned)
														 .groupBy(relevantColumnsAliased);

		final ConceptTreeCache treeCache = new ConceptTreeCache(treeConcept);
		executionService.fetchStream(query).forEach(record -> mapRecordToConceptElements(treeConcept, record, relevantColumnsAliased, treeCache));
	}

	/**
	 * @return A map from a connector to all relevant columns the connector's concept defines. A relevant column is any column that is used by a
	 * {@link CTCondition} which is part of any child of a concept, or it's a concept's connector column.
	 */
	private Map<Connector, Set<Field<?>>> collectRelevantColumns(final TreeConcept treeConcept) {
		return treeConcept.getConnectors().stream().collect(Collectors.toMap(
				Function.identity(),
				connector -> collectRelevantColumns(connector, treeConcept.getChildren())
						.stream()
						.map(column -> {
							final Field<Object> field = field(name(column));
							// connector columns are unioned, thus they need the same alias
							if (connector.getColumn() != null && connector.getColumn().resolve().getName().equals(column)) {
								return field.as(CONNECTOR_COLUMN);
							}
							// a condition which does not operate on the connector column MUST have the same name in all connector's tables
							return field;
						})
						.collect(Collectors.toSet())
		));
	}

	private Set<String> collectRelevantColumns(final Connector connector, final List<ConceptTreeChild> children) {
		final Set<String> relevantColumns = new HashSet<>();

		for (ConceptTreeChild child : children) {
			if (child.getCondition() == null && child.getChildren().isEmpty()) {
				continue;
			}

			final Set<String> childColumns = new HashSet<>();

			// Recursively collect columns from the current child's children, if they exist
			if (!child.getChildren().isEmpty()) {
				final Set<String> childrenColumns = collectRelevantColumns(connector, child.getChildren());
				childColumns.addAll(childrenColumns);
			}

			// Add columns from the child's condition, if it exists
			if (child.getCondition() != null) {
				final Set<String> conditionColumns = child.getCondition().getColumns(connector);
				childColumns.addAll(conditionColumns);
			}

			relevantColumns.addAll(childColumns);
		}

		return relevantColumns;
	}

	private Map<Connector, List<ColumnDateRange>> createColumnDateRanges(final TreeConcept treeConcept) {
		final AtomicInteger counter = new AtomicInteger(0);
		return treeConcept.getConnectors().stream().collect(Collectors.toMap(
				Function.identity(),
				connector -> createColumnDateRanges(connector, counter)
		));
	}

	private List<ColumnDateRange> createColumnDateRanges(final Connector connector, final AtomicInteger counter) {
		return connector.getValidityDates().stream()
						.map(functionProvider::forValidityDate)
						.map(daterange -> daterange.as("%s-%d".formatted(SharedAliases.DATES_COLUMN.getAlias(), counter.incrementAndGet())))
						.toList();
	}

	private Select<?> createConnectorQuery(
			final ConceptTreeConnector connector,
			final Map<Connector, Set<Field<?>>> relevantColumns,
			final Map<Connector, List<ColumnDateRange>> validityDateMap
	) {
		final Table<Record> connectorTable = table(name(connector.getResolvedTable().getName()));
		final Set<Field<?>> connectorColumns = relevantColumns.get(connector);
		final Field<Object> primaryKey = TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), databaseConfig).as(ENTITIES);

		// we have to select all possible validity dates of all connectors because we have to union multiple connectors
		final List<Field<?>> validityDates =
				validityDateMap.entrySet().stream()
							   .flatMap(entry -> entry.getValue().stream().map(columnDateRange -> entry.getKey() == connector
																								  ? columnDateRange
																								  : functionProvider.nulled(columnDateRange))
													  .flatMap(columnDateRange -> columnDateRange.toFields().stream()))
							   .toList();

		// connector might have a condition
		final Condition connectorCondition = toJooqCondition(connector, Optional.ofNullable(connector.getCondition()));

		return dslContext.select(primaryKey)
						 .select(connectorColumns)
						 .select(validityDates)
						 .from(connectorTable)
						 .where(connectorCondition);
	}

	private Condition toJooqCondition(final Connector connector, final Optional<CTCondition> childCondition) {
		final CTConditionContext context = CTConditionContext.create(connector, functionProvider);
		return childCondition.or(() -> Optional.ofNullable(connector.getCondition()))
							 .map(condition -> condition.convertToSqlCondition(context).condition())
							 .orElse(noCondition());
	}

	private void mapRecordToConceptElements(
			final TreeConcept treeConcept,
			final Record record,
			final List<Field<?>> relevantColumns,
			final ConceptTreeCache treeCache
	) {
		final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(record::intoMap);
		final MatchingStats.Entry entry = toMatchingStatsEntry(record);

		if (treeConcept.getChildren().isEmpty()) {
			addEntryToConceptElement(treeConcept, treeConcept.getName(), entry);
			return;
		}

		relevantColumns.stream().map(field -> record.get(field, String.class)).forEach(relevantColumnValue -> {
			try {
				final ConceptTreeChild mostSpecificChild = treeCache.findMostSpecificChild(relevantColumnValue, rowMap);

				//  database value did not match any node of the concept
				if (mostSpecificChild == null) {
					return;
				}

				// add stats for most specific child
				addEntryToConceptElement(mostSpecificChild, relevantColumnValue, entry);

				// add child stats to all parents till concept root
				ConceptTreeNode<?> current = mostSpecificChild.getParent();
				while (current != null) {
					addEntryToConceptElement(current, relevantColumnValue, entry);
					current = current.getParent();
				}
			}
			catch (ConceptConfigurationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private MatchingStats.Entry toMatchingStatsEntry(Record record) {
		final long events = record.get(EVENTS, Integer.class).longValue();
		final long entities = record.get(ENTITIES, Integer.class).longValue();
		final CDateRange dateSpan = toDateRange(record.get(DATES, String.class));
		return new MatchingStats.Entry(events, entities, dateSpan.getMinValue(), dateSpan.getMaxValue());
	}

	private CDateRange toDateRange(final String validityDateExpression) {
		final List<Integer> dateRange = executionService.getResultSetProcessor().getCDateSetParser().toEpochDayRange(validityDateExpression);
		return !dateRange.isEmpty() ? CDateRange.fromList(dateRange) : CDateRange.all();
	}

	private static void addEntryToConceptElement(final ConceptTreeNode<?> mostSpecificChild, final String columnKey, final MatchingStats.Entry entry) {
		final MatchingStats childMatchingStats;
		if (mostSpecificChild.getMatchingStats() == null) {
			childMatchingStats = new MatchingStats();
			((ConceptElement<?>) mostSpecificChild).setMatchingStats(childMatchingStats);
		}
		else {
			childMatchingStats = mostSpecificChild.getMatchingStats();
		}
		childMatchingStats.putEntry(columnKey, entry);
	}

}

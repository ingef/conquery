package com.bakdata.conquery.mode.local;

import static org.jooq.impl.DSL.asterisk;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.noField;
import static org.jooq.impl.DSL.table;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.Table;

@Slf4j
public class UpdateMatchingStatsSqlJob extends Job {

	private static final Name CONNECTOR_COLUMN = name("connector_column");
	private static final Name EVENTS = name("events");
	private static final Name ENTITIES = name("entities");
	private static final Name DATES = name("dates");

	private final DatabaseConfig databaseConfig;
	private final SqlExecutionService executionService;
	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final Set<ConceptId> concepts;
	private final ListeningExecutorService executors;
	private ListenableFuture<?> all;

	public UpdateMatchingStatsSqlJob(
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
		this.executors = MoreExecutors.listeningDecorator(executors);
	}

	@Override
	public void execute() throws Exception {

		log.debug("BEGIN update Matching stats for {} Concepts.", concepts.size());
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		final List<ListenableFuture<?>> runningQueries = concepts.stream()
																 .map(ConceptId::resolve)
																 .filter(UpdateMatchingStatsSqlJob::isTreeConcept)
																 .map(TreeConcept.class::cast)
																 .map(treeConcept -> executors.submit(() -> calculateMatchingStats(treeConcept)))
																 .collect(Collectors.toList());

		all = Futures.allAsList(runningQueries);
		while (!all.isDone()) {
			try {
				all.get(1, TimeUnit.MINUTES);
			}
			catch (TimeoutException exception) {
				log.debug("Still waiting for {}", this);
				if (log.isTraceEnabled()) {
					log.trace("Waiting for {}", executors);
				}
			}
		}

		stopWatch.stop();
		log.debug("DONE collecting matching stats. Elapsed time: {} ms.", stopWatch.getTime());
	}

	@Override
	public void cancel() {
		if (all != null) {
			all.cancel(true);
		}
		super.cancel();
	}

	@Override
	public String getLabel() {
		return "Calculating Matching Stats for %s.".formatted(executionService);
	}

	private static boolean isTreeConcept(final Concept<?> concept) {
		if (!(concept instanceof TreeConcept)) {
			log.error("Collecting MatchingStats is currently only supported for TreeConcepts.");
			return false;
		}
		return true;
	}

	private static void addEntryToConceptElement(final ConceptTreeNode<?> mostSpecificChild, final String columnKey, final MatchingStats.Entry entry) {
		if (mostSpecificChild.getMatchingStats() == null) {
			((ConceptElement<?>) mostSpecificChild).setMatchingStats(new MatchingStats());
		}

		mostSpecificChild.getMatchingStats().putEntry(columnKey, entry);
	}

	private void calculateMatchingStats(final TreeConcept treeConcept) {

		final Map<Connector, Set<Field<?>>> relevantColumns = collectRelevantColumns(treeConcept);
		final Map<Connector, List<ColumnDateRange>> validityDateMap = createColumnDateRanges(treeConcept);

		// union of all connectors of the concept
		final Select<?> unioned = treeConcept.getConnectors().stream()
											 .map(connector -> createConnectorQuery(connector, relevantColumns, validityDateMap))
											 .reduce(Select::unionAll)
											 .orElseThrow(IllegalStateException::new);

		// all connectors need the same columns originating from the concept definition - they might have different names in the respective connector tables,
		// but as we aliased them already, we can just use the unified aliases in the final query
		final List<Field<?>> relevantColumnsAliased = relevantColumns.get(treeConcept.getConnectors().get(0)).stream()
																	 .map(field -> field(field.getUnqualifiedName()))
																	 .collect(Collectors.toList());

		// if there is no validity date at all, no field is selected
		final Field<?> validityDateExpression = toValidityDateExpression(validityDateMap);

		final SelectJoinStep<Record> query = dslContext.select(relevantColumnsAliased)
													   .select(
															   count(asterisk()).as(EVENTS),
															   countDistinct(field(ENTITIES)).as(ENTITIES),
															   validityDateExpression.as(DATES)
													   )
													   .from(unioned);

		// not all dialects accept an empty group by () clause
		final Select<Record> finalQuery = relevantColumnsAliased.isEmpty() ? query : query.groupBy(relevantColumnsAliased);

		final ConceptTreeCache treeCache = new ConceptTreeCache(treeConcept);
		executionService.fetchStream(finalQuery)
						.forEach(record -> mapRecordToConceptElements(treeConcept, record, treeCache));
	}

	/**
	 * @return A map from a connector to all relevant columns the connector's concept defines. A relevant column is any column that is used by a
	 * {@link CTCondition} which is part of any child of a concept, or it's a concept's connector column.
	 */
	private Map<Connector, Set<Field<?>>> collectRelevantColumns(final TreeConcept treeConcept) {
		return treeConcept.getConnectors().stream()
						  .collect(Collectors.toMap(
								  Function.identity(),
								  connector -> collectRelevantColumns(connector, treeConcept)
						  ));
	}

	private Set<Field<?>> collectRelevantColumns(final Connector connector, TreeConcept concept) {
		final Set<Field<?>> out = new HashSet<>();

		if (connector.getColumn() != null) {
			out.add(field(name(connector.getColumn().resolve().getName())).as(CONNECTOR_COLUMN));
		}

		for (String name : collectRelevantColumns(concept.getChildren())) {
			out.add(field(name(name)));
		}

		return out;
	}

	private Set<String> collectRelevantColumns(final List<ConceptTreeChild> children) {
		return children.stream().flatMap(child -> collectRelevantColumns(child).stream()).collect(Collectors.toSet());
	}

	private Set<String> collectRelevantColumns(final ConceptTreeChild child) {
		final Set<String> childColumns = new HashSet<>();
		// Recursively collect columns from the current child's children, if they exist
		if (!child.getChildren().isEmpty()) {
			final Set<String> childrenColumns = collectRelevantColumns(child.getChildren());
			childColumns.addAll(childrenColumns);
		}
		// Add columns from the child's condition, if it exists
		if (child.getCondition() != null) {
			final Set<String> conditionColumns = child.getCondition().getAuxillaryColumns();
			childColumns.addAll(conditionColumns);
		}
		return childColumns;
	}

	private Map<Connector, List<ColumnDateRange>> createColumnDateRanges(final TreeConcept treeConcept) {
		final Map<Connector, List<ColumnDateRange>> map = new HashMap<>();
		final AtomicInteger counter = new AtomicInteger(0);
		for (final ConceptTreeConnector connector : treeConcept.getConnectors()) {
			if (connector.getValidityDates().isEmpty()) {
				continue;
			}
			map.put(connector, createColumnDateRanges(connector, counter));
		}
		return map;
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

		final List<Field<?>> validityDates = new ArrayList<>();

		for (Map.Entry<Connector, List<ColumnDateRange>> entry : validityDateMap.entrySet()) {
			for (ColumnDateRange columnDateRange : entry.getValue()) {

				// we have to select all possible validity dates of all connectors because we have to union multiple connectors
				ColumnDateRange dateRange = columnDateRange;

				// Therefore we usually select null
				if (entry.getKey() != connector) {
					dateRange = functionProvider.nulled(columnDateRange);
				}

				validityDates.addAll(dateRange.toFields());
			}
		}

		// connector might have a condition
		final Condition connectorCondition = connector.getCondition() == null
											 ? noCondition()
											 : toJooqCondition(connector, connector.getCondition());

		return dslContext.select(primaryKey)
						 .select(connectorColumns)
						 .select(validityDates)
						 .from(connectorTable)
						 .where(connectorCondition);
	}

	private Condition toJooqCondition(final Connector connector, CTCondition childCondition) {
		final CTConditionContext context = CTConditionContext.create(connector, functionProvider);
		return childCondition.convertToSqlCondition(context).condition();
	}

	/**
	 * Select the minimum of the least start date and the maximum of the greatest end date of all validity dates of all connectors.
	 */
	private Field<String> toValidityDateExpression(final Map<Connector, List<ColumnDateRange>> validityDateMap) {

		if (validityDateMap.isEmpty()) {
			return noField(String.class);
		}

		final List<ColumnDateRange> validityDates = validityDateMap.values().stream().flatMap(List::stream).map(functionProvider::toDualColumn).toList();
		// Need to use distinct as some ValidityDates overlap when using first/last day but also daterange
		final List<Field<Date>> allStarts = validityDates.stream().map(ColumnDateRange::getStart).distinct().toList();
		final List<Field<Date>> allEnds = validityDates.stream().map(ColumnDateRange::getEnd).distinct().toList();

		final ColumnDateRange minAndMax = ColumnDateRange.of(
				min(allStarts.size() > 1 ? functionProvider.least(allStarts) : allStarts.get(0)),
				max(allEnds.size() > 1 ? functionProvider.greatest(allEnds) : allEnds.get(0))
		);
		return functionProvider.daterangeStringExpression(minAndMax);
	}

	private void mapRecordToConceptElements(final TreeConcept treeConcept, final Record record, final ConceptTreeCache treeCache) {
		final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(record::intoMap);
		final MatchingStats.Entry entry = toMatchingStatsEntry(record);

		if (treeConcept.getChildren().isEmpty()) {
			addEntryToConceptElement(treeConcept, treeConcept.getName(), entry);
			return;
		}

		try {
			final String columnValue = record.get(CONNECTOR_COLUMN, String.class);

			final ConceptTreeChild mostSpecificChild = treeCache.findMostSpecificChild(columnValue, rowMap);

			//  database value did not match any node of the concept
			if (mostSpecificChild == null) {
				return;
			}

			// add child stats to all parents till concept root
			ConceptTreeNode<?> current = mostSpecificChild;
			while (current != null) {
				addEntryToConceptElement(current, columnValue, entry);
				current = current.getParent();
			}
		}
		catch (ConceptConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private MatchingStats.Entry toMatchingStatsEntry(Record record) {
		final long events = record.get(EVENTS, Integer.class).longValue();
		final long entities = record.get(ENTITIES, Integer.class).longValue();
		final CDateRange dateSpan = toDateRange(record.get(DATES, String.class));

		return new MatchingStats.Entry(events, entities, dateSpan.getMinValue(), dateSpan.getMaxValue());
	}

	private CDateRange toDateRange(final String validityDateExpression) {
		final List<Integer> dateRange = executionService.getResultSetProcessor().getCDateSetParser().toEpochDayRange(validityDateExpression);

		if (dateRange.isEmpty()) {
			return CDateRange.all();
		}

		return CDateRange.fromList(dateRange);
	}

}

package com.bakdata.conquery.mode.local;

import static org.jooq.impl.DSL.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.EqualCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.datasets.concepts.conditions.PrefixRangeCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;

@Slf4j
public class SqlUpdateMatchingStatsJob extends Job {

	private static final Name CONNECTOR_COLUMN = name("connector_column");
	private static final Name EVENTS = name("events");
	private static final String EVENTS_TABLE = "events_unioned";
	private static final Name ENTITIES = name("entities");
	private static final String ENTITIES_TABLE = "entities";
	private static final Name DATES = name("unioned");
	private static final String VALIDITY_DATES_TABLE = "validity_dates";

	private final DatabaseConfig databaseConfig;
	private final SqlExecutionService executionService;
	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final Set<ConceptId> concepts;
	private final ExecutorService executors;
	private final AtomicInteger counter;

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
		this.counter = new AtomicInteger(0);
	}

	@Override
	public String getLabel() {
		return "Calculating Matching Stats for %s.".formatted(executionService);
	}

	@Override
	public void execute() throws Exception {

		log.debug("BEGIN update Matching stats for {} Concepts.", concepts.size());

		final List<TreeConcept> regularApproach = new ArrayList<>();
		final List<TreeConcept> onlyEqualOrPrefixConditions = new ArrayList<>();

		concepts.stream()
				.map(ConceptId::resolve)
				.filter(SqlUpdateMatchingStatsJob::isTreeConcept)
				.forEach(concept -> {
					final TreeConcept treeConcept = (TreeConcept) concept;
					final List<ConceptTreeChild> conceptChildren = treeConcept.getChildren();
					if (conceptChildren.isEmpty()) {
						regularApproach.add(treeConcept);
					}
					else if (allConditionsOneOf(conceptChildren, List.of(EqualCondition.class, PrefixCondition.class, PrefixRangeCondition.class))) {
						onlyEqualOrPrefixConditions.add(treeConcept);
					}
					else {
						regularApproach.add(treeConcept);
					}
				});

		log.info("Matching Stats classification: regular => {}, onlyEqualOrPrefix => {}", regularApproach.size(), onlyEqualOrPrefixConditions.size());

		final long startTime = System.currentTimeMillis();
		final List<Future<Void>> runningQueries =
				Stream.concat(
							  regularApproach.stream().flatMap(concept -> walkAndCollectMatchingStats(concept.getConnectors(), concept)),
							  onlyEqualOrPrefixConditions.stream().map(AllEqualOrPrefixConditionsTask::new)
					  )
					  .map(executors::submit)
					  .toList();

		executors.shutdown();
		while (!executors.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for executors to set matching stats for all concepts...");
		}

		final long timeElapsed = System.currentTimeMillis() - startTime;
		log.info("DONE collecting matching stats. Elapsed time: {} ms. Executed standard queries: {}", timeElapsed, counter.get());

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

	private boolean allConditionsOneOf(final List<ConceptTreeChild> children, final List<Class<?>> conditions) {
		return children.stream().allMatch(child -> {
			if (child.getChildren().isEmpty()) {
				return child.getCondition() != null && conditions.stream().anyMatch(condition -> condition.isInstance(child.getCondition()));
			}
			if (child.getCondition() != null && conditions.stream().noneMatch(condition -> condition.isInstance(child.getCondition()))) {
				return false;
			}
			return allConditionsOneOf(child.getChildren(), conditions);
		});
	}

	private static void checkForError(final Future<Void> future) {
		try {
			future.get();
		}
		catch (ExecutionException | InterruptedException e) {
			log.error("Unknown error while querying SQL matching stats. Cause: \n", e.getCause());
		}
	}

	private Stream<RegularTask> walkAndCollectMatchingStats(final List<? extends Connector> connectors, final ConceptTreeNode<?> treeNode) {
		return Stream.concat(
				treeNode.getChildren().stream().flatMap(child -> walkAndCollectMatchingStats(connectors, child)),
				Stream.of(new RegularTask(connectors, (ConceptElement<?>) treeNode))
		);
	}

	private CDateRange toDateRange(final String validityDateExpression) {
		final List<Integer> dateRange = executionService.getResultSetProcessor().getCDateSetParser().toEpochDayRange(validityDateExpression);
		return !dateRange.isEmpty() ? CDateRange.fromList(dateRange) : null;
	}

	@RequiredArgsConstructor
	private class RegularTask implements Callable<Void> {

		private final List<? extends Connector> connectors;
		private final ConceptElement<?> treeNode;

		@Override
		public Void call() {
			final Optional<CTCondition> childCondition = treeNode instanceof ConceptTreeChild treeChild
														 ? Optional.of(treeChild.getCondition())
														 : Optional.empty();

			final long events = collectEventCount(connectors, childCondition);
			final long entities = collectEntityCount(connectors, childCondition);
			final CDateRange span = collectDateSpan(connectors, childCondition);

			final SqlMatchingStats matchingStats = new SqlMatchingStats(events, entities, span);
			treeNode.setMatchingStats(matchingStats);
			counter.incrementAndGet();

			return null;
		}

		/**
		 * Applies a count(*) on each connector's table, unions these tables and finally calculates the sum() of the count per connector
		 * to obtain the concept's total event count.
		 */
		private long collectEventCount(final List<? extends Connector> connectors, final Optional<CTCondition> childCondition) {

			final org.jooq.Table<Record1<Integer>> eventsUnioned =
					union(connectors, connector -> createCountEventsQuery(connector, childCondition), Select::unionAll, EVENTS_TABLE);

			final SelectJoinStep<Record1<BigDecimal>> eventsQuery = dslContext.select(sum(eventsUnioned.field(EVENTS, BigDecimal.class)).as(EVENTS))
																			  .from(eventsUnioned);

			final Result<?> result = executionService.fetch(eventsQuery);
			try {
				final BigDecimal events = (BigDecimal) result.getValue(0, field(EVENTS));
				return Objects.requireNonNull(events).longValue();
			}
			catch (Exception e) {
				log.error("Expecting exactly 1 column of numeric type and 1 row in Result when querying for events of a concept node. Error: ", e);
				return 0;
			}
		}

		private SelectConditionStep<Record1<Integer>> createCountEventsQuery(final Connector connector, final Optional<CTCondition> childCondition) {
			return dslContext.select(count().as(EVENTS))
							 .from(table(name(connector.getResolvedTable().getName())))
							 .where(toJooqCondition(connector, childCondition));
		}

		/**
		 * Selects the PIDs for each connector, unions these tables and does a countDistinct(pid) to obtain the concepts total entity count.
		 */
		private long collectEntityCount(final List<? extends Connector> connectors, final Optional<CTCondition> childCondition) {

			final org.jooq.Table<Record1<Object>> entitiesUnioned =
					union(connectors, connector -> createCountEntitiesQuery(connector, childCondition), Select::union, ENTITIES_TABLE);

			final SelectJoinStep<Record1<Integer>> entitiesQuery =
					dslContext.select(countDistinct(entitiesUnioned.field(ENTITIES)).as(ENTITIES))
							  .from(entitiesUnioned);

			final Result<?> result = executionService.fetch(entitiesQuery);
			try {
				// we will get an Integer as SQL return type of SUM select, but MatchingStats expect a long
				final Integer value = (Integer) result.getValue(0, field(ENTITIES));
				return Objects.requireNonNull(value).longValue();
			}
			catch (Exception e) {
				log.error("Expecting exactly 1 column of type Integer and 1 row in Result when querying for events of a concept node. Error: ", e);
				return 0;
			}
		}

		private SelectConditionStep<Record1<Object>> createCountEntitiesQuery(final Connector connector, final Optional<CTCondition> childCondition) {
			final Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), databaseConfig).as(ENTITIES);
			final Table<Record> connectorTable = table(name(connector.getResolvedTable().getName()));
			final Condition connectorCondition = toJooqCondition(connector, childCondition);
			return dslContext.select(primaryColumn)
							 .from(connectorTable)
							 .where(connectorCondition);
		}

		/**
		 * For each connector and each of its validity dates, we select the start and end date, union all these tables and select the min(start) and max(end)
		 * to obtain the concepts total date span.
		 *
		 * @return A {@link CDateRange} with the min and max validity date over all the given connectors. Null, if the given connectors have no validity date at all.
		 */
		private CDateRange collectDateSpan(final List<? extends Connector> connectors, final Optional<CTCondition> childCondition) {

			final Map<Connector, List<ColumnDateRange>> validityDateMap = connectors.stream().collect(
					// we create all validity dates with the same alias to union them later
					Collectors.toMap(Function.identity(), this::createColumnDateRanges)
			);
			if (validityDateMap.values().stream().allMatch(List::isEmpty)) {
				return null;
			}

			final org.jooq.Table<Record> validityDatesUnioned = unionAllValidityDates(validityDateMap, childCondition);
			// we just need any of the generated column date ranges to get the name of the unioned field(s)
			final ColumnDateRange anyOfTheUnionedDates = validityDateMap.get(connectors.get(0)).get(0);
			// ensure we have a start and end field (and not a single-column range), because we need to get the min(start) and max(end)
			final ColumnDateRange dualColumn = functionProvider.toDualColumn(anyOfTheUnionedDates);
			// the get the overall min and max
			final ColumnDateRange minAndMax = ColumnDateRange.of(min(dualColumn.getStart()), max(dualColumn.getEnd()));
			// finally, we create the proper string expression which handles possible +/-infinity date values
			final Field<String> validityDateExpression = functionProvider.daterangeStringExpression(minAndMax).as(DATES);
			final SelectJoinStep<Record1<String>> dateSpanQuery = dslContext.select(validityDateExpression)
																			.from(validityDatesUnioned);

			final Result<?> result = executionService.fetch(dateSpanQuery);
			try {
				final String dateExpression = (String) result.getValue(0, field(DATES));
				return toDateRange(dateExpression);
			}
			catch (Exception e) {
				log.error("Expecting exactly 1 column containing a daterange expression when querying for the date span of a concept. Error: ", e);
				return null;
			}
		}

		private List<ColumnDateRange> createColumnDateRanges(final Connector connector) {
			return connector.getValidityDates().stream()
							.map(functionProvider::forValidityDate)
							.map(daterange -> daterange.as(DATES.last()))
							.toList();
		}

		private org.jooq.Table<Record> unionAllValidityDates(final Map<Connector, List<ColumnDateRange>> validityDateMap, final Optional<CTCondition> childCondition) {
			return validityDateMap.entrySet().stream()
								  .flatMap(entry -> {
									  Connector connector = entry.getKey();
									  List<ColumnDateRange> validityDates = entry.getValue();
									  return validityDates.stream().map(columnDateRange -> createValidityDateQuery(columnDateRange, connector, childCondition));
								  })
								  .reduce((validityDate1, validityDate2) -> (SelectConditionStep<Record>) validityDate1.unionAll(validityDate2))
								  .orElseThrow(() -> new RuntimeException("Expected at least 1 validity date to be present."))
								  .asTable(name(VALIDITY_DATES_TABLE));
		}

		private SelectConditionStep<Record> createValidityDateQuery(
				final ColumnDateRange columnDateRange,
				final Connector connector,
				final Optional<CTCondition> childCondition
		) {
			return dslContext.select(columnDateRange.toFields())
							 .from(table(name(connector.getResolvedTable().getName())))
							 .where(toJooqCondition(connector, childCondition));
		}

		private static <T, R extends Record> org.jooq.Table<R> union(
				final Collection<T> input,
				final Function<T, Select<R>> mapper,
				final BinaryOperator<Select<R>> operator,
				final String tableName
		) {
			return input.stream()
						.map(mapper)
						.reduce(operator)
						.orElseThrow(() -> new IllegalStateException("Expected at least one element to union"))
						.asTable(name(tableName));
		}

		private Condition toJooqCondition(final Connector connector, final Optional<CTCondition> childCondition) {
			final CTConditionContext context = CTConditionContext.create(connector, functionProvider);
			return childCondition.or(() -> Optional.ofNullable(connector.getCondition()))
								 .map(condition -> condition.convertToSqlCondition(context).condition())
								 .orElse(noCondition());
		}
	}

	@RequiredArgsConstructor
	private class AllEqualOrPrefixConditionsTask implements Callable<Void> {

		private final TreeConcept treeConcept;

		@Override
		public Void call() {

			final Map<Connector, List<ColumnDateRange>> validityDateMap = createColumnDateRanges(treeConcept);
			final Select<?> unioned = treeConcept.getConnectors().stream()
												 .map(connector -> this.createConnectorQuery(connector, validityDateMap))
												 .reduce(Select::unionAll)
												 .orElseThrow(IllegalStateException::new);

			final List<ColumnDateRange> validityDates = validityDateMap.values().stream().flatMap(List::stream).map(functionProvider::toDualColumn).toList();
			final List<Field<Date>> allStarts = validityDates.stream().map(ColumnDateRange::getStart).toList();
			final List<Field<Date>> allEnds = validityDates.stream().map(ColumnDateRange::getEnd).toList();
			final ColumnDateRange minAndMax = ColumnDateRange.of(min(functionProvider.least(allStarts)), max(functionProvider.greatest((allEnds))));
			final Field<String> validityDateExpression = functionProvider.daterangeStringExpression(minAndMax).as(DATES);

			final Select<? extends Record> query = dslContext.select(
																	 field(CONNECTOR_COLUMN),
																	 count(asterisk()).as(EVENTS),
																	 countDistinct(field(ENTITIES)).as(ENTITIES),
																	 validityDateExpression
															 )
															 .from(unioned)
															 .groupBy(field(CONNECTOR_COLUMN));

			final Map<String, SqlMatchingStats> groupValueToStats = executionService.fetchStream(query).collect(Collectors.toMap(
					record -> record.get(CONNECTOR_COLUMN, String.class),
					record -> {
						final long events = record.get(EVENTS, Integer.class).longValue();
						final long entities = record.get(ENTITIES, Integer.class).longValue();
						final CDateRange dateSpan = toDateRange(record.get(DATES, String.class));
						return new SqlMatchingStats(events, entities, dateSpan);
					}
			));

			treeConcept.setMatchingStats(groupValueToStats.values().stream().reduce(SqlMatchingStats::add).orElseGet(SqlMatchingStats::empty));
			setAndAggregate(groupValueToStats, treeConcept.getChildren());

			return null;
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

		private Select<?> createConnectorQuery(final ConceptTreeConnector connector, final Map<Connector, List<ColumnDateRange>> validityDateMap) {

			final Table<Record> connectorTable = table(name(connector.getResolvedTable().getName()));
			final Field<Object> connectorColumn = field(name(connectorTable.getName(), connector.getColumn().resolve().getName())).as(CONNECTOR_COLUMN);
			final Field<Object> primaryKey = TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), databaseConfig).as(ENTITIES);

			// we have to select all possible validity dates of all connectors because we have to union multiple connectors
			final Stream<Field<?>> validityDates =
					validityDateMap.entrySet().stream().flatMap(entry -> entry.getValue().stream()
																			  .map(columnDateRange -> entry.getKey() == connector
																									  ? columnDateRange
																									  : functionProvider.nulled(columnDateRange))
																			  .flatMap(columnDateRange -> columnDateRange.toFields().stream()));

			return dslContext.select(Stream.concat(Stream.of(connectorColumn, primaryKey), validityDates).toList())
							 .from(connectorTable);
		}

		private void setAndAggregate(final Map<String, SqlMatchingStats> groupValueToStats, final List<ConceptTreeChild> children) {
			children.stream().parallel().forEach(child -> {
				final SqlMatchingStats nodeStats = new SqlMatchingStats();
				// node is leaf
				if (child.getChildren().isEmpty()) {
					collectByCondition(groupValueToStats, child, nodeStats);
				}
				else {
					// although node is not a leaf, it can have a condition
					if (child.getCondition() != null) {
						collectByCondition(groupValueToStats, child, nodeStats);
					}
					// recursively collect matching stats of children
					setAndAggregate(groupValueToStats, child.getChildren());
				}
				child.setMatchingStats(nodeStats);
			});
		}

		private static void collectByCondition(final Map<String, SqlMatchingStats> groupValueToStats, final ConceptTreeChild node, final SqlMatchingStats nodeStats) {

			// TODO make those methods of CTCondition and call directly on node.condition()
			if (node.getCondition() instanceof EqualCondition equalCondition) {
				equalCondition.getValues().forEach(val -> {
					final SqlMatchingStats statsForCondition = groupValueToStats.getOrDefault(val, SqlMatchingStats.empty());
					nodeStats.add(statsForCondition);
				});
				return;
			}
			else if (node.getCondition() instanceof PrefixCondition prefixCondition) {
				Arrays.stream(prefixCondition.getPrefixes()).forEach(prefix -> {
					final SqlMatchingStats statsForCondition = groupValueToStats.entrySet().stream()
																				.filter(entry -> entry.getKey().startsWith(prefix))
																				.map(Map.Entry::getValue)
																				.reduce(SqlMatchingStats::add)
																				.orElseGet(SqlMatchingStats::empty);
					nodeStats.add(statsForCondition);
				});
				return;
			}
			else if (node.getCondition() instanceof PrefixRangeCondition prefixRangeCondition) {
				final SqlMatchingStats statsForCondition = groupValueToStats.entrySet().stream()
																			.filter(entry -> {

																				final String groupValue = entry.getKey();
																				final String min = prefixRangeCondition.getMin();
																				final String max = prefixRangeCondition.getMax();

																				if (groupValue.length() < min.length()) {
																					return false;
																				}

																				String pref = groupValue.substring(0, min.length());
																				return min.compareTo(pref) <= 0 && max.compareTo(pref) >= 0;
																			})
																			.map(Map.Entry::getValue)
																			.reduce(SqlMatchingStats::add)
																			.orElseGet(SqlMatchingStats::empty);
				nodeStats.add(statsForCondition);
				return;
			}
			throw new IllegalArgumentException("Unsupported condition type: " + node.getCondition().getClass().getSimpleName());
		}

	}

}

package com.bakdata.conquery.mode.local;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
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
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Slf4j
public class SqlUpdateMatchingStatsJob extends Job {

	private static final String EVENTS_FIELD = "events";
	private static final String EVENTS_TABLE = "events_unioned";
	private static final String PRIMARY_COLUMN_ALIAS = SharedAliases.PRIMARY_COLUMN.getAlias();
	private static final String ENTITIES_TABLE = "entities";
	private static final String VALIDITY_DATE_SELECT = "unioned";
	private static final String VALIDITY_DATES_TABLE = "validity_dates";
	private static final String MIN_VALIDITY_DATE_FIELD = "min_validity_date";
	private static final String MAX_VALIDITY_DATE_FIELD = "max_validity_date";

	private final DatabaseConfig databaseConfig;
	private final SqlExecutionService executionService;
	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final Collection<Concept<?>> concepts;
	private final ExecutorService executors;

	public SqlUpdateMatchingStatsJob(
			DatabaseConfig databaseConfig,
			SqlExecutionService executionService,
			SqlFunctionProvider functionProvider,
			Collection<Concept<?>> concepts,
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

		concepts.stream()
				.filter(SqlUpdateMatchingStatsJob::isTreeConcept)
				.flatMap(concept -> collectMatchingStats(concept.getConnectors(), (TreeConcept) concept))
				.forEach(executors::submit);

		executors.shutdown();
		while (!executors.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for executors to set matching stats for all concepts...");
		}

		log.debug("DONE collecting matching stats.");
	}

	@Override
	public void cancel() {
		super.cancel();
		executors.shutdownNow();
	}

	private static boolean isTreeConcept(Concept<?> concept) {
		if (!(concept instanceof TreeConcept)) {
			log.error("Collecting MatchingStats is currently only supported for TreeConcepts.");
			return false;
		}
		return true;
	}

	private Stream<Runnable> collectMatchingStats(List<? extends Connector> connectors, ConceptTreeNode<?> treeNode) {
		return Stream.concat(
				treeNode.getChildren().stream().flatMap(child -> collectMatchingStats(connectors, child)),
				Stream.of(new SqlMatchingStatsTask(connectors, treeNode))
		);
	}

	/**
	 * Applies a count(*) on each connector's table, unions these tables and finally calculates the sum() of the count per connector
	 * to obtain the concept's total event count.
	 */
	private long collectEventCount(List<? extends Connector> connectors, Optional<CTCondition> childCondition) {

		org.jooq.Table<Record1<Integer>> eventsUnioned =
				union(connectors, connector -> createCountEventsQuery(connector, childCondition), Select::unionAll, EVENTS_TABLE);

		SelectJoinStep<Record1<BigDecimal>> eventsQuery = dslContext.select(DSL.sum(eventsUnioned.field(EVENTS_FIELD, BigDecimal.class)).as(EVENTS_FIELD))
																	.from(eventsUnioned);

		Result<?> result = executionService.fetch(eventsQuery);
		try {
			BigDecimal events = (BigDecimal) result.getValue(0, EVENTS_FIELD);
			return Objects.requireNonNull(events).longValue();
		}
		catch (Exception e) {
			log.error("Expecting exactly 1 column of numeric type and 1 row in Result when querying for events of a concept node. Error: ", e);
			return 0;
		}
	}

	private SelectConditionStep<Record1<Integer>> createCountEventsQuery(Connector connector, Optional<CTCondition> childCondition) {
		return dslContext.select(DSL.count().as(EVENTS_FIELD))
						 .from(DSL.table(DSL.name(connector.getTable().getName())))
						 .where(toJooqCondition(connector, childCondition));
	}

	/**
	 * Selects the PIDs for each connector, unions these tables and does a countDistinct(pid) to obtain the concepts total entity count.
	 */
	private long collectEntityCount(List<? extends Connector> connectors, Optional<CTCondition> childCondition) {

		org.jooq.Table<Record1<Object>> entitiesUnioned =
				union(connectors, connector -> createCountEntitiesQuery(connector, childCondition), Select::union, ENTITIES_TABLE);

		SelectJoinStep<Record1<Integer>> entitiesQuery = dslContext.select(
																		   DSL.countDistinct(entitiesUnioned.field(PRIMARY_COLUMN_ALIAS))
																			  .as(PRIMARY_COLUMN_ALIAS)
																   )
																   .from(entitiesUnioned);

		Result<?> result = executionService.fetch(entitiesQuery);
		try {
			// we will get an Integer as SQL return type of SUM select, but MatchingStats expect a long
			Integer value = (Integer) result.getValue(0, PRIMARY_COLUMN_ALIAS);
			return Objects.requireNonNull(value).longValue();
		}
		catch (Exception e) {
			log.error("Expecting exactly 1 column of type Integer and 1 row in Result when querying for events of a concept node. Error: ", e);
			return 0;
		}
	}

	private SelectConditionStep<Record1<Object>> createCountEntitiesQuery(Connector connector, Optional<CTCondition> childCondition) {
		Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(connector.getTable(), databaseConfig).as(PRIMARY_COLUMN_ALIAS);
		Table<Record> connectorTable = DSL.table(DSL.name(connector.getTable().getName()));
		Condition connectorCondition = toJooqCondition(connector, childCondition);
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
	private CDateRange collectDateSpan(List<? extends Connector> connectors, Optional<CTCondition> childCondition) {

		Map<Connector, List<ColumnDateRange>> validityDateMap = connectors.stream().collect(
				// we create all validity dates with the same alias to union them later
				Collectors.toMap(Function.identity(), connector -> createColumnDateRanges(connector, VALIDITY_DATE_SELECT))
		);
		if (validityDateMap.isEmpty()) {
			return null;
		}

		org.jooq.Table<Record> validityDatesUnioned = unionAllValidityDates(validityDateMap, childCondition);
		// we just need any of the generated column date ranges to get the name of the unioned field(s)
		ColumnDateRange anyOfTheUnionedDates = validityDateMap.get(connectors.get(0)).get(0);
		// ensure we have a start and end field (and not a single-column range), because we need to get the min(start) and max(end)
		ColumnDateRange dualColumn = functionProvider.toDualColumn(anyOfTheUnionedDates);
		// the get the overall min and max
		ColumnDateRange minAndMax = ColumnDateRange.of(DSL.min(dualColumn.getStart()), DSL.max(dualColumn.getEnd()));
		// finally, we create the proper string expression which handles possible +/-infinity date values
		Field<String> validityDateExpression = functionProvider.daterangeStringExpression(minAndMax).as(VALIDITY_DATE_SELECT);
		SelectJoinStep<Record1<String>> dateSpanQuery = dslContext.select(validityDateExpression)
																  .from(validityDatesUnioned);

		Result<?> result = executionService.fetch(dateSpanQuery);
		try (ResultSet resultSet = result.intoResultSet()) {

			// If no values were encountered this the result is empty: Table might be empty, or condition does not match any node.
			if (!resultSet.isBeforeFirst()) {
				return null;
			}

			resultSet.next(); // we advance to first line of the ResultSet
			List<Integer> dateRange = executionService.getResultSetProcessor().getDateRange(resultSet, 1);
			return CDateRange.fromList(dateRange);
		}
		catch (Exception e) {
			log.error("Expecting exactly 2 columns (start and end date) of type date when querying for the date span of a concept. Error: ", e);
			return null;
		}
	}

	private List<ColumnDateRange> createColumnDateRanges(Connector connector, String alias) {
		return connector.getValidityDates().stream()
						.map(functionProvider::forValidityDate)
						.map(daterange -> daterange.as(alias))
						.toList();
	}

	private org.jooq.Table<Record> unionAllValidityDates(Map<Connector, List<ColumnDateRange>> validityDateMap, Optional<CTCondition> childCondition) {
		return validityDateMap.entrySet().stream()
							  .flatMap(entry -> {
								  Connector connector = entry.getKey();
								  List<ColumnDateRange> validityDates = entry.getValue();
								  return validityDates.stream().map(columnDateRange -> createValidityDateQuery(columnDateRange, connector, childCondition));
							  })
							  .reduce((validityDate1, validityDate2) -> (SelectConditionStep<Record>) validityDate1.unionAll(validityDate2))
							  .orElseThrow(() -> new RuntimeException("Expected at least 1 validity date to be present."))
							  .asTable(DSL.name(VALIDITY_DATES_TABLE));
	}

	private SelectConditionStep<Record> createValidityDateQuery(ColumnDateRange columnDateRange, Connector connector, Optional<CTCondition> childCondition) {
		return dslContext.select(columnDateRange.toFields())
						 .from(DSL.table(DSL.name(connector.getTable().getName())))
						 .where(toJooqCondition(connector, childCondition));
	}

	private static <T, R extends Record> org.jooq.Table<R> union(
			Collection<T> input,
			Function<T, Select<R>> mapper,
			BinaryOperator<Select<R>> operator,
			String tableName
	) {
		return input.stream()
					.map(mapper)
					.reduce(operator)
					.orElseThrow(() -> new IllegalStateException("Expected at least one element to union"))
					.asTable(DSL.name(tableName));
	}

	private Condition toJooqCondition(Connector connector, Optional<CTCondition> childCondition) {
		CTConditionContext context = new CTConditionContext(connector.getTable(), connector.getColumn(), functionProvider);
		return childCondition.or(() -> Optional.ofNullable(connector.getCondition()))
							 .map(condition -> condition.convertToSqlCondition(context).condition())
							 .orElse(DSL.noCondition());
	}

	@RequiredArgsConstructor
	private class SqlMatchingStatsTask implements Runnable {

		private final List<? extends Connector> connectors;
		private final ConceptTreeNode<?> treeNode;

		@Override
		public void run() {
			Optional<CTCondition> childCondition = treeNode instanceof ConceptTreeChild treeChild
												   ? Optional.of(treeChild.getCondition())
												   : Optional.empty();

			long events = collectEventCount(connectors, childCondition);
			long entities = collectEntityCount(connectors, childCondition);
			CDateRange span = collectDateSpan(connectors, childCondition);

			SqlMatchingStats matchingStats = new SqlMatchingStats(events, entities, span);
			treeNode.setMatchingStats(matchingStats);
		}
	}

}

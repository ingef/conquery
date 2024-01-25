package com.bakdata.conquery.models.jobs;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.mode.local.SqlMatchingStats;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;

@Slf4j
public class SqlUpdateMatchingStatsJob extends Job {

	private static final String EVENTS_FIELD = "events";
	private static final String EVENTS_TABLE = "events_unioned";
	private static final String PRIMARY_COLUMN = "primary_column";
	private static final String ENTITIES_TABLE = "entities";
	private static final String VALIDITY_DATE_SELECT = "unioned";
	private static final String VALIDITY_DATES_TABLE = "validity_dates";
	private static final String MIN_VALIDITY_DATE_FIELD = "min_validity_date";
	private static final String MAX_VALIDITY_DATE_FIELD = "max_validity_date";

	private final SqlConnectorConfig sqlConnectorConfig;
	private final SqlExecutionService executionService;
	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final Collection<Concept<?>> concepts;

	public SqlUpdateMatchingStatsJob(
			SqlConnectorConfig sqlConnectorConfig,
			SqlExecutionService executionService,
			SqlFunctionProvider functionProvider,
			Collection<Concept<?>> concepts
	) {
		this.sqlConnectorConfig = sqlConnectorConfig;
		this.executionService = executionService;
		this.dslContext = executionService.getDslContext();
		this.functionProvider = functionProvider;
		this.concepts = concepts;
	}

	@Override
	public String getLabel() {
		return "Calculating Matching Stats for %s.".formatted(executionService);
	}

	@Override
	public void execute() throws Exception {

		log.debug("BEGIN update Matching stats for {} Concepts.", concepts.size());

		for (Concept<?> concept : concepts) {
			if (!(concept instanceof TreeConcept tree)) {
				log.error("Collecting MatchingStats is currently only supported for TreeConcepts.");
				break;
			}
			SqlMatchingStats matchingStats = collectMatchingStats(concept.getConnectors(), tree);
			concept.setMatchingStats(matchingStats);
		}

		log.debug("DONE collecting matching stats.");
	}

	private SqlMatchingStats collectMatchingStats(List<? extends Connector> connectors, ConceptTreeNode<?> treeNode) {

		treeNode.getChildren().forEach(child -> {
			SqlMatchingStats childStats = collectMatchingStats(connectors, child);
			child.setMatchingStats(childStats);
		});

		Optional<CTCondition> childCondition = treeNode instanceof ConceptTreeChild treeChild
											   ? Optional.of(treeChild.getCondition())
											   : Optional.empty();

		long events = collectEventCount(connectors, childCondition);
		long entities = collectEntityCount(connectors, childCondition);
		CDateRange span = collectDateSpan(connectors, childCondition);

		return new SqlMatchingStats(events, entities, span);
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

		SelectJoinStep<Record1<Integer>> entitiesQuery = dslContext.select(DSL.countDistinct(entitiesUnioned.field(PRIMARY_COLUMN)).as(PRIMARY_COLUMN))
																   .from(entitiesUnioned);

		Result<?> result = executionService.fetch(entitiesQuery);
		try {
			// we will get an Integer as SQL return type of SUM select, but MatchingStats expect a long
			Integer value = (Integer) result.getValue(0, PRIMARY_COLUMN);
			return Objects.requireNonNull(value).longValue();
		}
		catch (Exception e) {
			log.error("Expecting exactly 1 column of type Integer and 1 row in Result when querying for events of a concept node. Error: ", e);
			return 0;
		}
	}

	private SelectConditionStep<Record1<Object>> createCountEntitiesQuery(Connector connector, Optional<CTCondition> childCondition) {
		return dslContext.select(TablePrimaryColumnUtil.findPrimaryColumn(connector.getTable(), sqlConnectorConfig))
						 .from(DSL.table(DSL.name(connector.getTable().getName())))
						 .where(toJooqCondition(connector, childCondition));
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
		SelectJoinStep<Record2<Date, Date>> dateSpanQuery = dslContext.select(
																			  DSL.min(dualColumn.getStart()).as(MIN_VALIDITY_DATE_FIELD),
																			  DSL.max(dualColumn.getEnd()).as(MAX_VALIDITY_DATE_FIELD)
																	  )
																	  .from(validityDatesUnioned);

		Result<?> result = executionService.fetch(dateSpanQuery);
		try {
			LocalDate minDate = getDateFromResult(result, MIN_VALIDITY_DATE_FIELD, LocalDate.MIN);
			LocalDate maxDate = getDateFromResult(result, MAX_VALIDITY_DATE_FIELD, LocalDate.MAX);
			if (maxDate != LocalDate.MAX) {
				// we treat the end date as excluded internally when using ColumnDateRanges, but a CDateRange expects an inclusive range
				maxDate = maxDate.minusDays(1);
			}
			return CDateRange.of(minDate, maxDate);
		}
		catch (Exception e) {
			log.error("Expecting exactly 2 columns (start and end date) of type date when querying for the date span of a concept. Error: ", e);
			return null;
		}
	}

	private List<ColumnDateRange> createColumnDateRanges(Connector connector, String alias) {
		return connector.getValidityDates().stream()
						.map(validityDate -> functionProvider.daterange(validityDate, connector.getTable().getName(), alias))
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

	private LocalDate getDateFromResult(Result<?> result, String field, LocalDate defaultDate) {
		return Optional.ofNullable(result.getValue(0, field))
					   .map(Object::toString)
					   .map(LocalDate::parse)
					   .orElse(defaultDate);
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

}

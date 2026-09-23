package com.bakdata.conquery.sql.conquery;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptIdMapping;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Stopwatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

import static org.jooq.impl.DSL.*;

@Slf4j
@Data
public class SqlMatchingStats {

	/**
	 * Legacy backend implementation requires separation by source (in that case different shards). For sql it's a constant.
	 */
	private static final String SQL_SOURCE_MATCHING_STATS_LABEL = "sql";

	private static final Field<String> PID_FIELD = field(name("pid"), String.class);
	private static final Field<Date> LB_FIELD = field(name("lower_bound"), Date.class);
	private static final Field<Date> UB_FIELD = field(name("upper_bound"), Date.class);
	private static final Field<Integer> CONCEPT_ID_FIELD = field(name(ConceptIdMapping.RESOLVED_ID_COLUMN), Integer.class);
	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final String defaultPrimaryColumn;
	private final int fetchBatchSize = 100;
	private final int matchingStatsWorkers;
	private final int matchingStatsRetries;

	private static void assignStatsToPath(
			ConceptElement<?> element,
			MatchingStats.Accumulator[] matchingStats,
			String entity,
			int minDate,
			int maxDate
	) {
		while (element != null) {
			int localId = element.getLocalId();
			MatchingStats.Accumulator accumulator = matchingStats[localId];
			if (accumulator == null) {
				accumulator = new MatchingStats.Accumulator();
				matchingStats[localId] = accumulator;
			}
			accumulator.addEvents(entity, 1, minDate, maxDate);
			element = element.getParent();
		}
	}

	private static <T extends Record> Select<T> unionSelects(List<Select<? extends T>> connectorTableSelects) {
		Select<T> unioned = null;

		for (Select<? extends T> connectorTable : connectorTableSelects) {
			if (unioned == null) {
				unioned = (Select<T>) connectorTable;
				continue;
			}

			unioned = unioned.unionAll(connectorTable);
		}


		return unioned;
	}

	/**
	 * Assembles the join table and inserts it into the database.
	 *
	 * @param concept
	 */
	public void createConceptIdJoinTable(TreeConcept concept) {
		ConceptIdMapping mapping = new ConceptIdMapping(concept, functionProvider);
		Name tableName = mapping.getTableName();

		deleteConceptIdJoinTable(concept.getId());
		List<Field<?>> fields = createConceptIdsTable(tableName, mapping.tableFields());

		insertConceptIdMappings(tableName, fields, mapping.getRows(), dslContext);
		createConceptIdIndexes(mapping);
	}

	private void createConceptIdIndexes(ConceptIdMapping mapping) {
		if (dslContext.dialect().family() == SQLDialect.CLICKHOUSE) {
			log.debug("Skipping secondary indexes for ClickHouse concept id table {}", mapping.getTableName());
			return;
		}

		String indexToken = Integer.toUnsignedString(mapping.getTableName().hashCode(), 16);
		if (!mapping.getKeyFields().isEmpty()) {
			dslContext.createIndex(name("cq_map_%s_keys".formatted(indexToken)))
					.on(mapping.table(), mapping.getKeyFields().stream().map(Field::sortDefault).toList())
					.execute();
		}
		dslContext.createIndex(name("cq_map_%s_id".formatted(indexToken)))
				.on(mapping.table(), mapping.resolvedId().sortDefault())
				.execute();
	}

	@NotNull
	private Field<Date>[] collectValidityDateFields(Connector connector) {
		List<Field<Date>> validityDates = new ArrayList<>();

		for (ValidityDate validityDate : connector.getValidityDates()) {
			if (validityDate.isSingleColumnDaterange()) {
				Column column = validityDate.getColumn().get();
				validityDates.add(field(name(column.getName()), Date.class));
			} else {
				validityDates.add(field(name(validityDate.getStartColumn().getColumn()), Date.class));
				validityDates.add(field(name(validityDate.getEndColumn().getColumn()), Date.class));
			}

		}
		return (Field<Date>[]) validityDates.toArray(Field[]::new);
	}

	private void assignStats(TreeConcept concept, MatchingStats.Accumulator[] matchingStats) {
		for (int localId = 0; localId < matchingStats.length; localId++) {
			assignStats(concept.getElementByLocalId(localId), matchingStats[localId]);
		}
	}

	private void assignStats(ConceptElement<?> element, MatchingStats.Accumulator accumulator) {
		if (accumulator == null) {
			element.setMatchingStats(null);
			return;
		}

		element.setMatchingStats(MatchingStats.singleEntry(SQL_SOURCE_MATCHING_STATS_LABEL, accumulator.finish()));
	}

	@NotNull
	private MatchingStats.Accumulator[] readStats(TreeConcept concept, SelectJoinStep<? extends Record> selectJoinStep) {
		MatchingStats.Accumulator[] matchingStats = new MatchingStats.Accumulator[concept.countElements()];

		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info("BEGIN fetching matching stats for {}", concept.getId());
		log.trace("{}", selectJoinStep);

		try (Cursor<? extends Record> cursor = selectJoinStep.fetchSize(fetchBatchSize).fetchLazy()) {

			for (Record record : cursor) {

				Integer rawId = record.get(CONCEPT_ID_FIELD);
				ConceptElement<?> resolvedId;
				if (rawId == null) {
					resolvedId = concept;
				} else {
					resolvedId = concept.getElementByLocalId(rawId);
				}

				String entity = record.get(PID_FIELD);
				Date min = record.get(LB_FIELD);
				Date max = record.get(UB_FIELD);

				int minDate = min != null ? CDate.ofLocalDate(min.toLocalDate()) : Integer.MAX_VALUE;
				int maxDate = max != null ? CDate.ofLocalDate(max.toLocalDate()) : Integer.MIN_VALUE;

				assignStatsToPath(resolvedId, matchingStats, entity, minDate, maxDate);
			}
		}

		log.debug("DONE fetching matching stats for {} within {}", concept.getId(), stopwatch);

		return matchingStats;
	}

	private void insertConceptIdMappings(Name tableName, List<Field<?>> fieldNames, List<RowN> rows, DSLContext dsl) {
		log.info("BEGIN inserting {} rows into {}", rows.size(), tableName);
		Stopwatch stopwatch = Stopwatch.createStarted();

		// We're using batching here because some DBMS don't allow mass inserts.
		// There's a chance, we rework this to use a prepared statement with lots of bindings under the hood. But that needs to rework the entire stream of rows.
		List<InsertValuesStepN<?>> inserts = new ArrayList<>(rows.size());

		for (RowN row : rows) {
			inserts.add(dsl.insertInto(table(tableName)).columns(fieldNames).values(row));
		}

		dsl.batch(inserts).execute();

		log.debug("DONE inserting into {} within {}", tableName, stopwatch);
	}

	/**
	 * Create a table and its fields. Assumes the table has been dropped already.
	 */
	private List<Field<?>> createConceptIdsTable(Name tableName, List<Field<?>> fields) {
		log.debug("Creating table {} with fields {}", tableName, fields);

		CreateTableElementListStep createTable =
				dslContext.createTable(tableName)
						.columns(fields);

		createTable.execute();

		return fields;
	}

	public void collectMatchingStatsForConcept(TreeConcept concept, int tries) {
		int remainingTries = tries;
		while (true) {
			try {
				dslContext.connection(connection -> {
					SelectJoinStep<? extends Record> matchingStatsStatement = createMatchingStatsStatement(concept);
					MatchingStats.Accumulator[] matchingStats = readStats(concept, matchingStatsStatement);
					assignStats(concept, matchingStats);
				});
				return;
			} catch (DataAccessException e) {
				if (remainingTries == 0) {
					log.debug("Failed to collect matching stats for concept {}. No retries remaining.", concept.getId(), e);
					throw e;
				}

				log.debug("Failed to collect matching stats for concept {}. Retrying.", concept.getId(), (Exception) (log.isTraceEnabled() ? e : null));
				remainingTries--;
			}
		}
	}

	@NotNull
	private SelectJoinStep<? extends Record> createMatchingStatsStatement(TreeConcept concept) {

		List<Select<? extends Record>> connectorTables = new ArrayList<>();
		ConceptIdMapping mapping = new ConceptIdMapping(concept, functionProvider);

		Field<Date> positiveInfinity = functionProvider.getMaxDateExpression();
		Field<Date> negativeInfinity = functionProvider.getMinDateExpression();

		for (Connector connector : concept.getConnectors()) {

			Field<Date>[] validityDates = collectValidityDateFields(connector);

			Name tableName = name(connector.getResolvedTable().getName());

			Condition condition = noCondition();

			if (connector.getColumn() != null) {
				condition = field(name(tableName, name(connector.getColumn().getColumn()))).isNotNull();
			}

			if (connector.getCondition() != null) {
				CTConditionContext context = CTConditionContext.forConnector(connector, functionProvider);
				condition = condition.and(connector.getCondition().convertToSqlCondition(context).condition());
			}

			SelectConditionStep<? extends Record> connectorTable =
					dslContext.select(
									TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), defaultPrimaryColumn).as(PID_FIELD),
									// The infinities are intentionally swapped
									least(positiveInfinity, validityDates).as(LB_FIELD),
									greatest(negativeInfinity, validityDates).as(UB_FIELD),
									CONCEPT_ID_FIELD)
							.from(table(tableName))
							.leftJoin(mapping.table())
							// join onto the concept-ids table to assign the most specific id.
							.on(mapping.joinCondition(connector))
							.where(condition);

			connectorTables.add(connectorTable);
		}

		Name ct_name = name("connector_tables");
		CommonTableExpression<?> unioned = ct_name.as(unionSelects(connectorTables));

		SelectJoinStep<Record4<Integer, String, Date, Date>> records = dslContext.with(unioned).select(unioned.field(CONCEPT_ID_FIELD), PID_FIELD,
				// The infinities are intentionally swapped
				nullif(unioned.field(LB_FIELD), positiveInfinity).as(LB_FIELD), nullif(unioned.field(UB_FIELD), negativeInfinity).as(UB_FIELD)).from(ct_name);

		return records;
	}

	public void deleteConceptIdJoinTable(ConceptId concept) {
		Name tableName = ConceptIdMapping.tableName(concept);
		log.debug("Trying to delete id-table {}", tableName);

		try {
			dslContext.dropTable(tableName).execute();
		} catch (DataAccessException exception) {
			// Likely it doesn't exist. Some DBMS just don't support drop-IfExists so this is the next best thing :^)
			log.trace("Failed to drop table {}", tableName, exception);
		}
	}

}

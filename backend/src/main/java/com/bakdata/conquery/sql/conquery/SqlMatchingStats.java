package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.*;

import jakarta.validation.constraints.NotBlank;
import java.sql.Date;
import java.util.*;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

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
	private static final Field<Integer> CONCEPT_ID_FIELD = field(name("resolved_id"), Integer.class);
	private static final Set<Param<?>> NULL_PARAMS = Collections.singleton(inline(null, String.class));

	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final String defaultPrimaryColumn;
	private final int fetchBatchSize = 100;
	private final int matchingStatsWorkers;
	private final int matchingStatsRetries;

	private static void assignStatsToPath(
		ConceptElement<?> element,
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats,
		String entity,
		CDateRange span) {
		while (element != null) {
			ConceptElementId<?> id = element.getId();

			matchingStats.computeIfAbsent(id, (ignored) -> new MatchingStats.Entry()).addEvents(entity, 1, span);
			element = element.getParent();
		}
	}

	/**
	 * collect unique fields used/defined in the expressions.
	 */
	private static List<Field<?>> collectReferencedFields(List<CTCondition.ConceptConditions> conceptConditions) {
		List<Field<?>> fields = conceptConditions.stream()
			.flatMap(
				e -> e.conditions().keySet().stream())
			.distinct()
			.toList();
		return fields;
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

	private static Param<?> defaultValue(Field<?> field) {
		if (field.getDataType().isBoolean()) {
			return inline(false);
		}

		if (field.getDataType().isString()) {
			return inline(null, String.class);
		}

		throw new IllegalStateException("Fields of type %s are not expected".formatted(field.getDataType()));
	}

	/**
	 * Assembles the join table and inserts it into the database.
	 *
	 * @param concept
	 */
	public void createConceptIdJoinTable(TreeConcept concept) {
		CTConditionContext context = CTConditionContext.forJoinTables(functionProvider);

		List<CTCondition.ConceptConditions> conceptConditions = collectAllExpressions(concept, null, context);

		List<Field<?>> allFields = collectReferencedFields(conceptConditions);

		List<RowN> rows = expressionsToRows(conceptConditions, allFields);

		Name tableName = idsTableName(concept.getName());

		// Make sure there's no table present.
		deleteConceptIdJoinTable(concept.getId());
		List<Field<?>> fields = createConceptIdsTable(tableName, allFields);

		insertConceptIdMappings(tableName, fields, rows, dslContext);
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

	private void assignStats(Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats) {
		for (Map.Entry<ConceptElementId<?>, MatchingStats.Entry> entry : matchingStats.entrySet()) {
			ConceptElementId<?> conceptElementId = entry.getKey();

			MatchingStats stats = new MatchingStats();
			stats.putEntry(SQL_SOURCE_MATCHING_STATS_LABEL, entry.getValue());
			conceptElementId.resolve().setMatchingStats(stats);
		}
	}

	@NotNull
	private Map<ConceptElementId<?>, MatchingStats.Entry> readStats(
		TreeConcept concept,
		SelectJoinStep<? extends Record> selectJoinStep) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats = new HashMap<>();

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

				CDateRange span = CDateRange.of(
					min != null ? min.toLocalDate() : null,
					max != null ? max.toLocalDate() : null);

				assignStatsToPath(resolvedId, matchingStats, entity, span);
			}
		}

		log.debug("DONE fetching matching stats for {} within {}", concept.getId(), stopwatch);

		return matchingStats;
	}

	@NotNull
	private Name idsTableName(@NotBlank String name) {
		return name("%s_ids".formatted(name));
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
	 * Create table and fields. Assumes, table has been dropped already.
	 */
	private List<Field<?>> createConceptIdsTable(Name tableName, List<Field<?>> keyFields) {

		List<Field<?>> fields = new ArrayList<>();

		fields.addAll(keyFields);
		fields.addFirst(CONCEPT_ID_FIELD);

		log.debug("Creating table {} with fields {}", tableName, fields);

		//TODO Option to create primaryKeys and indices here, but Hana is a bit flaky with it, would need to differentiate the impls.

		CreateTableElementListStep createTable = dslContext.createTable(tableName).columns(fields);

		createTable.execute();

		return fields;
	}

	public ListenableFuture<?> collectMatchingStatsForConcept(
		TreeConcept concept,
		ListeningExecutorService executorService,
		int tries) {
		return executorService.submit(() -> {
			dslContext.connection(cfg -> {
				try {
					SelectJoinStep<? extends Record> matchingStatsStatement = createMatchingStatsStatement(concept);
					Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats = readStats(
						concept,
						matchingStatsStatement);
					assignStats(matchingStats);

				} catch (DataAccessException e) {
					log.debug(
						"Failed to connect to database for concept {}. Retrying.",
						concept.getId(),
						(Exception) (log.isTraceEnabled() || tries == 0 ? e : null));

					if (tries > 0) {
						collectMatchingStatsForConcept(concept, executorService, tries - 1);
					}
				}
			});
		});
	}

	@NotNull
	private SelectJoinStep<? extends Record> createMatchingStatsStatement(TreeConcept concept) {

		List<Select<? extends Record>> connectorTables = new ArrayList<>();

		Field<Date> positiveInfinity = functionProvider.getMaxDateExpression();
		Field<Date> negativeInfinity = functionProvider.getMinDateExpression();

		for (Connector connector : concept.getConnectors()) {

			CTConditionContext context = CTConditionContext.forConnector(connector, functionProvider);

			Field<Date>[] validityDates = collectValidityDateFields(connector);

			SelectConditionStep<? extends Record> connectorTable = dslContext.select(
				TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), defaultPrimaryColumn)
					.as(
						PID_FIELD),
				// The infinities are intentionally swapped
				least(positiveInfinity, validityDates).as(LB_FIELD),
				greatest(negativeInfinity, validityDates).as(UB_FIELD),
				CONCEPT_ID_FIELD)
				.from(table(name(connector.getResolvedTable().getName())))
				.leftJoin(
					idsTableName(concept.getName()))
				// join onto the concept-ids table to assign the most specific id.
				.on(getJoinConditions(concept, context))
				.where(
					connector.getCondition() != null ? connector.getCondition()
						.convertToSqlCondition(
							context)
						.condition() : noCondition());

			connectorTables.add(connectorTable);
		}

		Name ct_name = name("connector_tables");
		CommonTableExpression<?> unioned = ct_name.as(unionSelects(connectorTables));

		SelectJoinStep<Record4<Integer, String, Date, Date>> records = dslContext.with(unioned)
			.select(
				unioned.field(CONCEPT_ID_FIELD),
				PID_FIELD,
				// The infinities are intentionally swapped
				nullif(unioned.field(LB_FIELD), positiveInfinity).as(LB_FIELD),
				nullif(unioned.field(UB_FIELD), negativeInfinity).as(UB_FIELD))
			.from(ct_name);

		return records;
	}

	public void deleteConceptIdJoinTable(ConceptId concept) {
		Name tableName = idsTableName(concept.getName());
		log.debug("Trying to delete id-table {}", tableName);

		try {
			dslContext.dropTable(tableName).execute();
		} catch (DataAccessException exception) {
			// Likely it doesn't exist. Some DBMS just don't support drop-IfExists so this is the next best thing :^)
			log.trace("Failed to drop table {}", tableName, exception);
		}
	}

	/**
	 * Using the expressions of a concept, build a Condition that descibes the left-join onto the ids table, from any connector-table.
	 */
	private Condition getJoinConditions(TreeConcept concept, CTConditionContext context) {
		List<CTCondition.ConceptConditions> conceptConditions = collectAllExpressions(concept, null, context);


		if (conceptConditions.isEmpty()) {
			return context.getFunctionProvider().unconditionalJoinCondition();
		}

		Set<Condition> conditions = new HashSet<>();

		for (CTCondition.ConceptConditions conceptCondition : conceptConditions) {
			for (Map.Entry<Field<?>, CTCondition.FieldCondition> entry : conceptCondition.conditions().entrySet()) {
				conditions.add(entry.getKey().eq((Field) entry.getValue().extractor()));
			}
		}

		Condition reduced = conditions.stream().reduce(noCondition(), Condition::and);

		if (reduced.equals(noCondition())) {
			return context.getFunctionProvider().unconditionalJoinCondition();
		}

		return reduced;
	}

	private List<RowN> expressionsToRows(
		List<CTCondition.ConceptConditions> conceptConditions,
		List<Field<?>> allFields) {
		Map<List<Param<?>>, ConceptElement<?>> byDepth = new HashMap<>();

		for (CTCondition.ConceptConditions conceptCondition : conceptConditions) {
			ConceptElement<?> elt = conceptCondition.conceptElement();
			Map<Field<?>, CTCondition.FieldCondition> conditions = conceptCondition.conditions();

			List<Set<Param<?>>> rowValues = new ArrayList<>();
			for (Field<?> field : allFields) {
				if (conditions.containsKey(field)) {
					rowValues.add(conditions.get(field).params());
				} else {
					rowValues.add(Set.of(defaultValue(field)));
				}
			}

			Set<List<Param<?>>> flattened = Sets.cartesianProduct(rowValues);

			// Group by params, find deepest params. This ensures we map to the most-specific element.
			for (List<Param<?>> params : flattened) {
				byDepth.compute(params, (__, prior) -> {
					if (prior == null || prior.getDepth() < elt.getDepth()) {
						return elt;
					}
					if (prior.getDepth() == elt.getDepth() && !prior.equals(elt)) {
						log.warn(
							"Nodes {} and {} are mapped by the same params {}",
							prior.getId(),
							elt.getId(),
							params);
					}
					return prior;
				});
			}
		}

		List<RowN> rows = new ArrayList<>();

		for (Map.Entry<List<Param<?>>, ConceptElement<?>> entry : byDepth.entrySet()) {
			List<Param<?>> params = new ArrayList<>(entry.getKey().size() + 1);

			params.addFirst(val(entry.getValue().getLocalId()));
			params.addAll(entry.getKey());

			rows.add(row(params));
		}
		return rows;
	}

	/**
	 * Collect all mappings from values to conceptElement for the entire concept. This means the column-value and the auxiliary columns.
	 * We use them to construct a table building an injective mapping from values to concept element that can be used for performant joins instead of resolving the concept every time.
	 */
	private List<CTCondition.ConceptConditions> collectAllExpressions(
		ConceptElement<?> current,
		CTCondition.ConceptConditions parentConceptCondition,
		CTConditionContext context) {

		final CTCondition.ConceptConditions forCurrent = switch (current) {
			case TreeConcept concept -> new CTCondition.ConceptConditions(concept, Collections.emptyMap());
			// concept elements implicitly inherit the conditions of its parents
			case ConceptTreeChild child ->
				child.getCondition().buildExpression(context, current).and(parentConceptCondition);
			case null, default -> throw new IllegalStateException();
		};

		final List<CTCondition.ConceptConditions> out = new ArrayList<>();

		out.add(forCurrent);

		for (ConceptTreeChild child : current.getChildren()) {
			out.addAll(collectAllExpressions(child, forCurrent, context));
		}

		return out;
	}

	/**
	 * recursively build just a single expression
	 *
	 * @param current
	 * @param context TODO use this to implement joining in queries
	 */
	private CTCondition.ConceptConditions collectExpressionsForSingleNode(
		ConceptElement<?> current,
		CTConditionContext context) {

		if (current instanceof TreeConcept concept) {
			return new CTCondition.ConceptConditions(concept, Collections.emptyMap());
		}

		CTCondition.ConceptConditions parentConceptCondition = collectExpressionsForSingleNode(
			current.getParent(),
			context);
		CTCondition.ConceptConditions currentConceptCondition = ((ConceptTreeChild) current).getCondition()
			.buildExpression(
				context,
				current);

		return currentConceptCondition.and(parentConceptCondition);
	}


}

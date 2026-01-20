package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Condition;
import org.jooq.CreateTableElementListStep;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStepN;
import org.jooq.Name;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.RowN;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;

@Slf4j
@UtilityClass
public class SqlMatchingStats {

	private static final Field<String> PID_FIELD = field(name("pid"), String.class);
	private static final Field<Date> LB_FIELD = field(name("lowerBound"), Date.class);
	private static final Field<Date> UB_FIELD = field(name("upperBound"), Date.class);
	private static final Field<String> CONCEPT_ID_FIELD = field(name("resolvedId"), String.class);
	private final Set<Param<?>> NULL_PARAMS = Collections.singleton(inline(null, String.class));

	@NotNull
	private static List<Field<Date>> collectValidityDateFields(Connector connector, SqlFunctionProvider provider) {
		List<Field<Date>> validityDates = new ArrayList<>();

		for (ValidityDate validityDate : connector.getValidityDates()) {
			if (!validityDate.isSingleColumnDaterange()) {
				validityDates.add(field(name(validityDate.getStartColumn().getColumn()), Date.class));
				validityDates.add(field(name(validityDate.getEndColumn().getColumn()), Date.class));
				continue;
			}

			Column column = validityDate.getColumn().get();

			if (column.getType() == MajorTypeId.DATE) {
				validityDates.add(field(name(column.getName()), Date.class));
			}
			else if (column.getType() == MajorTypeId.DATE_RANGE) {
				Field<Object> rangeField = field(name(column.getName()));

				validityDates.add(provider.lower(rangeField));
				validityDates.add(provider.upper(rangeField));
			}
		}
		return validityDates;
	}

	@Nullable
	private static <T extends Record> Table<T> unionSelects(List<Select<? extends T>> connectorTables) {
		Select<T> unioned = null;

		for (Select<? extends T> connectorTable : connectorTables) {
			if (unioned == null) {
				unioned = (Select<T>) connectorTable;
				continue;
			}

			unioned = unioned.unionAll(connectorTable);
		}


		return table(unioned);
	}

	private static void assignStats(Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats) {
		for (Map.Entry<ConceptElementId<?>, MatchingStats.Entry> entry : matchingStats.entrySet()) {
			ConceptElementId<?> conceptElementId = entry.getKey();

			MatchingStats stats = new MatchingStats();
			stats.putEntry("sql", entry.getValue());
			conceptElementId.resolve().setMatchingStats(stats);
		}
	}

	@NotNull
	private static Map<ConceptElementId<?>, MatchingStats.Entry> resolveStats(
			TreeConcept concept,
			SelectJoinStep<? extends Record> selectJoinStep) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats = new HashMap<>();

		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info("BEGIN fetching matching stats for {}", concept.getId());
		log.debug("{}", selectJoinStep);

		try (Cursor<? extends Record> cursor = selectJoinStep
				.fetchSize(100).fetchLazy()) {

			for (Record record : cursor) {

				ConceptElementId<?> resolvedId = ConceptElementId.Parser.INSTANCE.parse(record.get(CONCEPT_ID_FIELD));
				resolvedId.setDomain(concept.getDomain());

				String entity = record.get(PID_FIELD);
				Date min = record.get(LB_FIELD);
				Date max = record.get(UB_FIELD);

				CDateRange span = CDateRange.of(min != null ? min.toLocalDate() : null, max != null ? max.toLocalDate() : null);

				ConceptElement<?> element = resolvedId.get();

				while (element != null) {
					matchingStats.computeIfAbsent(element.getId(), (ignored) -> new MatchingStats.Entry())
								 .addEvents(entity, 1, span);
					element = element.getParent();
				}
			}
		}

		log.debug("DONE fetching matching stats for {} within {}", concept.getId(), stopwatch);


		return matchingStats;
	}

	@NotNull
	private static Name getConceptIdsTable(TreeConcept concept) {
		return name("%s_ids".formatted(concept.getName()));
	}

	private static void insertConceptIdMappings(Name tableName, List<Field<?>> fieldNames, List<RowN> rows, DSLContext dsl) {
		log.info("BEGIN inserting {} rows into {}", rows.size(), tableName);

		InsertValuesStepN<Record> insertConceptTable = dsl.insertInto(table(tableName))
														  .columns(fieldNames)
														  .valuesOfRows(rows);

		insertConceptTable.execute();

		log.trace("DONE inserting into {}", tableName);
	}

	/**
	 * Drop the table, then recreate it.
	 * TODO add an index.
	 */
	private static void createConceptIdsTable(Name tableName, List<Field<?>> fieldNames, DSLContext dsl) {

		log.debug("Creating table {} with fields {}", tableName, fieldNames);

		dsl.dropTable(tableName)
		   .cascade()
		   .execute();

		CreateTableElementListStep createTable =
				dsl.createTable(tableName)
				   .columns(fieldNames);


		createTable.execute();

		//TODO null values still crash this :'(
		//		if (!allFields.isEmpty()) {
		//			String indexName = "%s_index".formatted(tableName.unquotedName().toString());
		//			dslContext.dropIndexIfExists(indexName).execute();
		//			dslContext.createIndex(indexName)
		//					  .on(table(tableName), allFields.stream().map(Field::sortDefault).toList())
		//					  .excludeNullKeys()
		//					  .execute();
		//		}
	}

	private static int getMaxIdLength(List<CTCondition.Expression> expressions) {
		return expressions.stream().mapToInt(e -> e.id().getId().toString().length()).max()
						  .orElse(0);
	}

	public static void collectMatchingStatsForConcept(TreeConcept concept, SqlFunctionProvider provider, DSLContext dslContext, DatabaseConfig dbConfig) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats =
				// The transaction should implicitly disable autocommit, which we want for using the cursor
				dslContext.transactionResult(cfg -> {

					SelectJoinStep<? extends Record> matchingStatsStatement = createMatchingStatsStatement(concept, provider, dbConfig, cfg.dsl());

					return resolveStats(concept, matchingStatsStatement);
				});

		assignStats(matchingStats);
	}

	@NotNull
	private static SelectJoinStep<? extends Record> createMatchingStatsStatement(
			TreeConcept concept, SqlFunctionProvider provider, DatabaseConfig dbConfig,
			DSLContext dslContext) {

		List<Select<? extends Record>> connectorTables = new ArrayList<>();

		Field<Date> positiveInfinitty = provider.toDateField(provider.getMaxDateExpression());
		Field<Date> negativeInifnity = provider.toDateField(provider.getMinDateExpression());

		for (Connector connector : concept.getConnectors()) {

			Field<String> pid = TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), dbConfig);
			Field<Date>[] validityDates = collectValidityDateFields(connector, provider).toArray(Field[]::new);

			CTConditionContext context = CTConditionContext.create(connector, provider);

			SelectConditionStep<? extends Record> connectorTable =
					dslContext.select(
									  pid.as(PID_FIELD),
									  // The infinities are intentionally swapped
									  least(positiveInfinitty, validityDates).as(LB_FIELD),
									  greatest(negativeInifnity, validityDates).as(UB_FIELD),
									  CONCEPT_ID_FIELD
							  )
							  .from(table(name(connector.getResolvedTable().getName())))
							  .leftJoin(getConceptIdsTable(concept))
							  .on(getJoinConditions(concept, context))
							  .where(connector.getCondition() != null ? connector.getCondition().convertToSqlCondition(context).condition() : noCondition());

			connectorTables.add(connectorTable);
		}

		SelectJoinStep<Record4<String, String, Date, Date>> records =
				dslContext.select(
								  CONCEPT_ID_FIELD,
								  PID_FIELD,
								  // The infinities are intentionally swapped
								  nullif(LB_FIELD, positiveInfinitty),
								  nullif(UB_FIELD, negativeInifnity)
						  )
						  .from(unionSelects(connectorTables));

		return records;
	}

	public void createConceptIdJoinTable(TreeConcept concept, SqlFunctionProvider provider, DSLContext dslContext) {

		CTConditionContext context = new CTConditionContext(field(name("col_val"), String.class), provider);

		buildAssignmentTable(concept, context, dslContext);
	}

	private static Condition getJoinConditions(TreeConcept concept, CTConditionContext context) {
		List<CTCondition.Expression> expressions = collectAllExpressions(concept, null, context);

		Collection<Field<?>> allFields = expressions.stream()
													.map(expression -> expression.conditions().keySet())
													.flatMap(Collection::stream)
													.collect(Collectors.toSet());

		Name idsTable = getConceptIdsTable(concept);

		Condition out = noCondition();

		for (Field eField : allFields) {
			// The id-tables names are derived from eField so this should work.
			out = out.and(eField.eq(field(name(idsTable, eField.getUnqualifiedName()))));
		}

		return out;
	}

	public void buildAssignmentTable(TreeConcept concept, CTConditionContext context, DSLContext dsl) {

		//TODO at some point this needs to be created, when the concept is inserted.
		List<CTCondition.Expression> expressions = collectAllExpressions(concept, null, context);

		List<Field<?>> allFields = expressions.stream()
											  .map(expression -> expression.conditions().keySet())
											  .flatMap(Collection::stream)
											  .distinct()
											  .toList();


		List<RowN> rows = toRows(expressions, allFields);

		Name tableName = getConceptIdsTable(concept);

		// the allfields are expressions to extract values from tables, we use them to generate the field names
		List<Field<?>> fieldNames = new ArrayList<>(allFields);
		fieldNames.addFirst(field(CONCEPT_ID_FIELD.getName(), VARCHAR(getMaxIdLength(expressions))));

		createConceptIdsTable(tableName, fieldNames, dsl);
		insertConceptIdMappings(tableName, fieldNames, rows, dsl);
	}

	@NotNull
	private List<RowN> toRows(List<CTCondition.Expression> expressions, List<Field<?>> allFields) {
		Map<List<Param<?>>, ConceptElement<?>> byDepth = new HashMap<>();

		for (CTCondition.Expression expression : expressions) {
			ConceptElement<?> elt = expression.id();

			List<Set<Param<?>>> rowValues = new ArrayList<>();
			for (Field<?> field : allFields) {
				rowValues.add(expression.conditions().getOrDefault(field, NULL_PARAMS));
			}

			Set<List<Param<?>>> flattened = Sets.cartesianProduct(rowValues);

			// Group by params, find deepest params. This ensures we map to the most-specific element.
			for (List<Param<?>> params : flattened) {
				byDepth.compute(params,
								(__, prior) -> prior == null || prior.getDepth() < elt.getDepth() ? elt : prior
				);
			}
		}

		List<RowN> rows = new ArrayList<>();

		for (Map.Entry<List<Param<?>>, ConceptElement<?>> entry : byDepth.entrySet()) {
			List<Param<?>> params = new ArrayList<>(entry.getKey().size() + 1);

			params.addFirst(val(entry.getValue().getId().toString()));
			params.addAll(entry.getKey());

			rows.add(row(params));
		}
		return rows;
	}

	private List<CTCondition.Expression> collectAllExpressions(ConceptElement<?> current, CTCondition.Expression parentExpression, CTConditionContext context) {
		final List<CTCondition.Expression> out = new ArrayList<>();
		final CTCondition.Expression forCurrent;

		if (current instanceof TreeConcept concept) {
			forCurrent = new CTCondition.Expression(concept, Collections.emptyMap());
		}
		else if (current instanceof ConceptTreeChild child) {
			forCurrent = child.getCondition()
							  .buildExpression(context, current)
							  .join(parentExpression);
		}
		else {
			throw new IllegalStateException();
		}

		out.add(forCurrent);

		for (ConceptTreeChild child : current.getChildren()) {
			out.addAll(collectAllExpressions(child, forCurrent, context));
		}

		return out;
	}

	/**
	 * recursively build just a single expression
	 * @param current
	 * @param context
	 * @return
	 */
	private CTCondition.Expression collectExpressionsForSingleNode(ConceptElement<?> current, CTConditionContext context) {

		if (current instanceof TreeConcept concept) {
			return new CTCondition.Expression(concept, Collections.emptyMap());
		}

		CTCondition.Expression parentExpression = collectExpressionsForSingleNode(current.getParent(), context);
		CTCondition.Expression currentExpression = ((ConceptTreeChild) current).getCondition().buildExpression(context, current);

		return currentExpression.join(parentExpression);
	}


}

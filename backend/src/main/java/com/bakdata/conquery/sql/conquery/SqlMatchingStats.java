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
import jakarta.validation.constraints.NotBlank;

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
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
@Data
public class SqlMatchingStats {

	private final Field<String> PID_FIELD = field(name("pid"), String.class);
	private final Field<Date> LB_FIELD = field(name("lower_bound"), Date.class);
	private final Field<Date> UB_FIELD = field(name("upper_bound"), Date.class);
	private final Field<String> CONCEPT_ID_FIELD = field(name("resolved_id"), String.class);
	private final Set<Param<?>> NULL_PARAMS = Collections.singleton(inline(null, String.class));

	private final DSLContext dslContext;
	private final SqlFunctionProvider functionProvider;
	private final DatabaseConfig dbConfig;
	private final int fetchBatchSize = 100; //TODO from dbConfig?

	private static void assignStatsToPath(ConceptElementId<?> resolvedId, Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats, String entity, CDateRange span) {
		ConceptElement<?> element = resolvedId.get();

		while (element != null) {
			matchingStats.computeIfAbsent(element.getId(), (ignored) -> new MatchingStats.Entry())
						 .addEvents(entity, 1, span);
			element = element.getParent();
		}
	}

	/**
	 * collect unique fields used/defined in the expressions.
	 */
	private static List<Field<?>> collectAllFields(List<CTCondition.Expression> expressions) {
		List<Field<?>> fields = expressions.stream()
										   .map(expression -> expression.conditions().keySet())
										   .flatMap(Collection::stream)
										   .distinct()
										   .toList();
		return fields;
	}

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

	/**
	 * Assembles the join table and inserts it into the database.
	 * @param concept
	 */
	public void createConceptIdJoinTable(TreeConcept concept) {
		CTConditionContext context = CTConditionContext.forJoinTables(functionProvider);

		List<CTCondition.Expression> expressions = collectAllExpressions(concept, null, context);

		List<Field<?>> allFields = collectAllFields(expressions);

		List<RowN> rows = expressionsToRows(expressions, allFields);

		Name tableName = idsTableName(concept.getName());

		// allFields are the statements to extract values from the underlying tables, we use them to generate the field names
		List<Field<?>> fieldNames = new ArrayList<>(allFields);
		fieldNames.addFirst(field(CONCEPT_ID_FIELD.getName(), VARCHAR(findMaxIdLength(expressions))));

		createConceptIdsTable(tableName, fieldNames);
		insertConceptIdMappings(tableName, fieldNames, rows, dslContext);
	}

	@NotNull
	private Field<Date>[] collectValidityDateFields(Connector connector) {
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

				validityDates.add(functionProvider.lower(rangeField));
				validityDates.add(functionProvider.upper(rangeField));
			}
		}
		return (Field<Date>[]) validityDates.toArray(Field[]::new);
	}

	private void assignStats(Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats) {
		for (Map.Entry<ConceptElementId<?>, MatchingStats.Entry> entry : matchingStats.entrySet()) {
			ConceptElementId<?> conceptElementId = entry.getKey();

			MatchingStats stats = new MatchingStats();
			stats.putEntry("sql", entry.getValue());
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

				String rawId = record.get(CONCEPT_ID_FIELD);
				ConceptElementId<?> resolvedId;
				if (rawId == null) {
					resolvedId = concept.getId();
				}
				else {
					resolvedId = ConceptElementId.Parser.INSTANCE.parse(rawId);
					resolvedId.setDomain(concept.getDomain());
				}

				String entity = record.get(PID_FIELD);
				Date min = record.get(LB_FIELD);
				Date max = record.get(UB_FIELD);

				CDateRange span = CDateRange.of(min != null ? min.toLocalDate() : null, max != null ? max.toLocalDate() : null);

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
	private void createConceptIdsTable(Name tableName, List<Field<?>> fieldNames) {

		log.debug("Creating table {} with fields {}", tableName, fieldNames);

		dslContext.dropTableIfExists(tableName)
				  .cascade()
				  .execute();

		CreateTableElementListStep createTable =
				dslContext.createTable(tableName)
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

	private int findMaxIdLength(List<CTCondition.Expression> expressions) {
		return expressions.stream().mapToInt(e -> e.conceptElement().getId().toString().length()).max()
						  .orElse(0);
	}

	public void collectMatchingStatsForConcept(TreeConcept concept) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats =
				// The transaction implicitly disables autocommit, which we need for using the cursor
				dslContext.transactionResult(cfg -> {

					SelectJoinStep<? extends Record> matchingStatsStatement = createMatchingStatsStatement(concept);

					return readStats(concept, matchingStatsStatement);
				});

		assignStats(matchingStats);
	}

	@NotNull
	private SelectJoinStep<? extends Record> createMatchingStatsStatement(TreeConcept concept) {

		List<Select<? extends Record>> connectorTables = new ArrayList<>();

		Field<Date> positiveInfinity = functionProvider.toDateField(functionProvider.getMaxDateExpression());
		Field<Date> negativeInfinity = functionProvider.toDateField(functionProvider.getMinDateExpression());

		for (Connector connector : concept.getConnectors()) {

			CTConditionContext context = CTConditionContext.forConnector(connector, functionProvider);

			Field<Date>[] validityDates = collectValidityDateFields(connector);

			SelectConditionStep<? extends Record> connectorTable =
					dslContext.select(
									  TablePrimaryColumnUtil.findPrimaryColumn(connector.getResolvedTable(), dbConfig).as(PID_FIELD),
									  // The infinities are intentionally swapped
									  least(positiveInfinity, validityDates).as(LB_FIELD),
									  greatest(negativeInfinity, validityDates).as(UB_FIELD),
									  CONCEPT_ID_FIELD
							  )
							  .from(table(name(connector.getResolvedTable().getName())))
							  .leftJoin(idsTableName(concept.getName()))
							  .on(getJoinConditions(concept, context)) // joint onto the concept-ids table to assign the most specific id.
							  .where(connector.getCondition() != null ? connector.getCondition().convertToSqlCondition(context).condition() : noCondition());

			connectorTables.add(connectorTable);
		}

		SelectJoinStep<Record4<String, String, Date, Date>> records =
				dslContext.select(
								  CONCEPT_ID_FIELD,
								  PID_FIELD,
								  // The infinities are intentionally swapped
								  nullif(LB_FIELD, positiveInfinity).as(LB_FIELD),
								  nullif(UB_FIELD, negativeInfinity).as(UB_FIELD)
						  )
						  .from(unionSelects(connectorTables));

		return records;
	}

	public void deleteConceptIdJoinTable(ConceptId concept) {
		Name tableName = idsTableName(concept.getName());
		log.debug("Dropping table {}", tableName);
		dslContext.dropTableIfExists(tableName)
				  .cascade()
				  .execute();
	}


	/**
	 * Using the expressions of a concept, build a Condition that descibes the left-join onto the ids table, from any connector-table.
	 */
	private Condition getJoinConditions(TreeConcept concept, CTConditionContext context) {
		List<CTCondition.Expression> expressions = collectAllExpressions(concept, null, context);

		Collection<Field<?>> allFields = collectAllFields(expressions);

		Name idsTable = idsTableName(concept.getName());

		Condition out = noCondition();

		for (Field eField : allFields) {
			// col_val needs extra handling because it's bound to the connector and not the concept.
			if (eField.equals(context.getConnectorColumn())) {
				out = out.and(eField.eq(CTConditionContext.COLUMN_VALUE_FIELD));
				continue;
			}

			// The conceptElement-tables names are derived from eField so this should work.
			out = out.and(eField.eq(field(name(idsTable, eField.getUnqualifiedName()))));
		}

		return out;
	}

	private List<RowN> expressionsToRows(List<CTCondition.Expression> expressions, List<Field<?>> allFields) {
		Map<List<Param<?>>, ConceptElement<?>> byDepth = new HashMap<>();

		for (CTCondition.Expression expression : expressions) {
			ConceptElement<?> elt = expression.conceptElement();

			List<Set<Param<?>>> rowValues = new ArrayList<>();
			for (Field<?> field : allFields) {
				rowValues.add(expression.conditions().getOrDefault(field, NULL_PARAMS));
			}

			Set<List<Param<?>>> flattened = Sets.cartesianProduct(rowValues);

			// Group by params, find deepest params. This ensures we map to the most-specific element.
			for (List<Param<?>> params : flattened) {
				byDepth.compute(params,
								(__, prior) -> {
									if (prior == null || prior.getDepth() < elt.getDepth()) {
										return elt;
									}
									if (prior.getDepth() == elt.getDepth() && !prior.equals(elt)) {
										log.warn("Nodes {} and {} are mapped by the same params {}", prior.getId(), elt.getId(), params);
									}
									return prior;
								}
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

	/**
	 * Collect all mappings from values to conceptElement for the entire concept. This means the column-value and the auxiliary columns.
	 * We use them to construct a table building an injective mapping from values to concept element that can be used for performant joins instead of resolving the concept every time.
	 */
	private List<CTCondition.Expression> collectAllExpressions(ConceptElement<?> current, CTCondition.Expression parentExpression, CTConditionContext context) {
		final List<CTCondition.Expression> out = new ArrayList<>();
		final CTCondition.Expression forCurrent;

		if (current instanceof TreeConcept concept) {
			forCurrent = new CTCondition.Expression(concept, Collections.emptyMap());
		}
		else if (current instanceof ConceptTreeChild child) {
			// concept elements implicitly inherit the conditions of its parents
			forCurrent = child.getCondition()
							  .buildExpression(context, current)
							  .and(parentExpression);
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
	 *
	 * TODO use this to implement joining in queries
	 */
	private CTCondition.Expression collectExpressionsForSingleNode(ConceptElement<?> current, CTConditionContext context) {

		if (current instanceof TreeConcept concept) {
			return new CTCondition.Expression(concept, Collections.emptyMap());
		}

		CTCondition.Expression parentExpression = collectExpressionsForSingleNode(current.getParent(), context);
		CTCondition.Expression currentExpression = ((ConceptTreeChild) current).getCondition().buildExpression(context, current);

		return currentExpression.and(parentExpression);
	}


}

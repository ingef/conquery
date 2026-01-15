package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public class SqlMatchingStats {

	@NotNull
	private static Field<String> idField(Identifiable<?, ?> current) {
		return field(val(current.getId().toString()));
	}

	@NotNull
	private static Name conceptResolveFunctionName(TreeConcept concept) {
		return name("resolve_id_%s".formatted(concept.getName()));
	}

	@NotNull
	private static List<Field<?>> collectValidityDateFields(Connector connector, SqlFunctionProvider provider) {
		List<Field<?>> validityDates = new ArrayList<>();

		for (ValidityDate validityDate : connector.getValidityDates()) {
			if (!validityDate.isSingleColumnDaterange()) {
				validityDates.add(field(name(validityDate.getStartColumn().getColumn())));
				validityDates.add(field(name(validityDate.getEndColumn().getColumn())));
				continue;
			}
			Column column = validityDate.getColumn().get();
			if (column.getType() == MajorTypeId.DATE) {
				validityDates.add(field(name(column.getName()), LocalDate.class));
			}
			else if (column.getType() == MajorTypeId.DATE_RANGE) {
				Field<Object> rangeField = field(name(column.getName()));

				validityDates.add(provider.lower(rangeField));
				validityDates.add(provider.upper(rangeField));
			}
		}
		return validityDates;
	}

	@NotNull
	private static Field<String> getResolveIdFunctionInvocation(TreeConcept concept, String connectorColumn, Set<String> columns) {
		List<Field<?>> params = new ArrayList<>();

		if (connectorColumn != null) {
			params.add(field(name(connectorColumn)));
		}
		else {
			params.add(inline(null, String.class));
		}

		columns.stream().sorted().map(nm -> field(name(nm))).forEachOrdered(params::add);

		return function(conceptResolveFunctionName(concept), String.class, params);
	}

	@Nullable
	private static Table unionSelects(List<Select<?>> connectorTables) {
		Select unioned = null;

		for (Select connectorTable : connectorTables) {
			if (unioned == null) {
				unioned = connectorTable;
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
			SelectJoinStep<Record4<String, String, Date, Date>> selectJoinStep) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats = new HashMap<>();

		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info("BEGIN fetching matching stats for {}", concept.getId());
		log.debug("{}", selectJoinStep);
		log.debug("{}", selectJoinStep.configuration().dsl().explain(selectJoinStep));


		try (Cursor<Record4<String, String, Date, Date>> cursor = selectJoinStep
				.fetchSize(100).fetchLazy()) {

			for (Record4<String, String, Date, Date> record : cursor) {

				ConceptElementId<?> resolvedId = ConceptElementId.Parser.INSTANCE.parse(record.component1());
				resolvedId.setDomain(concept.getDomain());
				String entity = record.component2();
				Date min = record.component3();
				Date max = record.component4();

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


	public void collectMatchingStatsForConcept(TreeConcept concept, SqlFunctionProvider provider, DSLContext dslContext, DatabaseConfig dbConfig) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats =
				// The transaction should implicitly disable autocommit, which we want for using the cursor
				dslContext.transactionResult(cfg -> {

					SelectJoinStep<Record4<String, String, Date, Date>> matchingStatsStatement = createMatchingStatsStatement(concept, provider, dbConfig, cfg.dsl());

					return resolveStats(concept, matchingStatsStatement);
				});

		assignStats(matchingStats);
	}

	@NotNull
	private SelectJoinStep<Record4<String, String, Date, Date>> createMatchingStatsStatement(
			TreeConcept concept, SqlFunctionProvider provider, DatabaseConfig dbConfig,
			DSLContext dslContext) {

		List<Select<?>> connectorTables = new ArrayList<>();

		Field<Date> positiveInfinitty = provider.toDateField(provider.getMaxDateExpression());
		Field<Date> negativeInifnity = provider.toDateField(provider.getMinDateExpression());

		for (Connector connector : concept.getConnectors()) {
			String connectorColumn = null;
			if (connector.getColumn() != null) {
				connectorColumn = connector.getColumn().get().getName();
			}

			CTConditionContext context = new CTConditionContext(false, connectorColumn, provider);

			com.bakdata.conquery.models.datasets.Table resolvedTable = connector.getResolvedTable();

			Field<?> pid = TablePrimaryColumnUtil.findPrimaryColumn(resolvedTable, dbConfig);

			Set<String> columns = getAuxiliaryColumns(concept);

			Field<String> resolveFunction = getResolveIdFunctionInvocation(concept, connectorColumn, columns);

			Field[] validityDatesArray = collectValidityDateFields(connector, provider).toArray(Field[]::new);


			SelectConditionStep<?> connectorTable =
					dslContext.select(
									  pid.as("pid"),
									  // The infinities are intentionally swapped
									  least(positiveInfinitty, validityDatesArray).as("lowerBound"),
									  greatest(negativeInifnity, validityDatesArray).as("upperBound"),
									  resolveFunction.as("resolvedId")
							  ).from(table(name(resolvedTable.getName())))
							  .where(connector.getCondition() != null ? connector.getCondition().convertToSqlCondition(context).condition() : noCondition());

			connectorTables.add(connectorTable);
		}

		Table<?> unioned = unionSelects(connectorTables);

		SelectJoinStep<Record4<String, String, Date, Date>> records =
				dslContext.select(
								  field(name("resolvedId"), String.class),
								  field(name("pid"), String.class).as("entity"),
								  // The infinities are intentionally swapped
								  nullif(field(name("lowerBound"), Date.class), positiveInfinitty).as("lb"),
								  nullif(field(name("upperBound"), Date.class), negativeInifnity).as("ub")
						  )
						  .from(unioned);

		return records;
	}

	public void createFunctionForConcept(TreeConcept concept, SqlFunctionProvider provider, DSLContext dslContext) {

		CTConditionContext context = new CTConditionContext(false, "col_val", provider);

		buildAssignmentTable(concept, context, dslContext);
	}

	@NotNull
	private Set<String> getAuxiliaryColumns(TreeConcept concept) {
		return concept.getChildren().stream()
					  .map(this::collectAuxiliaryColumns)
					  .flatMap(Collection::stream)
					  .collect(Collectors.toSet());
	}


	public void buildAssignmentTable(TreeConcept concept, CTConditionContext context, DSLContext dslContext) {

		List<CTCondition.Expression> expressions = collectAllExpressions(concept, context);

		Set<Param<?>> nullParams = Collections.singleton(inline(null, String.class));

		List<Field<?>> allFields = expressions.stream()
											  .map(expression -> expression.conditions().keySet())
											  .flatMap(Collection::stream)
											  .distinct()
											  .toList();

		List<RowN> rows = new ArrayList<>(expressions.size());

		Map<List<Param<?>>, ConceptElement<?>> byDepth = new HashMap<>();

		for (CTCondition.Expression expression : expressions) {
			ConceptElement<?> elt = expression.id();

			List<Set<Param<?>>> rowValues = new ArrayList<>();
			for (Field<?> field : allFields) {
				rowValues.add(expression.conditions().getOrDefault(field, nullParams));
			}

			Set<List<Param<?>>> flattened = Sets.cartesianProduct(rowValues);

			// just a group-by+max on the flattened params to always map to the most specific element
			for (List<Param<?>> params : flattened) {
				byDepth.compute(params,
								(ignored, prior) -> prior == null || prior.getDepth() < elt.getDepth() ? elt : prior
				);
			}
		}

		for (Map.Entry<List<Param<?>>, ConceptElement<?>> entry : byDepth.entrySet()) {
			ArrayList<Param<?>> params = new ArrayList<>(entry.getKey());

			params.addFirst(val(entry.getValue().getId().toString()));

			rows.add(row(params));
		}

		int idLength = expressions.stream().mapToInt(e -> e.id().getId().toString().length()).max()
								  .orElse(0);

		Name tableName = name("%s_ids".formatted(concept.getName()));
		// the allfields are expressions to extract values from tables, we use them to generate the field names
		List<Field<?>> fieldNames = new ArrayList<>(allFields);
		fieldNames.addFirst(field(name("concept"), VARCHAR(idLength)));

		dslContext.dropTable(tableName)
				.cascade()
				.execute();

		CreateTableElementListStep createTable =
				dslContext.createTable(tableName)
						  .columns(fieldNames);

		log.debug("Creating table {}", createTable);

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


		InsertValuesStepN<Record> insertConceptTable = dslContext.insertInto(table(tableName))
																 .columns(fieldNames)
																 .valuesOfRows(rows);

		log.info("{}", insertConceptTable);

		insertConceptTable.execute();
	}

	private List<CTCondition.Expression> collectAllExpressions(TreeConcept concept, CTConditionContext context) {
		List<CTCondition.Expression> out = new ArrayList<>();

		CTCondition.Expression rootExpression = new CTCondition.Expression(concept, Collections.emptyMap());

		out.add(rootExpression);

		for (ConceptTreeChild child : concept.getChildren()) {
			out.addAll(createForConceptTreeNode(child, rootExpression, context));
		}

		return out;
	}

	private List<CTCondition.Expression> createForConceptTreeNode(ConceptTreeChild current, CTCondition.Expression parentExpression, CTConditionContext context) {

		List<CTCondition.Expression> out = new ArrayList<>();

		CTCondition.Expression forCurrent = current.getCondition()
												   .expressions(context, current)
												   .join(parentExpression);

		out.add(forCurrent);

		for (ConceptTreeChild child : current.getChildren()) {
			out.addAll(createForConceptTreeNode(child, forCurrent, context));
		}

		return out;
	}


	private Set<String> collectAuxiliaryColumns(ConceptTreeChild current) {
		Set<String> auxiliaryColumns = new HashSet<>();
		if (current.getCondition() != null) {
			auxiliaryColumns.addAll(current.getCondition().auxiliaryColumns());
		}

		for (ConceptTreeChild child : current.getChildren()) {
			auxiliaryColumns.addAll(collectAuxiliaryColumns(child));
		}

		return auxiliaryColumns;
	}


}

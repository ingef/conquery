package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Case;
import org.jooq.CaseConditionStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import static org.jooq.impl.DSL.*;

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
		log.info("{}", matchingStats);

		for (Map.Entry<ConceptElementId<?>, MatchingStats.Entry> entry : matchingStats.entrySet()) {
			ConceptElementId<?> conceptElementId = entry.getKey();

			MatchingStats stats = new MatchingStats();
			stats.putEntry("sql", entry.getValue());
			conceptElementId.resolve().setMatchingStats(stats);
		}
	}

	@NotNull
	private static Map<ConceptElementId<?>, MatchingStats.Entry> resolveStats(TreeConcept concept, @MonotonicNonNull Result<Record4<String, String, Date, Date>> batch) {
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats = new HashMap<>();


		for (Record4<String, String, Date, Date> record : batch) {

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
		return matchingStats;
	}


	public void collectMatchingStatsForConcept(TreeConcept concept, SqlFunctionProvider provider, DSLContext dslContext) {

		SelectJoinStep<Record4<String, String, Date, Date>> matchingStatsStatement = createMatchingStatsStatement(concept, provider);

		Result<Record4<String, String, Date, Date>> result = dslContext.fetch(matchingStatsStatement);
		Map<ConceptElementId<?>, MatchingStats.Entry> matchingStats = resolveStats(concept, result);

		assignStats(matchingStats);
	}

	@NotNull
	private SelectJoinStep<Record4<String, String, Date, Date>> createMatchingStatsStatement(TreeConcept concept, SqlFunctionProvider provider) {

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
			Table<Record> tableName = table(name(resolvedTable.getName()));
			Name pid = name(resolvedTable.getPrimaryColumn().getName());

			Set<String> columns = getAuxiliaryColumns(concept);
			if (connectorColumn != null) {
				columns.remove(connectorColumn);
			}

			Field<String> resolveFunction = getResolveIdFunctionInvocation(concept, connectorColumn, columns);

			Field[] validityDatesArray = collectValidityDateFields(connector, provider).toArray(Field[]::new);

			SelectConditionStep<?> connectorTable = select(
					field(pid).as("pid"),
					// The infinities are intentionally swapped
					least(positiveInfinitty, validityDatesArray).as("lowerBound"),
					greatest(negativeInifnity, validityDatesArray).as("upperBound"),
					resolveFunction.as("resolvedId")
			).from(tableName)
			 .where(connector.getCondition() != null ? connector.getCondition().convertToSqlCondition(context).condition() : noCondition());

			connectorTables.add(connectorTable);
		}

		Table<?> unioned = unionSelects(connectorTables);

		SelectJoinStep<Record4<String, String, Date, Date>> records =
				select(
						field(name("resolvedId"), String.class),
						field(name("pid"), String.class).as("entity"),
						// The infinities are intentionally swapped
						nullif(field(name("lowerBound"), Date.class), positiveInfinitty).as("lb"),
						nullif(field(name("upperBound"), Date.class), negativeInifnity).as("ub")
				)
						.from(unioned);
		log.info("{}", records);

		return records;
	}

	public void createFunctionForConcept(TreeConcept concept, SqlFunctionProvider provider, DSLContext dslContext) {

		CTConditionContext context = new CTConditionContext(true, "col_val", provider);
		Name name = conceptResolveFunctionName(concept);

		Set<String> auxiliaryColumns = getAuxiliaryColumns(concept);
		auxiliaryColumns.remove("col_val");

		//TODO this could be simplified and shortened by using localIds instead of string-ids. But sql-results are less readable.
		Field<String> forConcept = forNode(idField(concept), concept.getChildren(), context);

		List<String> params = new ArrayList<>();
		params.add("col_val");

		auxiliaryColumns.stream()
						.sorted()
						.forEachOrdered(params::add);

		String statement = provider.createFunctionStatement(name, params, forConcept);
		dslContext.execute(statement);
		log.info("{}", statement);
	}

	@NotNull
	private Set<String> getAuxiliaryColumns(TreeConcept concept) {
		return concept.getChildren().stream()
					  .map(this::collectAuxiliaryColumns)
					  .flatMap(Collection::stream)
					  .collect(Collectors.toSet());
	}

	public Field<String> createForConceptTreeNode(ConceptTreeChild current, CTConditionContext context) {
		Field<String> currentId = idField(current);

		return forNode(currentId, current.getChildren(), context);
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

	private Field<String> forNode(Field<String> currentId, List<ConceptTreeChild> children, CTConditionContext context) {
		if (children.isEmpty()) {
			return currentId;
		}

		Case decode = decode();
		CaseConditionStep<String> step = null;

		for (ConceptTreeChild child : children) {
			WhereCondition converted = child.getCondition().convertToSqlCondition(context);

			Field<String> result = createForConceptTreeNode(child, context);

			step = step == null ? decode.when(converted.condition(), result)
								: step.when(converted.condition(), result);
		}

		return step.otherwise(currentId);
	}
}

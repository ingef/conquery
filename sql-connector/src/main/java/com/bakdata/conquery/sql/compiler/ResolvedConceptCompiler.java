package com.bakdata.conquery.sql.compiler;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.conversion.operation.FilterConversionContext;
import com.bakdata.conquery.sql.compiler.conversion.operation.ResolvedConditionConverter;
import com.bakdata.conquery.sql.compiler.conversion.operation.ResolvedFilterConverter;
import com.bakdata.conquery.sql.compiler.conversion.operation.ResolvedSelectConverter;
import com.bakdata.conquery.sql.compiler.conversion.operation.SelectConversionContext;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.LogicalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SchemaSql;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteInput;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCtePlanner;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePipelineAssembler;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePlan;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingSelection;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/** Connects resolved concepts to the connector-owned CTE pipeline. */
@UtilityClass
final class ResolvedConceptCompiler {

	static QueryStep compile(ConceptNode concept, EntitySchema entitySchema, CompilerDialect dialect,
			SqlNameGenerator names, boolean negate, Optional<DateRange> restriction,
			Optional<QueryStep> stratificationTable) {
		ResolvedSelectConverter selectConverter = new ResolvedSelectConverter();
		List<ConnectorCompilation> connectors = concept.connectors().stream()
				.map(connector -> compileConnector(concept, connector, dialect, names, restriction, selectConverter, stratificationTable))
				.flatMap(Optional::stream)
				.toList();
		QueryStep joined = LogicalQueryStepCompiler.compile(
				connectors.stream().map(ConnectorCompilation::step).toList(), JoinMode.FULL_OUTER,
				DateAggregationAction.MERGE, false, entitySchema, dialect, names);
		SqlTables tables = ConceptCtePlanner.planConcept(joined, names.conceptName(concept),
				concept.selects().stream().anyMatch(ResolvedConceptCompiler::isEventDateSelect), dialect, names);
		Selects joinedSelects = joined.getQualifiedSelects();
		Map<String, String> connectorTables = new LinkedHashMap<>();
		connectors.forEach(item -> connectorTables.put(item.connector().table().physicalName().getLast(),
				item.plan().tables().cteName(ConceptCteStep.PREPROCESSING)));
		List<ConceptSqlSelects> selects = concept.selects().stream()
				.map(select -> selectConverter.conceptSelect(select, new SelectConversionContext(
						dialect, names, tables, joinedSelects.getIds(), joinedSelects.getValidityDate(),
						names.selectName(select), connectorTables)))
				.toList();
		return ConceptCteCompiler.compileConcept(new ConceptCteInput(
				joined, selects, tables, negate, concept.dateAction() == DateAggregationAction.BLOCK
		));
	}

	private static Optional<ConnectorCompilation> compileConnector(ConceptNode concept, ResolvedConnector connector,
			CompilerDialect dialect, SqlNameGenerator names,
			Optional<DateRange> restriction, ResolvedSelectConverter selectConverter,
			Optional<QueryStep> stratificationTable) {
		String connectorName = names.conceptConnectorName(concept, connector);
		boolean connectorEventDateSelectsPresent = connector.selects().stream().anyMatch(ResolvedConceptCompiler::isEventDateSelect);
		boolean conceptEventDateSelectsPresent = concept.selects().stream().anyMatch(ResolvedConceptCompiler::isEventDateSelect);
		ConnectorCtePlan plan = ConceptCtePlanner.planConnector(connector.table(), connectorName,
				concept.dateAction() != DateAggregationAction.BLOCK,
				connectorEventDateSelectsPresent, conceptEventDateSelectsPresent, dialect, names);
		SqlIdColumns ids = connector.secondaryId()
				.map(secondary -> new SqlIdColumns(SchemaSql.field(connector.primaryId(), String.class),
						SchemaSql.field(secondary, String.class)))
				.orElseGet(() -> new SqlIdColumns(SchemaSql.field(connector.primaryId(), String.class)))
				.withAlias();
		ColumnDateRange validityDate = validityDate(connector.validityDate(), restriction, dialect);
		List<SqlFilters> filters = new ArrayList<>();
		ResolvedFilterConverter filterConverter = new ResolvedFilterConverter();
		FilterConversionContext filterContext = new FilterConversionContext(dialect, names, plan.tables(), ids);
		connector.filters().stream().map(filter -> filterConverter.convert(filter, filterContext)).forEach(filters::add);
		ResolvedConditionConverter conditionConverter = new ResolvedConditionConverter();
		List<WhereCondition> conditions = connector.conditions().stream()
				.map(condition -> conditionConverter.convert(condition, dialect))
				.collect(Collectors.toCollection(ArrayList::new));
		validityPresentCondition(connector.validityDate()).ifPresent(condition -> conditions.add(() -> condition));
		filters.add(new SqlFilters(ConnectorSqlSelects.none(),
				WhereClauses.builder().preprocessingConditions(conditions).build()));
		restriction.ifPresent(range -> filters.add(new SqlFilters(ConnectorSqlSelects.none(),
				WhereClauses.builder().eventFilter(new DateRestrictionCondition(dialect.dateRangeLiteral(range), validityDate)).build())));
		List<ConnectorSqlSelects> selects = new ArrayList<>();
		for (ResolvedSelect select : concept.selects()) {
			if (select instanceof BuiltInSelects.ConceptValues values) {
				values.columns().stream()
						.filter(column -> column.table().equals(connector.table()))
						.findFirst()
						.ifPresent(column -> {
							BuiltInSelects.ConceptValues connectorValues = new BuiltInSelects.ConceptValues(
									values.name(), List.of(column));
							selects.add(selectConverter.convert(connectorValues,
									selectContext(dialect, names, plan, ids, validityDate, connectorName,
											names.selectName(select), conceptValueSources(column, connector, plan, dialect))));
						});
			}
		}
		for (ResolvedSelect select : connector.selects()) {
			selects.add(selectConverter.convert(select, selectContext(dialect, names, plan, ids, validityDate,
					connectorName, names.selectName(select))));
		}
		org.jooq.Table<org.jooq.Record> sourceTable = connector.conceptIdMapping()
				.map(mapping -> mapping.selectedConcepts(plan.sourceTable(), dialect))
				.orElse(plan.sourceTable());
		return ConnectorCteCompiler.compileConnector(ConnectorCtePipelineAssembler.assemble(
				plan, sourceTable, ids, validityDate, selects, filters, stratificationTable))
				.map(step -> new ConnectorCompilation(connector, plan, step));
	}

	private static List<SelectConversionContext.ConceptColumnSource> conceptValueSources(
			ResolvedColumn column,
			ResolvedConnector connector,
			ConnectorCtePlan plan,
			CompilerDialect dialect
	) {
		if (!connector.conceptIdMapping().map(ConceptIdMappingSelection::resolveConceptIds).orElse(false)) {
			return List.of();
		}
		ConceptIdMappingSelection mapping = connector.conceptIdMapping().orElseThrow();
		return List.of(new SelectConversionContext.ConceptColumnSource(
						plan.sourceTable().getName(),
						mapping.source().resolvedConceptIds(plan.sourceTable(), dialect),
						mapping.source().resolvedIdOrRoot(mapping.rootLocalId()),
						List.of(SchemaSql.field(column, Object.class).isNotNull())
				));
	}

	private static SelectConversionContext selectContext(CompilerDialect dialect, SqlNameGenerator names,
			ConnectorCtePlan plan, SqlIdColumns ids, ColumnDateRange validityDate, String connectorName, String alias) {
		return new SelectConversionContext(dialect, names, plan.tables(), ids,
				Optional.of(validityDate.asValidityDateRange(connectorName)), alias);
	}

	private static SelectConversionContext selectContext(CompilerDialect dialect, SqlNameGenerator names,
			ConnectorCtePlan plan, SqlIdColumns ids, ColumnDateRange validityDate, String connectorName, String alias,
			List<SelectConversionContext.ConceptColumnSource> conceptColumnSources) {
		return new SelectConversionContext(dialect, names, plan.tables(), ids,
				Optional.of(validityDate.asValidityDateRange(connectorName)), alias, Map.of(), conceptColumnSources);
	}

	private static ColumnDateRange validityDate(ResolvedValidityDate value, Optional<DateRange> restriction,
			CompilerDialect dialect) {
		ColumnDateRange physical = switch (value) {
			case ResolvedValidityDate.None ignored -> restriction.map(dialect::dateRangeLiteral).orElseGet(dialect::unboundedDateRange);
			case ResolvedValidityDate.Point point -> dialect.dateRange(SchemaSql.field(point.column(), Date.class),
					SchemaSql.field(point.column(), Date.class));
			case ResolvedValidityDate.Range range -> dialect.dateRange(SchemaSql.field(range.start(), Date.class),
					SchemaSql.field(range.end(), Date.class));
		};
		if (restriction.isEmpty() || value instanceof ResolvedValidityDate.None) {
			return physical;
		}
		ColumnDateRange bounds = dialect.dateRangeLiteral(restriction.orElseThrow());
		return ColumnDateRange.of(
				DSL.when(physical.getStart().lessThan(bounds.getStart()), bounds.getStart()).otherwise(physical.getStart()),
				DSL.when(physical.getEnd().greaterThan(bounds.getEnd()), bounds.getEnd()).otherwise(physical.getEnd()));
	}

	private static Optional<Condition> validityPresentCondition(ResolvedValidityDate value) {
		return switch (value) {
			case ResolvedValidityDate.None ignored -> Optional.empty();
			case ResolvedValidityDate.Point point -> Optional.of(SchemaSql.field(point.column(), Object.class).isNotNull());
			case ResolvedValidityDate.Range range -> Optional.of(SchemaSql.field(range.start(), Object.class).isNotNull()
					.or(SchemaSql.field(range.end(), Object.class).isNotNull()));
		};
	}

	private static boolean isEventDateSelect(ResolvedSelect select) {
		return select instanceof BuiltInSelects.EventDateUnion || select instanceof BuiltInSelects.EventDurationSum;
	}

	private record ConnectorCompilation(ResolvedConnector connector, ConnectorCtePlan plan, QueryStep step) {
	}
}

package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteInput;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePipelineAssembler;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePipelineInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.sql.compiler.ir.condition.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.*;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;

import static org.jooq.impl.DSL.*;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private static QueryStep finishConceptConversion(QueryStep predecessor, CQConcept cqConcept, TablePath tablePath, ConversionContext context) {

		ConceptSqlTables universalTables = tablePath.createConceptTables(predecessor);
		Selects predecessorSelects = predecessor.getQualifiedSelects();
		Optional<ColumnDateRange> validityDate = predecessorSelects.getValidityDate();
		SqlIdColumns ids = predecessorSelects.getIds();

		SelectContext<ConceptSqlTables> selectContext = SelectContext.create(ids, validityDate, universalTables, context);
		List<ConceptSqlSelects> converted = cqConcept.getSelects().stream()
				.map(selectId -> {
					Select select = selectId.resolve();
					return context.getCompilerDialect().getSelectConverter(select).conceptSelect(select, selectContext);
				})
				.toList();

		return ConceptCteCompiler.compileConcept(new ConceptCteInput(
				predecessor,
				converted,
				universalTables,
				context.isNegation()
		));
	}

	public static SqlIdColumns convertIds(CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		Table table = cqTable.getConnector().resolve().getResolvedTable();
		Field<String> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(table, conversionContext.getDefaultPrimaryColumn());

		if (cqConcept.isExcludeFromSecondaryId()
				|| conversionContext.getSecondaryIdDescription() == null
				|| !cqTable.hasSelectedSecondaryId(conversionContext.getSecondaryIdDescription().getId())
		) {
			return new SqlIdColumns(primaryColumn).withAlias();
		}

		Column secondaryIdColumn = table.findSecondaryIdColumn(conversionContext.getSecondaryIdDescription().getId());

		Preconditions.checkArgument(
				secondaryIdColumn != null,
				"Expecting Table %s to have a matching secondary id for %s".formatted(
						table,
						conversionContext.getSecondaryIdDescription()
				)
		);

		Field<String> secondaryId = field(name(table.getName(), secondaryIdColumn.getName()), String.class);
		return new SqlIdColumns(primaryColumn, secondaryId).withAlias();
	}

	private static ColumnDateRange convertValidityDate(ConversionContext context, ValidityDate validityDate) {
		SqlFunctionProvider functionProvider = context.getFunctionProvider();
		ColumnDateRange sqlValidityDate;

		boolean hasValidityDate = validityDate != null;
		boolean hasDateRestriction = context.getDateRestrictionRange() != null;

		if (hasValidityDate) {
			if (hasDateRestriction) {
				sqlValidityDate = functionProvider.forValidityDate(validityDate, context.getDateRestrictionRange());
			} else {
				sqlValidityDate = functionProvider.forValidityDate(validityDate);
			}
		} else {
			if (hasDateRestriction) {
				sqlValidityDate = functionProvider.forCDateRange(context.getDateRestrictionRange());
			} else {
				sqlValidityDate = context.getCompilerDialect().unboundedDateRange();
			}
		}

		return sqlValidityDate;
	}

	private static SqlFilters collectConditionFilters(
			List<ConceptElement<?>> conceptElements, CQTable cqTable, SqlFunctionProvider functionProvider) {
		List<WhereCondition> conditions = new ArrayList<>();
		conditions.addAll(collectConditions(conceptElements, cqTable, functionProvider));

		ValidityDate validityDate = cqTable.findValidityDate();
		Condition validityDateFilter = noCondition();
		if (validityDate != null) {
			validityDateFilter = functionProvider.isNotEmptyValidityDate(validityDate);
		}

		return new SqlFilters(ConnectorSqlSelects.none(),
				WhereClauses.builder()
						.preprocessingConditions(conditions)
						.preprocessingCondition(new ConditionWrappingWhereCondition(validityDateFilter))
						.build()
		);
	}

	private static List<WhereCondition> collectConditions(List<ConceptElement<?>> conceptElements, CQTable cqTable, SqlFunctionProvider functionProvider) {

		List<WhereCondition> conditions = new ArrayList<>();

		convertConnectorCondition(cqTable, functionProvider).ifPresent(conditions::add);


		for (ConceptElement<?> conceptElement : conceptElements) {
			collectConditions(cqTable, conceptElement, functionProvider)
					.reduce(WhereCondition::and)
					.ifPresent(conditions::add);
		}

		return conditions;
	}

	/**
	 * Collects all conditions of a given {@link ConceptElement} by resolving the condition of the given node and all of its parent nodes.
	 */
	private static Stream<WhereCondition> collectConditions(CQTable cqTable, ConceptElement<?> conceptElement, SqlFunctionProvider functionProvider) {
		if (!(conceptElement instanceof ConceptTreeChild child)) {
			return Stream.empty();
		}
		WhereCondition childCondition = child.getCondition().convertToSqlCondition(CTConditionContext.forConnector(
				cqTable.getConnector().resolve(), functionProvider
		));
		return Stream.concat(
				collectConditions(cqTable, child.getParent(), functionProvider),
				Stream.of(childCondition)
		);
	}

	private static Optional<WhereCondition> convertConnectorCondition(CQTable cqTable, SqlFunctionProvider functionProvider) {
		final Connector connector = cqTable.getConnector().resolve();

		return Optional.ofNullable(connector.getCondition())
				.map(condition -> condition.convertToSqlCondition(CTConditionContext.forConnector(connector, functionProvider)));
	}

	private static SqlFilters dateRestrictionFilter(ConversionContext context, ColumnDateRange validityDate) {

		List<WhereCondition> conditions = new ArrayList<>();
		SqlFunctionProvider functionProvider = context.getFunctionProvider();

		if (context.getDateRestrictionRange() != null) {
			ColumnDateRange dateRestriction = functionProvider.forCDateRange(context.getDateRestrictionRange());
			conditions.add(new DateRestrictionCondition(dateRestriction, validityDate));
		}

		return new SqlFilters(
				ConnectorSqlSelects.none(),
				WhereClauses.builder()
						.eventFilters(conditions)
						.build()
		);
	}

	private static ConnectorSqlSelects createConceptColumnConnectorSqlSelects(CQConcept cqConcept, SelectContext<ConnectorSqlTables> selectContext) {


		return cqConcept.getSelects().stream()
				.map(SelectId::resolve)
				.filter(select -> select instanceof ConceptColumnSelect)
				.findFirst()
				.map(select -> selectContext.getCompilerDialect().getSelectConverter(select).connectorSelect(select, selectContext))
				.orElse(ConnectorSqlSelects.none());
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept cqConcept, ConversionContext context) {

		TablePath tablePath = new TablePath(cqConcept, context);
		List<QueryStep> convertedCQTables = cqConcept.getTables().stream()
				.flatMap(cqTable -> convertCqTable(tablePath, cqConcept, cqTable, context).stream())
				.toList();

		QueryStep joinedStep = QueryStepComposer.joinSteps(convertedCQTables, JoinMode.FULL_OUTER, DateAggregationAction.MERGE, context);
		QueryStep lastConceptStep = finishConceptConversion(joinedStep, cqConcept, tablePath, context);
		return context.withQueryStep(lastConceptStep);
	}

	private Optional<QueryStep> convertCqTable(TablePath tablePath, CQConcept cqConcept, CQTable cqTable, ConversionContext context) {
		ConnectorCtePipelineInput input = createConnectorCteInput(tablePath, cqConcept, cqTable, context);
		return ConnectorCteCompiler.compileConnector(input);
	}

	private ConnectorCtePipelineInput createConnectorCteInput(
			TablePath tablePath,
			CQConcept cqConcept,
			CQTable cqTable,
			ConversionContext conversionContext
	) {

		SqlIdColumns ids = convertIds(cqConcept, cqTable, conversionContext);
		ConnectorSqlTables connectorTables = tablePath.getConnectorTables(cqTable);
		ColumnDateRange validityDateCalculation = convertValidityDate(conversionContext, cqTable.findValidityDate());

		// convert filters
		SqlFunctionProvider functionProvider = conversionContext.getFunctionProvider();
		List<SqlFilters> allSqlFiltersForTable = new ArrayList<>();

		cqTable.getFilters().stream()
				.map(filterValue -> filterValue.convertToSqlFilter(ids, conversionContext, connectorTables))
				.forEach(allSqlFiltersForTable::add);

		List<ConceptElement<?>> conceptElements = cqConcept.getElements().stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList();
		allSqlFiltersForTable.add(collectConditionFilters(conceptElements, cqTable, functionProvider));

		allSqlFiltersForTable.add(dateRestrictionFilter(conversionContext, validityDateCalculation));

		// convert selects
		SelectContext<ConnectorSqlTables> selectContext = SelectContext.create(ids, Optional.of(validityDateCalculation.asValidityDateRange(connectorTables.getName())), connectorTables, conversionContext);
		List<ConnectorSqlSelects> allSelectsForTable = new ArrayList<>();
		ConnectorSqlSelects conceptColumnSelect = createConceptColumnConnectorSqlSelects(cqConcept, selectContext);
		allSelectsForTable.add(conceptColumnSelect);
		cqTable.getSelects()
				.stream()
				.map(SelectId::resolve)
				.map(select -> selectContext.getCompilerDialect().getSelectConverter(select).connectorSelect(select, selectContext))
				.forEach(allSelectsForTable::add);

		return ConnectorCtePipelineAssembler.assemble(
				connectorTables.getPlan(),
				ids,
				validityDateCalculation,
				allSelectsForTable,
				allSqlFiltersForTable,
				Optional.ofNullable(conversionContext.getStratificationTable())
		);
	}

}

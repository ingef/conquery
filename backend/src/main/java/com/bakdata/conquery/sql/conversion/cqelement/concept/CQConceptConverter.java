package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereConditionWrapper;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Condition;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private final List<ConnectorCte> connectorCtes;
	private final SqlFunctionProvider functionProvider;

	public CQConceptConverter(SqlFunctionProvider functionProvider) {
		this.functionProvider = functionProvider;
		this.connectorCtes = List.of(
				new PreprocessingCte(),
				new EventFilterCte(),
				new AggregationSelectCte(),
				new JoinBranches(),
				new AggregationFilterCte(),
				new FinalConnectorCte()
		);
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept cqConcept, ConversionContext context) {

		if (cqConcept.getTables().size() > 1) {
			throw new UnsupportedOperationException("Can't handle concepts with multiple tables for now.");
		}

		CQTableContext tableContext = createConceptCteContext(cqConcept, context);

		Optional<QueryStep> lastQueryStep = Optional.empty();
		for (ConnectorCte queryStep : this.connectorCtes) {
			Optional<QueryStep> convertedStep = queryStep.convert(tableContext, lastQueryStep);
			if (convertedStep.isEmpty()) {
				continue;
			}
			lastQueryStep = convertedStep;
			tableContext = tableContext.withPrevious(lastQueryStep.get());
		}

		return context.withQueryStep(lastQueryStep.orElseThrow(() -> new RuntimeException("No conversion for concept possible.")));
	}

	private CQTableContext createConceptCteContext(CQConcept cqConcept, ConversionContext context) {

		CQTable cqTable = cqConcept.getTables().get(0);
		String tableName = cqTable.getConnector().getTable().getName();
		String conceptLabel = context.getNameGenerator().conceptName(cqConcept);
		Optional<ColumnDateRange> validityDateSelect = convertValidityDate(cqTable, tableName, conceptLabel);

		ConnectorTables connectorTables = new ConnectorTables(conceptLabel, tableName, context.getNameGenerator());

		// convert filters
		List<SqlFilters> allFiltersForTable = new ArrayList<>();
		cqTable.getFilters().stream()
			   .map(filterValue -> filterValue.convertToSqlFilter(context, connectorTables))
			   .forEach(allFiltersForTable::add);
		collectConditionFilters(cqConcept, cqTable).ifPresent(allFiltersForTable::add);
		getDateRestriction(context, validityDateSelect).ifPresent(allFiltersForTable::add);

		// convert selects
		SelectContext selectContext = new SelectContext(context, cqConcept, conceptLabel, validityDateSelect, connectorTables);
		List<SqlSelects> conceptSelects = Stream.concat(cqConcept.getSelects().stream(), cqTable.getSelects().stream())
												.map(select -> select.convertToSqlSelects(selectContext))
												.toList();

		return CQTableContext.builder()
							 .conversionContext(context)
							 .sqlFilters(allFiltersForTable)
							 .sqlSelects(conceptSelects)
							 .validityDate(validityDateSelect)
							 .isExcludedFromDateAggregation(cqConcept.isExcludeFromTimeAggregation())
							 .connectorTables(connectorTables)
							 .conceptLabel(conceptLabel)
							 .build();
	}

	private Optional<ColumnDateRange> convertValidityDate(
			CQTable table,
			String tableName,
			String conceptLabel
	) {
		if (Objects.isNull(table.findValidityDate())) {
			return Optional.empty();
		}
		ColumnDateRange validityDate = this.functionProvider.daterange(table.findValidityDate(), tableName, conceptLabel);
		return Optional.of(validityDate);
	}

	private Optional<SqlFilters> getDateRestriction(ConversionContext context, Optional<ColumnDateRange> validityDate) {

		if (!dateRestrictionApplicable(context.dateRestrictionActive(), validityDate)) {
			return Optional.empty();
		}

		ColumnDateRange dateRestriction = this.functionProvider
				.daterange(context.getDateRestrictionRange())
				.asDateRestrictionRange();

		List<SqlSelect> dateRestrictionSelects = dateRestriction.toFields().stream()
																.map(FieldWrapper::new)
																.collect(Collectors.toList());

		Condition dateRestrictionCondition = this.functionProvider.dateRestriction(dateRestriction, validityDate.get());

		return Optional.of(new SqlFilters(
				SqlSelects.builder().preprocessingSelects(dateRestrictionSelects).build(),
				WhereClauses.builder().eventFilter(ConditionUtil.wrap(dateRestrictionCondition, ConditionType.EVENT)).build()
		));
	}

	private static boolean dateRestrictionApplicable(boolean dateRestrictionRequired, Optional<ColumnDateRange> validityDateSelect) {
		return dateRestrictionRequired && validityDateSelect.isPresent();
	}

	private Optional<SqlFilters> collectConditionFilters(CQConcept cqConcept, CQTable cqTable) {
		return collectConditions(cqConcept, cqTable)
				.stream()
				.map(WhereCondition::condition)
				.reduce(Condition::or)
				.map(condition -> new WhereConditionWrapper(condition, ConditionType.PREPROCESSING))
				.map(whereCondition -> new SqlFilters(
						SqlSelects.builder().build(),
						WhereClauses.builder().preprocessingCondition(whereCondition).build()
				));
	}

	private List<WhereCondition> collectConditions(CQConcept cqConcept, CQTable cqTable) {
		List<WhereCondition> conditions = new ArrayList<>();
		convertConnectorCondition(cqTable).ifPresent(conditions::add);
		cqConcept.getElements().stream()
				 .filter(conceptElement -> conceptElement instanceof ConceptTreeChild)
				 .forEach(conceptElement -> {
					 ConceptTreeChild child = (ConceptTreeChild) conceptElement;
					 Connector connector = cqTable.getConnector();
					 WhereCondition childCondition = child.getCondition().convertToSqlCondition(CTConditionContext.create(connector, this.functionProvider));
					 conditions.add(childCondition);
				 });
		return conditions;
	}

	private Optional<WhereCondition> convertConnectorCondition(CQTable cqTable) {
		return Optional.ofNullable(cqTable.getConnector().getCondition())
					   .map(condition -> condition.convertToSqlCondition(CTConditionContext.create(cqTable.getConnector(), this.functionProvider)));
	}

}

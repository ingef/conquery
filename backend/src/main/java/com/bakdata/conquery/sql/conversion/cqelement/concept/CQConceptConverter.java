package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private final List<ConceptCte> conceptCTEs;
	private final SqlFunctionProvider functionProvider;

	public CQConceptConverter(SqlFunctionProvider functionProvider) {
		this.functionProvider = functionProvider;
		this.conceptCTEs = List.of(
				new PreprocessingCte(),
				new EventFilterCte(),
				new AggregationSelectCte(),
				new AggregationFilterCte(),
				new FinalConceptCte()
		);
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept node, ConversionContext context) {

		if (node.getTables().size() > 1) {
			throw new UnsupportedOperationException("Can't handle concepts with multiple tables for now.");
		}

		ConceptCteContext conceptCteContext = createConceptCteContext(node, context);

		Optional<QueryStep> lastQueryStep = Optional.empty();
		for (ConceptCte queryStep : this.conceptCTEs) {
			Optional<QueryStep> convertedStep = queryStep.convert(conceptCteContext, lastQueryStep);
			if (convertedStep.isEmpty()) {
				continue;
			}
			lastQueryStep = convertedStep;
			conceptCteContext = conceptCteContext.withPrevious(lastQueryStep.get());
		}

		return context.toBuilder()
					  .queryStep(lastQueryStep.orElseThrow(() -> new RuntimeException("No conversion for concept possible.")))
					  .build();
	}

	private ConceptCteContext createConceptCteContext(CQConcept node, ConversionContext context) {

		CQTable table = node.getTables().get(0);
		String tableName = table.getConnector().getTable().getName();
		String conceptLabel = context.getNameGenerator().conceptName(node);
		Optional<ColumnDateRange> validityDateSelect = convertValidityDate(table, tableName, conceptLabel);

		Set<ConceptCteStep> requiredSteps = getRequiredSteps(table, context.dateRestrictionActive(), validityDateSelect);
		ConceptTables conceptTables = new ConceptTables(conceptLabel, requiredSteps, tableName, context.getNameGenerator());

		// convert filters
		Stream<SqlFilters> conceptFilters = table.getFilters().stream()
												 .map(filterValue -> filterValue.convertToSqlFilters(context, conceptTables));
		Stream<SqlFilters> dateRestrictionFilter = getDateRestriction(context, validityDateSelect).stream();
		List<SqlFilters> allFilters = Stream.concat(conceptFilters, dateRestrictionFilter).toList();

		// convert selects
		SelectContext selectContext = new SelectContext(context, node, conceptLabel, validityDateSelect, conceptTables);
		List<SqlSelects> conceptSelects = Stream.concat(node.getSelects().stream(), table.getSelects().stream())
												.map(select -> select.convertToSqlSelects(selectContext))
												.toList();

		return ConceptCteContext.builder()
								.conversionContext(context)
								.filters(allFilters)
								.selects(conceptSelects)
								.primaryColumn(DSL.field(DSL.name(context.getConfig().getPrimaryColumn())))
								.validityDate(validityDateSelect)
								.isExcludedFromDateAggregation(node.isExcludeFromTimeAggregation())
								.conceptTables(conceptTables)
								.conceptLabel(conceptLabel)
								.build();
	}

	/**
	 * Determines if event/aggregation filter steps are required.
	 *
	 * <p>
	 * {@link ConceptCteStep#MANDATORY_STEPS} are allways part of any concept conversion.
	 */
	private Set<ConceptCteStep> getRequiredSteps(CQTable table, boolean dateRestrictionRequired, Optional<ColumnDateRange> validityDateSelect) {
		Set<ConceptCteStep> requiredSteps = new HashSet<>(ConceptCteStep.MANDATORY_STEPS);

		if (dateRestrictionApplicable(dateRestrictionRequired, validityDateSelect)) {
			requiredSteps.add(ConceptCteStep.EVENT_FILTER);
		}

		table.getFilters().stream()
			 .flatMap(filterValue -> filterValue.getFilter().getRequiredSqlSteps().stream())
			 .forEach(requiredSteps::add);

		return requiredSteps;
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

}

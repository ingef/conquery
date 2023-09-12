package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.Filters;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.FieldSelect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.filter.FilterConversions;
import com.bakdata.conquery.sql.conversion.filter.FilterValueConversions;
import com.bakdata.conquery.sql.conversion.select.SelectContext;
import com.bakdata.conquery.sql.conversion.select.SelectConversions;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private final List<ConceptCte> conceptCTEs;
	private final FilterValueConversions filterValueConversions;
	private final SelectConversions selectConversions;

	public CQConceptConverter(FilterConversions filterConversions, SelectConversions selectConversions) {
		this.filterValueConversions = new FilterValueConversions(filterConversions);
		this.selectConversions = selectConversions;
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

		CQTable table = node.getTables().get(0);
		String conceptLabel = createConceptLabel(node, context);

		ConceptTables conceptTables = new ConceptTables(conceptLabel, getRequiredSteps(table), table.getConnector().getTable().getName());
		Optional<ColumnDateRange> validityDateSelect = convertValidityDate(context.getSqlDialect().getFunction(), table, conceptLabel, conceptTables);
		List<ConceptFilter> conceptFilters = convertConceptFilters(context, table, conceptTables, validityDateSelect);
		List<SqlSelects> conceptSelects = getConceptSelects(node, context, table, conceptLabel, conceptTables, validityDateSelect);

		CteContext cteContext = CteContext.builder()
										  .context(context)
										  .filters(conceptFilters)
										  .selects(conceptSelects)
										  .primaryColumn(DSL.field(DSL.name(context.getConfig().getPrimaryColumn())))
										  .validityDateRange(validityDateSelect)
										  .isExcludedFromDateAggregation(node.isExcludeFromTimeAggregation())
										  .conceptTables(conceptTables)
										  .conceptLabel(conceptLabel)
										  .build();

		Optional<QueryStep> lastQueryStep = Optional.empty();
		for (ConceptCte queryStep : this.conceptCTEs) {
			Optional<QueryStep> convertedStep = queryStep.convert(cteContext, lastQueryStep);
			if (convertedStep.isEmpty()) {
				continue;
			}
			lastQueryStep = convertedStep;
			cteContext = cteContext.withPrevious(lastQueryStep.get());
		}

		return context.withQueryStep(lastQueryStep.orElseThrow(() -> new RuntimeException("No conversion for concept possible. Required steps: %s".formatted(requiredSteps()))));
	}

	private Set<CteStep> getRequiredSteps(CQTable table) {
		if (table.getFilters().isEmpty()) {
			return CteStep.mandatorySteps();
		}
		return table.getFilters().stream()
					.flatMap(filterValue -> this.filterValueConversions.requiredSteps(filterValue).stream())
					.collect(Collectors.toSet());
	}

	/**
	 * Converts the concept-level selects before we convert the table-level selects,
	 * because {@link CQConcept#getResultInfos()} will create the result infos in the same order.
	 */
	private List<SqlSelects> getConceptSelects(
			CQConcept node,
			ConversionContext context,
			CQTable table,
			String conceptLabel,
			ConceptTables conceptTables,
			Optional<ColumnDateRange> validityDateSelect
	) {
		SelectContext selectContext = new SelectContext(context, node, conceptLabel, validityDateSelect, conceptTables);
		return Stream.concat(node.getSelects().stream(), table.getSelects().stream())
					 .map(select -> this.selectConversions.convert(select, selectContext))
					 .toList();
	}

	private List<ConceptFilter> convertConceptFilters(
			ConversionContext context,
			CQTable table,
			ConceptTables conceptTables,
			Optional<ColumnDateRange> validityDateSelect
	) {
		Stream<ConceptFilter> conceptFilters = table.getFilters().stream()
													.map(filterValue -> this.filterValueConversions.convert(filterValue, context, conceptTables));
		Stream<ConceptFilter> dateRestrictionFilter = getDateRestriction(context, validityDateSelect).stream();
		return Stream.concat(conceptFilters, dateRestrictionFilter).toList();
	}

	private static String createConceptLabel(CQConcept node, ConversionContext context) {
		// only relevant for debugging purposes as it will be part of the generated SQL query
		// we prefix each cte name of a concept with an incrementing counter to prevent naming collisions if the same concept is selected multiple times
		return "%s_%s".formatted(
				context.getQueryStepCounter(),
				WHITESPACE.matcher(node.getUserOrDefaultLabel(Locale.ENGLISH).toLowerCase()).replaceAll("_")
		);
	}

	private static Optional<ColumnDateRange> convertValidityDate(
			SqlFunctionProvider functionProvider,
			CQTable table,
			String conceptLabel,
			ConceptTables conceptTables
	) {
		if (Objects.isNull(table.findValidityDate())) {
			return Optional.empty();
		}
		return Optional.of(functionProvider.daterange(table.findValidityDate(), conceptTables.getPredecessorTableName(CteStep.PREPROCESSING), conceptLabel));
	}

	private static Optional<ConceptFilter> getDateRestriction(ConversionContext context, Optional<ColumnDateRange> validityDate) {

		if (!context.dateRestrictionActive() || validityDate.isEmpty()) {
			return Optional.empty();
		}

		ColumnDateRange dateRestriction = context.getSqlDialect().getFunction()
												 .daterange(context.getDateRestrictionRange())
												 .asDateRestrictionRange();

		List<ConquerySelect> dateRestrictionSelects = dateRestriction.toFields().stream()
																	 .map(FieldSelect::new)
																	 .collect(Collectors.toList());

		Condition dateRestrictionCondition = context.getSqlDialect().getFunction().dateRestriction(dateRestriction, validityDate.get());

		return Optional.of(new ConceptFilter(
				SqlSelects.builder().forPreprocessingStep(dateRestrictionSelects).build(),
				Filters.builder().event(Collections.singletonList(ConditionUtil.wrap(dateRestrictionCondition, FilterType.EVENT))).build()
		));
	}

}

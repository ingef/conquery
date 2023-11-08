package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashSet;
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
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConversions;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterValueConversions;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConversions;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.filter.ConceptFilter;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.model.filter.FilterType;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private final List<ConceptCte> conceptCTEs;
	private final FilterValueConversions filterValueConversions;
	private final SelectConversions selectConversions;
	private final SqlFunctionProvider functionProvider;

	public CQConceptConverter(FilterConversions filterConversions, SelectConversions selectConversions, SqlFunctionProvider functionProvider) {
		this.filterValueConversions = new FilterValueConversions(filterConversions);
		this.selectConversions = selectConversions;
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

		return context.withQueryStep(lastQueryStep.orElseThrow(() -> new RuntimeException("No conversion for concept possible. Required steps: %s".formatted(requiredSteps()))));
	}

	private ConceptCteContext createConceptCteContext(CQConcept node, ConversionContext context) {

		CQTable table = node.getTables().get(0);
		String tableName = table.getConnector().getTable().getName();
		String conceptLabel = createConceptLabel(node, context);
		Optional<ColumnDateRange> validityDateSelect = convertValidityDate(table, tableName, conceptLabel);

		Set<ConceptStep> requiredSteps = getRequiredSteps(table, context.dateRestrictionActive(), validityDateSelect);
		ConceptTables conceptTables = new ConceptTables(conceptLabel, requiredSteps, tableName);

		// convert filters
		Stream<ConceptFilter> conceptFilters = table.getFilters().stream()
													.map(filterValue -> this.filterValueConversions.convert(filterValue, context, conceptTables));
		Stream<ConceptFilter> dateRestrictionFilter = getDateRestriction(context, validityDateSelect).stream();
		List<ConceptFilter> allFilters = Stream.concat(conceptFilters, dateRestrictionFilter).toList();

		// convert selects
		SelectContext selectContext = new SelectContext(context, node, conceptLabel, validityDateSelect, conceptTables);
		List<SqlSelects> conceptSelects = Stream.concat(node.getSelects().stream(), table.getSelects().stream())
												.map(select -> this.selectConversions.convert(select, selectContext))
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
	 * {@link ConceptStep#MANDATORY_STEPS} are allways part of any concept conversion.
	 */
	private Set<ConceptStep> getRequiredSteps(CQTable table, boolean dateRestrictionRequired, Optional<ColumnDateRange> validityDateSelect) {
		Set<ConceptStep> requiredSteps = new HashSet<>(ConceptStep.MANDATORY_STEPS);

		if (dateRestrictionApplicable(dateRestrictionRequired, validityDateSelect)) {
			requiredSteps.add(ConceptStep.EVENT_FILTER);
		}

		table.getFilters().stream()
			 .flatMap(filterValue -> this.filterValueConversions.requiredSteps(filterValue).stream())
			 .forEach(requiredSteps::add);

		return requiredSteps;
	}

	private static String createConceptLabel(CQConcept node, ConversionContext context) {
		// only relevant for debugging purposes as it will be part of the generated SQL query
		// we prefix each cte name of a concept with an incrementing counter to prevent naming collisions if the same concept is selected multiple times
		return "%s_%s".formatted(
				context.getQueryStepCounter(),
				WHITESPACE.matcher(node.getUserOrDefaultLabel(Locale.ENGLISH).toLowerCase()).replaceAll("_")
		);
	}

	private Optional<ColumnDateRange> convertValidityDate(
			CQTable table,
			String tableName,
			String conceptLabel
	) {
		if (Objects.isNull(table.findValidityDate())) {
			return Optional.empty();
		}
		ColumnDateRange validityDate = functionProvider.daterange(table.findValidityDate(), tableName, conceptLabel);
		return Optional.of(validityDate);
	}

	private Optional<ConceptFilter> getDateRestriction(ConversionContext context, Optional<ColumnDateRange> validityDate) {

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

		return Optional.of(new ConceptFilter(
				SqlSelects.builder().forPreprocessingStep(dateRestrictionSelects).build(),
				Filters.builder().event(List.of(ConditionUtil.wrap(dateRestrictionCondition, FilterType.EVENT))).build()
		));
	}

	private static boolean dateRestrictionApplicable(boolean dateRestrictionRequired, Optional<ColumnDateRange> validityDateSelect) {
		return dateRestrictionRequired && validityDateSelect.isPresent();
	}

}

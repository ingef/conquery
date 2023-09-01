package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.error.ConqueryError;
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
	private final FilterValueConversions filterConverterService;
	private final SelectConversions selectConverterService;

	public CQConceptConverter(FilterConversions filterConversions, SelectConversions selectConverterService) {
		this.filterConverterService = new FilterValueConversions(filterConversions);
		this.selectConverterService = selectConverterService;
		this.conceptCTEs = List.of(
				new PreprocessingCte(),
				new EventFilterCte(),
				new AggregationSelectCte(),
				new AggregationFilterCte()
		);
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept node, ConversionContext context) {

		if (node.getTables().size() > 1) {
			throw new ConqueryError.SqlConversionError("Can't handle concepts with multiple tables for now.");
		}

		CQTable table = node.getTables().get(0);
		String conceptLabel = createConceptLabel(node, context);
		ConceptTableNames conceptTableNames = new ConceptTableNames(conceptLabel, table.getConnector().getTable().getName());

		Optional<ColumnDateRange> validityDateSelect = convertValidityDate(context.getSqlDialect().getFunction(), table, conceptLabel, conceptTableNames);
		List<ConceptFilter> conceptFilters = convertConceptFilters(context, table, conceptTableNames, validityDateSelect);
		List<SqlSelects> conceptSelects = getConceptSelects(node, context, table, conceptLabel, conceptTableNames, validityDateSelect);

		CteContext cteContext = CteContext.builder()
										  .context(context)
										  .filters(conceptFilters)
										  .selects(conceptSelects)
										  .primaryColumn(DSL.field(DSL.name(context.getConfig().getPrimaryColumn())))
										  .validityDateRange(validityDateSelect)
										  .isExcludedFromDateAggregation(node.isExcludeFromTimeAggregation())
										  .conceptTableNames(conceptTableNames)
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

		return context.withQueryStep(lastQueryStep.orElseThrow(() -> new ConqueryError.SqlConversionError("No conversion for concept possible.")));
	}

	private List<SqlSelects> getConceptSelects(
			CQConcept node,
			ConversionContext context,
			CQTable table,
			String conceptLabel,
			ConceptTableNames conceptTableNames,
			Optional<ColumnDateRange> validityDateSelect
	) {
		SelectContext selectContext = new SelectContext(context, node, conceptLabel, validityDateSelect, conceptTableNames);
		return Stream.concat(table.getSelects().stream(), node.getSelects().stream())
					 .map(select -> this.selectConverterService.convert(select, selectContext))
					 .toList();
	}

	private List<ConceptFilter> convertConceptFilters(
			ConversionContext context,
			CQTable table,
			ConceptTableNames conceptTableNames,
			Optional<ColumnDateRange> validityDateSelect
	) {
		Stream<ConceptFilter> conceptFilters = table.getFilters().stream()
													.map(filterValue -> this.filterConverterService.convert(filterValue, context, conceptTableNames));
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
			ConceptTableNames conceptTableNames
	) {
		if (Objects.isNull(table.findValidityDate())) {
			return Optional.empty();
		}
		return Optional.of(functionProvider.daterange(table.findValidityDate(), conceptTableNames.rootTable(), conceptLabel));
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

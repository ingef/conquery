package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.filter.FilterConverterService;
import com.bakdata.conquery.sql.conversion.select.SelectConverterService;
import org.jooq.Condition;
import org.jooq.Field;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private final FilterConverterService filterConverterService;
	private final SelectConverterService selectConverterService;

	public CQConceptConverter(FilterConverterService filterConverterService, SelectConverterService selectConverterService) {
		this.filterConverterService = filterConverterService;
		this.selectConverterService = selectConverterService;
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

		ConceptPreprocessingService preprocessingService = new ConceptPreprocessingService(node, context);
		CQTable table = node.getTables().get(0);
		String conceptLabel = this.getConceptLabel(node);

		QueryStep preprocessingStep = preprocessingService.buildPreprocessingQueryStepForTable(conceptLabel, table);
		QueryStep dateRestriction = this.buildDateRestrictionQueryStep(context, node, conceptLabel, preprocessingStep);
		QueryStep eventSelect = this.buildEventSelectQueryStep(context, table, conceptLabel, dateRestriction);
		QueryStep eventFilter = this.buildEventFilterQueryStep(context, table, conceptLabel, eventSelect);
		QueryStep finalStep = this.buildFinalQueryStep(conceptLabel, eventFilter);

		return context.withQuerySteps(List.of(finalStep));
	}

	private String getConceptLabel(CQConcept node) {
		// only relevant for debugging purposes as it will be part of the generated SQL query
		return node.getUserOrDefaultLabel(Locale.ENGLISH)
				   .toLowerCase()
				   .replace(' ', '_')
				   .replaceAll("\\s", "_");
	}

	/**
	 * selects:
	 * - all of previous step
	 */
	private QueryStep buildDateRestrictionQueryStep(
			ConversionContext context,
			CQConcept node,
			String conceptLabel,
			QueryStep previous
	) {
		if (((ConceptSelects) previous.getSelects()).getDateRestriction().isEmpty()) {
			return previous;
		}

		ConceptSelects dateRestrictionSelects = this.prepareDateRestrictionSelects(node, previous);
		List<Condition> dateRestriction = this.buildDateRestriction(context, previous);

		return QueryStep.builder()
						.cteName(createCteName(conceptLabel, "_date_restriction"))
						.fromTable(QueryStep.toTableLike(previous.getCteName()))
						.selects(dateRestrictionSelects)
						.conditions(dateRestriction)
						.predecessors(List.of(previous))
						.build();
	}

	/**
	 * selects:
	 * - all of previous steps
	 * - transformed columns with selects
	 */
	private QueryStep buildEventSelectQueryStep(
			ConversionContext context,
			CQTable table,
			String conceptLabel, QueryStep previous
	) {
		if (table.getSelects().isEmpty()) {
			return previous;
		}

		ConceptSelects eventSelectSelects = this.prepareEventSelectSelects(context, table, previous);

		return QueryStep.builder()
						.cteName(createCteName(conceptLabel, "_event_select"))
						.fromTable(QueryStep.toTableLike(previous.getCteName()))
						.selects(eventSelectSelects)
						.conditions(List.of())
						.predecessors(List.of(previous))
						.build();
	}

	/**
	 * selects:
	 * - all of previous step
	 * - remove filter
	 */
	private QueryStep buildEventFilterQueryStep(
			ConversionContext context,
			CQTable table,
			String conceptLabel,
			QueryStep previous
	) {
		if (table.getFilters().isEmpty()) {
			return previous;
		}

		ConceptSelects eventFilterSelects = this.prepareEventFilterSelects(previous);
		List<Condition> eventFilterConditions = this.buildEventFilterConditions(context, table);

		return QueryStep.builder()
						.cteName(createCteName(conceptLabel, "_event_filter"))
						.fromTable(QueryStep.toTableLike(previous.getCteName()))
						.selects(eventFilterSelects)
						.conditions(eventFilterConditions)
						.predecessors(List.of(previous))
						.build();
	}

	private ConceptSelects prepareDateRestrictionSelects(CQConcept node, QueryStep previous) {
		ConceptSelects.ConceptSelectsBuilder selectsBuilder = ((ConceptSelects) previous.getQualifiedSelects()).toBuilder();
		selectsBuilder.dateRestriction(Optional.empty());
		if (node.isExcludeFromTimeAggregation()) {
			selectsBuilder.validityDate(Optional.empty());
		}
		return selectsBuilder.build();
	}

	private List<Condition> buildDateRestriction(ConversionContext context, QueryStep previous) {
		return ((ConceptSelects) previous.getSelects()).getDateRestriction()
													   .map(dateRestrictionColumn -> getDateRestrictionAsCondition(context, previous, dateRestrictionColumn))
													   .orElseGet(List::of);
	}

	private static List<Condition> getDateRestrictionAsCondition(ConversionContext context, QueryStep previous, Field<Object> dateRestrictionColumn) {
		return previous.getSelects().getValidityDate().stream()
					   .map(validityDateColumn -> context.getSqlDialect().getFunction().dateRestriction(dateRestrictionColumn, validityDateColumn))
					   .toList();
	}

	private ConceptSelects prepareEventSelectSelects(
			ConversionContext context,
			CQTable table,
			QueryStep previous
	) {
		return ((ConceptSelects) previous.getQualifiedSelects()).withEventSelect(this.getEventSelects(context, table));
	}

	private ConceptSelects prepareEventFilterSelects(QueryStep previous) {
		return ((ConceptSelects) previous.getQualifiedSelects()).withEventFilter(List.of());
	}

	private List<Condition> buildEventFilterConditions(ConversionContext context, CQTable table) {
		return table.getFilters().stream()
					.map(filterValue -> this.filterConverterService.convert(filterValue, context))
					.toList();
	}

	private List<Field<Object>> getEventSelects(ConversionContext context, CQTable table) {
		return table.getSelects().stream()
					.map(select -> (Field<Object>) this.selectConverterService.convert(select, context))
					.toList();
	}

	/**
	 * selects:
	 * - all of previous step
	 */
	private QueryStep buildFinalQueryStep(String conceptLabel, QueryStep previous) {
		ConceptSelects finalSelects = ((ConceptSelects) previous.getQualifiedSelects());
		return QueryStep.builder()
						.cteName(createCteName(conceptLabel, ""))
						.fromTable(QueryStep.toTableLike(previous.getCteName()))
						.selects(finalSelects)
						.conditions(List.of())
						.predecessors(List.of(previous))
						.build();
	}

	private static String createCteName(String conceptLabel, String suffix) {
		return "concept_%s%s".formatted(conceptLabel, suffix);
	}

}

package com.bakdata.conquery.apiv1.query.concept.specific;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.NamespacedIdentifiableHolding;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@CPSType(id = "CONCEPT", base = CQElement.class)
@Slf4j
@ToString
public class CQConcept extends CQElement implements NamespacedIdentifiableHolding, ExportForm.DefaultSelectSettable {

	/**
	 * @implNote FK: this is a schema migration problem I'm not interested fixing right now.
	 */
	@JsonProperty("ids")
	@NotEmpty
	@NsIdRefCollection
	private List<ConceptElement<?>> elements = Collections.emptyList();

	@Valid
	@NotEmpty
	@NotNull
	@JsonManagedReference
	private List<CQTable> tables = Collections.emptyList();

	@NotNull
	@NsIdRefCollection
	private List<Select> selects = new ArrayList<>();

	private boolean excludeFromTimeAggregation = false;
	private boolean excludeFromSecondaryIdQuery = false;

	@InternalOnly
	@NotNull
	private boolean aggregateEventDates;

	@Override
	public String defaultLabel(Locale locale) {
		if (elements.isEmpty()) {
			return null;
		}

		if (elements.size() == 1 && elements.get(0).equals(getConcept())) {
			return getConcept().getLabel();
		}

		final StringBuilder builder = new StringBuilder();

		builder.append(getConcept().getLabel());
		builder.append(" - ");

		for (ConceptElement<?> id : elements) {
			if (id.equals(getConcept())) {
				continue;
			}
			builder.append(id.getLabel()).append("+");
		}

		builder.deleteCharAt(builder.length() - 1);

		return builder.toString();
	}

	@JsonIgnore
	public Concept<?> getConcept() {
		return elements.get(0).getConcept();
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Selects belong to the Concept.")
	public boolean isAllSelectsForConcept() {
		final Concept<?> concept = getConcept();

		if (!getSelects().stream().map(Select::getHolder).allMatch(concept::equals)) {
			log.error("Not all selects belong to Concept[{}]", concept);
			return false;
		}

		return true;
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all elements belong to the same Concept.")
	public boolean isAllElementsForConcept() {
		final Concept<?> concept = getConcept();

		if (!getElements().stream().map(ConceptElement::getConcept).allMatch(concept::equals)) {
			log.error("Not all elements belong to Concept[{}]", concept);
			return false;
		}

		return true;
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final List<Aggregator<?>> conceptAggregators = createAggregators(plan, selects);

		List<QPNode> tableNodes = new ArrayList<>();
		for (CQTable table : tables) {

			List<FilterNode<?>> filters = table.getFilters().stream()
											   .map(FilterValue::createNode)
											   .collect(Collectors.toList());

			//add filter to children
			List<Aggregator<?>> aggregators = new ArrayList<>();

			aggregators.addAll(conceptAggregators);

			final List<Aggregator<?>> connectorAggregators = createAggregators(plan, table.getSelects());

			// Exists aggregators hold a reference to their parent FiltersNode so they need to be treated separately.
			// They also don't need aggregation as they simply imitate their reference.
			List<ExistsAggregator> existsAggregators =
					connectorAggregators.stream()
										.filter(ExistsAggregator.class::isInstance)
										.map(ExistsAggregator.class::cast)
										.collect(Collectors.toList());

			aggregators.addAll(connectorAggregators);

			aggregators.removeIf(ExistsAggregator.class::isInstance);


			List<Aggregator<CDateSet>> eventDateUnionAggregators =
					aggregateEventDates ? List.of(new EventDateUnionAggregator(Set.of(table.getConnector().getTable())))
										: Collections.emptyList();

			aggregators.addAll(eventDateUnionAggregators);

			final QPNode filtersNode = getConcept().createConceptQuery(context, filters, aggregators, eventDateUnionAggregators);

			// Link up the ExistsAggregators to the node
			existsAggregators.forEach(agg -> agg.setReference(filtersNode));

			// Select if matching secondaryId available
			final boolean hasSelectedSecondaryId =
					Arrays.stream(table.getConnector().getTable().getColumns())
						  .map(Column::getSecondaryId)
						  .filter(Objects::nonNull)
						  .anyMatch(o -> Objects.equals(context.getSelectedSecondaryId(), o));

			final Column validityDateColumn = selectValidityDateColumn(table);

			final ConceptNode node = new ConceptNode(
					// TODO Don't set validity node, when no validity column exists. See workaround for this and remove it: https://github.com/bakdata/conquery/pull/1362
					new ValidityDateNode(validityDateColumn, filtersNode),
					elements,
					table,
					// if the node is excluded, don't pass it into the Node.
					!excludeFromSecondaryIdQuery && hasSelectedSecondaryId ? context.getSelectedSecondaryId() : null
			);

			tableNodes.add(node);
		}

		// We always merge on concept level
		final QPNode outNode = OrNode.of(tableNodes, aggregateEventDates ? DateAggregationAction.MERGE : DateAggregationAction.BLOCK);

		// Link concept-level Exists-select to outer node.
		conceptAggregators.stream()
						  .filter(aggregator -> aggregator instanceof ExistsAggregator)
						  .forEach(aggregator -> ((ExistsAggregator) aggregator).setReference(outNode));

		return outNode;
	}

	/**
	 * Generates Aggregators from Selects. These are collected and also appended to the list of aggregators in the
	 * query plan that contribute to columns the result.
	 */
	private static List<Aggregator<?>> createAggregators(ConceptQueryPlan plan, List<Select> selects) {
		return selects.stream()
					  .map(Select::createAggregator)
					  .peek(plan::registerAggregator)
					  .collect(Collectors.toList());
	}

	private Column selectValidityDateColumn(CQTable table) {
		if (table.getDateColumn() != null) {
			return table.getDateColumn().getValue().getColumn();
		}

		//else use this first defined validity date column
		if (!table.getConnector().getValidityDates().isEmpty()) {
			return table.getConnector().getValidityDates().get(0).getColumn();
		}

		return null;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		for (Select select : selects) {
			collector.add(new SelectResultInfo(select, this));
		}

		for (CQTable table : tables) {
			for (Select sel : table.getSelects()) {
				collector.add(new SelectResultInfo(sel, this));
			}
		}
	}

	@Override
	public void collectNamespacedObjects(Set<NamespacedIdentifiable<?>> identifiables) {
		identifiables.addAll(elements);
		identifiables.addAll(selects);
		tables.forEach(table -> identifiables.add(table.getConnector()));
	}

	@Override
	public void resolve(QueryResolveContext context) {
		this.aggregateEventDates = !(excludeFromTimeAggregation || DateAggregationMode.NONE.equals(context.getDateAggregationMode()));
	}

	@Override
	public void setDefaultExists() {
		boolean allTablesEmpty = getTables().stream()
											.map(CQTable::getSelects)
											.allMatch(List::isEmpty);

		if (!(getSelects().isEmpty() && (tables.isEmpty() || allTablesEmpty))) {
			// Don't fill if there are any selects on concept level or on any table level
			return;
		}

		List<Select> cSelects = new ArrayList<>(getSelects());
		cSelects.addAll(getConcept().getDefaultSelects());

		setSelects(cSelects);

		for (CQTable t : getTables()) {
			List<Select> conSelects = new ArrayList<>(t.getSelects());
			conSelects.addAll(t.getConnector().getDefaultSelects());
			t.setSelects(conSelects);
		}
	}
}

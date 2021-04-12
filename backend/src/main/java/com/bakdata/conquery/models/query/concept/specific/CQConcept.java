package com.bakdata.conquery.models.query.concept.specific;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.SelectHolder;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
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
import com.google.common.base.Strings;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@CPSType(id = "CONCEPT", base = CQElement.class)
@Slf4j
@ToString
public class CQConcept extends CQElement implements NamespacedIdHolding, ExportForm.DefaultSelectSettable {

	/**
	 * @implNote FK: this is a schema migration problem I'm not interested fixing right now.
	 */
	@JsonProperty("ids")
	@NotEmpty
	@NsIdRefCollection
	private List<ConceptElement<?>> elements = Collections.emptyList();

	@Valid
	@NotEmpty
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
	public String getLabel(Locale locale) {
		final String label = super.getLabel(locale);
		if (!Strings.isNullOrEmpty(label)) {
			return label;
		}

		return getDefaultLabel();
	}

	@Nullable
	@JsonIgnore
	public String getDefaultLabel() {
		if (elements.isEmpty()) {
			return null;
		}

		if(elements.size() == 1 && elements.get(0).equals(getConcept())) {
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

		if (!getSelects().stream().map(Select::getHolder).map(SelectHolder::findConcept).allMatch(concept::equals)) {
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

		List<Aggregator<?>> conceptAggregators = createAggregators(plan, selects);

		Concept<?> concept = getConcept();

		List<QPNode> tableNodes = new ArrayList<>();
		for (CQTable table : tables) {
			List<Select> resolvedSelects = table.getSelects();


			List<FilterNode<?>> filters = new ArrayList<>(table.getFilters().size());
			//add filter to children
			for (FilterValue f : table.getFilters()) {
				FilterNode agg = f.getFilter().createAggregator(f.getValue());
				if (agg != null) {
					filters.add(agg);
				}
			}

			List<Aggregator<?>> aggregators = new ArrayList<>();
			//add aggregators

			aggregators.addAll(conceptAggregators);

			final List<Aggregator<?>> connectorAggregators = createAggregators(plan, resolvedSelects);

			List<ExistsAggregator> existsAggregators = connectorAggregators.stream()
																		   .filter(ExistsAggregator.class::isInstance)
																		   .map(ExistsAggregator.class::cast)
																		   .collect(Collectors.toList());

			aggregators.addAll(connectorAggregators);

			aggregators.removeIf(ExistsAggregator.class::isInstance);

			Column validityDateColumn = selectValidityDateColumn(table);

			if(aggregateEventDates){
				aggregators.add(new EventDateUnionAggregator(Set.of(table.getConnector().getTable().getId())));
			}

			final QPNode filtersNode = concept.createConceptQuery(context, filters, aggregators);

			existsAggregators.forEach(agg -> agg.setReference(filtersNode));

			// Select if matching secondaryId available
			final boolean hasSelectedSecondaryId =
					Arrays.stream(table.getConnector().getTable().getColumns())
						  .map(Column::getSecondaryId)
						  .filter(Objects::nonNull)
						  .map(SecondaryIdDescription::getId)
						  .anyMatch(o -> Objects.equals(context.getSelectedSecondaryId(), o));

			tableNodes.add(
					new ConceptNode(
							elements,
							CBlock.calculateBitMask(elements),
							table,
							// TODO Don't set validity node, when no validity column exists. See workaround for this and remove it: https://github.com/bakdata/conquery/pull/1362
							new ValidityDateNode(
									selectValidityDateColumn(table),
									filtersNode
							),
							// if the node is excluded, don't pass it into the Node.
							!excludeFromSecondaryIdQuery && hasSelectedSecondaryId ? context.getSelectedSecondaryId() : null
					)
			);
		}

		if (tableNodes.isEmpty()) {
			throw new IllegalStateException(String.format("Unable to resolve any connector for Query[%s]", this));
		}

		// We always merge on concept level
		final QPNode outNode = OrNode.of(tableNodes, aggregateEventDates ? DateAggregationAction.MERGE : DateAggregationAction.BLOCK);

		for (Iterator<Aggregator<?>> iterator = conceptAggregators.iterator(); iterator.hasNext(); ) {
			Aggregator<?> aggregator = iterator.next();
			if (aggregator instanceof ExistsAggregator) {
				((ExistsAggregator) aggregator).setReference(outNode);
				iterator.remove();
			}
		}

		return outNode;
	}

	public static ConceptElement[] resolveConcepts(List<ConceptElementId<?>> ids, CentralRegistry centralRegistry) {
		return ids.stream()
				  .map(id -> centralRegistry.resolve(id.findConcept()).getElementById(id))
				  .toArray(ConceptElement[]::new);
	}

	/**
	 * Generates Aggregators from Selects. These are collected and also appended to the list of aggregators in the
	 * query plan that contribute to columns the result.
	 */
	private static List<Aggregator<?>> createAggregators(ConceptQueryPlan plan, List<Select> select) {

		List<Aggregator<?>> nodes = new ArrayList<>();

		for (Select s : select) {
			Aggregator<?> agg = s.createAggregator();

			plan.addAggregator(agg);
			nodes.add(agg);
		}
		return nodes;
	}

	private Column selectValidityDateColumn(CQTable table) {
		if (table.getDateColumn() != null) {
			return table.getConnector()
						.getValidityDateColumn(table.getDateColumn().getValue());
		}

		//else use this first defined validity date column
		if (!table.getConnector().getValidityDates().isEmpty()) {
			return table.getConnector().getValidityDates().get(0).getColumn();
		}

		return null;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		selects.forEach(sel -> collector.add(new SelectResultInfo(sel, this)));
		for (CQTable table : tables) {
			table.getSelects()
				 .forEach(sel -> collector.add(new SelectResultInfo(sel, this)));
		}
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		checkNotNull(namespacedIds);
		elements.forEach(ce -> namespacedIds.add(ce.getId()));
		selects.forEach(select -> namespacedIds.add(select.getId()));
		tables.forEach(table -> namespacedIds.add(table.getConnector().getId()));
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

package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.NamespacedIdentifiableHolding;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@CPSType(id = "CONCEPT", base = CQElement.class)
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true, doNotUseGetters = true)
public class CQConcept extends CQElement implements NamespacedIdentifiableHolding, ExportForm.DefaultSelectSettable {

	/**
	 * @implNote FK: this is a schema migration problem I'm not interested fixing right now.
	 */
	@JsonProperty("ids")
	@NotEmpty
	private List<ConceptElementId<?>> elements = Collections.emptyList();

	@Valid
	@NotEmpty
	@NotNull
	@JsonManagedReference
	private List<CQTable> tables = Collections.emptyList();

	@NotNull
	private List<SelectId> selects = new ArrayList<>();

	private boolean excludeFromTimeAggregation;

	//TODO FK 2.12.2021: remove this after successful recode.
	@JsonAlias("excludeFromSecondaryIdQuery")
	private boolean excludeFromSecondaryId;

	@JsonView(View.InternalCommunication.class)
	private boolean aggregateEventDates;

	public static CQConcept forSelect(Select select) {
		final CQConcept cqConcept = new CQConcept();
		cqConcept.setElements(List.of(select.getHolder().findConcept().getId()));

		if (select.getHolder() instanceof Connector) {
			final CQTable table = new CQTable();
			cqConcept.setTables(List.of(table));

			table.setConnector(((Connector) select.getHolder()).getId());

			table.setSelects(List.of(select.getId()));
		}
		else {
			cqConcept.setTables(((Concept<?>) select.getHolder())
										.getConnectors().stream()
										.map(conn -> {
											final CQTable table = new CQTable();
											table.setConnector(conn.getId());
											return table;
										}).toList());

			cqConcept.setSelects(List.of(select.getId()));
		}

		return cqConcept;
	}

	public static CQConcept forConnector(Connector source) {
		final CQConcept cqConcept = new CQConcept();
		cqConcept.setElements(List.of(source.getConcept().getId()));
		final CQTable cqTable = new CQTable();
		cqTable.setConcept(cqConcept);
		cqTable.setConnector(source.getId());
		cqConcept.setTables(List.of(cqTable));

		return cqConcept;
	}

	@Override
	public String defaultLabel(Locale locale) {
		if (elements.isEmpty()) {
			return null;
		}

		if (elements.size() == 1 && elements.get(0).equals(getConcept().getId())) {
			return getConcept().getLabel();
		}

		final StringBuilder builder = new StringBuilder();

		builder.append(getConcept().getLabel());
		builder.append(" ");

		for (ConceptElementId<?> id : elements) {
			ConceptElement<?> conceptElement = id.resolve();
			if (conceptElement.equals(getConcept())) {
				continue;
			}
			builder.append(conceptElement.getLabel()).append("+");
		}

		builder.deleteCharAt(builder.length() - 1);

		return builder.toString();
	}

	@JsonIgnore
	public Concept<?> getConcept() {
		return elements.get(0).resolve().getConcept();
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Selects belong to the Concept.")
	public boolean isAllSelectsForConcept() {
		final Concept<?> concept = getConcept();

		if (!getSelects().stream().map(SelectId::<Select>resolve).map(Select::getHolder).allMatch(concept::equals)) {
			log.error("Not all selects belong to Concept[{}]", concept);
			return false;
		}

		return true;
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all elements belong to the same Concept.")
	public boolean isAllElementsForConcept() {
		final ConceptId concept = getConcept().getId();

		if (!getElements().stream().map(ConceptElementId::findConcept).allMatch(concept::equals)) {
			log.error("Not all elements belong to Concept[{}]", concept);
			return false;
		}

		return true;
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final List<Aggregator<?>> conceptAggregators = createAggregators(plan, selects);

		final List<QPNode> tableNodes = new ArrayList<>();
		for (CQTable table : tables) {

			final List<FilterNode<?>> filters = table.getFilters().stream()
													 .map(FilterValue::createNode)
													 .collect(Collectors.toList());

			//add filter to children
			final List<Aggregator<?>> aggregators = new ArrayList<>();

			aggregators.addAll(conceptAggregators);

			final List<Aggregator<?>> connectorAggregators = createAggregators(plan, table.getSelects());

			// Exists aggregators hold a reference to their parent FiltersNode so they need to be treated separately.
			// They also don't need aggregation as they simply imitate their reference.
			final List<ExistsAggregator> existsAggregators =
					connectorAggregators.stream()
										.filter(ExistsAggregator.class::isInstance)
										.map(ExistsAggregator.class::cast)
										.collect(Collectors.toList());

			aggregators.addAll(connectorAggregators);

			aggregators.removeIf(ExistsAggregator.class::isInstance);


			final List<Aggregator<CDateSet>> eventDateUnionAggregators =
					aggregateEventDates ? List.of(new EventDateUnionAggregator(Set.of(table.getConnector().<Connector>resolve().getResolvedTable())))
										: Collections.emptyList();

			aggregators.addAll(eventDateUnionAggregators);

			final QPNode
					conceptSpecificNode =
					getConcept().createConceptQuery(context, filters, aggregators, eventDateUnionAggregators, selectValidityDate(table));

			// Link up the ExistsAggregators to the node
			existsAggregators.forEach(agg -> agg.setReference(conceptSpecificNode));

			// Select if matching secondaryId available
			final boolean hasSelectedSecondaryId = table.hasSelectedSecondaryId(context.getSelectedSecondaryId());

			final ConceptNode node = new ConceptNode(
					conceptSpecificNode,
					elements.stream().map(id -> (ConceptElement) id.resolve()).toList(),
					table,
					// if the node is excluded, don't pass it into the Node.
					!excludeFromSecondaryId && hasSelectedSecondaryId ? context.getSelectedSecondaryId() : null
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

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {

	}

	/**
	 * Generates Aggregators from Selects. These are collected and also appended to the list of aggregators in the
	 * query plan that contribute to columns the result.
	 */
	private static List<Aggregator<?>> createAggregators(ConceptQueryPlan plan, List<SelectId> selects) {
		return selects.stream()
					  .map(SelectId::<Select>resolve)
					  .map(Select::createAggregator)
					  .peek(plan::registerAggregator)
					  .collect(Collectors.toList());
	}

	private ValidityDate selectValidityDate(CQTable table) {
		if (table.getDateColumn() != null) {
			return table.getDateColumn().getValue().resolve();
		}

		//else use this first defined validity date column
		final Connector connector = table.getConnector().resolve();
		if (!connector.getValidityDates().isEmpty()) {
			return connector.getValidityDates().get(0);
		}

		return null;
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();

		for (SelectId select : selects) {
			resultInfos.add(select.resolve().getResultInfo(this));
		}

		for (CQTable table : tables) {
			for (SelectId sel : table.getSelects()) {
				resultInfos.add(sel.resolve().getResultInfo(this));
			}
		}

		return resultInfos;
	}

	@Override
	public void collectNamespacedObjects(Set<? super NamespacedIdentifiable<?>> identifiables) {
		final List<ConceptElement<?>> list = (List<ConceptElement<?>>) elements.stream().map(ConceptElementId::resolve).toList();
		identifiables.addAll(list);
		identifiables.addAll(selects.stream().map(Id::resolve).toList());
		tables.forEach(table -> identifiables.add(table.getConnector().resolve()));
	}

	@Override
	public void resolve(QueryResolveContext context) {
		aggregateEventDates = !(excludeFromTimeAggregation || DateAggregationMode.NONE.equals(context.getDateAggregationMode()));
		tables.forEach(t -> t.resolve(context));
	}

	@Override
	public void setDefaultExists() {
		final boolean allTablesEmpty = getTables().stream()
												  .map(CQTable::getSelects)
												  .allMatch(List::isEmpty);

		if (!(getSelects().isEmpty() && (tables.isEmpty() || allTablesEmpty))) {
			// Don't fill if there are any selects on concept level or on any table level
			return;
		}

		final List<SelectId> cSelects = new ArrayList<>(getSelects());
		cSelects.addAll(getConcept().getDefaultSelects().stream().map(Select::getId).toList());

		setSelects(cSelects);

		for (CQTable t : getTables()) {
			final List<SelectId> conSelects = new ArrayList<>(t.getSelects());
			conSelects.addAll(t.getConnector().resolve().getDefaultSelects().stream().map(Select::getId).toList());
			t.setSelects(conSelects);
		}
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		final Set<Connector> connectors = getTables().stream().map(CQTable::getConnector).map(ConnectorId::resolve).collect(Collectors.toSet());

		return new RequiredEntities(context.getBucketManager()
										   .getEntitiesWithConcepts(getElements().stream()
																				 .map(id -> (ConceptElement) id.resolve())
																				 .toList(), connectors, context.getDateRestriction()));
	}
}

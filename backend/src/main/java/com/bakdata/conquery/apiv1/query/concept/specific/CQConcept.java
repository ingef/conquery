package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
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
import com.bakdata.conquery.models.query.queryplan.filter.AggregationResultFilterNode;
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

	public static CQConcept forSelect(SelectId selectId) {
		final CQConcept cqConcept = new CQConcept();

		cqConcept.setElements(List.of(selectId.findConcept()));

		switch (selectId) {
			case ConceptSelectId conceptSelectId -> {
				cqConcept.setTables(conceptSelectId.getConcept().resolve()
												   .getConnectors().stream()
												   .map(conn -> {
													   final CQTable table = new CQTable();
													   table.setConnector(conn.getId());
													   return table;
												   }).toList());

				cqConcept.setSelects(List.of(conceptSelectId));
			}
			case ConnectorSelectId connectorSelectId -> {
				final CQTable table = new CQTable();
				cqConcept.setTables(List.of(table));

				table.setConnector(connectorSelectId.getConnector());

				table.setSelects(List.of(connectorSelectId));
				table.setConcept(cqConcept);
			}
		}

		return cqConcept;
	}

	public static CQConcept forConnector(ConnectorId source) {
		final CQConcept cqConcept = new CQConcept();

		cqConcept.setElements(List.of(source.getConcept()));
		final CQTable cqTable = new CQTable();
		cqTable.setConcept(cqConcept);
		cqTable.setConnector(source);
		cqConcept.setTables(List.of(cqTable));

		return cqConcept;
	}

	@Override
	public String defaultLabel(Locale locale) {
		if (elements.isEmpty()) {
			return null;
		}

		if (elements.size() == 1 && elements.get(0).equals(getConceptId())) {
			return getConcept().getLabel();
		}

		final StringBuilder builder = new StringBuilder();

		builder.append(getConcept().getLabel());
		builder.append(" ");

		for (ConceptElementId<?> id : elements) {
			if (id.equals(getConceptId())) {
				continue;
			}

			ConceptElement<?> conceptElement = id.resolve();
			builder.append(conceptElement.getLabel()).append("+");
		}

		builder.deleteCharAt(builder.length() - 1);

		return builder.toString();
	}

	@JsonIgnore
	public ConceptId getConceptId() {
		return elements.get(0).findConcept();
	}

	@JsonIgnore
	public Concept<?> getConcept() {
		return getConceptId().resolve();
	}

	@Override
	public void resolve(QueryResolveContext context) {
		aggregateEventDates = !(excludeFromTimeAggregation || DateAggregationMode.NONE.equals(context.getDateAggregationMode()));
		tables.forEach(t -> t.resolve(context));
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final List<Aggregator<?>> conceptAggregators = createAggregators(plan, selects, context);

		final List<QPNode> tableNodes = new ArrayList<>();
		for (CQTable table : tables) {

			final List<FilterNode<?>> filters = createFilters(table, context.isDisableAggregationFilters());

			//add filter to children

			final List<Aggregator<?>> aggregators = new ArrayList<>(conceptAggregators);

			final List<Aggregator<?>> connectorAggregators = createAggregators(plan, table.getSelects(), context);

			// Exists aggregators hold a reference to their parent FiltersNode, so they need to be treated separately.
			// They also don't need aggregation as they simply imitate their reference.
			final List<ExistsAggregator> existsAggregators =
					connectorAggregators.stream()
										.filter(ExistsAggregator.class::isInstance)
										.map(ExistsAggregator.class::cast)
										.toList();

			aggregators.addAll(connectorAggregators);

			aggregators.removeIf(ExistsAggregator.class::isInstance);


			final EventDateUnionAggregator eventDateUnionAggregator =
					aggregateEventDates ? new EventDateUnionAggregator()
										: null;

			if (aggregateEventDates) {
				aggregators.add(eventDateUnionAggregator);
			}

			final QPNode
					conceptSpecificNode =
					getConcept().createConceptQuery(context, filters, aggregators, eventDateUnionAggregator, selectValidityDate(table));

			if (eventDateUnionAggregator != null) {
				eventDateUnionAggregator.setOwner(conceptSpecificNode);
			}

			// Link up the ExistsAggregators to the node
			existsAggregators.forEach(agg -> agg.setReference(conceptSpecificNode));

			// Select if matching secondaryId available
			final boolean hasSelectedSecondaryId = context.getSelectedSecondaryId() != null && table.hasSelectedSecondaryId(context.getSelectedSecondaryId().getId());

			final ConceptNode node = new ConceptNode(
					conceptSpecificNode,
					elements.stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList(),
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

	/**
	 * Generates Aggregators from Selects. These are collected and also appended to the list of aggregators in the
	 * query plan that contribute to columns the result.
	 */
	private static List<Aggregator<?>> createAggregators(ConceptQueryPlan plan, List<? extends SelectId> selects, QueryPlanContext context) {
		if (context.isDisableAggregators()) {
			return Collections.emptyList();
		}

		return selects.stream()
					  .map(SelectId::resolve)
					  .map(Select::createAggregator)
					  .peek(plan::registerAggregator)
					  .collect(Collectors.toList());
	}

	private static List<FilterNode<?>> createFilters(CQTable table, boolean disableAggregationFilters) {
		Stream<? extends FilterNode<?>> filterNodes = table.getFilters().stream()
														   .map(FilterValue::createNode);

		if (disableAggregationFilters) {
			return filterNodes
					.filter(filterNode -> !(filterNode instanceof AggregationResultFilterNode))
					.collect(Collectors.toList());
		}


		return filterNodes
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
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {

	}

	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();

		for (SelectId select : selects) {
			Select resolved = select.resolve();
			resultInfos.add(resolved.getResultInfo(this));
		}

		for (CQTable table : tables) {
			for (SelectId sel : table.getSelects()) {
				Select resolved = sel.resolve();
				resultInfos.add(resolved.getResultInfo(this));
			}
		}

		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		final Set<ConnectorId> connectors = getTables().stream().map(CQTable::getConnector).collect(Collectors.toSet());

		return new RequiredEntities(context.getBucketManager()
										   .getEntitiesWithConcepts(getElements(),
																	connectors, context.getDateRestriction()
										   ));
	}


	@JsonIgnore
	@ValidationMethod(message = "Not all Selects belong to the Concept.")
	public boolean isAllSelectsForConcept() {
		final ConceptId concept = getConceptId();

		if (!getSelects().stream().map(SelectId::findConcept).allMatch(concept::equals)) {
			log.error("Not all selects belong to Concept[{}]", concept);
			return false;
		}

		return true;
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all elements belong to the same Concept.")
	public boolean isAllElementsForConcept() {

		final ConceptId concept = getConceptId();

		if (!getElements().stream().map(ConceptElementId::findConcept).allMatch(concept::equals)) {
			log.error("Not all elements belong to Concept[{}]", concept);
			return false;
		}

		return true;
	}

	@Override
	public void collectNamespacedObjects(Set<? super NamespacedIdentifiable<?>> identifiables) {
		for (ConceptElementId<?> element : elements) {
			identifiables.add(element.resolve());
		}

		for (SelectId select : selects) {
			identifiables.add(select.resolve());
		}
		for (CQTable table : tables) {
			identifiables.add(table.getConnector().resolve());
		}
	}

	@Override
	public void setDefaultSelects() {
		final boolean allTablesEmpty = getTables().stream()
												  .map(CQTable::getSelects)
												  .allMatch(List::isEmpty);

		if (!(getSelects().isEmpty() && (tables.isEmpty() || allTablesEmpty))) {
			// Don't fill if there are any selects on concept level or on any table level
			return;
		}

		final List<SelectId> cSelects = new ArrayList<>(getSelects());
		cSelects.addAll(getConcept().getDefaultSelects());

		setSelects(cSelects);

		for (CQTable t : getTables()) {
			final List<ConnectorSelectId> conSelects = new ArrayList<>(t.getSelects());
			conSelects.addAll(t.getConnector().resolve()
							   .getDefaultSelects().stream()
							   .map(Select::getId)
							   .map(ConnectorSelectId.class::cast)
							   .toList());

			t.setSelects(conSelects);
		}
	}
}

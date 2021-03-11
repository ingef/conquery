package com.bakdata.conquery.models.query.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
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
import com.google.common.collect.MoreCollectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter @Setter
@CPSType(id="CONCEPT", base=CQElement.class)
@Slf4j
@ToString
public class CQConcept extends CQElement implements NamespacedIdHolding {

	/**
	 * @implNote FK: this is a schema migration problem I'm not interested fixing right now.
	 */
	@JsonProperty("ids")
	@Valid @NotEmpty @NsIdRefCollection
	private List<ConceptElement<?>> elements = Collections.emptyList();

	@Valid @NotEmpty @JsonManagedReference
	private List<CQTable> tables = Collections.emptyList();

	@Valid @NotNull
	@NsIdRefCollection
	private List<Select> selects = new ArrayList<>();

	private boolean excludeFromTimeAggregation = false;
	private boolean excludeFromSecondaryIdQuery = false;

	@InternalOnly @NotNull
	private boolean aggregateEventDates;

	@Override
	public String getLabel(Locale cfg) {
		final String label = super.getLabel(cfg);
		if(!Strings.isNullOrEmpty(label)) {
			return label;
		}

		if(elements.isEmpty()){
			return null;
		}

		final StringBuilder builder = new StringBuilder();

		builder.append(getConcept().getLabel());

		builder.append(" - ");

		for (ConceptElement<?> id : elements) {
			builder.append(id.getLabel()).append("+");
		}

		builder.deleteCharAt(builder.length() - 1);


		return builder.toString();
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		List<Aggregator<?>> conceptAggregators = createAggregators(plan, selects);

		Concept<?> concept = getConcept();

		final SecondaryIdDescriptionId secondaryId = context.getSelectedSecondaryId();

		List<QPNode> tableNodes = new ArrayList<>();
		for(CQTable table : tables) {
			try {
				table.setResolvedConnector(concept.getConnectorByName(table.getId().getConnector()));
			}
			catch (NoSuchElementException exc){
				log.warn("Unable to resolve connector `{}` in dataset `{}`.",table.getId().getConnector(), concept.getDataset(), exc);
				continue;
			}

			List<Select> resolvedSelects = table.getSelects();


			List<FilterNode<?>> filters = new ArrayList<>(table.getFilters().size());
			//add filter to children
			for(FilterValue f : table.getFilters()) {
				FilterNode agg = f.getFilter().createAggregator(f.getValue());
				if(agg != null) {
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
				aggregators.add(new EventDateUnionAggregator(Set.of(table.getResolvedConnector().getTable().getId())));
			}

			concept.getConcept();
			final QPNode filtersNode = concept.createConceptQuery(context, filters, aggregators);

			existsAggregators.forEach(agg -> agg.setReference(filtersNode));
			
			final Connector connector = table.getResolvedConnector();

			// Select matching secondaryId if available
			final SecondaryIdDescriptionId selectedSecondaryId =
					Arrays.stream(connector.getTable().getColumns())
						  .map(Column::getSecondaryId)
						  .filter(Objects::nonNull)
						  .map(SecondaryIdDescription::getId)
						  .filter(o -> {
							  return Objects.equals(secondaryId, o);
						  })
						  .collect(MoreCollectors.toOptional())
						  .orElse(null);

			tableNodes.add(
					new ConceptNode(
							elements,
							CBlock.calculateBitMask(elements),
							table,
							// TODO Don't set validity node, when no validity column exists. See workaround for this and remove it: https://github.com/bakdata/conquery/pull/1362
							new ValidityDateNode(
									validityDateColumn,
									filtersNode
							),
							// if the node is excluded, don't pass it into the Node.
							excludeFromSecondaryIdQuery ? null : selectedSecondaryId
					)
			);
		}

		if(tableNodes.isEmpty()){
			throw new IllegalStateException(String.format("Unable to resolve any connector for Query[%s]", this));
		}

		// We always merge on concept level
		final QPNode outNode = OrNode.of(tableNodes, DateAggregationAction.MERGE);

		for (Iterator<Aggregator<?>> iterator = conceptAggregators.iterator(); iterator.hasNext(); ) {
			Aggregator<?> aggregator = iterator.next();
			if (aggregator instanceof ExistsAggregator) {
				((ExistsAggregator) aggregator).setReference(outNode);
				iterator.remove();
			}
		}

		return outNode;
	}

	@JsonIgnore
	public Concept<?> getConcept() {
		return elements.get(0).getConcept();
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
			return table.getResolvedConnector()
						.getValidityDateColumn(table.getDateColumn().getValue());
		}

		//else use this first defined validity date column
		if (!table.getResolvedConnector().getValidityDates().isEmpty()) {
			return table.getResolvedConnector().getValidityDates().get(0).getColumn();
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
		tables.forEach(table -> namespacedIds.add(table.getId()));
	}

	@Override
	public void resolve(QueryResolveContext context) {
		this.aggregateEventDates = !Objects.equals(context.getDateAggregationMode(), DateAggregationMode.NONE);
	}
}

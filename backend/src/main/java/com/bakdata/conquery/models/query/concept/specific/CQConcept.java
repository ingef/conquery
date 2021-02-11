package com.bakdata.conquery.models.query.concept.specific;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
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
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.MoreCollectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter
@CPSType(id="CONCEPT", base=CQElement.class)
@Slf4j
@FieldNameConstants
@JsonDeserialize(using = CQConceptDeserializer.class)
@ToString
public class CQConcept extends CQElement implements NamespacedIdHolding {

	@Valid @NotEmpty
	private List<ConceptElementId<?>> ids = Collections.emptyList();
	@Valid @NotEmpty @JsonManagedReference
	private List<CQTable> tables = Collections.emptyList();

	@Valid @NotNull
	@NsIdRefCollection
	private List<Select> selects = new ArrayList<>();

	private boolean excludeFromTimeAggregation = false;
	private boolean excludeFromSecondaryIdQuery = false;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		ConceptElement<?>[] concepts = resolveConcepts(ids, context.getCentralRegistry());

		List<Aggregator<?>> conceptAggregators = createAggregators(plan, selects);

		Concept<?> concept = concepts[0].getConcept();

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


			if(!excludeFromTimeAggregation && context.isGenerateSpecialDateUnion()) {
				aggregators.add(plan.getSpecialDateUnion());
			}

			final QPNode filtersNode = conceptChild(concept, context, filters, aggregators);

			existsAggregators.forEach(agg -> agg.setReference(filtersNode));
			
			final Connector connector = table.getResolvedConnector();

			// Select matching secondaryId if available
			final SecondaryIdDescriptionId selectedSecondaryId =
					Arrays.stream(connector.getTable().getColumns())
						  .map(Column::getSecondaryId)
						  .filter(Objects::nonNull)
						  .map(SecondaryIdDescription::getId)
						  .filter(o -> Objects.equals(context.getSelectedSecondaryId(), o))
						  .collect(MoreCollectors.toOptional())
						  .orElse(null);

			tableNodes.add(
				new ConceptNode(
						concepts,
						CBlock.calculateBitMask(concepts),
						table,
						// TODO Don't set validity node, when no validity column exists. See workaround for this and remove it: https://github.com/bakdata/conquery/pull/1362
						new ValidityDateNode(
						selectValidityDateColumn(table),
						filtersNode
					),
						// if the node is excluded, don't pass it into the Node.
					excludeFromSecondaryIdQuery ? null : selectedSecondaryId
				)
			);
		}

		if(tableNodes.isEmpty()){
			throw new IllegalStateException(String.format("Unable to resolve any connector for query `%s`", getLabel()));
		}

		final QPNode outNode = OrNode.of(tableNodes);

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

	protected QPNode conceptChild(Concept<?> concept, QueryPlanContext context, List<FilterNode<?>> filters, List<Aggregator<?>> aggregators) {
		if (filters.isEmpty() && aggregators.isEmpty()) {
			return new Leaf();
		}
		return FiltersNode.create(filters, aggregators);
	}

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
			table.getSelects().forEach(sel -> collector.add(new SelectResultInfo(sel, this)));
		}
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		checkNotNull(namespacedIds);
		namespacedIds.addAll(ids);
		selects.forEach(select -> namespacedIds.add(select.getId()));
		tables.forEach(table -> namespacedIds.add(table.getId()));
	}

	@Override
	public void resolve(QueryResolveContext context) {
		// Do nothing
	}
}

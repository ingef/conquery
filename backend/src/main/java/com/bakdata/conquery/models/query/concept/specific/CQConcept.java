package com.bakdata.conquery.models.query.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.queryplan.specific.SpecialDateUnionAggregatorNode;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Getter @Setter
@CPSType(id="CONCEPT", base=CQElement.class)
@Slf4j
public class CQConcept implements CQElement {

	private String label;

	/**
	 * All Concept Elements are coming from the same Concept.
	 */
	@Valid @NotEmpty
	private List<ConceptElementId<?>> ids;
	@Valid @NotEmpty @JsonManagedReference
	private List<CQTable> tables;

	@Valid @NotNull
	@NsIdRefCollection
	private List<Select> selects = new ArrayList<>();

	private boolean excludeFromTimeAggregation = false;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		final ConceptElement<?>[] concepts = resolveConcepts(ids, context.getCentralRegistry());
		final Concept<?> concept = concepts[0].getConcept();

		List<AggregatorNode<?>> conceptAggregators = createConceptAggregators(concept, selects);
		conceptAggregators.forEach(node -> plan.addAggregator(node.getAggregator()));


		List<QPNode> tableNodes = new ArrayList<>();
		for(CQTable table : tables) {
			try {
				table.setResolvedConnector(concept.getConnectorByName(table.getId().getConnector()));
			}
			catch (NoSuchElementException exc){
				log.warn("Unable to resolve connector `{}` in dataset `{}`.",table.getId().getConnector(), concept.getDataset(), exc);
				continue;
			}

			List<FilterNode<?>> filters = new ArrayList<>(table.getFilters().size());
			//add filter to children
			for(FilterValue filterValue : table.getFilters()) {
				FilterNode agg = filterValue.getFilter().createAggregator(filterValue.getValue());
				if(agg != null) {
					filters.add(agg);
				}
			}

			final List<AggregatorNode<?>> tableAggregators = createConceptAggregators(concept, table.getSelects());
			conceptAggregators.forEach(node -> plan.addAggregator(node.getAggregator()));

			final List<QPNode> aggregators = new ArrayList<>();
			//add aggregators

			aggregators.addAll(tableAggregators);
			aggregators.addAll(conceptAggregators);


			if(!excludeFromTimeAggregation && context.isGenerateSpecialDateUnion()) {
				aggregators.add(new SpecialDateUnionAggregatorNode(
					table.getResolvedConnector().getTable().getId(),
					plan.getSpecialDateUnion()
				));
			}

			tableNodes.add(
					new ConceptNode(
							concepts,
							calculateBitMask(concepts),
							table,
							new ValidityDateNode(
									selectValidityDateColumn(table),
									createConceptChild(concept, context, filters, aggregators)
							)
					)
			);
		}

		if(tableNodes.isEmpty()){
			throw new IllegalStateException(String.format("Unable to resolve any connector for query `%s`", label));
		}

		return OrNode.of(tableNodes);
	}

	private long calculateBitMask(ConceptElement<?>[] concepts) {
		long mask = 0;
		for(ConceptElement<?> concept : concepts) {
			mask |= concept.calculateBitMask();
		}
		return mask;
	}

	private ConceptElement[] resolveConcepts(List<ConceptElementId<?>> ids, CentralRegistry centralRegistry) {
		return
				ids
					.stream()
					.map(id -> centralRegistry.resolve(id.findConcept()).getElementById(id))
					.toArray(ConceptElement[]::new);
	}

	protected QPNode createConceptChild(Concept<?> concept, QueryPlanContext context, List<FilterNode<?>> filters, List<QPNode> aggregators) {
		QPNode result = AndNode.of(aggregators);
		if(!filters.isEmpty()) {
			result = new FiltersNode(filters, result);
		}
		return result;
	}

	private static List<AggregatorNode<?>> createConceptAggregators(Concept<?> concept, List<Select> select) {
		return select.stream()
					 .map(Select::createAggregator)
					 .map(AggregatorNode::new)
					 .collect(Collectors.toList());
	}

	private Column selectValidityDateColumn(CQTable t) {
		if(t.selectedValidityDate() != null) {
			return t
				.getResolvedConnector()
				.getValidityDateColumn(t.selectedValidityDate());
		}

		//else use this first defined validity date column
		else if(!t.getResolvedConnector().getValidityDates().isEmpty()) {
			return t.getResolvedConnector().getValidityDates().get(0).getColumn();
		}
		else {
			return null;
		}
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		selects.forEach(sel -> collector.add(new SelectResultInfo(sel,this)));
		for (CQTable table : tables) {
			table.getSelects().forEach(sel -> collector.add(new SelectResultInfo(sel,this)));
		}
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		namespacedIds.addAll(ids);
		selects.forEach(select -> namespacedIds.add(select.getId()));
		tables.forEach(table -> namespacedIds.add(table.getId()));
		
	}
	
	@Override
	public void visit(QueryVisitor visitor) {
		visitor.visitConcept(this);
	}
}

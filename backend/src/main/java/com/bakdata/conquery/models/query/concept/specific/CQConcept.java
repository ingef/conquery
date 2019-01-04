package com.bakdata.conquery.models.query.concept.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.specific.ValidityDateSelectionFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQSelectFilter;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.queryplan.specific.SpecialDateUnionAggregatorNode;
import com.bakdata.conquery.models.query.select.Select;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="CONCEPT", base=CQElement.class)
public class CQConcept implements CQElement {

	private String label;
	@Valid @NotEmpty
	private List<ConceptElementId<?>> ids;
	@Valid @NotEmpty @JsonManagedReference
	private List<CQTable> tables;
	@Valid @NotNull
	private List<Select> select = Collections.emptyList();

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		ConceptElement[] concepts = ids
			.stream()
			.map(id ->
				context.getCentralRegistry().resolve(id.findConcept()).getElementById(id)
			)
			.toArray(ConceptElement[]::new);
		
		List<AggregatorNode<?>> conceptAggregators = createConceptAggregators(plan, select);
		
		
		Concept<?> c = concepts[0].getConcept();
		
		List<QPNode> tableNodes = new ArrayList<>();
		for(CQTable t : tables) {
			t.setResolvedConnector(c.getConnectorByName(t.getId().getConnector()));
			
			List<FilterNode<?,?>> filters = new ArrayList<>(t.getFilters().size());
			//add filter to children
			for(FilterValue<?> f : t.getFilters()) {
				FilterNode<?,?> agg = ((Filter)f.getFilter()).createAggregator(f);
				if(agg != null) {
					filters.add(agg);
				}
			}
			
			List<QPNode> aggregators = new ArrayList<>();
			//add aggregators
			aggregators.addAll(conceptAggregators);
			aggregators.addAll(createConceptAggregators(plan, t.getSelect()));
			aggregators.add(new SpecialDateUnionAggregatorNode(
					t.getResolvedConnector().getTable().getId(),
					(SpecialDateUnion) plan.getAggregators().get(0)
			));
			
			tableNodes.add(
				new ConceptNode(
					concepts,
					t,
					selectValidityDateColumn(t),
					conceptChild(filters, aggregators)
				)
			);
		}
		
		return AndNode.of(tableNodes);
	}

	private QPNode conceptChild(List<FilterNode<?, ?>> filters, List<QPNode> aggregators) {
		QPNode result = AndNode.of(aggregators);
		if(!filters.isEmpty()) {
			result = new FiltersNode(filters, result);
		}
		return result;
	}

	private List<AggregatorNode<?>> createConceptAggregators(QueryPlan plan, List<Select> select) {
		if(select.isEmpty())
			return Collections.emptyList();
		
		List<AggregatorNode<?>> nodes = new ArrayList<>();
		for(Select s:select) {
			AggregatorNode<?> agg = s.createAggregator(plan.getAggregators().size());
			plan.getAggregators().add(agg.getAggregator());
			nodes.add(agg);
		}
		return nodes;
	}
	
	private Column selectValidityDateColumn(CQTable t) {
		//check if we have a manually selected validity date then use that
		for(FilterValue<?> fv : t.getFilters()) {
			if(fv instanceof CQSelectFilter && fv.getFilter() instanceof ValidityDateSelectionFilter) {
				return t
					.getResolvedConnector()
					.getValidityDateColumn(((CQSelectFilter)fv).getValue());
			}
		}
		
		//else use this first defined validity date column
		if(!t.getResolvedConnector().getValidityDates().isEmpty())
			return t.getResolvedConnector().getValidityDates().get(0).getColumn();
		else
			return null;
	}
}

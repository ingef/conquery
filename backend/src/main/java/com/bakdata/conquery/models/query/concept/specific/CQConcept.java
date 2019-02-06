package com.bakdata.conquery.models.query.concept.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.filters.specific.ValidityDateSelectionFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
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
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@CPSType(id = "CONCEPT", base = CQElement.class)
public class CQConcept implements CQElement {

	private String label;
	@Valid
	@NotEmpty
	private List<ConceptElementId<?>> ids;
	@Valid
	@NotEmpty
	@JsonManagedReference
	private List<CQTable> tables;
	@Valid
	@NotNull
	private List<SelectId> select = Collections.emptyList();

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		ConceptElement[] concepts = resolveConcepts(ids, context.getCentralRegistry());
		Select[] selects = resolveSelects(select, context.getCentralRegistry());

		Map<SelectId, AggregatorNode<?>> queryAggregators = createConceptAggregators(plan, selects);


		//TODO How do I properly setup this aggregators' Id?
		SelectId included = new SelectId(null, "dates");
		plan.addAggregator(new SpecialDateUnion(included));

		Concept<?> c = concepts[0].getConcept();

		List<QPNode> tableNodes = new ArrayList<>();

		for (CQTable table : tables) {

			table.setResolvedConnector(c.getConnectorByName(table.getId().getConnector()));

			Select[] resolvedSelects =
					table.getSelect()
						 .stream()
						 .map(name -> context.getCentralRegistry().resolve(name.getConnector()).getSelect(name))
						 .toArray(Select[]::new);

			table.setResolvedSelects(resolvedSelects);

			List<QPNode> aggregators = new ArrayList<>();
			//add aggregators
			aggregators.add(new SpecialDateUnionAggregatorNode(
					table.getResolvedConnector().getTable().getId(),
					(SpecialDateUnion) plan.getAggregators().get(included)
			));

			aggregators.addAll(queryAggregators.values());

			Map<SelectId, AggregatorNode<?>> tableAggregators = createConceptAggregators(plan, table.getResolvedSelects());

			aggregators.addAll(tableAggregators.values());

			List<FilterNode<?, ?>> filters = new ArrayList<>(table.getFilters().size());
			//add filter to children
			for (FilterValue filterValue : table.getFilters()) {

				FilterNode agg = filterValue.getFilter().createFilter(filterValue, tableAggregators.get(filterValue.getFilter().getSelectId()).getAggregator());
				if (agg != null) {
					filters.add(agg);
				}
			}


			tableNodes.add(
					new ConceptNode(
							concepts,
							table,
							selectValidityDateColumn(table),
							conceptChild(filters, aggregators)
					)
			);
		}

		return AndNode.of(tableNodes);
	}

	private Select[] resolveSelects(List<SelectId> select, CentralRegistry centralRegistry) {
		return select.stream()
					 .map(name -> centralRegistry.resolve(name.getConnector()).getSelect(name))
					 .toArray(Select[]::new);
	}

	private ConceptElement[] resolveConcepts(List<ConceptElementId<?>> ids, CentralRegistry centralRegistry) {
		return
				ids.stream()
				   .map(id -> centralRegistry.resolve(id.findConcept()).getElementById(id))
				   .toArray(ConceptElement[]::new);
	}

	private QPNode conceptChild(List<FilterNode<?, ?>> filters, List<QPNode> aggregators) {
		QPNode result = AndNode.of(aggregators);
		if (!filters.isEmpty()) {
			result = new FiltersNode(filters, result);
		}
		return result;
	}

	private Map<SelectId, AggregatorNode<?>> createConceptAggregators(QueryPlan plan, Select[] selects) {

		Map<SelectId, AggregatorNode<?>> nodes = new HashMap<>(selects.length);

		for (Select select : selects) {
			AggregatorNode<?> agg = select.createAggregatorNode(plan.getAggregators().size());

			plan.addAggregator(agg.getAggregator());

			nodes.put(select.getId(), agg);
		}
		return nodes;
	}

	private Column selectValidityDateColumn(CQTable t) {
		//check if we have a manually selected validity date then use that
		for (FilterValue<?> fv : t.getFilters()) {
			if (fv instanceof CQSelectFilter && fv.getFilter() instanceof ValidityDateSelectionFilter) {
				return t
							   .getResolvedConnector()
							   .getValidityDateColumn(((CQSelectFilter) fv).getValue());
			}
		}

		//else use this first defined validity date column
		if (!t.getResolvedConnector().getValidityDates().isEmpty())
			return t.getResolvedConnector().getValidityDates().get(0).getColumn();
		else
			return null;
	}
}

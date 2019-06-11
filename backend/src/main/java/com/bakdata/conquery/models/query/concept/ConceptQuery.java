package com.bakdata.conquery.models.query.concept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "CONCEPT_QUERY", base = IQuery.class)
public class ConceptQuery implements IQuery {

	@Valid
	@NotNull
	protected CQElement root;

	@Override
	public QueryPlan createQueryPlan(QueryPlanContext context) {
		ConceptQueryPlan qp = ConceptQueryPlan.create();
		qp.setChild(root.createQueryPlan(context, qp));
		return qp;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public ConceptQuery resolve(QueryResolveContext context) {
		this.root = root.resolve(context);
		return this;
	}

	public List<SelectDescriptor> collectSelects() {
		return root.collectSelects();
	}

	@Override
	public List<ResultInfo> collectResultInfos(PrintSettings config) {
		List<ResultInfo> header = new ArrayList<>();
		header.add(ConqueryConstants.DATES_INFO);
		return collectResultInfos(this.collectSelects(), header, config);
	}
	
	
	public static List<ResultInfo> collectResultInfos(List<SelectDescriptor> selects, List<ResultInfo> header, PrintSettings config) {
		HashMap<String, Integer> ocurrences = new HashMap<>();
		/*
		 * Column name is constructed from the most specific concept id the CQConcept
		 * has and the selector.
		 */
		for (SelectDescriptor selectDescriptor : selects) {
			Select select = selectDescriptor.getSelect();
			String columnName = config.getNameExtractor().apply(selectDescriptor);
			Integer occurence = ocurrences.computeIfAbsent(columnName, str -> Integer.valueOf(0));

			header.add(new SelectResultInfo(columnName, select.getResultType(), occurence, occurence.intValue(), select));
		}
		return header;
	}
}
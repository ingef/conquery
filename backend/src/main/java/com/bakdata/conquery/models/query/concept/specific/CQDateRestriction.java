package com.bakdata.conquery.models.query.concept.specific;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "DATE_RESTRICTION", base = CQElement.class)
@Setter
@Getter
public class CQDateRestriction implements CQElement {
	@NotNull
	private Range<LocalDate> dateRange;
	@Valid
	@NotNull
	private CQElement child;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		QPNode childAgg = child.createQueryPlan(context, plan);

		//insert behind every ConceptAggregator
		List<QPNode> openList = new ArrayList<>();

		openList.add(childAgg);

		int i = 0;

		while (i < openList.size()) {
			QPNode current = openList.get(i);
			if (current instanceof ConceptNode) {
				ConceptNode ca = (ConceptNode) current;
				ca.setChild(new DateRestrictingNode(CDateRange.of(dateRange), ca.getChild()));
			}
			else {
				openList.addAll(current.getChildren());
			}
			i++;
		}

		return childAgg;
	}
}

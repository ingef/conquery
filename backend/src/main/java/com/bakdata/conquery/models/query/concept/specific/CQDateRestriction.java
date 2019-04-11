package com.bakdata.conquery.models.query.concept.specific;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.SelectDescriptor;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;

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

		//insert behind every ValidityDateNode
		List<QPNode> openList = new ArrayList<>();

		openList.add(childAgg);

		int i = 0;

		while (i < openList.size()) {
			QPNode current = openList.get(i);
			if (current instanceof ValidityDateNode) {
				ValidityDateNode validityDateNode = (ValidityDateNode) current;
				validityDateNode.setChild(new DateRestrictingNode(
					CDateSet.create(Collections.singleton(CDateRange.of(dateRange))),
					validityDateNode.getChild()
				));
			}
			else {
				openList.addAll(current.getChildren());
			}
			i++;
		}

		return childAgg;
	}

	@Override
	public CQElement resolve(QueryResolveContext context) {
		child = child.resolve(context);
		return this;
	}
	
	@Override
	public void collectSelects(Deque<SelectDescriptor> select) {
		child.collectSelects(select);
	}
}

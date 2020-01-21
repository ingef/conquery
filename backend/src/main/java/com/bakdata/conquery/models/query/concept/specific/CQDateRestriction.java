package com.bakdata.conquery.models.query.concept.specific;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;
import com.bakdata.conquery.models.query.queryplan.specific.NegatingNode;
import com.bakdata.conquery.models.query.queryplan.specific.ValidityDateNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
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
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		QPNode childAgg = child.createQueryPlan(context.withDateRestriction(CDateRange.of(dateRange)), plan);

		//insert behind every ValidityDateNode
		Queue<QPNode> openList = new ArrayDeque<>();

		openList.add(childAgg);

		while (!openList.isEmpty()) {
			QPNode current = openList.poll();
			if (current instanceof ValidityDateNode) {
				ValidityDateNode validityDateNode = (ValidityDateNode) current;

				validityDateNode.setChild(new DateRestrictingNode(
					CDateSet.create(Collections.singleton(CDateRange.of(dateRange))),
					validityDateNode.getChild()
				));
			}
			else if(current instanceof NegatingNode) {
				//we can't push date restrictions past negations
			}
			else {
				openList.addAll(current.getChildren());
			}
		}

		return childAgg;
	}

	@Override
	public CQElement resolve(QueryResolveContext context) {
		child = child.resolve(context);
		return this;
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		child.collectResultInfos(collector);
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		child.collectNamespacedIds(namespacedIds);
	}
	
	@Override
	public void visit(QueryVisitor visitor) {
		CQElement.super.visit(visitor);
		child.visit(visitor);
	}
}

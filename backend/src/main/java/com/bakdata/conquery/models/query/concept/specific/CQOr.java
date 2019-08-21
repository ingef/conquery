package com.bakdata.conquery.models.query.concept.specific;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.SelectDescriptor;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;

import lombok.Getter;
import lombok.Setter;

@CPSType(id="OR", base=CQElement.class)
public class CQOr implements CQElement {
	@Getter @Setter @NotEmpty @Valid
	private List<CQElement> children;
	
	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		QPNode[] aggs = new QPNode[children.size()];
		for(int i=0;i<aggs.length;i++) {
			aggs[i] = children.get(i).createQueryPlan(context, plan);
		}
		return new OrNode(Arrays.asList(aggs));
	}
	
	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		for(CQElement c:children) {
			c.collectRequiredQueries(requiredQueries);
		}
	}

	@Override
	public CQElement resolve(QueryResolveContext context) {
		children.replaceAll(c->c.resolve(context));
		return this;
	}
	
	@Override
	public void collectSelects(Deque<SelectDescriptor> select) {
		for(CQElement c:children) {
			c.collectSelects(select);
		}
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		for(CQElement c:children) {
			c.collectNamespacedIds(namespacedIds);
		}
	}
	
	@Override
	public void visit(QueryVisitor visitor) {
		for(CQElement c:children) {
			c.visit(visitor);
		}
	}
}

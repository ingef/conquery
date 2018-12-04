package com.bakdata.conquery.models.query.concept.specific;

import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;

import lombok.Getter;
import lombok.Setter;

@CPSType(id="AND", base=CQElement.class)
public class CQAnd implements CQElement {
	@Getter @Setter @NotEmpty @Valid
	private List<CQElement> children;

	@Override
	public QPNode createQueryPlan(CentralRegistry registry, QueryPlan plan) {
		QPNode[] aggs = new QPNode[children.size()];
		for(int i=0;i<aggs.length;i++) {
			aggs[i] = children.get(i).createQueryPlan(registry, plan);
		}
		return new AndNode(Arrays.asList(aggs));
	}
}

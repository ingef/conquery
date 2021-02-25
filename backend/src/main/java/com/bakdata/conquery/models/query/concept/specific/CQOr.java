package com.bakdata.conquery.models.query.concept.specific;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.util.QueryUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@CPSType(id = "OR", base = CQElement.class)
public class CQOr extends CQElement implements ForcedExists {
	@Getter
	@Setter
	@NotEmpty
	@Valid
	private List<CQElement> children;

	@Getter
	@Setter
	private boolean createExists = false;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		QPNode[] nodes = new QPNode[children.size()];

		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = children.get(i).createQueryPlan(context, plan);
		}

		final QPNode or = OrNode.of(Arrays.asList(nodes));

		if (createExists) {
			final ExistsAggregator existsAggregator = new ExistsAggregator(or.collectRequiredTables());
			existsAggregator.setReference(or);
			plan.addAggregator(existsAggregator);
		}

		return or;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		for (CQElement c : children) {
			c.collectRequiredQueries(requiredQueries);
		}
	}

	@Override
	public void resolve(QueryResolveContext context) {
		children.forEach(c -> c.resolve(context));
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector, PrintSettings cfg) {
		for (CQElement c : children) {
			c.collectResultInfos(collector, cfg);
		}

		if (createExists) {
			collector.add(new SimpleResultInfo(getLabel(cfg), ResultType.BooleanT.INSTANCE));
		}
	}

	@Override
	public String getLabel(PrintSettings cfg) {
		String label = super.getLabel(cfg);
		if (label != null) {
			return label;
		}

		return QueryUtils.createDefaultMultiLabel(children, " " + cfg.getC10N(CQElementC10n.class).or() + " ", cfg);
	}



	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		for (CQElement c : children) {
			c.visit(visitor);
		}
	}
}

package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A wrapper for {@link CQElement}s to provide additional infos to parts of a query.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@CPSType(id = "RESULT_INFO_DECORATOR", base = CQElement.class)
public class ResultInfoDecorator extends CQElement {

	@NotNull
	private ClassToInstanceMap<Object> values = MutableClassToInstanceMap.create();
	@NotNull
	private CQElement child;

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		int index = collector.getInfos().size();
		child.collectResultInfos(collector);
		collector.getInfos()
				 .listIterator(index)
				 .forEachRemaining(sd -> {
					 for (Class entry : values.keySet()) {
						 sd.addAppendix(entry, values.getInstance(entry));
					 }
				 });
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		return child.createQueryPlan(context, plan);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		child.visit(visitor);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
		child.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		child.resolve(context);
	}
}

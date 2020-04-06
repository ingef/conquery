package com.bakdata.conquery.models.query.concept;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.LongFormAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;
import com.bakdata.conquery.models.query.queryplan.specific.TableRequiringAggregatorNode;
import com.google.common.collect.Lists;

public enum ConceptQueryResultFormat {
	WIDE {
		@Override
		public QPNode createAggregators(QueryPlanContext context, ConceptQueryPlan plan, CQConcept concept, CQTable table) {
			List<AggregatorNode<?>> aggregators = new ArrayList<>();
			aggregators.addAll(Lists.transform(concept.getSelects(), s->new AggregatorNode<>(s.createAggregator())));
			aggregators.addAll(Lists.transform(table.getSelects(), s->new AggregatorNode<>(s.createAggregator())));
			
			for(AggregatorNode<?> aggNode : aggregators) {
				plan.addAggregator(aggNode.getAggregator());
			}

			if(!concept.isExcludeFromTimeAggregation() && context.isGenerateSpecialDateUnion()) {
				aggregators.add(new TableRequiringAggregatorNode<>(
					table.getResolvedConnector().getTable().getId(),
					plan.getSpecialDateUnion()
				));
			}
			
			return AndNode.of(aggregators);
		}
	},
	LONG {
		@Override
		public QPNode createAggregators(QueryPlanContext context, ConceptQueryPlan plan, CQConcept concept, CQTable table) {
			LongFormAggregator n = new LongFormAggregator(table.getResolvedConnector().getTable());
			plan.addAggregator(n);
			return new AggregatorNode<>(n);
		}
	};

	public abstract QPNode createAggregators(QueryPlanContext context, ConceptQueryPlan plan, CQConcept concept, CQTable table);
}

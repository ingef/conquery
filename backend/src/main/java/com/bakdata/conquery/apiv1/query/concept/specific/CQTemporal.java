package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.validation.Valid;

import c10n.C10N;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalRelationMode;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSelector;
import com.bakdata.conquery.models.query.resultinfo.FixedLabelResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;
import lombok.Setter;

/**
 * Temporal queries allow users to query for temporal relationships:
 *
 * <pre>
 * {
 * 	"type" : "TEMPORAL",
 * 	"reference" : {}, // Concept A
 * 	"compare" : {}, // Concept B
 * 	"mode" : {"type": "BEFORE", "days" : {"min" : 10}},
 * 	"referenceSelector" : "EARLIEST",
 * 	"compareSelector" : "EARLIEST",
 * }
 * </pre>
 * => The earliest sub-period of Concept A is included, if the earliest sub-period of B is at least 10 days before it.
 * <p>
 * <hr />
 *
 * <pre>
 * {
 * 	"type" : "TEMPORAL",
 * 	"reference" : {}, // Concept A
 * 	"compare" : {}, // Concept B
 * 	"mode" : {"type": "BEFORE", "days" : {"min" : 10}},
 * 	"referenceSelector" : "ANY",
 * 	"compareSelector" : "EARLIEST",
 * }
 * </pre>
 * => All sub-periods of Concept A are included, if the earliest sub-period of Concept B is 10 days before it.
 * <p>
 * <hr />
 */
@Data
@CPSType(id = "TEMPORAL", base = CQElement.class)
public class CQTemporal extends CQElement {

	@Valid
	private final CQElement index;
	@Valid
	private final CQElement compare;

	@Valid
	private final TemporalRelationMode mode;
	private final TemporalSelector indexSelector;
	private final TemporalSelector compareSelector;


	@Setter
	private boolean showCompareDate;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final ConceptQueryPlan indexSubPlan = createIndexPlan(getIndexQuery(), context, plan);

		// These aggregators will be fed with the actual aggregation results of the sub results
		final List<ConstantValueAggregator<List>> shimAggregators = createShimAggregators();

		TemporalQueryNode queryNode = new TemporalQueryNode(context.getStorage().getDataset().getAllIdsTable(), getIndexSelector(),
															getMode(),
															getCompareSelector(),
															getCompareQuery(),
															context,
															indexSubPlan,
															shimAggregators.stream().map(ConstantValueAggregator::getValue).collect(Collectors.toList())
		);

		if (showCompareDate) {
			plan.registerAggregator(queryNode.getCompareDateAggregator());
		}

		shimAggregators.forEach(plan::registerAggregator);
		return queryNode;
	}


	private ConceptQueryPlan createIndexPlan(CQElement query, QueryPlanContext context, ConceptQueryPlan plan) {
		final ConceptQueryPlan subPlan = new ConceptQueryPlan(true);
		final QPNode indexNode = query.createQueryPlan(context, plan);

		subPlan.getDateAggregator().registerAll(indexNode.getDateAggregators());
		subPlan.setChild(indexNode);
		return subPlan;
	}

	/**
	 * Makes it such that Before is just a special case of After, reducing code and testing requirements.
	 */
	private CQElement getIndexQuery() {
		return switch (mode) {
			case TemporalRelationMode.Before ignored -> compare;

			case TemporalRelationMode.After ignored -> index;
			case TemporalRelationMode.While ignored -> index;
		};
	}

	private List<ConstantValueAggregator<List>> createShimAggregators() {
		return compare.getResultInfos().stream().map(info -> new ConstantValueAggregator<List>(new ArrayList<>())).toList();
	}

	/**
	 * Makes it such that Before is just a special case of After, reducing code and testing requirements.
	 */
	private CQElement getCompareQuery() {
		return switch (mode) {
			case TemporalRelationMode.Before ignored -> index;

			case TemporalRelationMode.After ignored -> compare;
			case TemporalRelationMode.While ignored -> compare;
		};
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		index.collectRequiredQueries(requiredQueries);
		compare.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		index.visit(visitor);
		compare.visit(visitor);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		index.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
		compare.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();
		resultInfos.addAll(index.getResultInfos());

		if (showCompareDate) {
			resultInfos.add(new FixedLabelResultInfo(new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE), Collections.emptySet()) {
				@Override
				public String userColumnName(PrintSettings printSettings) {
					return C10N.get(ResultHeadersC10n.class, printSettings.getLocale()).temporalCompareLabel(index.userLabel(printSettings.getLocale()));
				}
			});
		}

		resultInfos.addAll(compare.getResultInfos());
		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getIndex().collectRequiredEntities(context);
	}


}

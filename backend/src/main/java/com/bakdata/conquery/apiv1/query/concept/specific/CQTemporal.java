package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.validation.Valid;

import c10n.C10N;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
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
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
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
 *
 * @implNote Index/Compare may be swapped by mode, if the specific element is necessary, always use getIndexQuery/getCompareQuery.
 */
@Data
@CPSType(id = "TEMPORAL", base = CQElement.class)
public class CQTemporal extends CQElement {

	/**
	 * subQuery to select the temporal relations "index-date", which is used as source for date-restrictions of compare.
	 * < br/>
	 * Aggregators are treated as normal.
	 */
	@Valid
	private final CQElement index;

	/**
	 * Subquery, whose date-restriction is set in relation to sub-periods of index. Conforming to the definitions of mode and selectors.
	 * < br/>
	 * Aggregators are returned as array of results per contained sub-period. (This is a bit hard to read, but allows users to access them)
	 */
	@Valid
	private final CQElement compare;

	/**
	 * Defines the relation between index and compare:
	 * - compare 5 to 10 days BEFORE compare
	 * - compare 5 to 10 days AFTER compare
	 * - compare WHILE index
	 * <br />
	 * Every continuous period in index' result is treated as its own period: {2020-01-01/2020-01-05, 2020-01-10/2020-01-15} is evaluated as the period 2020-01-01/2020-01-05 and 2020-01-10/2020-01-15 separately.
	 *
	 */
	@Valid
	private final TemporalRelationMode mode;
	private final TemporalSelector indexSelector;
	private final TemporalSelector compareSelector;

	/**
	 * If true, also output the inner compareDates that are satisfied.
	 * <br />
	 * @implNote Currently frontend doesn't access this.
	 */
	@Setter
	private boolean showCompareDate;


	@View.Internal
	private int nShimAggregators = -1;


	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		// These aggregators will be fed with the actual aggregation results of the sub results
		final List<ConstantValueAggregator<List>> shimAggregators = createShimAggregators(nShimAggregators);

		ConceptQueryPlan indexPlan = createIndexPlan(context);
		TemporalQueryNode queryNode = new TemporalQueryNode(context.getStorage().getDataset().getAllIdsTable(),
															indexPlan,
															getIndexSelector(),
															getMode(),
															createCompareQueryPlan(context, true),
															createCompareQueryPlan(context, false),
															getCompareSelector(),
															shimAggregators
		);

		indexPlan.getAggregators().stream().skip(1).forEach(plan::registerAggregator);

		if (showCompareDate) {
			plan.registerAggregator(queryNode.getCompareDateAggregator());
		}

		shimAggregators.forEach(plan::registerAggregator);
		return queryNode;
	}

	private static List<ConstantValueAggregator<List>> createShimAggregators(int nShimAggregators) {
		return IntStream.range(0, nShimAggregators)
						.mapToObj(ignored -> new ConstantValueAggregator<List>(new ArrayList<>()))
						.toList();
	}

	private ConceptQueryPlan createIndexPlan(QueryPlanContext context) {
		return new ConceptQuery(index).createQueryPlan(context);
	}

	private ConceptQueryPlan createCompareQueryPlan(QueryPlanContext context, boolean withAggregation) {
		return new ConceptQuery(compare).createQueryPlan(context.withDisableAggregationFilters(withAggregation).withDisableAggregationFilters(withAggregation));
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

		// Avoids getResultInfos in createQueryPlan
		nShimAggregators = compare.getResultInfos().size();
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();
		resultInfos.addAll(index.getResultInfos());

		if (showCompareDate) {
			resultInfos.add(new FixedLabelResultInfo(new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE), Collections.emptySet()) {
				@Override
				public String userColumnName(PrintSettings printSettings) {
					return C10N.get(ResultHeadersC10n.class, printSettings.getLocale()).temporalCompareLabel(compare.userLabel(printSettings.getLocale()));
				}
			});
		}

		for (ResultInfo resultInfo : compare.getResultInfos()) {
			// Wrap resultInfo in ListT
			ResultInfo listWrapper = new ResultInfo(resultInfo.getSemantics()) {

				@Override
				public String userColumnName(PrintSettings printSettings) {
					return resultInfo.userColumnName(printSettings);
				}

				@Override
				public String defaultColumnName(PrintSettings printSettings) {
					return resultInfo.defaultColumnName(printSettings);
				}

				@Override
				public ResultType getType() {
					return new ResultType.ListT<>(resultInfo.getType());
				}

				@Override
				public String getDescription() {
					return resultInfo.getDescription();
				}

				@Override
				public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
					return printerFactory.getListPrinter(resultInfo.createPrinter(printerFactory, printSettings), printSettings);
				}
			};

			resultInfos.add(listWrapper);
		}

		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return index.collectRequiredEntities(context)
					.intersect(compare.collectRequiredEntities(context));
	}
}

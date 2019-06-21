package com.bakdata.eva.forms.queries;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.bakdata.eva.models.forms.DateContext;
import com.bakdata.eva.query.aggregators.PeriodAverageAggregator;
import com.bakdata.eva.query.aggregators.PeriodSumAggregator;
import com.bakdata.eva.query.aggregators.ValueAtBeginOfQuarterAggregator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;

public class FormQueryPlan implements QueryPlan {

	private final ConceptQueryPlan plan;
	private final Int2ObjectMap<List<DateContext>> includedEntities;
	private List<DateContext> contained;
	private List<ConceptQueryPlan> containedChildren;
	private List<TableId> requiredTables;
	private Entity entity;
	@Getter
	private int aggregatorOffset = 0;
	private int addedAggregators = 0;


	public FormQueryPlan(ConceptQueryPlan plan, Int2ObjectMap<List<DateContext>> includedEntities, List<TableId> requiredTables, int aggregatorOffset) {
		this.plan = plan;
		this.includedEntities = includedEntities;
		this.requiredTables = requiredTables;
		this.aggregatorOffset = aggregatorOffset;
	}

	@Override
	public void init(Entity entity) {
		this.entity = entity;
		contained = includedEntities.get(entity.getId());
		if(contained != null) {
			containedChildren = new ArrayList<>(contained.size());
			for(int i=0; i<contained.size(); i++) {
				addedAggregators = 0;

				CloneContext newCtx = new CloneContext();

				// If this is a subquery of an export form, change period aggregators to other FirstValue, as they do not make sense there.
				if (contained.get(i).getIndex() != null) {
					for (Aggregator<?> aggregator : plan.getAggregators()) {
						if (aggregator instanceof PeriodSumAggregator) {
							newCtx.inject(aggregator, new ValueAtBeginOfQuarterAggregator<>(((PeriodSumAggregator) aggregator).getColumn()));
						}else if (aggregator instanceof PeriodAverageAggregator) {
							newCtx.inject(aggregator, new ValueAtBeginOfQuarterAggregator<>(((PeriodAverageAggregator) aggregator).getColumn()));
						}
					}
				}

				ConceptQueryPlan containedChild = plan.clone(newCtx);

				//add index value
				containedChild.addAggregator(
					aggregatorOffset,
					new ConstantValueAggregator(
						contained.get(i).getIndex(),
						ResultType.INTEGER
					)
				);
				addedAggregators++;
				// add event date
				LocalDate eventDate = contained.get(i).getEventDate();
				if(eventDate != null) {
					containedChild.addAggregator(
						aggregatorOffset+addedAggregators,
						new ConstantValueAggregator(
							eventDate,
							ResultType.DATE
							)
						);
					addedAggregators++;
				}
				//add date range
				containedChild.addAggregator(
					aggregatorOffset+addedAggregators,
					new ConstantValueAggregator(
						contained.get(i).getDateRange().toString(),
						ResultType.STRING
					)
				);
				addedAggregators++;

				containedChild.init(entity);
				containedChildren.add(containedChild);
			}
		}
		else {
			containedChildren = Collections.emptyList();
		}
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		for(int i=0; i< containedChildren.size(); i++) {
			CDateSet newSet = CDateSet.create(ctx.getDateRestriction());
			newSet.retainAll(contained.get(i).getDateRange());
			containedChildren.get(i).nextTable(
				ctx.withDateRestriction(newSet),
				currentTable
			);
		}
	}

	@Override
	public void nextBlock(Bucket bucket) {
		for(QueryPlan qp : containedChildren) {
			qp.nextBlock(bucket);
		}
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		boolean interesting = false;
		for(QueryPlan qp : containedChildren) {
			interesting |= qp.isOfInterest(bucket);
		}
		return interesting;
	}

	@Override
	public EntityResult createResult() {
		if(contained == null) {
			return EntityResult.notContained();
		}
		else {
			List<Object[]> values = new ArrayList<>(containedChildren.size());
			for(ConceptQueryPlan child : containedChildren) {
				EntityResult res = child.createResult();
				if(res instanceof SinglelineContainedEntityResult) {
					values.add(((SinglelineContainedEntityResult) res).getValues());
				}
				else if(res instanceof MultilineContainedEntityResult) {
					values.addAll(((MultilineContainedEntityResult) res).getValues());
				}
				else {
					Object[] result = new Object[child.getAggregatorSize()];
					for(int i=0;i<result.length;i++)
						result[i] = child.getAggregators().get(i).getAggregationResult();
					values.add(result);
				}
			}
			return EntityResult.multilineOf(entity.getId(), values);
		}
	}

	@Override
	public FormQueryPlan clone(CloneContext ctx) {
		return new FormQueryPlan(plan.clone(ctx), includedEntities, requiredTables, aggregatorOffset);
	}

	@Override
	public int getAggregatorSize() {
		return plan.getAggregatorSize() + addedAggregators;
	}

	@Override
	public void addAggregator(Aggregator<?> aggregator) {
		plan.addAggregator(aggregator);
	}

	@Override
	public void addAggregator(int index, Aggregator<?> aggregator) {
		if(index <= aggregatorOffset) {
			aggregatorOffset++;
		}
		plan.addAggregator(index, aggregator);
	}

	@Override
	public SpecialDateUnion getSpecialDateUnion() {
		return plan.getSpecialDateUnion();
	}

	@Override
	public void nextEvent(Bucket bucket, int event) {
		for(QueryPlan qp : containedChildren) {
			qp.nextEvent(bucket, event);
		}
	}

	@Override
	public boolean isContained() {
		return contained != null;
	}
}

package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.ICDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

/**
 * QueryNode implementing the logic for TemporalQueries.
 * Executes two queries and compares the times they are included, the entity is included according to a specified {@link PrecedenceMatcher}.
 */
@Getter
public class TemporalQueryNode extends QPNode {

	/**
	 * Matcher to be used when testing for inclusion.
	 */
	private PrecedenceMatcher matcher;

	/**
	 * QueryPlan for the events to be compared to.
	 */
	private SampledNode reference;

	/**
	 * QueryPlan for the events being compared.
	 */
	private SampledNode preceding;

	/**
	 * The {@link SpecialDateUnion} to be fed with the included dataset.
	 */
	private SpecialDateUnion dateUnion;

	public TemporalQueryNode(SampledNode reference, SampledNode preceding, PrecedenceMatcher matcher, SpecialDateUnion dateUnion) {
		this.reference = reference;
		this.preceding = preceding;
		this.matcher = matcher;
		this.dateUnion = dateUnion;
	}

	@Override
	public QPNode doClone(CloneContext ctx) {
		return new TemporalQueryNode(ctx.clone((SampledNode) reference), ctx.clone((SampledNode) preceding), matcher, ctx.clone(dateUnion));
	}

	/**
	 * Collects required tables of {@link #reference} and {@link #preceding} into {@code out}.
	 *
	 * @param out the set to be filled with data.
	 */
	@Override
	public void collectRequiredTables(Set<TableId> out) {
		out.addAll(getReference().getChild().collectRequiredTables());
		out.addAll(getPreceding().getChild().collectRequiredTables());
	}

	/**
	 * Initializes the {@link TemporalQueryNode} and its children.
	 *
	 * @param entity the Entity to be worked on.
	 */
	@Override
	public void init(Entity entity) {
		super.init(entity);

		reference.getChild().init(entity);
		preceding.getChild().init(entity);
	}

	/**
	 * Calls nextBlock on its children.
	 */
	@Override
	public void nextBlock(Bucket bucket) {
		reference.getChild().nextBlock(bucket);
		preceding.getChild().nextBlock(bucket);
	}

	/**
	 * Calls nextBlock on its children.documentation code for refactored matchers.
	 */
	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		reference.getChild().nextTable(ctx, currentTable);
		preceding.getChild().nextTable(ctx, currentTable);
	}

	/**
	 * Delegates aggregation to {@link #reference} and {@link #preceding}.
	 */
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		reference.getChild().nextEvent(bucket, event);
		preceding.getChild().nextEvent(bucket, event);
	}

	/**
	 * Retrieves the {@link ConceptQueryPlan#getSpecialDateUnion()} ()} time of {@link #reference} and {@link #preceding}.
	 * Then tests whether they match the specific criteria for inclusion.
	 * If the criteria are met, the matching {@link ICDateSet} is put into the @{@link SpecialDateUnion} node of the Queries associated QueryPlan.
	 *
	 * @return true, iff the Events match the specific criteria.
	 */
	@Override
	public final boolean isContained() {
		if (!reference.getChild().isContained()) {
			return false;
		}

		ICDateSet referenceDurations = getReference().getChild().getSpecialDateUnion().getResultSet();
		// Create copy as we are mutating the set
		ICDateSet precedingDurations = CDateSet.create(getPreceding().getChild().getSpecialDateUnion().getResultSet());


		OptionalInt sampledReference = getReference().getSampler().sample(referenceDurations);

		if (sampledReference.isEmpty()) {
			return false;
		}

		matcher.removePreceding(precedingDurations, sampledReference.getAsInt());

		OptionalInt sampledPreceding = getPreceding().getSampler().sample(precedingDurations);

		if (matcher.isContained(sampledReference, sampledPreceding)) {
			dateUnion.merge(referenceDurations);
			return true;
		}

		return false;
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return 
			reference.getChild().isOfInterest(bucket)
			| //call isOfInterest on both children because some nodes use it for initialization 
			preceding.getChild().isOfInterest(bucket)
		;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return 
			reference.getChild().isOfInterest(entity)
			| //call isOfInterest on both children because some nodes use it for initialization 
			preceding.getChild().isOfInterest(entity)
		;
	}
}

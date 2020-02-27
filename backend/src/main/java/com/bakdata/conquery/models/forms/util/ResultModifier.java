package com.bakdata.conquery.models.forms.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;

public class ResultModifier {

	/**
	 * Modifies the given result lines with the given function.
	 * 
	 * If the result is not contained this method creates a default result row anyways.
	 */
	public static List<Object[]> modify(EntityResult inResult, ConceptQueryPlan subPlan, UnaryOperator<Object[]> modification) {
		if(inResult.isFailed()) {
			throw new RuntimeException("failed result can't be modified: "+inResult);
		}
		if(!inResult.isContained()) {
			Object[] result = new Object[subPlan.getAggregatorSize()];
			int aggIdx = 0;
			for(Aggregator<?> agg : subPlan.getAggregators()) {
				// Fill with null, except for EXIST aggregators
				if(agg instanceof ExistsAggregator) {
					result[aggIdx] = false;
				}
				aggIdx++;
			}
			return Collections.singletonList(modification.apply(result));
		}
		if(inResult instanceof SinglelineContainedEntityResult) {
			return Collections.singletonList(
				modification.apply(
					((SinglelineContainedEntityResult) inResult).getValues()
				)
			);
		}
		else if(inResult instanceof MultilineContainedEntityResult) {
			var values = ((MultilineContainedEntityResult) inResult).getValues();
			List<Object[]> result = new ArrayList<>(values);
			result.replaceAll(modification);
			return result;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Modifies the given result lines with the given function.
	 * 
	 * If the result is not contained this method creates a default result row anyways.
	 */
	// TODO We can maybe refactor this by using the method above. But this might require a new result type (ArrayContainedEntityResult?)
	public static List<Object[]> modify(EntityResult inResult, ArrayConceptQueryPlan arrayPlan, UnaryOperator<Object[]> modification) {
		if(inResult.isFailed()) {
			throw new RuntimeException("failed result can't be modified: "+inResult);
		}
		if(!inResult.isContained()) {
			Object[] result = new Object[arrayPlan.getAggregatorSize()];
			int aggIdx = 0;
			for(ConceptQueryPlan subPlan : arrayPlan.getChildPlans()) {
				for(Aggregator<?> agg : subPlan.getAggregators()) {
					// Fill with null, except for EXIST aggregators
					if(agg instanceof ExistsAggregator) {
						result[aggIdx] = false;
					}
					aggIdx++;
				}
			}
			return Collections.singletonList(modification.apply(result));
		}
		if(inResult instanceof SinglelineContainedEntityResult) {
			Object[] result = ((SinglelineContainedEntityResult) inResult).getValues();
			int aggIdx = 0;
			/* Special handling here aswell because a subquery might not be contained but has an EXIST select.
			 * This would cause null (empty cell), so we fetch all EXIST result and put them to the end result.
			 */
			for(ConceptQueryPlan subPlan : arrayPlan.getChildPlans()) {
				for(Aggregator<?> agg : subPlan.getAggregators()) {
					// Fill EXIST aggregators with false which evaluated to 'null'
					if(agg instanceof ExistsAggregator && Objects.isNull(result[aggIdx])) {
						result[aggIdx] = false;
					}
					aggIdx++;
				}
			}
			
			return Collections.singletonList(
				modification.apply(
					result
				)
			);
		}
		else if(inResult instanceof MultilineContainedEntityResult) {
			 List<Object[]> values = ((MultilineContainedEntityResult) inResult).getValues();
			List<Object[]> result = new ArrayList<>(values);
			result.replaceAll(modification);
			return result;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Modifies the given result lines with the given function.
	 * 
	 * If the result is not contained this method returns an empty list
	 */
	public static List<Object[]> modify(EntityResult inResult, UnaryOperator<Object[]> modification) {
		if(inResult.isFailed()) {
			throw new RuntimeException("failed result can't be modified: "+inResult);
		}
		if(!inResult.isContained()) {
			return Collections.emptyList();
		}
		if(inResult instanceof SinglelineContainedEntityResult) {
			return Collections.singletonList(
				modification.apply(
					((SinglelineContainedEntityResult) inResult).getValues()
				)
			);
		}
		else if(inResult instanceof MultilineContainedEntityResult) {
			var values = ((MultilineContainedEntityResult) inResult).getValues();
			List<Object[]> result = new ArrayList<>(values);
			result.replaceAll(modification);
			return result;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

}

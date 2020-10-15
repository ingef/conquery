package com.bakdata.conquery.models.forms.util;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.util.functions.ChainableUnaryOperator;

public class ResultModifier {
	/**
	 * Modifies the given result lines with the given function.
	 * 
	 * If the result is not contained this method creates a default result row anyways.
	 */
	public static <T extends ContainedEntityResult> T modify(T inResult, UnaryOperator<Object[]> modification) {
		
		inResult.modifyResultLinesInplace(modification);
		
		return inResult;
	}
	
	public static ChainableUnaryOperator<Object[]> existAggValuesSetterFor(List<Aggregator<?>> aggregators, OptionalInt firstAggPos) {
		return (result) -> setExistAggValues(aggregators, result, firstAggPos);
	}
	
	private static Object[] setExistAggValues(List<Aggregator<?>> aggregators, Object[] result, OptionalInt firstAggPos){
		int aggIdx = firstAggPos.orElse(0);
		/* Special handling here, because a subquery might not be contained but has an EXIST select.
		 * This would cause null (empty cell), so we fetch all EXIST result and put them to the end result.
		 */
		for(Aggregator<?> agg : aggregators) {
			// Fill EXIST aggregators with false which evaluated to 'null'
			if(agg instanceof ExistsAggregator && Objects.isNull(result[aggIdx])) {
				result[aggIdx] = false;
			}
			aggIdx++;
		}
		return result;
	}
	
//	/**
//	 * Modifies the given result lines with the given function.
//	 * 
//	 * If the result is not contained this method returns an empty list
//	 */
//	public static List<Object[]> modify(EntityResult inResult, UnaryOperator<Object[]> modification) {
//		if(inResult.isFailed()) {
//			throw new RuntimeException("failed result can't be modified: "+inResult);
//		}
//		if(!inResult.isContained()) {
//			return Collections.emptyList();
//		}
//		
//		if(inResult instanceof SinglelineContainedEntityResult) {
//			return Collections.singletonList(
//				modification.apply(
//					((SinglelineContainedEntityResult) inResult).getValues()
//				)
//			);
//		}
//		else if(inResult instanceof MultilineContainedEntityResult) {
//			var values = ((MultilineContainedEntityResult) inResult).getValues();
//			List<Object[]> result = new ArrayList<>(values);
//			result.replaceAll(modification);
//			return result;
//		}
//		else {
//			throw new UnsupportedOperationException();
//		}
//	}

}

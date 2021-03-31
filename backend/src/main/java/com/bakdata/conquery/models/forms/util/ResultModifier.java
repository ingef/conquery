package com.bakdata.conquery.models.forms.util;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.util.functions.ChainableUnaryOperator;

public class ResultModifier {

	/**
	 * Modifies the given result lines with the given function.
	 * 
	 * If the result is not contained this method creates a default result row
	 * anyways.
	 */
	public static <T extends EntityResult> T modify(T inResult, UnaryOperator<Object[]> modification) {

		inResult.modifyResultLinesInplace(modification);

		return inResult;
	}

	public static ChainableUnaryOperator<Object[]> existAggValuesSetterFor(List<Aggregator<?>> aggregators, OptionalInt firstAggPos) {
		return (result) -> setExistAggValues(aggregators, result, firstAggPos);
	}

	private static Object[] setExistAggValues(List<Aggregator<?>> aggregators, Object[] result, OptionalInt firstAggPos) {
		int aggIdx = firstAggPos.orElse(0);
		/*
		 * Special handling here, because a subquery might not be contained but has an
		 * EXIST select. This would cause null (empty cell), so we fetch all EXIST
		 * result and put them to the end result.
		 */
		for (int i = 0; i < aggregators.size(); i++) {
			Aggregator<?> agg = aggregators.get(i);
			// Fill EXIST aggregators with false which evaluated to 'null'
			if (agg instanceof ExistsAggregator && Objects.isNull(result[i + aggIdx])) {
				result[i + aggIdx] = agg.getAggregationResult();
			}
		}
		return result;
	}
}

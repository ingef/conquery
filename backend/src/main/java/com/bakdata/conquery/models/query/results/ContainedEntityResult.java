package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public interface ContainedEntityResult extends EntityResult {

	int getEntityId();
	/**
	 * Provides the number of columns this result contains.
	 */
	int columnCount();
	Stream<Object[]> streamValues();
	
	static Stream<ContainedEntityResult> filterCast(EntityResult result) {
		if(result instanceof ContainedEntityResult) {
			return Stream.of(result.asContained());
		}
		return Stream.empty();
	}
	
	@Override
	default ContainedEntityResult asContained() {
		return this;
	}
	
	/**
	 * Returns a list of the computed result line for this entity on the query.
	 */
	List<Object[]> listResultLines();
	
	/**
	 * Allows to modify the underlying result directly. The return value of the line modifier is the new line.
	 * So the modifier can change the array without reallocation by return the reference it received. Alternative it can 
	 * allocate a new result line and return that reference instead. 
	 * @param lineModifier A modifier(-chain) for a result line.
	 */
	void modifyResultLinesInplace(UnaryOperator<Object[]> lineModifier);



	CDateSet collectValidityDates(QueryPlan plan);

	static CDateSet collectValidityDates(QueryPlan plan, Object[] resultLine) {
		CDateSet dateSet = CDateSet.create();
		for(int pos :plan.getValidityDateResultPositions()) {
			Object date = resultLine[pos];
			if(date == null) {
				continue;
			}
			else if(date instanceof CDateSet) {
				dateSet.addAll((CDateSet) date);
			}
			else if(date instanceof CDateRange) {
				dateSet.add((CDateRange) date);
			}
			else {
				throw new IllegalStateException("Encountered unhandled type during date aggregation: " + date.getClass() + "(" + date + ")");
			}
		}
		return dateSet;
	}
}

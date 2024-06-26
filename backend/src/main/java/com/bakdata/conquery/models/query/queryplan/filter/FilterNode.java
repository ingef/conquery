package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.query.queryplan.EventIterating;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public abstract sealed class FilterNode<FILTER_VALUE> extends EventIterating permits EventFilterNode, AggregationResultFilterNode {

	@Setter(AccessLevel.PROTECTED) @Getter
	protected FILTER_VALUE filterValue;


}

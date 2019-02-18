package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.select.Select;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
@CPSType(id = "CONSTANT_VALUE", base = Select.class)
public class ConstantValueSelect extends Select {
	@Getter @Setter
	private String value;
	
	@Override
	protected ConstantValueAggregator createAggregator() {
		return new ConstantValueAggregator(value);
	}
}

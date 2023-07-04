package com.bakdata.conquery.sql.conversion.filter;

import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class RealRangeConverter implements FilterConverter<FilterValue.CQRealRangeFilter> {

	@Override
	public Condition convert(FilterValue.CQRealRangeFilter filter, ConversionContext context) {
		Field<Object> field = DSL.field(FilterConverter.getColumnName(filter));

		Optional<Condition> greaterOrEqualCondition = Optional.ofNullable(filter.getValue().getMin()).map(field::greaterOrEqual);
		Optional<Condition> lessOrEqualCondition = Optional.ofNullable(filter.getValue().getMax()).map(field::lessOrEqual);
		return Stream.concat(greaterOrEqualCondition.stream(), lessOrEqualCondition.stream())
					 .reduce(Condition::and)
					 .orElseThrow(() -> new IllegalArgumentException("Missing min or max value for real range filter."));
	}

	@Override
	public Class<FilterValue.CQRealRangeFilter> getConversionClass() {
		return FilterValue.CQRealRangeFilter.class;
	}

}

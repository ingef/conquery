package com.bakdata.conquery.sql.conversion.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class FirstValueConverter implements SelectConverter<FirstValueSelect> {

	public Field<Object> convert(FirstValueSelect select, ConversionContext context) {
		SqlFunctionProvider fn = context.getSqlDialect().getFunction();
		return fn.first(DSL.name(select.getColumn().getName()));
	}

	@Override
	public Class<FirstValueSelect> getConversionClass() {
		return FirstValueSelect.class;
	}
}

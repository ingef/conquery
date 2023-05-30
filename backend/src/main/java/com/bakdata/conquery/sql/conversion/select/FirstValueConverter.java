package com.bakdata.conquery.sql.conversion.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import org.jooq.Field;

public class FirstValueConverter implements SelectConverter<FirstValueSelect> {

	public Field<Object> convert(FirstValueSelect select, ConversionContext context) {
		SqlFunctionProvider fn = context.getSqlDialect().getFunction();
		return fn.first(select.getColumnName());
	}

	@Override
	public Class<FirstValueSelect> getConversionClass() {
		return FirstValueSelect.class;
	}
}
